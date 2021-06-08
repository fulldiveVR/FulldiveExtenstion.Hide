package pan.alexander.tordnscrypt.iptables;
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

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import pan.alexander.tordnscrypt.modules.ModulesStatus;
import pan.alexander.tordnscrypt.settings.PathVars;
import pan.alexander.tordnscrypt.utils.PrefManager;
import pan.alexander.tordnscrypt.vpn.Util;

import static pan.alexander.tordnscrypt.settings.tor_ips.UnlockTorIpsFrag.IPS_FOR_CLEARNET_TETHER;
import static pan.alexander.tordnscrypt.settings.tor_ips.UnlockTorIpsFrag.IPS_TO_UNLOCK_TETHER;
import static pan.alexander.tordnscrypt.utils.RootExecService.LOG_TAG;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.RUNNING;
import static pan.alexander.tordnscrypt.utils.enums.OperationMode.ROOT_MODE;

public class Tethering {
    private final Context context;

    public static volatile boolean usbTetherOn = false;
    public static volatile boolean ethernetOn = false;

    static final String wifiAPAddressesRange = "192.168.43.0/24";
    static final String usbModemAddressesRange = "192.168.42.0/24";
    private static final String addressVPN = "10.1.10.1";
    public static String addressLocalPC = "192.168.0.100";

    static String vpnInterfaceName = "tun0";
    static String wifiAPInterfaceName = "wlan0";
    static String usbModemInterfaceName = "rndis0";
    private static String ethernetInterfaceName = "eth0";

    private final PathVars pathVars;
    private String iptables = "iptables ";
    private boolean apIsOn = false;

    Tethering(Context context) {
        this.context = context;
        pathVars = PathVars.getInstance(context);
    }

