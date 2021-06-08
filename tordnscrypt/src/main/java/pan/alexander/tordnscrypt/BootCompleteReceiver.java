package pan.alexander.tordnscrypt;
/*
    This file is part of VPN.

    VPN is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    VPN is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with VPN.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2019-2021 by Garmatin Oleksandr invizible.soft@gmail.com
*/

import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import pan.alexander.tordnscrypt.modules.ModulesAux;
import pan.alexander.tordnscrypt.modules.ModulesService;
import pan.alexander.tordnscrypt.modules.ModulesStatus;
import pan.alexander.tordnscrypt.settings.PathVars;
import pan.alexander.tordnscrypt.settings.PreferencesFastFragment;
import pan.alexander.tordnscrypt.utils.ApManager;
import pan.alexander.tordnscrypt.utils.FileShortener;
import pan.alexander.tordnscrypt.utils.PrefManager;
import pan.alexander.tordnscrypt.modules.ModulesKiller;
import pan.alexander.tordnscrypt.modules.ModulesRunner;
import pan.alexander.tordnscrypt.utils.enums.ModuleState;
import pan.alexander.tordnscrypt.utils.enums.OperationMode;
import pan.alexander.tordnscrypt.vpn.service.ServiceVPNHelper;

import static pan.alexander.tordnscrypt.modules.ModulesService.actionStopServiceForeground;
import static pan.alexander.tordnscrypt.modules.ModulesStateLoop.SAVED_DNSCRYPT_STATE_PREF;
import static pan.alexander.tordnscrypt.modules.ModulesStateLoop.SAVED_ITPD_STATE_PREF;
import static pan.alexander.tordnscrypt.modules.ModulesStateLoop.SAVED_TOR_STATE_PREF;
import static pan.alexander.tordnscrypt.utils.RootExecService.LOG_TAG;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.STOPPED;
import static pan.alexander.tordnscrypt.utils.enums.OperationMode.ROOT_MODE;
import static pan.alexander.tordnscrypt.utils.enums.OperationMode.UNDEFINED;
import static pan.alexander.tordnscrypt.utils.enums.OperationMode.VPN_MODE;

public class BootCompleteReceiver extends BroadcastReceiver {
    public static final String ALWAYS_ON_VPN = "pan.alexander.tordnscrypt.ALWAYS_ON_VPN";
    public static final String SHELL_SCRIPT_CONTROL = "pan.alexander.tordnscrypt.SHELL_SCRIPT_CONTROL";

    private Context context;
    private String appDataDir;

