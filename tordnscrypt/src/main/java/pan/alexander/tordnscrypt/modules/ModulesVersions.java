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

package pan.alexander.tordnscrypt.modules;

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
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.jrummyapps.android.shell.Shell;
import com.jrummyapps.android.shell.ShellNotFoundException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pan.alexander.tordnscrypt.settings.PathVars;
import pan.alexander.tordnscrypt.utils.CachedExecutor;
import pan.alexander.tordnscrypt.utils.RootCommands;

import static pan.alexander.tordnscrypt.utils.RootExecService.COMMAND_RESULT;
import static pan.alexander.tordnscrypt.utils.RootExecService.DNSCryptRunFragmentMark;
import static pan.alexander.tordnscrypt.utils.RootExecService.I2PDRunFragmentMark;
import static pan.alexander.tordnscrypt.utils.RootExecService.LOG_TAG;
import static pan.alexander.tordnscrypt.utils.RootExecService.TorRunFragmentMark;

public class ModulesVersions {
    private static volatile ModulesVersions holder;

    private String dnsCryptVersion = "";
    private String torVersion = "";
    private String itpdVersion = "";

    private Shell.Console console;

    private ModulesVersions() {
    }

    public static ModulesVersions getInstance() {
        if (holder == null) {
            synchronized (ModulesVersions.class) {
                if (holder == null) {
                    holder = new ModulesVersions();
                }
            }
        }
        return holder;
    }

    public void refreshVersions(final Context context) {

        CachedExecutor.INSTANCE.getExecutorService().submit(() -> {
            //openCommandShell();

            PathVars pathVars = getPathVars(context);

            //checkModulesVersions(pathVars);
            checkModulesVersionsModern(pathVars);

            if (isBinaryFileAccessible(pathVars.getDNSCryptPath()) && !dnsCryptVersion.isEmpty()) {
                sendResult(context, dnsCryptVersion, DNSCryptRunFragmentMark);
            }

            if (isBinaryFileAccessible(pathVars.getTorPath()) && !torVersion.isEmpty()) {
                sendResult(context, torVersion, TorRunFragmentMark);
            }

            if (isBinaryFileAccessible(pathVars.getITPDPath()) && !itpdVersion.isEmpty()) {
                sendResult(context, itpdVersion, I2PDRunFragmentMark);
            }

            //closeCommandShell();
        });
    }

    private PathVars getPathVars(Context context) {
        return PathVars.getInstance(context);
    }

    private boolean isBinaryFileAccessible(String path) {
        File file = new File(path);
        return file.isFile() && file.canExecute();
    }

    private void sendResult(Context context, String version, int mark){

        if (version == null) {
            return;
        }

        RootCommands comResult = new RootCommands(new ArrayList<>(Collections.singletonList(version)));
        Intent intent = new Intent(COMMAND_RESULT);
        intent.putExtra("CommandsResult",comResult);
        intent.putExtra("Mark",mark);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void checkModulesVersions(PathVars pathVars) {
        if (console == null || console.isClosed()) {
            return;
        }

        dnsCryptVersion = console.run(
                "echo 'DNSCrypt_version'",
                pathVars.getDNSCryptPath() + " --version")
                .getStdout();

        torVersion = console.run(
                "echo 'Tor_version'",
                pathVars.getTorPath() + " --version")
                .getStdout();

        itpdVersion = console.run(
                "echo 'ITPD_version'",
                pathVars.getITPDPath() + " --version")
                .getStdout();
    }

    private void checkModulesVersionsModern(PathVars pathVars) {

        List<String> dnsCryptOutput = new ProcessStarter().startProcess(pathVars.getDNSCryptPath() + " --version").stdout;
        if (!dnsCryptOutput.isEmpty()) {
            dnsCryptVersion = "DNSCrypt_version " + dnsCryptOutput.get(0);
        }

        List<String> torOutput = new ProcessStarter().startProcess(pathVars.getTorPath() + " --version").stdout;
        if (!torOutput.isEmpty()) {
            torVersion = "Tor_version " + torOutput.get(0);
        }

        List<String> itpdOutput = new ProcessStarter().startProcess(pathVars.getITPDPath() + " --version").stdout;
        if (!itpdOutput.isEmpty()) {
            itpdVersion = "ITPD_version " + itpdOutput.get(0);
        }
    }

    private void openCommandShell() {
        closeCommandShell();

        try {
            console = Shell.SH.getConsole();
        } catch (ShellNotFoundException e) {
            Log.e(LOG_TAG, "ModulesStatus: SH shell not found! " + e.getMessage() + e.getCause());
        }
    }

    private void closeCommandShell() {

        if (console != null && !console.isClosed()) {
            console.run("exit");
            console.close();
        }
        console = null;
    }

}
