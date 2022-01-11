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

package pan.alexander.tordnscrypt.utils;
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
import android.content.SharedPreferences;

import java.util.LinkedHashSet;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;

public class PrefManager {

    private static final String PREF_NAME = "TorPlusDNSCryptPref";
    private final SharedPreferences sPref;

    public PrefManager(Context context) {
        this.sPref = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
    }

    public boolean getBoolPref(String stringID) {
        return sPref.getBoolean(stringID, false);
    }

    public void setBoolPref(String stringID, boolean boolValue) {
        SharedPreferences.Editor edit = sPref.edit();
        edit.putBoolean(stringID, boolValue);
        edit.apply();
    }

    public String getStrPref(String stringID) {
        return sPref.getString(stringID, "");
    }

    public void setStrPref(String stringID, String stringValue) {
        SharedPreferences.Editor edit = sPref.edit();
        edit.putString(stringID, stringValue);
        edit.apply();
    }

    public void setSetStrPref(String stringID, Set<String> stringSet) {
        SharedPreferences.Editor edit = sPref.edit();
        edit.remove(stringID);
        edit.apply();
        edit.putStringSet(stringID, stringSet);
        edit.apply();
    }

    public Set<String> getSetStrPref(String stringID) {
        Set<String> stringSetDef = new LinkedHashSet<>();
        return sPref.getStringSet(stringID, stringSetDef);
    }

    public float getFloatPref(String floatID) {
        return sPref.getFloat(floatID, 0f);
    }

    public void setFloatPref(String floatID, float floatValue) {
        SharedPreferences.Editor edit = sPref.edit();
        edit.putFloat(floatID, floatValue);
        edit.apply();
    }

    public int getIntPref(String intID) {
        return sPref.getInt(intID, 0);
    }

    public void setIntPref(String intID, int intValue) {
        SharedPreferences.Editor edit = sPref.edit();
        edit.putInt(intID, intValue);
        edit.apply();
    }

    public static String getPrefName() {
        return PREF_NAME;
    }
}