    @NonNull
    List<String> activateTethering(boolean privacyMode) {

        if (context == null) {
            return new ArrayList<>();
        }

        iptables = pathVars.getIptablesPath();
        String ip6tables = pathVars.getIp6tablesPath();
        String busybox = pathVars.getBusyboxPath();

        ModulesStatus modulesStatus = ModulesStatus.getInstance();
        boolean dnsCryptRunning = modulesStatus.getDnsCryptState() == RUNNING;
        boolean torRunning = modulesStatus.getTorState() == RUNNING;
        boolean itpdRunning = modulesStatus.getItpdState() == RUNNING;


        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean torTethering = shPref.getBoolean("pref_common_tor_tethering", false) && torRunning;
        boolean itpdTethering = shPref.getBoolean("pref_common_itpd_tethering", false) && itpdRunning;
        boolean routeAllThroughTorTether = shPref.getBoolean("pref_common_tor_route_all", false);
        boolean blockHotspotHttp = shPref.getBoolean("pref_common_block_http", false);
        addressLocalPC = shPref.getString("pref_common_local_eth_device_addr", "192.168.0.100");
        boolean lan = shPref.getBoolean("Allow LAN", false);
        boolean ttlFix = modulesStatus.isFixTTL() && (modulesStatus.getMode() == ROOT_MODE) && !modulesStatus.isUseModulesWithRoot();
        apIsOn = new PrefManager(context).getBoolPref("APisON");
        Set<String> ipsToUnlockTether = new PrefManager(context).getSetStrPref(IPS_TO_UNLOCK_TETHER);
        Set<String> ipsForClearNetTether = new PrefManager(context).getSetStrPref(IPS_FOR_CLEARNET_TETHER);

        setInterfaceNames();

        String bypassLanPrerouting = "";
        String bypassLanForward = "";
        if (lan) {
            StringBuilder nonTorRanges = new StringBuilder();
            for (String address: Util.nonTorList) {
                nonTorRanges.append(address).append(" ");
            }
            nonTorRanges.deleteCharAt(nonTorRanges.lastIndexOf(" "));

            bypassLanPrerouting = "non_tor=\"" + nonTorRanges + "\"; " +
                    "for _lan in $non_tor; do " +
                    iptables + "-t nat -A tordnscrypt_prerouting -d $_lan -j ACCEPT; " +
                    "done";
            bypassLanForward = "non_tor=\"" + nonTorRanges + "\"; " +
                    "for _lan in $non_tor; do " +
                    iptables + "-A tordnscrypt_forward -d $_lan -j ACCEPT; " +
                    "done";
        }

        String torSitesBypassPrerouting = "";
        String torSitesBypassForward = "";

        String torSitesRedirectPreroutingWiFi = "";
        String torSitesRedirectPreroutingUSBModem = "";
        String torSitesRedirectPreroutingEthernet = "";

        String torSitesRejectNonTCPForwardWiFi = "";
        String torSitesRejectNonTCPForwardUSBModem = "";
        String torSitesRejectNonTCPForwardEthernet = "";

        if (routeAllThroughTorTether) {
            StringBuilder torSitesBypassPreroutingBuilder = new StringBuilder();
            StringBuilder torSitesBypassForwardBuilder = new StringBuilder();

            for (String ipForClearNetTether: ipsForClearNetTether) {
                torSitesBypassPreroutingBuilder.append(iptables).append("-t nat -A tordnscrypt_prerouting -p all -d ").append(ipForClearNetTether).append(" -j ACCEPT; ");
                torSitesBypassForwardBuilder.append(iptables).append("-A tordnscrypt_forward -p all -d ").append(ipForClearNetTether).append(" -j ACCEPT; ");
            }

            if (torSitesBypassPreroutingBuilder.length() > 2) {
                torSitesBypassPrerouting = torSitesBypassPreroutingBuilder.substring(0, torSitesBypassPreroutingBuilder.length() - 2);
            }
            if (torSitesBypassForwardBuilder.length() > 2) {
                torSitesBypassForward = torSitesBypassForwardBuilder.substring(0, torSitesBypassForwardBuilder.length() - 2);
            }
        } else {
            StringBuilder torSitesRedirectPreroutingWiFiBuilder = new StringBuilder();
            StringBuilder torSitesRedirectPreroutingUSBModemBuilder = new StringBuilder();
            StringBuilder torSitesRedirectPreroutingEthernetBuilder = new StringBuilder();

            StringBuilder torSitesRejectNonTCPForwardWiFiBuilder = new StringBuilder();
            StringBuilder torSitesRejectNonTCPForwardUSBModemBuilder = new StringBuilder();
            StringBuilder torSitesRejectNonTCPForwardEthernetBuilder = new StringBuilder();

            for (String ipToUnlockTether: ipsToUnlockTether) {
                torSitesRedirectPreroutingWiFiBuilder.append(iptables).append("-t nat -A tordnscrypt_prerouting -i ").append(wifiAPInterfaceName).append(" -p tcp -d ").append(ipToUnlockTether).append(" -j REDIRECT --to-port ").append(pathVars.getTorTransPort()).append(" || true; ");
                torSitesRedirectPreroutingUSBModemBuilder.append(iptables).append("-t nat -A tordnscrypt_prerouting -i ").append(usbModemInterfaceName).append(" -p tcp -d ").append(ipToUnlockTether).append(" -j REDIRECT --to-port ").append(pathVars.getTorTransPort()).append(" || true; ");
                torSitesRedirectPreroutingEthernetBuilder.append(iptables).append("-t nat -A tordnscrypt_prerouting -i ").append(ethernetInterfaceName).append(" -p tcp -d ").append(ipToUnlockTether).append(" -j REDIRECT --to-port ").append(pathVars.getTorTransPort()).append(" || true; ");

                torSitesRejectNonTCPForwardWiFiBuilder.append(iptables).append("-A tordnscrypt_forward -i ").append(wifiAPInterfaceName).append(" ! -p tcp -d ").append(ipToUnlockTether).append(" -j REJECT || true; ");
                torSitesRejectNonTCPForwardUSBModemBuilder.append(iptables).append("-A tordnscrypt_forward -i ").append(usbModemInterfaceName).append(" ! -p tcp -d ").append(ipToUnlockTether).append(" -j REJECT || true; ");
                torSitesRejectNonTCPForwardEthernetBuilder.append(iptables).append("-A tordnscrypt_forward -i ").append(ethernetInterfaceName).append(" ! -p tcp -d ").append(ipToUnlockTether).append(" -j REJECT || true; ");
            }

            torSitesRedirectPreroutingWiFi = removeRedundantSymbols(torSitesRedirectPreroutingWiFiBuilder);
            torSitesRedirectPreroutingUSBModem = removeRedundantSymbols(torSitesRedirectPreroutingUSBModemBuilder);
            torSitesRedirectPreroutingEthernet = removeRedundantSymbols(torSitesRedirectPreroutingEthernetBuilder);

            torSitesRejectNonTCPForwardWiFi = removeRedundantSymbols(torSitesRejectNonTCPForwardWiFiBuilder);
            torSitesRejectNonTCPForwardUSBModem = removeRedundantSymbols(torSitesRejectNonTCPForwardUSBModemBuilder);
            torSitesRejectNonTCPForwardEthernet = removeRedundantSymbols(torSitesRejectNonTCPForwardEthernetBuilder);
        }

        String blockHttpRuleForwardTCP = "";
        String blockHttpRuleForwardUDP = "";
        String blockHttpRulePreroutingTCPwifi = "";
        String blockHttpRulePreroutingUDPwifi = "";
        String blockHttpRulePreroutingTCPusb = "";
        String blockHttpRulePreroutingUDPusb = "";
        String blockHttpRulePreroutingTCPeth = "";
        String blockHttpRulePreroutingUDPeth = "";
        if (blockHotspotHttp) {
            blockHttpRuleForwardTCP = iptables + "-A tordnscrypt_forward -p tcp --dport 80 -j REJECT";
            blockHttpRuleForwardUDP = iptables + "-A tordnscrypt_forward -p udp --dport 80 -j REJECT";
            blockHttpRulePreroutingTCPwifi = iptables + "-t nat -A tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -p tcp ! -d " + wifiAPAddressesRange + " --dport 80 -j RETURN || true";
            blockHttpRulePreroutingUDPwifi = iptables + "-t nat -A tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -p udp ! -d " + wifiAPAddressesRange + " --dport 80 -j RETURN || true";
            blockHttpRulePreroutingTCPusb = iptables + "-t nat -A tordnscrypt_prerouting -i " + usbModemInterfaceName + " -p tcp ! -d " + usbModemAddressesRange + " --dport 80 -j RETURN || true";
            blockHttpRulePreroutingUDPusb = iptables + "-t nat -A tordnscrypt_prerouting -i " + usbModemInterfaceName + " -p udp ! -d " + usbModemAddressesRange + " --dport 80 -j RETURN || true";
            blockHttpRulePreroutingTCPeth = iptables + "-t nat -A tordnscrypt_prerouting -i " + ethernetInterfaceName + " -p tcp ! -d " + addressLocalPC + " --dport 80 -j RETURN || true";
            blockHttpRulePreroutingUDPeth = iptables + "-t nat -A tordnscrypt_prerouting -i " + ethernetInterfaceName + " -p udp ! -d " + addressLocalPC + " --dport 80 -j RETURN || true";
        }


        List<String> bypassITPDTunnelPorts = new ArrayList<>();
        Set<String> ports = new PrefManager(context).getSetStrPref("ITPDTunnelsPorts");
        if (ports != null && ports.size() > 0) {
            for (String port : ports) {
                if (!port.isEmpty()) {
                    bypassITPDTunnelPorts.add(iptables + "-t nat -A tordnscrypt_prerouting -p tcp -m tcp --dport " + port + " -j ACCEPT");
                    bypassITPDTunnelPorts.add(iptables + "-t nat -A tordnscrypt_prerouting -p udp -m udp --dport " + port + " -j ACCEPT");
                }
            }
        }

        List<String> tetheringCommands = new ArrayList<>();
        boolean tetherIptablesRulesIsClean = new PrefManager(context).getBoolPref("TetherIptablesRulesIsClean");
        boolean ttlFixed = new PrefManager(context).getBoolPref("TTLisFixed");

        if (!torTethering && !itpdTethering && ((!apIsOn && !usbTetherOn && !ethernetOn) || !dnsCryptRunning)) {

            if (tetherIptablesRulesIsClean) {
                return tetheringCommands;
            }

            new PrefManager(context).setBoolPref("TetherIptablesRulesIsClean", true);

            tetheringCommands = new ArrayList<>(Arrays.asList(
                    ip6tables + "-D INPUT -j DROP 2> /dev/null || true",
                    ip6tables + "-I INPUT -j DROP || true",
                    ip6tables + "-D FORWARD -j DROP 2> /dev/null || true",
                    ip6tables + "-I FORWARD -j DROP",
                    iptables + "-t nat -F tordnscrypt_prerouting 2> /dev/null",
                    iptables + "-F tordnscrypt_forward 2> /dev/null",
                    iptables + "-t nat -D PREROUTING -j tordnscrypt_prerouting 2> /dev/null || true",
                    iptables + "-D FORWARD -j tordnscrypt_forward 2> /dev/null || true"
            ));

            if (ttlFixed) {
                tetheringCommands.addAll(unfixTTLCommands());
            }

        } else if (!privacyMode) {

            new PrefManager(context).setBoolPref("TetherIptablesRulesIsClean", false);

            if (!torTethering && !itpdTethering) {
                tetheringCommands = new ArrayList<>(Arrays.asList(
                        iptables + "-I FORWARD -j DROP",
                        ip6tables + "-D INPUT -j DROP 2> /dev/null || true",
                        ip6tables + "-I INPUT -j DROP || true",
                        ip6tables + "-D FORWARD -j DROP 2> /dev/null || true",
                        ip6tables + "-I FORWARD -j DROP",
                        iptables + "-t nat -F tordnscrypt_prerouting 2> /dev/null",
                        iptables + "-F tordnscrypt_forward 2> /dev/null",
                        iptables + "-t nat -D PREROUTING -j tordnscrypt_prerouting 2> /dev/null || true",
                        iptables + "-D FORWARD -j tordnscrypt_forward 2> /dev/null || true",
                        busybox + "sleep 1",
                        iptables + "-t nat -N tordnscrypt_prerouting 2> /dev/null",
                        iptables + "-N tordnscrypt_forward 2> /dev/null",
                        iptables + "-t nat -A PREROUTING -j tordnscrypt_prerouting",
                        iptables + "-A FORWARD -j tordnscrypt_forward",
                        busybox + "sleep 1",
                        iptables + "-D tordnscrypt -p udp -m udp --dport 67 -j ACCEPT 2> /dev/null || true",
                        iptables + "-D tordnscrypt -p udp -m udp --dport 68 -j ACCEPT 2> /dev/null || true",
                        iptables + "-I tordnscrypt -p udp -m udp --dport 67 -j ACCEPT",
                        iptables + "-I tordnscrypt -p udp -m udp --dport 68 -j ACCEPT",
                        iptables + "-D tordnscrypt -p udp -m udp --sport 67 -j ACCEPT 2> /dev/null || true",
                        iptables + "-D tordnscrypt -p udp -m udp --sport 68 -j ACCEPT 2> /dev/null || true",
                        iptables + "-I tordnscrypt -p udp -m udp --sport 67 -j ACCEPT",
                        iptables + "-I tordnscrypt -p udp -m udp --sport 68 -j ACCEPT",
                        busybox + "sleep 1",
                        blockHttpRulePreroutingTCPwifi,
                        blockHttpRulePreroutingUDPwifi,
                        blockHttpRulePreroutingTCPusb,
                        blockHttpRulePreroutingUDPusb,
                        blockHttpRulePreroutingTCPeth,
                        blockHttpRulePreroutingUDPeth,
                        busybox + "sleep 1",
                        iptables + "-A tordnscrypt_forward -p tcp --dport 53 -j ACCEPT",
                        iptables + "-A tordnscrypt_forward -p udp --dport 53 -j ACCEPT",
                        blockHttpRuleForwardTCP,
                        blockHttpRuleForwardUDP,
                        iptables + "-D FORWARD -j DROP 2> /dev/null || true"
                ));

                if (ttlFix) {
                    tetheringCommands.addAll(fixTTLCommands());
                } else if (ttlFixed) {
                    tetheringCommands.addAll(unfixTTLCommands());
                }

            } else if (torTethering && routeAllThroughTorTether && itpdTethering) {
                tetheringCommands = new ArrayList<>(Arrays.asList(
                        iptables + "-I FORWARD -j DROP",
                        ip6tables + "-D INPUT -j DROP 2> /dev/null || true",
                        ip6tables + "-I INPUT -j DROP || true",
                        ip6tables + "-D FORWARD -j DROP 2> /dev/null || true",
                        ip6tables + "-I FORWARD -j DROP",
                        iptables + "-t nat -F tordnscrypt_prerouting 2> /dev/null",
                        iptables + "-F tordnscrypt_forward 2> /dev/null",
                        iptables + "-t nat -D PREROUTING -j tordnscrypt_prerouting 2> /dev/null || true",
                        iptables + "-D FORWARD -j tordnscrypt_forward 2> /dev/null || true",
                        busybox + "sleep 1",
                        iptables + "-t nat -N tordnscrypt_prerouting 2> /dev/null",
                        iptables + "-N tordnscrypt_forward 2> /dev/null",
                        iptables + "-t nat -A PREROUTING -j tordnscrypt_prerouting",
                        iptables + "-A FORWARD -j tordnscrypt_forward",
                        busybox + "sleep 1",
                        iptables + "-D tordnscrypt -p udp -m udp --dport 67 -j ACCEPT 2> /dev/null || true",
                        iptables + "-D tordnscrypt -p udp -m udp --dport 68 -j ACCEPT 2> /dev/null || true",
                        iptables + "-I tordnscrypt -p udp -m udp --dport 67 -j ACCEPT",
                        iptables + "-I tordnscrypt -p udp -m udp --dport 68 -j ACCEPT",
                        iptables + "-D tordnscrypt -p udp -m udp --sport 67 -j ACCEPT 2> /dev/null || true",
                        iptables + "-D tordnscrypt -p udp -m udp --sport 68 -j ACCEPT 2> /dev/null || true",
                        iptables + "-I tordnscrypt -p udp -m udp --sport 67 -j ACCEPT",
                        iptables + "-I tordnscrypt -p udp -m udp --sport 68 -j ACCEPT",
                        busybox + "sleep 1",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -d " + wifiAPAddressesRange + " -j ACCEPT || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + usbModemInterfaceName + " -d " + usbModemAddressesRange + " -j ACCEPT || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + ethernetInterfaceName + " -d " + addressLocalPC + " -j ACCEPT || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -p tcp -d 10.191.0.1 -j REDIRECT --to-ports " + pathVars.getITPDHttpProxyPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -p udp -d 10.191.0.1 -j REDIRECT --to-ports " + pathVars.getITPDHttpProxyPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + usbModemInterfaceName + " -p tcp -d 10.191.0.1 -j REDIRECT --to-ports " + pathVars.getITPDHttpProxyPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + usbModemInterfaceName + " -p udp -d 10.191.0.1 -j REDIRECT --to-ports " + pathVars.getITPDHttpProxyPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + ethernetInterfaceName + " -p tcp -d 10.191.0.1 -j REDIRECT --to-ports " + pathVars.getITPDHttpProxyPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + ethernetInterfaceName + " -p udp -d 10.191.0.1 -j REDIRECT --to-ports " + pathVars.getITPDHttpProxyPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -p tcp -d " + pathVars.getTorVirtAdrNet() + " -j REDIRECT --to-ports " + pathVars.getTorTransPort(),
                        blockHttpRulePreroutingTCPwifi,
                        blockHttpRulePreroutingUDPwifi,
                        blockHttpRulePreroutingTCPusb,
                        blockHttpRulePreroutingUDPusb,
                        blockHttpRulePreroutingTCPeth,
                        blockHttpRulePreroutingUDPeth,
                        torSitesBypassPrerouting,
                        bypassLanPrerouting,
                        iptables + "-t nat -A tordnscrypt_prerouting -p tcp -m tcp --dport " + pathVars.getTorSOCKSPort() + " -j ACCEPT",
                        iptables + "-t nat -A tordnscrypt_prerouting -p udp -m udp --dport " + pathVars.getTorSOCKSPort() + " -j ACCEPT",
                        iptables + "-t nat -A tordnscrypt_prerouting -p tcp -m tcp --dport " + pathVars.getITPDSOCKSPort() + " -j ACCEPT",
                        iptables + "-t nat -A tordnscrypt_prerouting -p udp -m udp --dport " + pathVars.getITPDSOCKSPort() + " -j ACCEPT"
                        ));

                List<String> tetheringCommandsPart2 = new ArrayList<>(Arrays.asList(
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -p tcp -j REDIRECT --to-ports " + pathVars.getTorTransPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + usbModemInterfaceName + " -p tcp -j REDIRECT --to-ports " + pathVars.getTorTransPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + ethernetInterfaceName + " -p tcp -j REDIRECT --to-ports " + pathVars.getTorTransPort() + " || true",
                        iptables + "-A tordnscrypt_forward -p udp --dport 53 -j ACCEPT",
                        iptables + "-A tordnscrypt_forward -p tcp --dport 53 -j ACCEPT",
                        blockHttpRuleForwardTCP,
                        blockHttpRuleForwardUDP,
                        torSitesBypassForward,
                        bypassLanForward,
                        iptables + "-A tordnscrypt_forward -m state --state ESTABLISHED,RELATED -j RETURN",
                        iptables + "-A tordnscrypt_forward -j REJECT",
                        iptables + "-D FORWARD -j DROP 2> /dev/null || true"
                ));

                tetheringCommands.addAll(bypassITPDTunnelPorts);
                tetheringCommands.addAll(tetheringCommandsPart2);

                if (ttlFix) {
                    tetheringCommands.addAll(fixTTLCommands());
                } else if (ttlFixed) {
                    tetheringCommands.addAll(unfixTTLCommands());
                }

            } else if (torTethering && itpdTethering) {
                tetheringCommands = new ArrayList<>(Arrays.asList(
                        iptables + "-I FORWARD -j DROP",
                        ip6tables + "-D INPUT -j DROP 2> /dev/null || true",
                        ip6tables + "-I INPUT -j DROP || true",
                        ip6tables + "-D FORWARD -j DROP 2> /dev/null || true",
                        ip6tables + "-I FORWARD -j DROP",
                        iptables + "-t nat -F tordnscrypt_prerouting 2> /dev/null",
                        iptables + "-F tordnscrypt_forward 2> /dev/null",
                        iptables + "-t nat -D PREROUTING -j tordnscrypt_prerouting 2> /dev/null || true",
                        iptables + "-D FORWARD -j tordnscrypt_forward 2> /dev/null || true",
                        busybox + "sleep 1",
                        iptables + "-t nat -N tordnscrypt_prerouting 2> /dev/null",
                        iptables + "-N tordnscrypt_forward 2> /dev/null",
                        iptables + "-t nat -A PREROUTING -j tordnscrypt_prerouting",
                        iptables + "-A FORWARD -j tordnscrypt_forward",
                        busybox + "sleep 1",
                        iptables + "-D tordnscrypt -p udp -m udp --dport 67 -j ACCEPT 2> /dev/null || true",
                        iptables + "-D tordnscrypt -p udp -m udp --dport 68 -j ACCEPT 2> /dev/null || true",
                        iptables + "-I tordnscrypt -p udp -m udp --dport 67 -j ACCEPT",
                        iptables + "-I tordnscrypt -p udp -m udp --dport 68 -j ACCEPT",
                        iptables + "-D tordnscrypt -p udp -m udp --sport 67 -j ACCEPT 2> /dev/null || true",
                        iptables + "-D tordnscrypt -p udp -m udp --sport 68 -j ACCEPT 2> /dev/null || true",
                        iptables + "-I tordnscrypt -p udp -m udp --sport 67 -j ACCEPT",
                        iptables + "-I tordnscrypt -p udp -m udp --sport 68 -j ACCEPT",
                        busybox + "sleep 1",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -d " + wifiAPAddressesRange + " -j ACCEPT || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + usbModemInterfaceName + " -d " + usbModemAddressesRange + " -j ACCEPT || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + ethernetInterfaceName + " -d " + addressLocalPC + " -j ACCEPT || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -p tcp -d 10.191.0.1 -j REDIRECT --to-ports " + pathVars.getITPDHttpProxyPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -p udp -d 10.191.0.1 -j REDIRECT --to-ports " + pathVars.getITPDHttpProxyPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + usbModemInterfaceName + " -p tcp -d 10.191.0.1 -j REDIRECT --to-ports " + pathVars.getITPDHttpProxyPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + usbModemInterfaceName + " -p udp -d 10.191.0.1 -j REDIRECT --to-ports " + pathVars.getITPDHttpProxyPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + ethernetInterfaceName + " -p tcp -d 10.191.0.1 -j REDIRECT --to-ports " + pathVars.getITPDHttpProxyPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + ethernetInterfaceName + " -p udp -d 10.191.0.1 -j REDIRECT --to-ports " + pathVars.getITPDHttpProxyPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -p tcp -d " + pathVars.getTorVirtAdrNet() + " -j REDIRECT --to-ports " + pathVars.getTorTransPort(),
                        blockHttpRulePreroutingTCPwifi,
                        blockHttpRulePreroutingUDPwifi,
                        blockHttpRulePreroutingTCPusb,
                        blockHttpRulePreroutingUDPusb,
                        blockHttpRulePreroutingTCPeth,
                        blockHttpRulePreroutingUDPeth,
                        busybox + "sleep 1",
                        torSitesRedirectPreroutingWiFi,
                        torSitesRedirectPreroutingUSBModem,
                        torSitesRedirectPreroutingEthernet,
                        iptables + "-A tordnscrypt_forward -p tcp --dport 53 -j ACCEPT",
                        iptables + "-A tordnscrypt_forward -p udp --dport 53 -j ACCEPT",
                        //Block all except TCP for Tor sites
                        torSitesRejectNonTCPForwardWiFi,
                        torSitesRejectNonTCPForwardUSBModem,
                        torSitesRejectNonTCPForwardEthernet,
                        blockHttpRuleForwardTCP,
                        blockHttpRuleForwardUDP,
                        iptables + "-D FORWARD -j DROP 2> /dev/null || true"
                ));

                if (ttlFix) {
                    tetheringCommands.addAll(fixTTLCommands());
                } else if (ttlFixed) {
                    tetheringCommands.addAll(unfixTTLCommands());
                }

            } else if (itpdTethering) {
                tetheringCommands = new ArrayList<>(Arrays.asList(
                        iptables + "-I FORWARD -j DROP",
                        ip6tables + "-D INPUT -j DROP 2> /dev/null || true",
                        ip6tables + "-I INPUT -j DROP || true",
                        ip6tables + "-D FORWARD -j DROP 2> /dev/null || true",
                        ip6tables + "-I FORWARD -j DROP",
                        iptables + "-t nat -F tordnscrypt_prerouting 2> /dev/null",
                        iptables + "-F tordnscrypt_forward 2> /dev/null",
                        iptables + "-t nat -D PREROUTING -j tordnscrypt_prerouting 2> /dev/null || true",
                        iptables + "-D FORWARD -j tordnscrypt_forward 2> /dev/null || true",
                        busybox + "sleep 1",
                        iptables + "-t nat -N tordnscrypt_prerouting 2> /dev/null",
                        iptables + "-N tordnscrypt_forward 2> /dev/null",
                        iptables + "-t nat -A PREROUTING -j tordnscrypt_prerouting",
                        iptables + "-A FORWARD -j tordnscrypt_forward",
                        busybox + "sleep 1",
                        iptables + "-D tordnscrypt -p udp -m udp --dport 67 -j ACCEPT 2> /dev/null || true",
                        iptables + "-D tordnscrypt -p udp -m udp --dport 68 -j ACCEPT 2> /dev/null || true",
                        iptables + "-I tordnscrypt -p udp -m udp --dport 67 -j ACCEPT",
                        iptables + "-I tordnscrypt -p udp -m udp --dport 68 -j ACCEPT",
                        iptables + "-D tordnscrypt -p udp -m udp --sport 67 -j ACCEPT 2> /dev/null || true",
                        iptables + "-D tordnscrypt -p udp -m udp --sport 68 -j ACCEPT 2> /dev/null || true",
                        iptables + "-I tordnscrypt -p udp -m udp --sport 67 -j ACCEPT",
                        iptables + "-I tordnscrypt -p udp -m udp --sport 68 -j ACCEPT",
                        busybox + "sleep 1",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -d " + wifiAPAddressesRange + " -j ACCEPT || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + usbModemInterfaceName + " -d " + usbModemAddressesRange + " -j ACCEPT || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + ethernetInterfaceName + " -d " + addressLocalPC + " -j ACCEPT || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -p tcp -d 10.191.0.1 -j REDIRECT --to-ports " + pathVars.getITPDHttpProxyPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -p udp -d 10.191.0.1 -j REDIRECT --to-ports " + pathVars.getITPDHttpProxyPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + usbModemInterfaceName + " -p tcp -d 10.191.0.1 -j REDIRECT --to-ports " + pathVars.getITPDHttpProxyPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + usbModemInterfaceName + " -p udp -d 10.191.0.1 -j REDIRECT --to-ports " + pathVars.getITPDHttpProxyPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + ethernetInterfaceName + " -p tcp -d 10.191.0.1 -j REDIRECT --to-ports " + pathVars.getITPDHttpProxyPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + ethernetInterfaceName + " -p udp -d 10.191.0.1 -j REDIRECT --to-ports " + pathVars.getITPDHttpProxyPort() + " || true",
                        iptables + "-A tordnscrypt_forward -p tcp --dport 53 -j ACCEPT",
                        iptables + "-A tordnscrypt_forward -p udp --dport 53 -j ACCEPT",
                        blockHttpRuleForwardTCP,
                        blockHttpRuleForwardUDP,
                        iptables + "-D FORWARD -j DROP 2> /dev/null || true"
                ));

                if (ttlFix) {
                    tetheringCommands.addAll(fixTTLCommands());
                } else if (ttlFixed) {
                    tetheringCommands.addAll(unfixTTLCommands());
                }

            } else if (routeAllThroughTorTether) {
                tetheringCommands = new ArrayList<>(Arrays.asList(
                        iptables + "-I FORWARD -j DROP",
                        ip6tables + "-D INPUT -j DROP 2> /dev/null || true",
                        ip6tables + "-I INPUT -j DROP || true",
                        ip6tables + "-D FORWARD -j DROP 2> /dev/null || true",
                        ip6tables + "-I FORWARD -j DROP",
                        iptables + "-t nat -F tordnscrypt_prerouting 2> /dev/null",
                        iptables + "-F tordnscrypt_forward 2> /dev/null",
                        iptables + "-t nat -D PREROUTING -j tordnscrypt_prerouting 2> /dev/null || true",
                        iptables + "-D FORWARD -j tordnscrypt_forward 2> /dev/null || true",
                        busybox + "sleep 1",
                        iptables + "-t nat -N tordnscrypt_prerouting 2> /dev/null",
                        iptables + "-N tordnscrypt_forward 2> /dev/null",
                        iptables + "-t nat -A PREROUTING -j tordnscrypt_prerouting",
                        iptables + "-A FORWARD -j tordnscrypt_forward",
                        busybox + "sleep 1",
                        iptables + "-D tordnscrypt -p udp -m udp --dport 67 -j ACCEPT 2> /dev/null || true",
                        iptables + "-D tordnscrypt -p udp -m udp --dport 68 -j ACCEPT 2> /dev/null || true",
                        iptables + "-I tordnscrypt -p udp -m udp --dport 67 -j ACCEPT",
                        iptables + "-I tordnscrypt -p udp -m udp --dport 68 -j ACCEPT",
                        iptables + "-D tordnscrypt -p udp -m udp --sport 67 -j ACCEPT 2> /dev/null || true",
                        iptables + "-D tordnscrypt -p udp -m udp --sport 68 -j ACCEPT 2> /dev/null || true",
                        iptables + "-I tordnscrypt -p udp -m udp --sport 67 -j ACCEPT",
                        iptables + "-I tordnscrypt -p udp -m udp --sport 68 -j ACCEPT",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -d " + wifiAPAddressesRange + " -j ACCEPT || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + usbModemInterfaceName + " -d " + usbModemAddressesRange + " -j ACCEPT || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + ethernetInterfaceName + " -d " + addressLocalPC + " -j ACCEPT || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -p tcp -d " + pathVars.getTorVirtAdrNet() + " -j REDIRECT --to-ports " + pathVars.getTorTransPort(),
                        busybox + "sleep 1",
                        blockHttpRulePreroutingTCPwifi,
                        blockHttpRulePreroutingUDPwifi,
                        blockHttpRulePreroutingTCPusb,
                        blockHttpRulePreroutingUDPusb,
                        blockHttpRulePreroutingTCPeth,
                        blockHttpRulePreroutingUDPeth,
                        torSitesBypassPrerouting,
                        bypassLanPrerouting,
                        iptables + "-t nat -A tordnscrypt_prerouting -p tcp -m tcp --dport " + pathVars.getTorSOCKSPort() + " -j ACCEPT",
                        iptables + "-t nat -A tordnscrypt_prerouting -p udp -m udp --dport " + pathVars.getTorSOCKSPort() + " -j ACCEPT",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -p tcp -j REDIRECT --to-ports " + pathVars.getTorTransPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + usbModemInterfaceName + " -p tcp -j REDIRECT --to-ports " + pathVars.getTorTransPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + ethernetInterfaceName + " -p tcp -j REDIRECT --to-ports " + pathVars.getTorTransPort() + " || true",
                        iptables + "-A tordnscrypt_forward -p tcp --dport 53 -j ACCEPT",
                        iptables + "-A tordnscrypt_forward -p udp --dport 53 -j ACCEPT",
                        blockHttpRuleForwardTCP,
                        blockHttpRuleForwardUDP,
                        torSitesBypassForward,
                        bypassLanForward,
                        iptables + "-A tordnscrypt_forward -m state --state ESTABLISHED,RELATED -j RETURN",
                        iptables + "-A tordnscrypt_forward -j REJECT",
                        iptables + "-D FORWARD -j DROP 2> /dev/null || true"
                ));

                if (ttlFix) {
                    tetheringCommands.addAll(fixTTLCommands());
                } else if (ttlFixed) {
                    tetheringCommands.addAll(unfixTTLCommands());
                }

            } else {
                tetheringCommands = new ArrayList<>(Arrays.asList(
                        iptables + "-I FORWARD -j DROP",
                        ip6tables + "-D INPUT -j DROP 2> /dev/null || true",
                        ip6tables + "-I INPUT -j DROP || true",
                        ip6tables + "-D FORWARD -j DROP 2> /dev/null || true",
                        ip6tables + "-I FORWARD -j DROP",
                        iptables + "-t nat -F tordnscrypt_prerouting 2> /dev/null",
                        iptables + "-F tordnscrypt_forward 2> /dev/null",
                        iptables + "-t nat -D PREROUTING -j tordnscrypt_prerouting 2> /dev/null || true",
                        iptables + "-D FORWARD -j tordnscrypt_forward 2> /dev/null || true",
                        busybox + "sleep 1",
                        iptables + "-t nat -N tordnscrypt_prerouting 2> /dev/null",
                        iptables + "-N tordnscrypt_forward 2> /dev/null",
                        iptables + "-t nat -A PREROUTING -j tordnscrypt_prerouting",
                        iptables + "-A FORWARD -j tordnscrypt_forward",
                        busybox + "sleep 1",
                        iptables + "-D tordnscrypt -p udp -m udp --dport 67 -j ACCEPT 2> /dev/null || true",
                        iptables + "-D tordnscrypt -p udp -m udp --dport 68 -j ACCEPT 2> /dev/null || true",
                        iptables + "-I tordnscrypt -p udp -m udp --dport 67 -j ACCEPT",
                        iptables + "-I tordnscrypt -p udp -m udp --dport 68 -j ACCEPT",
                        iptables + "-D tordnscrypt -p udp -m udp --sport 67 -j ACCEPT 2> /dev/null || true",
                        iptables + "-D tordnscrypt -p udp -m udp --sport 68 -j ACCEPT 2> /dev/null || true",
                        iptables + "-I tordnscrypt -p udp -m udp --sport 67 -j ACCEPT",
                        iptables + "-I tordnscrypt -p udp -m udp --sport 68 -j ACCEPT",
                        busybox + "sleep 1",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -d " + wifiAPAddressesRange + " -j ACCEPT || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + usbModemInterfaceName + " -d " + usbModemAddressesRange + " -j ACCEPT || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + ethernetInterfaceName + " -d " + addressLocalPC + " -j ACCEPT || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -p tcp -d " + pathVars.getTorVirtAdrNet() + " -j REDIRECT --to-ports " + pathVars.getTorTransPort(),
                        blockHttpRulePreroutingTCPwifi,
                        blockHttpRulePreroutingUDPwifi,
                        blockHttpRulePreroutingTCPusb,
                        blockHttpRulePreroutingUDPusb,
                        blockHttpRulePreroutingTCPeth,
                        blockHttpRulePreroutingUDPeth,
                        busybox + "sleep 1",
                        torSitesRedirectPreroutingWiFi,
                        torSitesRedirectPreroutingUSBModem,
                        torSitesRedirectPreroutingEthernet,
                        iptables + "-A tordnscrypt_forward -p tcp --dport 53 -j ACCEPT",
                        iptables + "-A tordnscrypt_forward -p udp --dport 53 -j ACCEPT",
                        //Block all except TCP for Tor sites
                        torSitesRejectNonTCPForwardWiFi,
                        torSitesRejectNonTCPForwardUSBModem,
                        torSitesRejectNonTCPForwardEthernet,
                        blockHttpRuleForwardTCP,
                        blockHttpRuleForwardUDP,
                        iptables + "-D FORWARD -j DROP 2> /dev/null || true"
                ));

                if (ttlFix) {
                    tetheringCommands.addAll(fixTTLCommands());
                } else if (ttlFixed) {
                    tetheringCommands.addAll(unfixTTLCommands());
                }
            }
        } else {

            new PrefManager(context).setBoolPref("TetherIptablesRulesIsClean", false);

            if (torTethering) {
                tetheringCommands = new ArrayList<>(Arrays.asList(
                        iptables + "-I FORWARD -j DROP",
                        ip6tables + "-D INPUT -j DROP 2> /dev/null || true",
                        ip6tables + "-I INPUT -j DROP || true",
                        ip6tables + "-D FORWARD -j DROP 2> /dev/null || true",
                        ip6tables + "-I FORWARD -j DROP",
                        iptables + "-t nat -F tordnscrypt_prerouting 2> /dev/null",
                        iptables + "-F tordnscrypt_forward 2> /dev/null",
                        iptables + "-t nat -D PREROUTING -j tordnscrypt_prerouting 2> /dev/null || true",
                        iptables + "-D FORWARD -j tordnscrypt_forward 2> /dev/null || true",
                        busybox + "sleep 1",
                        iptables + "-t nat -N tordnscrypt_prerouting 2> /dev/null",
                        iptables + "-N tordnscrypt_forward 2> /dev/null",
                        iptables + "-t nat -A PREROUTING -j tordnscrypt_prerouting",
                        iptables + "-A FORWARD -j tordnscrypt_forward",
                        busybox + "sleep 1",
                        iptables + "-D tordnscrypt -p udp -m udp --dport 67 -j ACCEPT 2> /dev/null || true",
                        iptables + "-D tordnscrypt -p udp -m udp --dport 68 -j ACCEPT 2> /dev/null || true",
                        iptables + "-I tordnscrypt -p udp -m udp --dport 67 -j ACCEPT",
                        iptables + "-I tordnscrypt -p udp -m udp --dport 68 -j ACCEPT",
                        iptables + "-D tordnscrypt -p udp -m udp --sport 67 -j ACCEPT 2> /dev/null || true",
                        iptables + "-D tordnscrypt -p udp -m udp --sport 68 -j ACCEPT 2> /dev/null || true",
                        iptables + "-I tordnscrypt -p udp -m udp --sport 67 -j ACCEPT",
                        iptables + "-I tordnscrypt -p udp -m udp --sport 68 -j ACCEPT",
                        busybox + "sleep 1",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -d " + wifiAPAddressesRange + " -j ACCEPT || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + usbModemInterfaceName + " -d " + usbModemAddressesRange + " -j ACCEPT || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + ethernetInterfaceName + " -d " + addressLocalPC + " -j ACCEPT || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -p tcp -d " + pathVars.getTorVirtAdrNet() + " -j REDIRECT --to-ports " + pathVars.getTorTransPort(),
                        blockHttpRulePreroutingTCPwifi,
                        blockHttpRulePreroutingUDPwifi,
                        blockHttpRulePreroutingTCPusb,
                        blockHttpRulePreroutingUDPusb,
                        blockHttpRulePreroutingTCPeth,
                        blockHttpRulePreroutingUDPeth,
                        torSitesBypassPrerouting,
                        bypassLanPrerouting,
                        iptables + "-t nat -A tordnscrypt_prerouting -p tcp -m tcp --dport " + pathVars.getTorSOCKSPort() + " -j ACCEPT",
                        iptables + "-t nat -A tordnscrypt_prerouting -p udp -m udp --dport " + pathVars.getTorSOCKSPort() + " -j ACCEPT",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -p tcp -j REDIRECT --to-ports " + pathVars.getTorTransPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + usbModemInterfaceName + " -p tcp -j REDIRECT --to-ports " + pathVars.getTorTransPort() + " || true",
                        iptables + "-t nat -A tordnscrypt_prerouting -i " + ethernetInterfaceName + " -p tcp -j REDIRECT --to-ports " + pathVars.getTorTransPort() + " || true",
                        iptables + "-A tordnscrypt_forward -p tcp --dport 53 -j ACCEPT",
                        iptables + "-A tordnscrypt_forward -p udp --dport 53 -j ACCEPT",
                        blockHttpRuleForwardTCP,
                        blockHttpRuleForwardUDP,
                        torSitesBypassForward,
                        bypassLanForward,
                        iptables + "-A tordnscrypt_forward -m state --state ESTABLISHED,RELATED -j RETURN",
                        iptables + "-A tordnscrypt_forward -j REJECT",
                        iptables + "-D FORWARD -j DROP 2> /dev/null || true"
                ));

                if (ttlFix) {
                    tetheringCommands.addAll(fixTTLCommands());
                } else if (ttlFixed) {
                    tetheringCommands.addAll(unfixTTLCommands());
                }

            } else {

                if (tetherIptablesRulesIsClean) {
                    return tetheringCommands;
                }

                new PrefManager(context).setBoolPref("TetherIptablesRulesIsClean", true);

                tetheringCommands = new ArrayList<>(Arrays.asList(
                        ip6tables + "-D INPUT -j DROP 2> /dev/null || true",
                        ip6tables + "-I INPUT -j DROP || true",
                        ip6tables + "-D FORWARD -j DROP 2> /dev/null || true",
                        ip6tables + "-I FORWARD -j DROP",
                        iptables + "-t nat -F tordnscrypt_prerouting 2> /dev/null",
                        iptables + "-F tordnscrypt_forward 2> /dev/null",
                        iptables + "-t nat -D PREROUTING -j tordnscrypt_prerouting 2> /dev/null || true",
                        iptables + "-D FORWARD -j tordnscrypt_forward 2> /dev/null || true"
                        ));

                if (ttlFixed) {
                    tetheringCommands.addAll(unfixTTLCommands());
                }

            }

        }

        return cleanupCommands(tetheringCommands);
    }

