package pan.alexander.tordnscrypt.dialogs;
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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import pan.alexander.tordnscrypt.R;
import pan.alexander.tordnscrypt.utils.PrefManager;

import static pan.alexander.tordnscrypt.utils.RootExecService.LOG_TAG;

public class NotificationHelper extends ExtendedDialogFragment {

    private String tag = "";
    private static String message = "";
    public static final String TAG_HELPER = "pan.alexander.tordnscrypt.HELPER_NOTIFICATION";
    private static NotificationHelper notificationHelper = null;

    @Override
    public AlertDialog.Builder assignBuilder() {

        Activity activity = getActivity();
        if (activity == null) {
            return null;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.CustomAlertDialogTheme);
        builder.setMessage(message)
                .setTitle(R.string.helper_dialog_title)
                .setPositiveButton(R.string.ok, (dialog, which) -> notificationHelper = null)
                .setNegativeButton(R.string.dont_show, (dialog, id) -> {
                    new PrefManager(activity).setBoolPref("helper_no_show_" + tag, true);
                    notificationHelper = null;
                    dismiss();
                });

        return builder;
    }

    public static NotificationHelper setHelperMessage(Context context, String message, String preferenceTag) {

        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if ((!new PrefManager(context).getBoolPref("helper_no_show_" + preferenceTag)
                    || sharedPreferences.getBoolean("pref_common_show_help", false)
                    || preferenceTag.matches("\\d+"))
                    && notificationHelper == null) {
                notificationHelper = new NotificationHelper();
                notificationHelper.tag = preferenceTag;
                NotificationHelper.message = message;
                return notificationHelper;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "NotificationHelper exception " + e.getMessage() + " " + e.getCause());
        }

        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        notificationHelper = null;
    }
}
