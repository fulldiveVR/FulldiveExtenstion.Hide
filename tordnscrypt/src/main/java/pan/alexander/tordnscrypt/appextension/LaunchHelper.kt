/*
 * Copyright (c) 2022 FullDive
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pan.alexander.tordnscrypt.appextension

import android.content.Context
import androidx.preference.PreferenceManager
import pan.alexander.tordnscrypt.modules.ModulesKiller
import pan.alexander.tordnscrypt.modules.ModulesRunner
import pan.alexander.tordnscrypt.modules.ModulesStatus
import pan.alexander.tordnscrypt.utils.PrefManager
import pan.alexander.tordnscrypt.utils.enums.ModuleState

object LaunchHelper {

    val runningStates = listOf(ModuleState.STARTING, ModuleState.RESTARTING, ModuleState.RUNNING)
    val progressStates = listOf(ModuleState.STARTING, ModuleState.STOPPING)

    fun startVPN(context: Context) {
        val modulesStatus = ModulesStatus.getInstance()
        val prefManager = PrefManager(context)
        val hideIp = prefManager.getBoolPref("HideIp")
        val protectDns = prefManager.getBoolPref("ProtectDns")
        val accessITP = prefManager.getBoolPref("AccessITP")

//        context.contentResolver.insert(getContentUri(AppExtensionState.PROGRESS.id), null)

        if (modulesStatus.dnsCryptState == ModuleState.STOPPED && modulesStatus.torState == ModuleState.STOPPED && modulesStatus.itpdState == ModuleState.STOPPED) {
            if (protectDns) {
                switchDNSCrypt(context, modulesStatus)
            }
            if (hideIp) {
                switchTor(context, modulesStatus)
            }
            if (accessITP) {
                switchItp(context, modulesStatus)
            }
        }

        val uri = getContentUri(AppExtensionState.PROGRESS.id)
        context.contentResolver.insert(uri, null)
    }

    fun stopVPN(context: Context) {
        val modulesStatus = ModulesStatus.getInstance()
        val prefManager = PrefManager(context)
        val hideIp = prefManager.getBoolPref("HideIp")
        val protectDns = prefManager.getBoolPref("ProtectDns")
        val accessITP = prefManager.getBoolPref("AccessITP")

        if (!(modulesStatus.dnsCryptState == ModuleState.STOPPED && modulesStatus.torState == ModuleState.STOPPED && modulesStatus.itpdState == ModuleState.STOPPED)) {

            if (protectDns && modulesStatus.dnsCryptState != ModuleState.STOPPED) {
                switchDNSCrypt(context, modulesStatus)
            }
            if (hideIp && modulesStatus.torState != ModuleState.STOPPED) {
                switchTor(context, modulesStatus)
            }
            if (accessITP && modulesStatus.itpdState != ModuleState.STOPPED) {
                switchItp(context, modulesStatus)
            }
        }

        val uri = getContentUri(getNewExtensionState(modulesStatus))
        context.contentResolver.insert(uri, null)
    }

    fun getCurrentExtensionState(modulesStatus: ModulesStatus): String {
        val states = listOf(
            modulesStatus.torState,
            modulesStatus.dnsCryptState,
            modulesStatus.itpdState
        )
        return when {
            isChangingState() -> AppExtensionState.PROGRESS
            states.any { runningStates.contains(it) } -> {
                AppExtensionState.START
            }
            states.any { ModuleState.FAULT == it } -> {
                AppExtensionState.FAILURE
            }
            else -> AppExtensionState.STOP
        }.id
    }

    fun isChangingState(): Boolean {
        val modulesStatus = ModulesStatus.getInstance()
        val states = listOf(
            modulesStatus.torState,
            modulesStatus.dnsCryptState,
            modulesStatus.itpdState
        )
        return states.any { progressStates.contains(it) }
    }

    private fun getNewExtensionState(modulesStatus: ModulesStatus): String {
        val states = listOf(
            modulesStatus.torState,
            modulesStatus.dnsCryptState,
            modulesStatus.itpdState
        )

        return when {
            states.any { runningStates.contains(it) } -> {
                AppExtensionState.STOP
            }
            states.any { ModuleState.FAULT == it } -> {
                AppExtensionState.FAILURE
            }
            else -> AppExtensionState.START
        }.id
    }

    private fun switchDNSCrypt(context: Context, modulesStatus: ModulesStatus) {
        if (modulesStatus.dnsCryptState != ModuleState.RUNNING) {
            if (!modulesStatus.isTorReady) {
                allowSystemDNS(context, modulesStatus)
            }
            ModulesRunner.runDNSCrypt(context)
        } else {
            ModulesKiller.stopDNSCrypt(context)
        }
    }

    private fun switchTor(context: Context, modulesStatus: ModulesStatus) {
        if (modulesStatus.torState != ModuleState.RUNNING) {
            if (!modulesStatus.isDnsCryptReady) {
                allowSystemDNS(context, modulesStatus)
            }
            ModulesRunner.runTor(context)
        } else if (modulesStatus.torState == ModuleState.RUNNING) {
            ModulesKiller.stopTor(context)
        }
    }

    private fun switchItp(context: Context, modulesStatus: ModulesStatus) {
        if (modulesStatus.dnsCryptState != ModuleState.RUNNING) {
            ModulesRunner.runITPD(context)
        } else {
            ModulesKiller.stopITPD(context)
        }
    }

    private fun allowSystemDNS(context: Context, modulesStatus: ModulesStatus) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        if ((!modulesStatus.isRootAvailable || !modulesStatus.isUseModulesWithRoot)
            && !sharedPreferences.getBoolean("ignore_system_dns", false)
        ) {
            modulesStatus.isSystemDNSAllowed = true
        }
    }
}