    List<String> fastUpdate() {
        List<String> tetheringCommands = new ArrayList<>();
        boolean tetherIptablesRulesIsClean = new PrefManager(context).getBoolPref("TetherIptablesRulesIsClean");

        if (tetherIptablesRulesIsClean) {
            return tetheringCommands;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        addressLocalPC = sharedPreferences.getString("pref_common_local_eth_device_addr", "192.168.0.100");
        apIsOn = new PrefManager(context).getBoolPref("APisON");

        setInterfaceNames();

        String ip6tables = pathVars.getIp6tablesPath();
        String busybox = pathVars.getBusyboxPath();

        tetheringCommands.addAll(Arrays.asList(
                iptables + "-I FORWARD -j DROP",
                ip6tables + "-D INPUT -j DROP 2> /dev/null || true",
                ip6tables + "-I INPUT -j DROP || true",
                ip6tables + "-D FORWARD -j DROP 2> /dev/null || true",
                ip6tables + "-I FORWARD -j DROP",
                iptables + "-t nat -D PREROUTING -j tordnscrypt_prerouting 2> /dev/null || true",
                iptables + "-D FORWARD -j tordnscrypt_forward 2> /dev/null || true",
                busybox + "sleep 1",
                iptables + "-t nat -A PREROUTING -j tordnscrypt_prerouting",
                iptables + "-A FORWARD -j tordnscrypt_forward",
                iptables + "-D FORWARD -j DROP 2> /dev/null || true"
        ));

        return tetheringCommands;
    }

    void setInterfaceNames() {
        final String addressesRangeUSB = "192.168.42.";
        final String addressesRangeWiFi = "192.168.43.";

        usbTetherOn = false;
        ethernetOn = false;

        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements(); ) {

                NetworkInterface intf = en.nextElement();

                if (intf.isLoopback()) {
                    continue;
                }
                if (intf.isVirtual()) {
                    continue;
                }
                if (!intf.isUp()) {
                    continue;
                }

                setVpnInterfaceName(intf);

                if (intf.isPointToPoint()) {
                    continue;
                }
                if (intf.getHardwareAddress() == null) {
                    continue;
                }

                if (intf.getName().replaceAll("\\d+", "").equalsIgnoreCase("eth")) {
                    ethernetOn = true;
                    ethernetInterfaceName = intf.getName();
                    Log.i(LOG_TAG, "LAN interface name " + ethernetInterfaceName);
                }

                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                     enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    String hostAddress = inetAddress.getHostAddress();

                    if (hostAddress.contains(addressesRangeWiFi)) {
                        this.apIsOn = true;
                        wifiAPInterfaceName = intf.getName();
                        Log.i(LOG_TAG, "WiFi AP interface name " + wifiAPInterfaceName);
                    }

                    if (hostAddress.contains(addressesRangeUSB)) {
                        usbTetherOn = true;
                        usbModemInterfaceName = intf.getName();
                        Log.i(LOG_TAG, "USB Modem interface name " + usbModemInterfaceName);
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(LOG_TAG, "Tethering SocketException " + e.getMessage() + " " + e.getCause());
        }

        if (usbTetherOn && !new PrefManager(context).getBoolPref("ModemIsON")) {
            new PrefManager(context).setBoolPref("ModemIsON", true);
            ModulesStatus.getInstance().setIptablesRulesUpdateRequested(context, true);
        } else if (!usbTetherOn && new PrefManager(context).getBoolPref("ModemIsON")) {
            new PrefManager(context).setBoolPref("ModemIsON", false);
            ModulesStatus.getInstance().setIptablesRulesUpdateRequested(context, true);
        }
    }

    private void setVpnInterfaceName(NetworkInterface intf) throws SocketException {

        if (!intf.isPointToPoint()) {
            return;
        }

        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
             enumIpAddr.hasMoreElements(); ) {
            InetAddress inetAddress = enumIpAddr.nextElement();
            String hostAddress = inetAddress.getHostAddress();

            if (hostAddress.contains(addressVPN)) {
                vpnInterfaceName = intf.getName();
                Log.i(LOG_TAG, "VPN interface name " + vpnInterfaceName);
            }
        }
    }

