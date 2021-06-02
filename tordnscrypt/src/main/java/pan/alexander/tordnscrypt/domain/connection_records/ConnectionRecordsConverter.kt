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
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import pan.alexander.tordnscrypt.domain.entities.ConnectionRecord
import pan.alexander.tordnscrypt.modules.ModulesStatus
import pan.alexander.tordnscrypt.settings.firewall.APPS_ALLOW_GSM_PREF
import pan.alexander.tordnscrypt.settings.firewall.APPS_ALLOW_LAN_PREF
import pan.alexander.tordnscrypt.settings.firewall.APPS_ALLOW_ROAMING
import pan.alexander.tordnscrypt.settings.firewall.APPS_ALLOW_WIFI_PREF
import pan.alexander.tordnscrypt.settings.tor_apps.ApplicationData
import pan.alexander.tordnscrypt.utils.CachedExecutor
import pan.alexander.tordnscrypt.utils.PrefManager
import pan.alexander.tordnscrypt.utils.RootExecService.LOG_TAG
import pan.alexander.tordnscrypt.utils.Utils.getHostByIP
import pan.alexander.tordnscrypt.utils.enums.OperationMode
import pan.alexander.tordnscrypt.vpn.Util
import pan.alexander.tordnscrypt.vpn.service.ServiceVPN
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Future

private const val REVERSE_LOOKUP_QUEUE_CAPACITY = 100
private const val IP_TO_HOST_ADDRESS_MAP_SIZE = 500

class ConnectionRecordsConverter(context: Context) {

    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val blockIPv6: Boolean = sharedPreferences.getBoolean("block_ipv6", true)
    private var compatibilityMode = sharedPreferences.getBoolean("swCompatibilityMode", false)
    private val meteredNetwork = Util.isMeteredNetwork(context)
    private val vpnDNS = ServiceVPN.vpnDnsSet
    private val modulesStatus = ModulesStatus.getInstance()
    private val fixTTL = (modulesStatus.isFixTTL && modulesStatus.mode == OperationMode.ROOT_MODE
            && !modulesStatus.isUseModulesWithRoot)

    private val dnsQueryLogRecords = ArrayList<ConnectionRecord>()
    private val dnsQueryLogRecordsSublist = ArrayList<ConnectionRecord>()
    private val reverseLookupQueue = ArrayBlockingQueue<String>(REVERSE_LOOKUP_QUEUE_CAPACITY, true)
    private val ipToHostAddressMap = mutableMapOf<String, String>()
    private var futureTask: Future<*>? = null

    private val firewallEnabled = PrefManager(context).getBoolPref("FirewallEnabled")
    private var appsAllowed = mutableSetOf<Int>()
    private val appsLanAllowed = mutableListOf<Int>()

    init {
        if (firewallEnabled) {
            PrefManager(context).getSetStrPref(APPS_ALLOW_LAN_PREF).forEach { appsLanAllowed.add(it.toInt()) }

            var tempSet: MutableSet<String>? = null
            if (Util.isWifiActive(context) || Util.isEthernetActive(context)) {
                tempSet = PrefManager(context).getSetStrPref(APPS_ALLOW_WIFI_PREF)
            } else if (Util.isCellularActive(context)) {
                tempSet = PrefManager(context).getSetStrPref(APPS_ALLOW_GSM_PREF)
            } else if (Util.isRoaming(context)) {
                tempSet = PrefManager(context).getSetStrPref(APPS_ALLOW_ROAMING)
            }

            tempSet?.forEach { appsAllowed.add(it.toInt()) }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            compatibilityMode = true
        }
    }

    fun convertRecords(dnsQueryRawRecords: List<ConnectionRecord?>): List<ConnectionRecord> {

        dnsQueryLogRecords.clear()

        startReverseLookupQueue()

        dnsQueryRawRecords.forEach { addRecord(it) }

        return dnsQueryLogRecords
    }

    private fun addRecord(dnsQueryRawRecord: ConnectionRecord?) {

        if (dnsQueryRawRecord == null) {
            return
        }

        if (dnsQueryLogRecords.isNotEmpty()) {
            if (dnsQueryRawRecord.uid != -1000) {
                addUID(dnsQueryRawRecord)
                return
            } else if (isIdenticalRecord(dnsQueryRawRecord)) {
                return
            }
        }

        setQueryBlocked(dnsQueryRawRecord)

        if (dnsQueryRawRecord.blocked) {
            dnsQueryLogRecords.removeAll { it == dnsQueryRawRecord }
        }

        dnsQueryLogRecords.add(dnsQueryRawRecord)
    }

    private fun isIdenticalRecord(dnsQueryRawRecord: ConnectionRecord): Boolean {

        for (i in dnsQueryLogRecords.size - 1 downTo 0) {
            val record = dnsQueryLogRecords[i]

            if (dnsQueryRawRecord.aName == record.aName
                    && dnsQueryRawRecord.qName == record.qName
                    && dnsQueryRawRecord.hInfo == record.hInfo
                    && dnsQueryRawRecord.rCode == record.rCode
                    && dnsQueryRawRecord.saddr == record.saddr) {

                if (dnsQueryRawRecord.daddr.isNotEmpty() && record.daddr.isNotEmpty()) {
                    if (!record.daddr.contains(dnsQueryRawRecord.daddr.trim())) {
                        dnsQueryLogRecords[i] = record.apply { daddr = daddr + ", " + dnsQueryRawRecord.daddr.trim() }
                    }
                    return true
                }
            }
        }

        return false
    }

