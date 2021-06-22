package pan.alexander.tordnscrypt.appextension

import android.content.Context
import androidx.preference.PreferenceManager
import pan.alexander.tordnscrypt.modules.ModulesKiller
import pan.alexander.tordnscrypt.modules.ModulesRunner
import pan.alexander.tordnscrypt.modules.ModulesStatus
import pan.alexander.tordnscrypt.utils.PrefManager
import pan.alexander.tordnscrypt.utils.enums.ModuleState

object LaunchHelper {

    val runningStates =
        listOf(ModuleState.STARTING, ModuleState.RESTARTING, ModuleState.RUNNING)

    fun launchVPN(context: Context) {
        val modulesStatus = ModulesStatus.getInstance()
        val state = modulesStatus.torState
        val prefManager = PrefManager(context)
        val hideIp = prefManager.getBoolPref("HideIp")
        val protectDns = prefManager.getBoolPref("ProtectDns")
        val accessITP = prefManager.getBoolPref("AccessITP")

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
        } else {
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
//        Log.d("AppExtensionTest", "getExtensionState $state")
        val states = listOf(
            modulesStatus.torState,
            modulesStatus.dnsCryptState,
            modulesStatus.itpdState
        )
        return when {
            states.any { runningStates.contains(it) } -> {
                AppExtensionState.START
            }
            states.any { ModuleState.FAULT == it } -> {
                AppExtensionState.FAILURE
            }
            else -> AppExtensionState.STOP
        }.toString()
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
        }.toString()
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