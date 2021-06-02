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

package pan.alexander.tordnscrypt.data

import android.util.Log
import pan.alexander.tordnscrypt.utils.RootExecService.LOG_TAG
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

private const val CONNECT_TIMEOUT = 1

class HtmlReader(val port: Int) {

    private var con: HttpURLConnection? = null

    fun readLines(): List<String> {

        var lines = emptyList<String>()

        try {
            lines = tryReadLines()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "HtmlReader exception ${e.message} ${e.cause}")
        } finally {
            con?.disconnect()
        }

        return lines
    }

    private fun tryReadLines(): List<String> {
        val lines = mutableListOf<String>()

        val url = URL("http://127.0.0.1:$port/")
        con = url.openConnection() as HttpURLConnection

        val connection = con ?: return emptyList()

        connection.apply {
            requestMethod = "GET"
            setRequestProperty(
                "User-Agent", "Mozilla/5.0 (Linux; Android 9.0.1; " +
                        "Mi Mi) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Mobile Safari/537.36"
            )
            connectTimeout = CONNECT_TIMEOUT * 1000
            connect()
        }

        val code = connection.responseCode
        if (code != HttpURLConnection.HTTP_OK) {
            return lines
        }

        connection.inputStream.bufferedReader().use { reader ->
            var line = reader.readLine()
            while (line != null) {
                lines.add(line)
                line = reader.readLine()
            }
        }

        return lines
    }
}