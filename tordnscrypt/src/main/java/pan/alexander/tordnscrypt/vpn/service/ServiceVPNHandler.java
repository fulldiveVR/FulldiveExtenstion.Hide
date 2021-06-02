package pan.alexander.tordnscrypt.vpn.service;
/*
    This file is part of Fulldive VPN.

    Fulldive VPN is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Fulldive VPN is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Fulldive VPN.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2019-2021 by Garmatin Oleksandr invizible.soft@gmail.com
*/

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pan.alexander.tordnscrypt.R;
import pan.alexander.tordnscrypt.arp.ArpScanner;
import pan.alexander.tordnscrypt.iptables.ModulesIptablesRules;
import pan.alexander.tordnscrypt.modules.ModulesAux;
import pan.alexander.tordnscrypt.modules.ModulesStatus;
import pan.alexander.tordnscrypt.settings.firewall.FirewallFragmentKt;
import pan.alexander.tordnscrypt.utils.PrefManager;
import pan.alexander.tordnscrypt.utils.enums.ModuleState;
import pan.alexander.tordnscrypt.utils.enums.VPNCommand;
import pan.alexander.tordnscrypt.vpn.Rule;
import pan.alexander.tordnscrypt.vpn.Util;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static pan.alexander.tordnscrypt.modules.ModulesService.DEFAULT_NOTIFICATION_ID;
import static pan.alexander.tordnscrypt.utils.RootExecService.LOG_TAG;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.STOPPED;
import static pan.alexander.tordnscrypt.utils.enums.OperationMode.ROOT_MODE;
import static pan.alexander.tordnscrypt.vpn.service.ServiceVPN.EXTRA_COMMAND;
import static pan.alexander.tordnscrypt.vpn.service.ServiceVPN.EXTRA_REASON;


public class ServiceVPNHandler extends Handler {
    private static ServiceVPNHandler serviceVPNHandler;
    private static List<Rule> listRule;
    private final ServiceVPN serviceVPN;
    private ServiceVPN.Builder last_builder = null;
    private ArpScanner arpScanner;

    private ServiceVPNHandler(Looper looper, ServiceVPN serviceVPN) {
        super(looper);
        this.serviceVPN = serviceVPN;
    }

    static ServiceVPNHandler getInstance(Looper looper, ServiceVPN serviceVPN) {
        return serviceVPNHandler = new ServiceVPNHandler(looper, serviceVPN);
    }

