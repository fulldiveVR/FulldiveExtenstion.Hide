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

package pan.alexander.tordnscrypt.dialogs;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.Locale;

import pan.alexander.tordnscrypt.BuildConfig;
import pan.alexander.tordnscrypt.R;
import pan.alexander.tordnscrypt.appextension.StringUtils;
import pan.alexander.tordnscrypt.utils.PrefManager;

public class BackgroundSettingsDialog {
    public static AlertDialog.Builder getDialogBuilder(Context context) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context, R.style.CustomDialogTheme);

        LayoutInflater lInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (lInflater == null) {
            new PrefManager(context).setBoolPref("BatterySettings", true);
            return null;
        }

        View view = lInflater.inflate(R.layout.background_settings_layout, null, false);

        if (view == null) {
            new PrefManager(context).setBoolPref("BatterySettings", true);
            return null;
        }

        boolean isMeizu = (Build.MANUFACTURER.toLowerCase(Locale.ROOT).equals("meizu"));
        String string;
        if (isMeizu) {
            string = context.getString(R.string.first_time_battery_meizu);
        } else {
            string = context.getString(R.string.first_time_battery);
        }

        TextView textView = view.findViewById(R.id.textViewBattery);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        if (textView != null) {
            textView.setText(string);
        }
        alertDialog.setView(view);

        alertDialog.setCancelable(false);

        alertDialog.setPositiveButton(R.string.add_button, (dialog, id) -> {
            new PrefManager(context).setBoolPref("BatterySettings", true);
            if (isMeizu) {
                openFlymeSecurityApp(context);
            } else {
                checkDoze(context);
            }
            dialog.dismiss();
        });

        alertDialog.setNegativeButton(R.string.cancel, (dialog, id) -> {
            dialog.dismiss();
        });

        return alertDialog;
    }

    private static void checkDoze(Context context) {
        Intent intent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            PackageManager packageManager = context.getPackageManager();
            if (packageManager != null) {
                ResolveInfo info = packageManager.resolveActivity(intent, 0);
                if (info != null) {
                    try {
                        context.startActivity(intent);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    private static void openFlymeSecurityApp(Context context) {
        Intent intent = new Intent("com.meizu.safe.security.SHOW_APPSEC");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra("packageName", BuildConfig.APPLICATION_ID);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
