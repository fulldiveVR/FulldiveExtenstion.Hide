package pan.alexander.tordnscrypt.modules;

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

import android.content.Context;
import android.content.Intent;
import android.os.Process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pan.alexander.tordnscrypt.settings.PathVars;
import pan.alexander.tordnscrypt.utils.RootCommands;
import pan.alexander.tordnscrypt.utils.RootExecService;

class ContextUIDUpdater {
    private final Context context;
    private final String appDataDir;
    private final String busyboxPath;

    ContextUIDUpdater(Context context) {
        this.context = context;
        PathVars pathVars = PathVars.getInstance(context);
        appDataDir = pathVars.getAppDataDir();
        busyboxPath = pathVars.getBusyboxPath();
    }

    void updateModulesContextAndUID() {

        String appUID = String.valueOf(Process.myUid());
        List<String> commands;
        if (ModulesStatus.getInstance().isUseModulesWithRoot()) {
            commands = new ArrayList<>(Arrays.asList(
                    busyboxPath + "chown -R 0.0 " + appDataDir + "/app_data/dnscrypt-proxy 2> /dev/null",
                    busyboxPath + "chown -R 0.0 " + appDataDir + "/dnscrypt-proxy.pid 2> /dev/null",
                    busyboxPath + "chown -R 0.0 " + appDataDir + "/tor_data 2> /dev/null",
                    busyboxPath + "chown -R 0.0 " + appDataDir + "/tor.pid 2> /dev/null",
                    busyboxPath + "chown -R 0.0 " + appDataDir + "/i2pd_data 2> /dev/null",
                    busyboxPath + "chown -R 0.0 " + appDataDir + "/i2pd.pid 2> /dev/null"
            ));
        } else {
            commands = new ArrayList<>(Arrays.asList(
                    busyboxPath + "chown -R " + appUID + "." + appUID + " " + appDataDir + "/app_data/dnscrypt-proxy 2> /dev/null",
                    busyboxPath + "chown -R " + appUID + "." + appUID + " " + appDataDir + "/dnscrypt-proxy.pid 2> /dev/null",
                    "restorecon -R " + appDataDir + "/app_data/dnscrypt-proxy 2> /dev/null",
                    "restorecon -R " + appDataDir + "/dnscrypt-proxy.pid 2> /dev/null",

                    busyboxPath + "chown -R " + appUID + "." + appUID + " " + appDataDir + "/tor_data 2> /dev/null",
                    busyboxPath + "chown -R " + appUID + "." + appUID + " " + appDataDir + "/tor.pid 2> /dev/null",
                    "restorecon -R " + appDataDir + "/tor_data 2> /dev/null",
                    "restorecon -R " + appDataDir + "/tor.pid 2> /dev/null",

                    busyboxPath + "chown -R " + appUID + "." + appUID + " " + appDataDir + "/i2pd_data 2> /dev/null",
                    busyboxPath + "chown -R " + appUID + "." + appUID + " " + appDataDir + "/i2pd.pid 2> /dev/null",
                    "restorecon -R " + appDataDir + "/i2pd_data 2> /dev/null",
                    "restorecon -R " + appDataDir + "/i2pd.pid 2> /dev/null",

                    busyboxPath + "chown -R " + appUID + "." + appUID + " " + appDataDir + "/logs 2> /dev/null",
                    "restorecon -R " + appDataDir + "/logs 2> /dev/null"
            ));
        }

        RootCommands rootCommands = new RootCommands(commands);
        Intent intent = new Intent(context, RootExecService.class);
        intent.setAction(RootExecService.RUN_COMMAND);
        intent.putExtra("Commands", rootCommands);
        intent.putExtra("Mark", RootExecService.NullMark);
        RootExecService.performAction(context, intent);
    }
}