    private fun addUID(dnsQueryRawRecord: ConnectionRecord) {
        var savedRecord: ConnectionRecord? = null
        dnsQueryLogRecordsSublist.clear()

        val uidBlocked = if (firewallEnabled) {
            if (compatibilityMode && dnsQueryRawRecord.uid == ApplicationData.SPECIAL_UID_KERNEL
                    || fixTTL && dnsQueryRawRecord.uid == ApplicationData.SPECIAL_UID_KERNEL) {
                false
            }  else if (isIpInLanRange(dnsQueryRawRecord.daddr)) {
                !appsLanAllowed.contains(dnsQueryRawRecord.uid)
            } else {
                !appsAllowed.contains(dnsQueryRawRecord.uid)
            }
        } else {
            false
        }

        for (index in dnsQueryLogRecords.size - 1 downTo 0) {
            val record = dnsQueryLogRecords[index]
            if (savedRecord == null && record.daddr.contains(dnsQueryRawRecord.daddr) && record.uid == -1000) {
                record.blocked = uidBlocked
                record.unused = false
                dnsQueryLogRecordsSublist.add(record)
                savedRecord = record
            } else if (savedRecord != null && savedRecord.aName == record.cName) {
                record.blocked = uidBlocked
                record.unused = false
                dnsQueryLogRecordsSublist.add(record)
                savedRecord = record
            } else if (savedRecord != null && savedRecord.aName != record.cName) {
                break
            }
        }

        if (savedRecord != null) {

            val dnsQueryNewRecord = ConnectionRecord(savedRecord.qName, savedRecord.aName, savedRecord.cName,
                    savedRecord.hInfo, -1, dnsQueryRawRecord.saddr, "", dnsQueryRawRecord.uid)
            dnsQueryNewRecord.blocked = uidBlocked
            dnsQueryNewRecord.unused = false

            dnsQueryLogRecordsSublist.add(dnsQueryNewRecord)

        } else if (vpnDNS != null && !vpnDNS.contains(dnsQueryRawRecord.daddr)) {

            if (!meteredNetwork && dnsQueryRawRecord.daddr.isNotEmpty()) {
                val host = ipToHostAddressMap[dnsQueryRawRecord.daddr]

                if (host == null) {
                    makeReverseLookup(dnsQueryRawRecord.daddr)
                } else if (host != dnsQueryRawRecord.daddr) {
                    dnsQueryRawRecord.reverseDNS = host
                }
            }

            dnsQueryRawRecord.blocked = uidBlocked

            dnsQueryRawRecord.unused = false

            dnsQueryLogRecords.removeAll { it == dnsQueryRawRecord }
            dnsQueryLogRecords.add(dnsQueryRawRecord)
        }

        if (dnsQueryLogRecordsSublist.isNotEmpty()) {
            dnsQueryLogRecords.removeAll(dnsQueryLogRecordsSublist)
            dnsQueryLogRecords.addAll(dnsQueryLogRecordsSublist.reversed())
        }
    }

    private fun makeReverseLookup(ip: String) {
        if (!reverseLookupQueue.contains(ip)) {
            reverseLookupQueue.offer(ip)
        }
    }

    private fun startReverseLookupQueue() {

        if(futureTask?.isDone == false) {
            return
        }

        futureTask = CachedExecutor.getExecutorService().submit {
            try {

                while (!Thread.currentThread().isInterrupted) {
                    val ip = reverseLookupQueue.take()

                    val host = getHostByIP(ip)

                    if (ipToHostAddressMap.size > IP_TO_HOST_ADDRESS_MAP_SIZE) {
                        val pairs = ipToHostAddressMap.toList()
                        ipToHostAddressMap.clear()
                        ipToHostAddressMap.plus(pairs.subList(pairs.size/2, pairs.size))
                    }

                    ipToHostAddressMap[ip] = host
                }

            } catch (ignored: InterruptedException) {
            } catch (exception: Exception) {
                Log.e(LOG_TAG, "DNSQueryLogRecordsConverter reverse lookup exception " + exception.message + " " + exception.cause)
            }
        }
    }

    private fun setQueryBlocked(dnsQueryRawRecord: ConnectionRecord): Boolean {

        if (dnsQueryRawRecord.daddr == "0.0.0.0"
                || dnsQueryRawRecord.daddr == "127.0.0.1"
                || dnsQueryRawRecord.daddr == "::"
                || dnsQueryRawRecord.daddr.contains(":") && blockIPv6
                || dnsQueryRawRecord.hInfo.contains("dnscrypt")
                || dnsQueryRawRecord.rCode != 0) {

            dnsQueryRawRecord.blockedByIpv6 = (dnsQueryRawRecord.hInfo.contains("block_ipv6")
                    || dnsQueryRawRecord.daddr == "::"
                    || dnsQueryRawRecord.daddr.contains(":") && blockIPv6)

            dnsQueryRawRecord.blocked = true
            dnsQueryRawRecord.unused = false

        } else if (dnsQueryRawRecord.daddr.isBlank()
                && dnsQueryRawRecord.cName.isBlank()
                && !dnsQueryRawRecord.aName.contains(".in-addr.arpa")) {
            dnsQueryRawRecord.blocked = true
            dnsQueryRawRecord.unused = false
        } else {
            dnsQueryRawRecord.unused = true
        }

        return dnsQueryRawRecord.blocked
    }

    fun onStop() {
        futureTask?.let {
            if (!it.isDone) {
                it.cancel(true)
                futureTask = null
            }
        }
    }

    private fun isIpInLanRange(destAddress: String): Boolean {
        if (destAddress.isBlank()) {
            return false
        }

        for (address in Util.nonTorList) {
            if (Util.isIpInSubnet(destAddress, address)) {
                return true
            }
        }
        return false
    }
}
