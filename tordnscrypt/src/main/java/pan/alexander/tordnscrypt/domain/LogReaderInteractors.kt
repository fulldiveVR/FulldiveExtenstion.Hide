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

package pan.alexander.tordnscrypt.domain

import pan.alexander.tordnscrypt.data.ConnectionRecordsRepositoryImpl
import pan.alexander.tordnscrypt.data.ModulesLogRepositoryImpl
import pan.alexander.tordnscrypt.domain.connection_records.ConnectionRecordsInteractor
import pan.alexander.tordnscrypt.domain.connection_records.OnConnectionRecordsUpdatedListener
import pan.alexander.tordnscrypt.domain.log_reader.LogReaderLoop
import pan.alexander.tordnscrypt.domain.log_reader.dnscrypt.DNSCryptInteractor
import pan.alexander.tordnscrypt.domain.log_reader.dnscrypt.OnDNSCryptLogUpdatedListener
import pan.alexander.tordnscrypt.domain.log_reader.itpd.ITPDHtmlInteractor
import pan.alexander.tordnscrypt.domain.log_reader.itpd.ITPDInteractor
import pan.alexander.tordnscrypt.domain.log_reader.itpd.OnITPDHtmlUpdatedListener
import pan.alexander.tordnscrypt.domain.log_reader.itpd.OnITPDLogUpdatedListener
import pan.alexander.tordnscrypt.domain.log_reader.tor.OnTorLogUpdatedListener
import pan.alexander.tordnscrypt.domain.log_reader.tor.TorInteractor

class LogReaderInteractors private constructor() :
    DNSCryptInteractorInterface,
    TorInteractorInterface,
    ITPDInteractorInterface,
    ConnectionRecordsInteractorInterface {

    private val modulesLogRepository = ModulesLogRepositoryImpl()
    private val connectionsRepository = ConnectionRecordsRepositoryImpl()

    private val dnsCryptInteractor = DNSCryptInteractor(modulesLogRepository)
    private val torInteractor = TorInteractor(modulesLogRepository)
    private val itpdInteractor = ITPDInteractor(modulesLogRepository)
    private val itpdHtmlInteractor = ITPDHtmlInteractor(modulesLogRepository)
    private val connectionRecordsInteractor = ConnectionRecordsInteractor(connectionsRepository)

    private val logReaderLoop = LogReaderLoop(
        dnsCryptInteractor,
        torInteractor,
        itpdInteractor,
        itpdHtmlInteractor,
        connectionRecordsInteractor
    )

    companion object {
        @Volatile
        var logReaderInteractors: LogReaderInteractors? = null

        fun getInteractor(): LogReaderInteractors {
            if (logReaderInteractors == null) {
                synchronized(LogReaderInteractors::class.java) {
                    if (logReaderInteractors == null) {
                        logReaderInteractors = LogReaderInteractors()
                    }
                }
            }
            return logReaderInteractors ?: LogReaderInteractors()
        }
    }

    override fun addOnDNSCryptLogUpdatedListener(onDNSCryptLogUpdatedListener: OnDNSCryptLogUpdatedListener) {
        dnsCryptInteractor.addListener(onDNSCryptLogUpdatedListener)
        logReaderLoop.startLogsParser()
    }

    override fun removeOnDNSCryptLogUpdatedListener(onDNSCryptLogUpdatedListener: OnDNSCryptLogUpdatedListener) {
        dnsCryptInteractor.removeListener(onDNSCryptLogUpdatedListener)
    }

    override fun addOnTorLogUpdatedListener(onTorLogUpdatedListener: OnTorLogUpdatedListener) {
        torInteractor.addListener(onTorLogUpdatedListener)
        logReaderLoop.startLogsParser()
    }

    override fun removeOnTorLogUpdatedListener(onTorLogUpdatedListener: OnTorLogUpdatedListener) {
        torInteractor.removeListener(onTorLogUpdatedListener)
    }

    override fun addOnITPDLogUpdatedListener(onITPDLogUpdatedListener: OnITPDLogUpdatedListener) {
        itpdInteractor.addListener(onITPDLogUpdatedListener)
        logReaderLoop.startLogsParser()
    }

    override fun removeOnITPDLogUpdatedListener(onITPDLogUpdatedListener: OnITPDLogUpdatedListener) {
        itpdInteractor.removeListener(onITPDLogUpdatedListener)
    }

    override fun addOnITPDHtmlUpdatedListener(onITPDHtmlUpdatedListener: OnITPDHtmlUpdatedListener) {
        itpdHtmlInteractor.addListener(onITPDHtmlUpdatedListener)
        logReaderLoop.startLogsParser()
    }

    override fun removeOnITPDHtmlUpdatedListener(onITPDHtmlUpdatedListener: OnITPDHtmlUpdatedListener) {
        itpdHtmlInteractor.removeListener(onITPDHtmlUpdatedListener)
    }

    override fun addOnConnectionRecordsUpdatedListener(onConnectionRecordsUpdatedListener: OnConnectionRecordsUpdatedListener) {
        connectionRecordsInteractor.addListener(onConnectionRecordsUpdatedListener)
        logReaderLoop.startLogsParser()
    }

    override fun removeOnConnectionRecordsUpdatedListener(onConnectionRecordsUpdatedListener: OnConnectionRecordsUpdatedListener) {
        connectionRecordsInteractor.removeListener(onConnectionRecordsUpdatedListener)
    }

    override fun clearConnectionRecords() {
        connectionRecordsInteractor.clearConnectionRecords()
    }
}