    List<String> fixTTLCommands() {
        new PrefManager(context).setBoolPref("TTLisFixed", true);

        List<String> commands = new ArrayList<>(Arrays.asList(
                iptables + "-I FORWARD -j DROP",
                "echo 64 > /proc/sys/net/ipv4/ip_default_ttl 2> /dev/null || true",
                "ip rule delete from " + wifiAPAddressesRange + " lookup 63 2> /dev/null || true",
                "ip rule delete from " + usbModemAddressesRange + " lookup 62 2> /dev/null || true",
                "ip rule delete from " + addressLocalPC + " lookup 64 2> /dev/null || true",
                "ip route delete default dev " + vpnInterfaceName + " scope link table 63 2> /dev/null || true",
                "ip route delete default dev " + vpnInterfaceName + " scope link table 62 2> /dev/null || true",
                "ip route delete default dev " + vpnInterfaceName + " scope link table 64 2> /dev/null || true",
                "ip route delete " + wifiAPAddressesRange + " dev " + wifiAPInterfaceName + " scope link table 63 2> /dev/null || true",
                "ip route delete " + usbModemAddressesRange + " dev " + usbModemInterfaceName + " scope link table 62 2> /dev/null || true",
                "ip route delete " + addressLocalPC + " dev " + ethernetInterfaceName + " scope link table 64 2> /dev/null || true",
                "ip route delete broadcast 255.255.255.255 dev " + wifiAPInterfaceName + " scope link table 63 2> /dev/null || true",
                "ip route delete broadcast 255.255.255.255 dev " + usbModemInterfaceName + " scope link table 62 2> /dev/null || true",
                "ip route delete broadcast 255.255.255.255 dev " + ethernetInterfaceName + " scope link table 64 2> /dev/null || true",
                iptables + "-D FORWARD -j tordnscrypt_forward 2> /dev/null || true",
                //iptables + "-t nat -D POSTROUTING -o " + vpnInterfaceName + " -j MASQUERADE || true",
                iptables + "-t nat -D tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -p tcp -m tcp --dport 53 -j DNAT --to-destination " + pathVars.getDNSCryptFallbackRes() + " 2> /dev/null || true",
                iptables + "-t nat -D tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -p udp -m udp --dport 53 -j DNAT --to-destination " + pathVars.getDNSCryptFallbackRes() + " 2> /dev/null || true",
                iptables + "-t nat -D tordnscrypt_prerouting -i " + usbModemInterfaceName + " -p tcp -m tcp --dport 53 -j DNAT --to-destination " + pathVars.getDNSCryptFallbackRes() + " 2> /dev/null || true",
                iptables + "-t nat -D tordnscrypt_prerouting -i " + usbModemInterfaceName + " -p udp -m udp --dport 53 -j DNAT --to-destination " + pathVars.getDNSCryptFallbackRes() + " 2> /dev/null || true",
                iptables + "-t nat -D tordnscrypt_prerouting -i " + ethernetInterfaceName + " -p tcp -m tcp --dport 53 -j DNAT --to-destination " + pathVars.getDNSCryptFallbackRes() + " 2> /dev/null || true",
                iptables + "-t nat -D tordnscrypt_prerouting -i " + ethernetInterfaceName + " -p udp -m udp --dport 53 -j DNAT --to-destination " + pathVars.getDNSCryptFallbackRes() + " 2> /dev/null || true",
                iptables + "-t nat -I tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -p tcp -m tcp --dport 53 -j DNAT --to-destination " + pathVars.getDNSCryptFallbackRes(),
                iptables + "-t nat -I tordnscrypt_prerouting -i " + wifiAPInterfaceName + " -p udp -m udp --dport 53 -j DNAT --to-destination " + pathVars.getDNSCryptFallbackRes(),
                iptables + "-t nat -I tordnscrypt_prerouting -i " + usbModemInterfaceName + " -p tcp -m tcp --dport 53 -j DNAT --to-destination " + pathVars.getDNSCryptFallbackRes(),
                iptables + "-t nat -I tordnscrypt_prerouting -i " + usbModemInterfaceName + " -p udp -m udp --dport 53 -j DNAT --to-destination " + pathVars.getDNSCryptFallbackRes(),
                iptables + "-t nat -I tordnscrypt_prerouting -i " + ethernetInterfaceName + " -p tcp -m tcp --dport 53 -j DNAT --to-destination " + pathVars.getDNSCryptFallbackRes(),
                iptables + "-t nat -I tordnscrypt_prerouting -i " + ethernetInterfaceName + " -p udp -m udp --dport 53 -j DNAT --to-destination " + pathVars.getDNSCryptFallbackRes(),
                iptables + "-D tordnscrypt_forward -m state --state ESTABLISHED,RELATED -j RETURN 2> /dev/null && "
                        + iptables + "-I tordnscrypt_forward -m state --state ESTABLISHED,RELATED -j ACCEPT 2> /dev/null || true",
                iptables + "-D tordnscrypt_forward -o !" + vpnInterfaceName + " -j REJECT 2> /dev/null || "
                        + iptables + "-D tordnscrypt_forward -o !tun0 -j REJECT 2> /dev/null || "
                        + iptables + "-D tordnscrypt_forward -o !tun1 -j REJECT 2> /dev/null",
                iptables + "-I tordnscrypt_forward -o !" + vpnInterfaceName + " -j REJECT",
                iptables + "-D tordnscrypt_forward -p all -j ACCEPT 2> /dev/null || true",
                iptables + "-A tordnscrypt_forward -p all -j ACCEPT 2> /dev/null",
                iptables + "-I FORWARD -j tordnscrypt_forward 2> /dev/null",
                //iptables + "-t nat -I POSTROUTING -o " + vpnInterfaceName + " -j MASQUERADE",
                "ip rule add from " + wifiAPAddressesRange + " lookup 63 2> /dev/null || true",
                "ip rule add from " + usbModemAddressesRange + " lookup 62 2> /dev/null || true",
                "ip rule add from " + addressLocalPC + " lookup 64 2> /dev/null || true",
                "ip route add default dev " + vpnInterfaceName + " scope link table 63 || true",
                "ip route add default dev " + vpnInterfaceName + " scope link table 62 || true",
                "ip route add default dev " + vpnInterfaceName + " scope link table 64 || true",
                "ip route add " + wifiAPAddressesRange + " dev " + wifiAPInterfaceName + " scope link table 63 || true",
                "ip route add " + usbModemAddressesRange + " dev " + usbModemInterfaceName + " scope link table 62 || true",
                "ip route add " + addressLocalPC + " dev " + ethernetInterfaceName + " scope link table 64 || true",
                "ip route add broadcast 255.255.255.255 dev " + wifiAPInterfaceName + " scope link table 63 || true",
                "ip route add broadcast 255.255.255.255 dev " + usbModemInterfaceName + " scope link table 62 || true",
                "ip route add broadcast 255.255.255.255 dev " + ethernetInterfaceName + " scope link table 64 || true",
                iptables + "-D FORWARD -j DROP 2> /dev/null || true"
                //iptables + "-D PREROUTING -t mangle -p udp --dport 53 -j MARK --set-mark 111 || true",
                //iptables + "-A PREROUTING -t mangle -p udp --dport 53 -j MARK --set-mark 111",
                //"ip rule add from " + wifiAPAddressesRange + " fwmark 111 lookup 62"
        ));

        return cleanupCommands(commands);
    }

