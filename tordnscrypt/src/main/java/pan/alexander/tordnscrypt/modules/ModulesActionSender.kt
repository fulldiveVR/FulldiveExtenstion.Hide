/*
 * This file is part of InviZible Pro.
 *     InviZible Pro is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *     InviZible Pro is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *     You should have received a copy of the GNU General Public License
 *     along with InviZible Pro.  If not, see <http://www.gnu.org/licenses/>.
 *     Copyright 2019-2022 by Garmatin Oleksandr invizible.soft@gmail.com
 */

package pan.alexander.tordnscrypt.modules

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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import pan.alexander.tordnscrypt.ApplicationBase
import pan.alexander.tordnscrypt.utils.Utils.isShowNotification
import java.lang.ref.WeakReference

object ModulesActionSender {
    fun sendIntent(context: Context, action: String) {

        val intent = Intent(context, ModulesService::class.java)
        intent.action = action

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra("showNotification", true)

            var isActivityActive = false
            if (context.applicationContext is ApplicationBase) {
                val applicationExt = context.applicationContext as ApplicationBase
                val currentActivity: WeakReference<Activity>? = applicationExt.currentActivity
                isActivityActive = currentActivity?.get()?.isFinishing == false
            }

            if (isActivityActive) {
                context.startService(intent)
            } else {
                context.startForegroundService(intent)
            }
        } else {
            intent.putExtra("showNotification", isShowNotification(context))
            context.startService(intent)
        }
    }
}