    @Override
    public void onReceive(final Context context, Intent intent) {

        final String BOOT_COMPLETE = "android.intent.action.BOOT_COMPLETED";
        final String QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON";
        final String HTC_QUICKBOOT_POWERON = "com.htc.intent.action.QUICKBOOT_POWERON";
        final String REBOOT = "android.intent.action.REBOOT";
        final String MY_PACKAGE_REPLACED = "android.intent.action.MY_PACKAGE_REPLACED";
        this.context = context;

        final PathVars pathVars = PathVars.getInstance(context.getApplicationContext());
        appDataDir = pathVars.getAppDataDir();

        final boolean tethering_autostart;

        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(context);

        String action = intent.getAction();

        if (action == null) {
            return;
        }

        Log.i(LOG_TAG, "Receive " + action);

        if (action.equalsIgnoreCase(BOOT_COMPLETE)
                || action.equalsIgnoreCase(QUICKBOOT_POWERON)
                || action.equalsIgnoreCase(HTC_QUICKBOOT_POWERON)
                || action.equalsIgnoreCase(REBOOT)
                || action.equalsIgnoreCase(MY_PACKAGE_REPLACED)
                || action.equals(ALWAYS_ON_VPN)
                || action.equals(SHELL_SCRIPT_CONTROL)) {

            if (action.equals(SHELL_SCRIPT_CONTROL) && !shPref.getBoolean("pref_common_shell_control", false)) {
                Log.w(LOG_TAG, "BootCompleteReceiver received SHELL_CONTROL, but the appropriate option is disabled!");
                return;
            }

            new PrefManager(context).setBoolPref("APisON", false);
            new PrefManager(context).setBoolPref("ModemIsON", false);

            tethering_autostart = shPref.getBoolean("pref_common_tethering_autostart", false);

            boolean rootIsAvailable = new PrefManager(context).getBoolPref("rootIsAvailable");
            boolean runModulesWithRoot = shPref.getBoolean("swUseModulesRoot", false);
            boolean fixTTL = shPref.getBoolean("pref_common_fix_ttl", false);
            String operationMode = new PrefManager(context).getStrPref("OPERATION_MODE");

            OperationMode mode = UNDEFINED;
            if (!operationMode.isEmpty()) {
                mode = OperationMode.valueOf(operationMode);
            }

            ModulesAux.switchModes(context, rootIsAvailable, runModulesWithRoot, mode);

            boolean autoStartDNSCrypt = shPref.getBoolean("swAutostartDNS", false);
            boolean autoStartTor = shPref.getBoolean("swAutostartTor", false);
            boolean autoStartITPD = shPref.getBoolean("swAutostartITPD", false);

            boolean savedDNSCryptStateRunning = ModulesAux.isDnsCryptSavedStateRunning(context);
            boolean savedTorStateRunning = ModulesAux.isTorSavedStateRunning(context);
            boolean savedITPDStateRunning = ModulesAux.isITPDSavedStateRunning(context);

            if (action.equalsIgnoreCase(MY_PACKAGE_REPLACED) || action.equalsIgnoreCase(ALWAYS_ON_VPN)) {
                autoStartDNSCrypt = savedDNSCryptStateRunning;
                autoStartTor = savedTorStateRunning;
                autoStartITPD = savedITPDStateRunning;
            } else if (action.equals(SHELL_SCRIPT_CONTROL)) {
                autoStartDNSCrypt = intent.getIntExtra("dnscrypt", 0) == 1;
                autoStartTor = intent.getIntExtra("tor", 0) == 1;
                autoStartITPD = intent.getIntExtra("i2p", 0) == 1;

                Log.i(LOG_TAG, "SHELL_SCRIPT_CONTROL start: " +
                        "DNSCrypt " + autoStartDNSCrypt + " Tor " + autoStartTor + " ITPD " + autoStartITPD);
            } else {
                resetModulesSavedState(context);
            }

            if (savedDNSCryptStateRunning || savedTorStateRunning || savedITPDStateRunning) {
                stopServicesForeground(context, mode, fixTTL);
            }

            if (autoStartITPD) {
                shortenTooLongITPDLog();
            }

            ModulesStatus modulesStatus = ModulesStatus.getInstance();
            modulesStatus.setFixTTL(fixTTL);

            if (tethering_autostart) {

                if (!action.equalsIgnoreCase(MY_PACKAGE_REPLACED) && !action.equalsIgnoreCase(ALWAYS_ON_VPN)
                        && !action.equals(SHELL_SCRIPT_CONTROL)) {
                    startHOTSPOT();
                }

            }

            if (autoStartDNSCrypt && !runModulesWithRoot
                    && !shPref.getBoolean("ignore_system_dns", false)
                    && !action.equalsIgnoreCase(MY_PACKAGE_REPLACED)
                    && !action.equals(SHELL_SCRIPT_CONTROL)) {
                modulesStatus.setSystemDNSAllowed(true);
            }

            if (autoStartDNSCrypt && autoStartTor && autoStartITPD) {
                startStopRestartModules(true, true, true);
            } else if (autoStartDNSCrypt && autoStartTor) {
                startStopRestartModules(true, true, false);
            } else if (autoStartDNSCrypt && !autoStartITPD) {
                startStopRestartModules(true, false, false);
                stopRefreshTorUnlockIPs(context);
            } else if (!autoStartDNSCrypt && autoStartTor && !autoStartITPD) {
                startStopRestartModules(false, true, false);
            } else if (!autoStartDNSCrypt && !autoStartTor && autoStartITPD) {
                startStopRestartModules(false, false, true);
                stopRefreshTorUnlockIPs(context);
            } else if (!autoStartDNSCrypt && autoStartTor) {
                startStopRestartModules(false, true, true);
            } else if (autoStartDNSCrypt) {
                startStopRestartModules(true, false, true);
                stopRefreshTorUnlockIPs(context);
            } else {
                startStopRestartModules(false, false, false);
                stopRefreshTorUnlockIPs(context);
            }

            if ((autoStartDNSCrypt || autoStartTor || autoStartITPD) && (mode == VPN_MODE || fixTTL)) {
                final Intent prepareIntent = VpnService.prepare(context);

                if (prepareIntent == null) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(() -> {
                        shPref.edit().putBoolean("VPNServiceEnabled", true).apply();

                        String reason;
                        switch (action) {
                            case MY_PACKAGE_REPLACED:
                                reason = "MY_PACKAGE_REPLACED";
                                break;
                            case ALWAYS_ON_VPN:
                                reason = "ALWAYS_ON_VPN";
                                break;
                            case SHELL_SCRIPT_CONTROL:
                                reason = "SHELL_SCRIPT_CONTROL";
                                break;
                            default:
                                reason = "Boot complete";
                                break;
                        }

                        ServiceVPNHelper.start(reason, context);
                    }, 2000);
                }
            }
        }
    }

    private void shortenTooLongITPDLog() {
        FileShortener.shortenTooTooLongFile(appDataDir + "/logs/i2pd.log");
    }

    private void startHOTSPOT() {

        new PrefManager(context).setBoolPref("APisON", true);

        ApManager apManager = new ApManager(context);
        if (!apManager.configApState()) {
            Intent intent_tether = new Intent(Intent.ACTION_MAIN, null);
            intent_tether.addCategory(Intent.CATEGORY_LAUNCHER);
            ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
            intent_tether.setComponent(cn);
            intent_tether.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(intent_tether);
            } catch (Exception e) {
                Log.e(LOG_TAG, "BootCompleteReceiver startHOTSPOT exception " + e.getMessage() + " " + e.getCause());
            }
        }
    }

    private void startStopRestartModules(boolean autoStartDNSCrypt, boolean autoStartTor, boolean autoStartITPD) {

        ModulesStatus modulesStatus = ModulesStatus.getInstance();

        if (autoStartDNSCrypt) {
            runDNSCrypt();
            modulesStatus.setIptablesRulesUpdateRequested(true);
        } else if (ModulesAux.isDnsCryptSavedStateRunning(context)) {
            stopDNSCrypt();
        } else {
            modulesStatus.setDnsCryptState(STOPPED);
        }

        if (autoStartTor) {
            runTor();
            modulesStatus.setIptablesRulesUpdateRequested(true);
        } else if (ModulesAux.isTorSavedStateRunning(context)){
            stopTor();
        } else {
            modulesStatus.setTorState(STOPPED);
        }

        if (autoStartITPD) {
            runITPD();
            modulesStatus.setIptablesRulesUpdateRequested(true);
        } else if (ModulesAux.isITPDSavedStateRunning(context)){
            stopITPD();
        } else {
            modulesStatus.setItpdState(STOPPED);
        }

        saveModulesStateRunning(autoStartDNSCrypt, autoStartTor, autoStartITPD);

    }

    private void saveModulesStateRunning(boolean saveDNSCryptRunning, boolean saveTorRunning, boolean saveITPDRunning) {
        ModulesAux.saveDNSCryptStateRunning(context, saveDNSCryptRunning);
        ModulesAux.saveTorStateRunning(context, saveTorRunning);
        ModulesAux.saveITPDStateRunning(context, saveITPDRunning);
    }

    private void resetModulesSavedState(Context context) {
        new PrefManager(context).setStrPref(SAVED_DNSCRYPT_STATE_PREF, ModuleState.UNDEFINED.toString());
        new PrefManager(context).setStrPref(SAVED_TOR_STATE_PREF, ModuleState.UNDEFINED.toString());
        new PrefManager(context).setStrPref(SAVED_ITPD_STATE_PREF, ModuleState.UNDEFINED.toString());
    }

    private void runDNSCrypt() {
        ModulesRunner.runDNSCrypt(context);
    }

    private void runTor() {
        ModulesRunner.runTor(context);
    }

    private void runITPD() {
        ModulesRunner.runITPD(context);
    }

    private void stopDNSCrypt() {
        ModulesKiller.stopDNSCrypt(context);
    }

    private void stopTor() {
        ModulesKiller.stopTor(context);
    }

    private void stopITPD() {
        ModulesKiller.stopITPD(context);
    }

    private void stopRefreshTorUnlockIPs(Context context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            jobScheduler.cancel(PreferencesFastFragment.mJobId);
        }
    }

    private void stopServicesForeground(Context context, OperationMode mode, boolean fixTTL) {

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mode == VPN_MODE || mode == ROOT_MODE && fixTTL) {
                Intent stopVPNServiceForeground = new Intent(context, VpnService.class);
                stopVPNServiceForeground.setAction(actionStopServiceForeground);
                stopVPNServiceForeground.putExtra("showNotification", true);
                context.startForegroundService(stopVPNServiceForeground);
            }

            Intent stopModulesServiceForeground = new Intent(context, ModulesService.class);
            stopModulesServiceForeground.setAction(actionStopServiceForeground);
            context.startForegroundService(stopModulesServiceForeground);
            stopModulesServiceForeground.putExtra("showNotification", true);

            Log.i(LOG_TAG, "BootCompleteReceiver stop running services foreground");
        }
    }
}