    private List<String> unfixTTLCommands() {
        new PrefManager(context).setBoolPref("TTLisFixed", false);

        List<String> commands;

        if (ethernetOn) {
            commands = new ArrayList<>(Arrays.asList(
                    "ip rule delete from " + wifiAPAddressesRange + " lookup 63 2> /dev/null || true",
                    "ip rule delete from " + usbModemAddressesRange + " lookup 62 2> /dev/null || true",
                    "ip rule delete from " + addressLocalPC + " lookup 64 2> /dev/null || true"
                    ));
        } else {
            commands = new ArrayList<>(Arrays.asList(
                    "ip rule delete from " + wifiAPAddressesRange + " lookup 63 2> /dev/null || true",
                    "ip rule delete from " + usbModemAddressesRange + " lookup 62 2> /dev/null || true"
                    //iptables + "-D tordnscrypt_forward -o !" + vpnInterfaceName + " -j REJECT 2> /dev/null || true",
                    //iptables + "-t nat -D POSTROUTING -o " + vpnInterfaceName + " -j MASQUERADE || true"
            ));
        }

        return commands;
    }

    private List<String> cleanupCommands(List<String> commands) {
        if (!usbTetherOn) {
            for (int i = 0; i < commands.size(); i++) {
                String command = commands.get(i);
                if (command.contains(usbModemInterfaceName) || command.contains(usbModemAddressesRange) || command.contains("table 62")) {
                    commands.set(i, "");
                }
            }
        }

        if (!apIsOn) {
            for (int i = 0; i < commands.size(); i++) {
                String command = commands.get(i);
                if (command.contains(wifiAPInterfaceName) || command.contains(wifiAPAddressesRange) || command.contains("table 63")) {
                    commands.set(i, "");
                }
            }
        }

        if (!ethernetOn || addressLocalPC.trim().isEmpty()) {
            for (int i = 0; i < commands.size(); i++) {
                String command = commands.get(i);
                if (command.contains(ethernetInterfaceName) || command.contains(addressLocalPC) || command.contains("table 64")) {
                    commands.set(i, "");
                }
            }
        }

        return commands;
    }

    private String removeRedundantSymbols(StringBuilder stringBuilder) {
        if (stringBuilder.length() > 2) {
            return stringBuilder.substring(0, stringBuilder.length() - 2);
        } else {
            return "";
        }
    }
}
