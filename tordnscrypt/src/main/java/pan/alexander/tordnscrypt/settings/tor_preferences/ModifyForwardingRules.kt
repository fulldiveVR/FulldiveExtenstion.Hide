package pan.alexander.tordnscrypt.settings.tor_preferences

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

import android.content.Context
import android.util.Log
import pan.alexander.tordnscrypt.modules.ModulesRestarter
import pan.alexander.tordnscrypt.modules.ModulesStatus
import pan.alexander.tordnscrypt.settings.PathVars
import pan.alexander.tordnscrypt.utils.RootExecService.LOG_TAG
import pan.alexander.tordnscrypt.utils.enums.ModuleState
import java.io.File
import java.io.PrintWriter

class ModifyForwardingRules(private val context: Context,
                            private val lineToReplaceTo: String) {

    private val forwardingRulesFile: String = PathVars.getInstance(context).dnsCryptForwardingRulesPath
    private val localForwardingRulesFile: String = PathVars.getInstance(context).dnsCryptLocalForwardingRulesPath
    private val cacheDir: String = PathVars.getInstance(context).appDataDir + "/cache/"
    private val tempFile: String = "$cacheDir/tmpForwardingRules.txt"
    private val lineToFindRegExp = Regex("^onion +127.0.0.1:\\d+$")

    fun getRunnable() : Runnable {
        return Runnable {
            try {
                val dir = File(cacheDir)
                if (!dir.isDirectory) {
                    dir.mkdirs()
                }

                replaceLineInFile(forwardingRulesFile, tempFile)
                replaceLineInFile(localForwardingRulesFile, tempFile)

                restartDNSCryptIfRequired()

            } catch (e: java.lang.Exception) {
                Log.e(LOG_TAG, "ImportRules Exception " + e.message + " " + e.cause)
            }
        }
    }

    private fun replaceLineInFile(inputFilePath: String, tmpOutputFilePath: String) {
        try {

            val inputFile = File(inputFilePath)
            val outputFile = File(tmpOutputFilePath)

            if (inputFile.isFile) {
                outputFile.printWriter().use {printWriter ->
                    writeToFile(inputFile, printWriter)
                }

                outputFile.copyTo(inputFile, true)
                outputFile.delete()
            }

        } catch (e: Exception) {
            Log.e(LOG_TAG, "ImportRules Exception " + e.message + " " + e.cause)
        }
    }

    private fun writeToFile(inputFile: File, printWriter: PrintWriter) {

        try {

            inputFile.bufferedReader().use {
                var line = it.readLine()?.trim()
                while (line != null && !Thread.currentThread().isInterrupted) {

                    if (line.matches(lineToFindRegExp)) {
                        printWriter.println(lineToReplaceTo)
                    } else if (line.isNotEmpty()) {
                        printWriter.println(line)
                    }
                    line = it.readLine()?.trim()
                }
            }

        } catch (e: Exception) {
            Log.e(LOG_TAG, "ModifyForwardingRules " + e.message + " " + e.cause)
        }
    }

    private fun restartDNSCryptIfRequired() {
        if (ModulesStatus.getInstance().dnsCryptState == ModuleState.RUNNING) {
            ModulesRestarter.restartDNSCrypt(context)
        }
    }
}