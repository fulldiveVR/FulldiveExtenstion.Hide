package pan.alexander.tordnscrypt.dialogs;

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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import pan.alexander.tordnscrypt.R;
import pan.alexander.tordnscrypt.utils.PrefManager;

public class AgreementDialog {
    public static AlertDialog.Builder getDialogBuilder(Context context) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context, R.style.CustomDialogTheme);

        LayoutInflater lInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (lInflater == null) {
            new PrefManager(context).setBoolPref("Agreement", true);
            return null;
        }

        View view = lInflater.inflate(R.layout.agreement_layout, null, false);

        if (view == null) {
            new PrefManager(context).setBoolPref("Agreement", true);
            return null;
        }

        alertDialog.setView(view);

        alertDialog.setCancelable(false);

        alertDialog.setPositiveButton(R.string.agree, (dialog, id) -> {
            new PrefManager(context).setBoolPref("Agreement", true);
            dialog.dismiss();
        });
        alertDialog.setNegativeButton(R.string.disagree, ((dialog, id) -> System.exit(0)));

        return alertDialog;
    }
}