    void queue(Intent intent) {
        VPNCommand cmd = (VPNCommand) intent.getSerializableExtra(EXTRA_COMMAND);
        Message msg = serviceVPNHandler.obtainMessage();
        msg.obj = intent;
        if (cmd != null) {
            msg.what = cmd.ordinal();
            serviceVPNHandler.sendMessage(msg);
        }
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        try {
            //synchronized (serviceVPN) {
            handleIntent((Intent) msg.obj);
            //}
        } catch (Throwable ex) {
            Log.e(LOG_TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
        }
    }

    private void handleIntent(Intent intent) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(serviceVPN);

        VPNCommand cmd = (VPNCommand) intent.getSerializableExtra(EXTRA_COMMAND);
        String reason = intent.getStringExtra(EXTRA_REASON);
        Log.i(LOG_TAG, "VPN Handler Executing intent=" + intent + " command=" + cmd + " reason=" + reason +
                " vpn=" + (serviceVPN.vpn != null) + " user=" + (Process.myUid() / 100000));

        try {
            if (cmd != null) {
                switch (cmd) {
                    case START:
                        start();
                        break;

                    case RELOAD:
                        reload();
                        break;

                    case STOP:
                        stop();
                        break;

                    default:
                        Log.e(LOG_TAG, "VPN Handler Unknown command=" + cmd);
                }
            }

            // Stop service if needed
            if (!serviceVPNHandler.hasMessages(VPNCommand.START.ordinal()) &&
                    !serviceVPNHandler.hasMessages(VPNCommand.RELOAD.ordinal()) &&
                    !prefs.getBoolean("VPNServiceEnabled", false))
                stopServiceVPN();

            // Request garbage collection
            System.gc();
        } catch (Throwable ex) {
            Log.e(LOG_TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));

            serviceVPN.reloading = false;

            if (cmd == VPNCommand.START || cmd == VPNCommand.RELOAD) {
                if (VpnService.prepare(serviceVPN) == null) {
                    Log.w(LOG_TAG, "VPN Handler prepared connected=" + serviceVPN.last_connected);
                    if (serviceVPN.last_connected && !(ex instanceof StartFailedException)) {
                        Toast.makeText(serviceVPN, serviceVPN.getText(R.string.vpn_mode_error), Toast.LENGTH_SHORT).show();
                    }
                    // Retried on connectivity change
                } else {
                    Toast.makeText(serviceVPN, serviceVPN.getText(R.string.vpn_mode_error), Toast.LENGTH_SHORT).show();

                    // Disable firewall
                    if (!(ex instanceof StartFailedException)) {
                        prefs.edit().putBoolean("VPNServiceEnabled", false).apply();
                    }
                }
            }
        }
    }

    private void start() {

        arpScanner = ArpScanner.INSTANCE.getInstance(serviceVPN, null);

        if (serviceVPN.vpn == null) {

            listRule = Rule.getRules(serviceVPN);
            List<String> listAllowed = getAllowedRules(listRule);

            last_builder = serviceVPN.getBuilder(listAllowed, listRule);
            serviceVPN.vpn = startVPN(last_builder);

            if (serviceVPN.vpn == null) {
                throw new StartFailedException("VPN Handler Start VPN Service Failed");
            }

            serviceVPN.startNative(serviceVPN.vpn, listAllowed, listRule);
        }
    }

    private void reload() {
        serviceVPN.reloading = true;

        ModulesStatus modulesStatus = ModulesStatus.getInstance();
        boolean fixTTL = modulesStatus.isFixTTL() && (modulesStatus.getMode() == ROOT_MODE)
                && !modulesStatus.isUseModulesWithRoot();

        String oldVpnInterfaceName = "";
        if (fixTTL) {
            oldVpnInterfaceName = ModulesIptablesRules.blockTethering(serviceVPN);
        }

        listRule = Rule.getRules(serviceVPN);
        List<String> listAllowed = getAllowedRules(listRule);

        ServiceVPN.Builder builder = serviceVPN.getBuilder(listAllowed, listRule);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            last_builder = builder;
            Log.i(LOG_TAG, "VPN Handler Legacy restart");

            if (serviceVPN.vpn != null) {
                serviceVPN.stopNative();
                stopVPN(serviceVPN.vpn);
                serviceVPN.vpn = null;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
            serviceVPN.vpn = startVPN(last_builder);

        } else {
            if (serviceVPN.vpn != null && builder.equals(last_builder)) {
                Log.i(LOG_TAG, "VPN Handler Native restart");
                serviceVPN.stopNative();

            } else {
                last_builder = builder;

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(serviceVPN);
                boolean handover = prefs.getBoolean("VPN handover", true);
                Log.i(LOG_TAG, "VPN Handler restart handover=" + handover);

                if (handover) {
                    // Attempt seamless handover
                    ParcelFileDescriptor prev = serviceVPN.vpn;
                    serviceVPN.vpn = startVPN(builder);

                    if (prev != null && serviceVPN.vpn == null) {
                        Log.w(LOG_TAG, "VPN Handler Handover failed");
                        serviceVPN.stopNative();
                        stopVPN(prev);
                        prev = null;
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException ignored) {
                        }
                        serviceVPN.vpn = startVPN(last_builder);
                        if (serviceVPN.vpn == null)
                            throw new IllegalStateException("VPN Handler Handover failed");
                    }

                    if (prev != null) {
                        serviceVPN.stopNative();
                        stopVPN(prev);
                    }
                } else {
                    if (serviceVPN.vpn != null) {
                        serviceVPN.stopNative();
                        stopVPN(serviceVPN.vpn);
                    }

                    serviceVPN.vpn = startVPN(builder);
                }
            }
        }

        if (serviceVPN.vpn == null)
            throw new StartFailedException("VPN Handler Start VPN Service Failed");

        serviceVPN.startNative(serviceVPN.vpn, listAllowed, listRule);

        if (fixTTL) {
            String finalOldVpnInterfaceName = oldVpnInterfaceName;
            postDelayed(() -> {
                modulesStatus.setFixTTLRulesUpdateRequested(serviceVPN, true);
                ModulesIptablesRules.allowTethering(serviceVPN, finalOldVpnInterfaceName);
            }, 1000);
        }

        serviceVPN.reloading = false;

        arpScanner.reset(serviceVPN, serviceVPN.last_connected || serviceVPN.last_connected_override);
    }

    private void stop() {
        if (serviceVPN.vpn != null) {
            serviceVPN.stopNative();
            stopVPN(serviceVPN.vpn);
            serviceVPN.vpn = null;
            serviceVPN.unPrepare();
        }

        stopServiceVPN();
    }

    private List<String> getAllowedRules(List<Rule> listRule) {
        List<String> listAllowed = new ArrayList<>();

        // Update connected state
        serviceVPN.last_connected = Util.isConnected(serviceVPN);

        //Request disconnected state confirmation in case of Always on VPN is enabled
        if (!serviceVPN.last_connected) {
            Util.isConnectedAsynchronousConfirmation(serviceVPN);
        }

        if (serviceVPN.last_connected || serviceVPN.last_connected_override) {

            if (!new PrefManager(serviceVPN).getBoolPref("FirewallEnabled")) {
                for (Rule rule: listRule) {
                    listAllowed.add(String.valueOf(rule.uid));
                }
            } else if (Util.isWifiActive(serviceVPN) || Util.isEthernetActive(serviceVPN)) {
                listAllowed.addAll(new PrefManager(serviceVPN).getSetStrPref(FirewallFragmentKt.APPS_ALLOW_WIFI_PREF));
            } else if (Util.isCellularActive(serviceVPN)) {
                listAllowed.addAll(new PrefManager(serviceVPN).getSetStrPref(FirewallFragmentKt.APPS_ALLOW_GSM_PREF));
            } else if (Util.isRoaming(serviceVPN)) {
                listAllowed.addAll(new PrefManager(serviceVPN).getSetStrPref(FirewallFragmentKt.APPS_ALLOW_ROAMING));
            }
        }

        Log.i(LOG_TAG, "VPN Handler Allowed " + listAllowed.size() + " of " + listRule.size());
        return listAllowed;
    }

    private ParcelFileDescriptor startVPN(ServiceVPN.Builder builder) throws SecurityException {
        try {
            ParcelFileDescriptor pfd = builder.establish();

            // Set underlying network
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ConnectivityManager cm = (ConnectivityManager) serviceVPN.getSystemService(CONNECTIVITY_SERVICE);
                Network active = (cm == null ? null : cm.getActiveNetwork());
                if (active != null) {
                    Log.i(LOG_TAG, "VPN Handler Setting underlying network=" + cm.getNetworkInfo(active));
                    serviceVPN.setUnderlyingNetworks(new Network[]{active});
                }
            }

            return pfd;
        } catch (SecurityException ex) {
            throw ex;
        } catch (Throwable ex) {
            Log.e(LOG_TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
            return null;
        }
    }

    void stopVPN(ParcelFileDescriptor pfd) {
        Log.i(LOG_TAG, "VPN Handler Stopping");
        try {
            pfd.close();
        } catch (IOException ex) {
            Log.e(LOG_TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
        }
    }

    private void stopServiceVPN() {

        if (serviceVPN == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && serviceVPN.notificationManager != null) {
            try {
                serviceVPN.notificationManager.cancel(DEFAULT_NOTIFICATION_ID);
                serviceVPN.stopForeground(true);
            } catch (Exception e) {
                Log.e(LOG_TAG, "ServiceVPNHandler stopServiceVPN exception " + e.getMessage() + " " + e.getCause());
            }
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(serviceVPN);
        prefs.edit().putBoolean("VPNServiceEnabled", false).apply();

        serviceVPN.stopSelf();

        ModulesStatus modulesStatus = ModulesStatus.getInstance();
        ModuleState dnsCryptState = modulesStatus.getDnsCryptState();
        ModuleState torState = modulesStatus.getTorState();
        ModuleState itpdState = modulesStatus.getItpdState();

        //If modules are running start ModulesService Foreground, which is background because of serviceVPN.stopSelf() with same notification id
        if (dnsCryptState != STOPPED || torState != STOPPED || itpdState != STOPPED) {
            ModulesAux.requestModulesStatusUpdate(serviceVPN);
        }
    }

    private static class StartFailedException extends IllegalStateException {
        StartFailedException(String msg) {
            super(msg);
        }
    }

    public static List<Rule> getAppsList() {
        return listRule;
    }
}
