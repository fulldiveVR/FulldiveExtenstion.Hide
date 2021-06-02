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

package pan.alexander.tordnscrypt.domain.connection_records

import android.content.Context
import androidx.preference.PreferenceManager
import pan.alexander.tordnscrypt.TopFragment
import pan.alexander.tordnscrypt.domain.entities.ConnectionRecord
import pan.alexander.tordnscrypt.iptables.Tethering
import pan.alexander.tordnscrypt.modules.ModulesStatus
import pan.alexander.tordnscrypt.utils.PrefManager
import pan.alexander.tordnscrypt.utils.enums.OperationMode
import pan.alexander.tordnscrypt.vpn.service.ServiceVPNHandler
import java.util.*

private const val MAX_LINES_IN_LOG = 200

class ConnectionRecordsParser(private val applicationContext: Context) {

    private val modulesStatus = ModulesStatus.getInstance()
    private val sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
    private val apIsOn = PrefManager(applicationContext).getBoolPref("APisON")
    private val localEthernetDeviceAddress =
        sharedPreferences.getString("pref_common_local_eth_device_addr", "192.168.0.100")
            ?: "192.168.0.100"

    fun formatLines(connectionRecords: List<ConnectionRecord>): String {

        val fixTTL =
            modulesStatus.isFixTTL && modulesStatus.mode == OperationMode.ROOT_MODE && !modulesStatus.isUseModulesWithRoot

        val lines = StringBuilder()

        lines.append("<br />")

        var start = 0
        val logSize: Int = connectionRecords.size
        if (logSize > MAX_LINES_IN_LOG) {
            start = logSize - MAX_LINES_IN_LOG
        }

        for (i in start until logSize) {

            val record = connectionRecords[i]

            if (TopFragment.appVersion.startsWith("g") && record.blocked && record.blockedByIpv6
                /*remove artifacts*/
                || (record.aName.trim() == "=" || record.qName.trim() == "=")
                && record.uid == -1000
            ) {
                continue
            }

            if (record.blocked) {
                lines.append("<font color=#f08080>")
            } else if (record.uid != -1000 && record.daddr.trim().isNotEmpty()) {
                lines.append("<font color=#E7AD42>")
            } else if (record.unused) {
                lines.append("<font color=#9e9e9e>")
            } else {
                lines.append("<font color=#009688>")
            }

            if (record.uid != -1000) {
                var appName = ""
                val appList = ServiceVPNHandler.getAppsList()
                if (appList != null) {
                    for (rule in appList) {
                        if (rule.uid == record.uid) {
                            appName = rule.appName
                            break
                        }
                    }
                }
                if (appName.isEmpty() || record.uid == 1000) {
                    appName =
                        applicationContext.packageManager.getNameForUid(record.uid) ?: "Undefined"
                }
                if (apIsOn && fixTTL && record.saddr.contains("192.168.43.")) {
                    lines.append("<b>").append("WiFi").append("</b>").append(" -> ")
                } else if (Tethering.usbTetherOn && fixTTL && record.saddr.contains("192.168.42.")) {
                    lines.append("<b>").append("USB").append("</b>").append(" -> ")
                } else if (Tethering.ethernetOn && fixTTL && record.saddr.contains(
                        localEthernetDeviceAddress
                    )
                ) {
                    lines.append("<b>").append("LAN").append("</b>").append(" -> ")
                } else if (appName.isNotEmpty()) {
                    lines.append("<b>").append(appName).append("</b>").append(" -> ")
                } else {
                    lines.append("<b>").append("Unknown UID").append(record.uid).append("</b>")
                        .append(" -> ")
                }
            }

            if (record.aName.trim().isNotEmpty()) {
                lines.append(record.aName.toLowerCase(Locale.ROOT))
                if (record.blocked && record.blockedByIpv6) {
                    lines.append(" ipv6")
                }
            } else if (record.qName.trim().isNotEmpty()) {
                lines.append(record.qName.toLowerCase(Locale.ROOT))
            }

            if (record.cName.trim().isNotEmpty() && record.uid == -1000) {
                lines.append(" -> ").append(record.cName.toLowerCase(Locale.ROOT))
            }
            if (record.daddr.trim().isNotEmpty()
                && (!record.daddr.contains("0.0.0.0")
                        && !record.daddr.contains("127.0.0.1")
                        || record.uid != -1000)
            ) {
                if (record.uid == -1000) {
                    lines.append(" -> ")
                }
                if (record.uid != -1000 && record.reverseDNS.isNotEmpty()) {
                    lines.append(record.reverseDNS).append(" -> ")
                }
                lines.append(record.daddr)
            }
            lines.append("</font>")

            if (i < connectionRecords.size - 1) {
                lines.append("<br />")
            }
        }

        return lines.toString()
    }
}