package pan.alexander.tordnscrypt.backup

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

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import pan.alexander.tordnscrypt.R
import pan.alexander.tordnscrypt.installer.Installer
import pan.alexander.tordnscrypt.modules.ModulesStatus
import pan.alexander.tordnscrypt.utils.CachedExecutor
import pan.alexander.tordnscrypt.utils.PrefManager
import pan.alexander.tordnscrypt.utils.RootExecService.LOG_TAG
import java.lang.ref.WeakReference

class ResetHelper(activity: Activity, backupFragment: BackupFragment) : Installer(activity) {

    private var activityWeakReference: WeakReference<Activity> = WeakReference(activity)
    private val backupFragmentWeakReference: WeakReference<BackupFragment> = WeakReference(backupFragment)

    fun resetSettings() {
        CachedExecutor.getExecutorService().submit {
            try {

                registerReceiver(activityWeakReference.get())

                if (ModulesStatus.getInstance().isUseModulesWithRoot) {
                    stopAllRunningModulesWithRootCommand()
                } else {
                    stopAllRunningModulesWithNoRootCommand()
                }

                check(waitUntilAllModulesStopped()) { "Unexpected interruption" }

                check(!interruptInstallation) { "Installation interrupted" }

                unRegisterReceiver(activityWeakReference.get())

                removeInstallationDirsIfExists()
                createLogsDir()

                extractDNSCrypt()
                extractTor()
                extractITPD()

                chmodExtractedDirs()

                correctAppDir()

                val code = saveSomeOldInfo(activityWeakReference.get())
                val defaultSharedPref = PreferenceManager.getDefaultSharedPreferences(activityWeakReference.get())
                resetSharedPreferences(defaultSharedPref)
                val sharedPreferences = activityWeakReference.get()?.getSharedPreferences(PrefManager.getPrefName(), Context.MODE_PRIVATE)
                resetSharedPreferences(sharedPreferences)
                restoreOldInfo(activityWeakReference.get(), code)

                savePreferencesModulesInstalled(true)

                refreshModulesStatus(activityWeakReference.get())

            } catch (e: Exception) {
                backupFragmentWeakReference.get()?.showToast(activityWeakReference.get()?.getString(R.string.wrong))

                Log.e(LOG_TAG, "ResetHelper resetSettings exception ${e.message} ${e.cause} ${
                    e.stackTrace.joinToString { "," }
                }")
            } finally {
                backupFragmentWeakReference.get()?.closePleaseWaitDialog()
            }
        }
    }

    private fun saveSomeOldInfo(context: Context?): String? {
        var code: String? = ""
        if (context?.getString(R.string.appVersion)?.endsWith("o") == true) {
            code = PrefManager(context).getStrPref("registrationCode")
        }
        return code
    }

    private fun restoreOldInfo(context: Context?, code: String?) {
        if (code?.isNotEmpty() == true) {
            context?.let { PrefManager(it).setStrPref("registrationCode", code) }
        }
    }

    private fun resetSharedPreferences(sharedPref: SharedPreferences?) {
        sharedPref?.edit()?.apply {
            clear()
            apply()
            Log.i(LOG_TAG, "ResetHelper resetSharedPreferences OK")
        }
    }

    fun setActivity(activity: Activity) {
        activityWeakReference = WeakReference(activity)
    }
}