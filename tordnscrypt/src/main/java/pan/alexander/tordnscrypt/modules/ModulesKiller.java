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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import eu.chainfire.libsuperuser.Shell;
import pan.alexander.tordnscrypt.settings.PathVars;
import pan.alexander.tordnscrypt.utils.PrefManager;
import pan.alexander.tordnscrypt.utils.RootCommands;
import pan.alexander.tordnscrypt.utils.file_operations.FileOperations;

import static pan.alexander.tordnscrypt.modules.ModulesService.DNSCRYPT_KEYWORD;
import static pan.alexander.tordnscrypt.modules.ModulesService.ITPD_KEYWORD;
import static pan.alexander.tordnscrypt.modules.ModulesService.TOR_KEYWORD;
import static pan.alexander.tordnscrypt.utils.RootExecService.COMMAND_RESULT;
import static pan.alexander.tordnscrypt.utils.RootExecService.DNSCryptRunFragmentMark;
import static pan.alexander.tordnscrypt.utils.RootExecService.I2PDRunFragmentMark;
import static pan.alexander.tordnscrypt.utils.RootExecService.LOG_TAG;
import static pan.alexander.tordnscrypt.utils.RootExecService.TorRunFragmentMark;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.RESTARTING;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.RUNNING;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.STOPPED;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.STOPPING;

public class ModulesKiller {
    private final Service service;
    private final String appDataDir;
    private final String busyboxPath;
    private final String dnscryptPath;
    private final String torPath;
    private final String itpdPath;

    private final ModulesStatus modulesStatus;

    private final ReentrantLock reentrantLock;

    private static Thread dnsCryptThread;
    private static Thread torThread;
    private static Thread itpdThread;

    ModulesKiller(Service service, PathVars pathVars) {
        this.service = service;
        appDataDir = pathVars.getAppDataDir();
        busyboxPath = pathVars.getBusyboxPath();
        dnscryptPath = pathVars.getDNSCryptPath();
        torPath = pathVars.getTorPath();
        itpdPath = pathVars.getITPDPath();
        modulesStatus = ModulesStatus.getInstance();
        reentrantLock = new ReentrantLock();
    }

    public static void stopDNSCrypt(Context context) {
        sendStopIntent(context, ModulesService.actionStopDnsCrypt);
    }

    public static void stopTor(Context context) {
        sendStopIntent(context, ModulesService.actionStopTor);
    }

    public static void stopITPD(Context context) {
        sendStopIntent(context, ModulesService.actionStopITPD);
    }

    private static void sendStopIntent(Context context, String action) {
        ModulesActionSender.INSTANCE.sendIntent(context, action);
    }

    private void sendResultIntent(int moduleMark, String moduleKeyWord, String binaryPath) {
        RootCommands comResult = new RootCommands(new ArrayList<>(Arrays.asList(moduleKeyWord, binaryPath)));
        Intent intent = new Intent(COMMAND_RESULT);
        intent.putExtra("CommandsResult", comResult);
        intent.putExtra("Mark", moduleMark);
        LocalBroadcastManager.getInstance(service).sendBroadcast(intent);
    }

    private void makeDelay(int sec) {
        try {
            TimeUnit.SECONDS.sleep(sec);
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Modules killer makeDelay interrupted! " + e.getMessage() + " " + e.getCause());
        }
    }

    void setDnsCryptThread(Thread dnsCryptThread) {
        ModulesKiller.dnsCryptThread = dnsCryptThread;
    }

    void setTorThread(Thread torThread) {
        ModulesKiller.torThread = torThread;
    }

    void setItpdThread(Thread itpdThread) {
        ModulesKiller.itpdThread = itpdThread;
    }

    Thread getDnsCryptThread() {
        return dnsCryptThread;
    }

    Thread getTorThread() {
        return torThread;
    }

    Thread getItpdThread() {
        return itpdThread;
    }

    Runnable getDNSCryptKillerRunnable() {
        return () -> {

            if (modulesStatus.getDnsCryptState() != RESTARTING) {
                modulesStatus.setDnsCryptState(STOPPING);
            }

            reentrantLock.lock();

            try {
                String dnsCryptPid = readPidFile(appDataDir + "/dnscrypt-proxy.pid");

                boolean moduleStartedWithRoot = new PrefManager(service).getBoolPref("DNSCryptStartedWithRoot");
                boolean rootIsAvailable = modulesStatus.isRootAvailable();

                boolean result = doThreeAttemptsToStopModule(dnscryptPath, dnsCryptPid, dnsCryptThread, moduleStartedWithRoot);

                if (!result) {

                    if (rootIsAvailable) {
                        Log.w(LOG_TAG, "ModulesKiller cannot stop DNSCrypt. Stop with root method!");
                        result = killModule(dnscryptPath, dnsCryptPid, dnsCryptThread, true, "SIGKILL", 10);
                    }

                    if (!moduleStartedWithRoot && !result) {
                        Log.w(LOG_TAG, "ModulesKiller cannot stop DNSCrypt. Stop with interrupt thread!");

                        makeDelay(5);

                        result = stopModuleWithInterruptThread(dnsCryptThread);
                    }
                }

                if (moduleStartedWithRoot) {
                    if (!result) {
                        if (modulesStatus.getDnsCryptState() != RESTARTING) {
                            ModulesAux.saveDNSCryptStateRunning(service, true);
                            sendResultIntent(DNSCryptRunFragmentMark, DNSCRYPT_KEYWORD, dnscryptPath);
                        }

                        modulesStatus.setDnsCryptState(RUNNING);

                        Log.e(LOG_TAG, "ModulesKiller cannot stop DNSCrypt!");

                    } else {
                        if (modulesStatus.getDnsCryptState() != RESTARTING) {
                            ModulesAux.saveDNSCryptStateRunning(service, false);
                            modulesStatus.setDnsCryptState(STOPPED);
                            sendResultIntent(DNSCryptRunFragmentMark, DNSCRYPT_KEYWORD, "");
                        }
                    }
                } else {
                    if (dnsCryptThread != null && dnsCryptThread.isAlive()) {

                        if (modulesStatus.getDnsCryptState() != RESTARTING) {
                            ModulesAux.saveDNSCryptStateRunning(service, true);
                            sendResultIntent(DNSCryptRunFragmentMark, DNSCRYPT_KEYWORD, dnscryptPath);
                        }

                        modulesStatus.setDnsCryptState(RUNNING);

                        Log.e(LOG_TAG, "ModulesKiller cannot stop DNSCrypt!");
                    } else {

                        if (modulesStatus.getDnsCryptState() != RESTARTING) {
                            ModulesAux.saveDNSCryptStateRunning(service, false);
                            modulesStatus.setDnsCryptState(STOPPED);
                            sendResultIntent(DNSCryptRunFragmentMark, DNSCRYPT_KEYWORD, "");
                        }
                    }
                }
            } catch (Exception e){
                Log.e(LOG_TAG, "ModulesKiller getDNSCryptKillerRunnable exception " + e.getMessage() + " " + e.getCause());
            } finally {
                reentrantLock.unlock();
            }

        };
    }


    Runnable getTorKillerRunnable() {
        return () -> {

            if (modulesStatus.getTorState() != RESTARTING) {
                modulesStatus.setTorState(STOPPING);
            }

            reentrantLock.lock();

            try {
                String torPid = readPidFile(appDataDir + "/tor.pid");

                boolean moduleStartedWithRoot = new PrefManager(service).getBoolPref("TorStartedWithRoot");
                boolean rootIsAvailable = modulesStatus.isRootAvailable();

                boolean result = doThreeAttemptsToStopModule(torPath, torPid, torThread, moduleStartedWithRoot);

                if (!result) {

                    if (rootIsAvailable) {
                        Log.w(LOG_TAG, "ModulesKiller cannot stop Tor. Stop with root method!");
                        result = killModule(torPath, torPid, torThread, true, "SIGKILL", 10);
                    }

                    if (!moduleStartedWithRoot && !result) {
                        Log.w(LOG_TAG, "ModulesKiller cannot stop Tor. Stop with interrupt thread!");

                        makeDelay(5);

                        result = stopModuleWithInterruptThread(torThread);
                    }

                }

                if (moduleStartedWithRoot) {
                    if (!result) {
                        if (modulesStatus.getTorState() != RESTARTING) {
                            sendResultIntent(TorRunFragmentMark, TOR_KEYWORD, torPath);
                            ModulesAux.saveTorStateRunning(service, true);
                        }

                        modulesStatus.setTorState(RUNNING);

                        Log.e(LOG_TAG, "ModulesKiller cannot stop Tor!");

                    } else {
                        if (modulesStatus.getTorState() != RESTARTING) {
                            ModulesAux.saveTorStateRunning(service, false);
                            modulesStatus.setTorState(STOPPED);
                            sendResultIntent(TorRunFragmentMark, TOR_KEYWORD, "");
                        }
                    }
                } else {
                    if (torThread != null && torThread.isAlive()) {

                        if (modulesStatus.getTorState() != RESTARTING) {
                            ModulesAux.saveTorStateRunning(service, true);
                            sendResultIntent(TorRunFragmentMark, TOR_KEYWORD, torPath);
                        }

                        modulesStatus.setTorState(RUNNING);

                        Log.e(LOG_TAG, "ModulesKiller cannot stop Tor!");
                    } else {

                        if (modulesStatus.getTorState() != RESTARTING) {
                            ModulesAux.saveTorStateRunning(service, false);
                            modulesStatus.setTorState(STOPPED);
                            sendResultIntent(TorRunFragmentMark, TOR_KEYWORD, "");
                        }
                    }
                }
            } catch (Exception e){
                Log.e(LOG_TAG, "ModulesKiller getTorKillerRunnable exception " + e.getMessage() + " " + e.getCause());
            } finally {
                reentrantLock.unlock();
            }

        };
    }

    Runnable getITPDKillerRunnable() {
        return () -> {

            if (modulesStatus.getItpdState() != RESTARTING) {
                modulesStatus.setItpdState(STOPPING);
            }

            reentrantLock.lock();

            try {
                String itpdPid = readPidFile(appDataDir + "/i2pd.pid");

                boolean moduleStartedWithRoot = new PrefManager(service).getBoolPref("ITPDStartedWithRoot");
                boolean rootIsAvailable = modulesStatus.isRootAvailable();

                boolean result = doThreeAttemptsToStopModule(itpdPath, itpdPid, itpdThread, moduleStartedWithRoot);

                if (!result) {

                    if (rootIsAvailable) {
                        Log.w(LOG_TAG, "ModulesKiller cannot stop I2P. Stop with root method!");
                        result = killModule(itpdPath, itpdPid, itpdThread, true, "SIGKILL", 10);
                    }

                    if (!moduleStartedWithRoot && !result) {
                        Log.w(LOG_TAG, "ModulesKiller cannot stop I2P. Stop with interrupt thread!");

                        makeDelay(5);

                        result = stopModuleWithInterruptThread(itpdThread);
                    }
                }

                if (moduleStartedWithRoot) {
                    if (!result) {
                        if (modulesStatus.getItpdState() != RESTARTING) {
                            ModulesAux.saveITPDStateRunning(service, true);
                            sendResultIntent(I2PDRunFragmentMark, ITPD_KEYWORD, itpdPath);
                        }

                        modulesStatus.setItpdState(RUNNING);

                        Log.e(LOG_TAG, "ModulesKiller cannot stop I2P!");

                    } else {
                        if (modulesStatus.getItpdState() != RESTARTING) {
                            ModulesAux.saveITPDStateRunning(service, false);
                            modulesStatus.setItpdState(STOPPED);
                            sendResultIntent(I2PDRunFragmentMark, ITPD_KEYWORD, "");
                        }

                    }
                }

                if (itpdThread != null && itpdThread.isAlive()) {

                    if (modulesStatus.getItpdState() != RESTARTING) {
                        ModulesAux.saveITPDStateRunning(service, true);
                        sendResultIntent(I2PDRunFragmentMark, ITPD_KEYWORD, itpdPath);
                    }

                    modulesStatus.setItpdState(RUNNING);

                    Log.e(LOG_TAG, "ModulesKiller cannot stop I2P!");
                } else {

                    if (modulesStatus.getItpdState() != RESTARTING) {
                        ModulesAux.saveITPDStateRunning(service, false);
                        modulesStatus.setItpdState(STOPPED);
                        sendResultIntent(I2PDRunFragmentMark, ITPD_KEYWORD, "");
                    }
                }
            } catch (Exception e){
                Log.e(LOG_TAG, "ModulesKiller getITPDKillerRunnable exception " + e.getMessage() + " " + e.getCause());
            } finally {
                reentrantLock.unlock();
            }

        };
    }

    private boolean killModule(String module, String pid, Thread thread, boolean killWithRoot, String signal, int delaySec) {
        boolean result = false;

        if (module.contains("/")) {
            module = module.substring(module.lastIndexOf("/"));
        }

        List<String> preparedCommands = prepareKillCommands(module, pid, signal, killWithRoot);

        if ((thread == null || !thread.isAlive()) && modulesStatus.isRootAvailable()
                || killWithRoot) {

            String sleep = busyboxPath + "sleep " + delaySec;
            String checkString = busyboxPath + "pgrep -l " + module;

            List<String> commands = new ArrayList<>(preparedCommands);
            commands.add(sleep);
            commands.add(checkString);

            List<String> shellResult = killWithSU(module, commands);

            if (shellResult != null) {
                result = !shellResult.toString().toLowerCase().contains(module.toLowerCase().trim());
            }

            if (shellResult != null) {
                Log.i(LOG_TAG, "Kill " + module + " with root: result " + result + "\n" + shellResult.toString());
            } else {
                Log.i(LOG_TAG, "Kill " + module + " with root: result false");
            }
        } else {

            if (!pid.isEmpty()) {
                killWithPid(signal, pid, delaySec);
            }

            if (thread != null) {
                result = !thread.isAlive();
            }

            List<String> shellResult = null;
            if (!result) {
                shellResult = killWithSH(module, preparedCommands, delaySec);

                if (thread != null) {
                    result = !thread.isAlive();
                }
            }

            if (shellResult != null) {
                Log.i(LOG_TAG, "Kill " + module + " without root: result " + result + "\n" + shellResult.toString());
            } else {
                Log.i(LOG_TAG, "Kill " + module + " without root: result " + result);
            }
        }

        return result;
    }

    private void killWithPid(String signal, String pid, int delay) {
        try {
            if (signal.isEmpty()) {
                android.os.Process.sendSignal(Integer.parseInt(pid), 15);
            } else {
                android.os.Process.killProcess(Integer.parseInt(pid));
            }
            makeDelay(delay);
        } catch (Exception e) {
            Log.e(LOG_TAG, "ModulesKiller killWithPid exception " + e.getMessage() + " " + e.getCause());
        }
    }

    @SuppressWarnings("deprecation")
    private List<String> killWithSH(String module, List<String> commands, int delay) {
        List<String> shellResult = null;
        try {
            shellResult = Shell.SH.run(commands);
            makeDelay(delay);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Kill " + module + " without root exception " + e.getMessage() + " " + e.getCause());
        }
        return shellResult;
    }

    @SuppressWarnings("deprecation")
    private List<String> killWithSU(String module, List<String> commands) {
        List<String> shellResult = null;
        try {
            shellResult = Shell.SU.run(commands);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Kill " + module + " with root exception " + e.getMessage() + " " + e.getCause());
        }
        return shellResult;
    }

    //kill default signal SIGTERM - 15, SIGKILL -9, SIGQUIT - 3
    private List<String> prepareKillCommands(String module, String pid, String signal, boolean killWithRoot) {
        List<String> result;

        if (pid.isEmpty() || killWithRoot) {
            String killStringToyBox = "toybox pkill " + module;
            String killString = "pkill " + module;
            String killStringBusybox = busyboxPath + "pkill " + module;
            String killAllStringBusybox = busyboxPath + "kill $(pgrep " + module + ")";
            if (!signal.isEmpty()) {
                killStringToyBox = "toybox pkill -" + signal + " " + module;
                killString = "pkill -" + signal + " " + module;
                killStringBusybox = busyboxPath + "pkill -" + signal + " " + module;
                killAllStringBusybox = busyboxPath + "kill -s " + signal + " $(pgrep " + module + ")";
            }

            result = new ArrayList<>(Arrays.asList(
                    killStringBusybox,
                    killAllStringBusybox,
                    killStringToyBox,
                    killString
            ));
        } else {
            String killAllStringToolBox = "toolbox kill " + pid;
            String killStringToyBox = "toybox kill " + pid;
            String killString = "kill " + pid;
            String killStringBusyBox = busyboxPath + "kill " + pid;
            if (!signal.isEmpty()) {
                killAllStringToolBox = "toolbox kill -s " + signal + " " + pid;
                killStringToyBox = "toybox kill -s " + signal + " " + pid;
                killString = "kill -s " + signal + " " + pid;
                killStringBusyBox = busyboxPath + "kill -s " + signal + " " + pid;
            }

            result = new ArrayList<>(Arrays.asList(
                    killStringBusyBox,
                    killAllStringToolBox,
                    killStringToyBox,
                    killString
            ));
        }

        return result;
    }

    private boolean doThreeAttemptsToStopModule(String modulePath, String pid, Thread thread, boolean moduleStartedWithRoot) {
        boolean result = false;
        int attempts = 0;
        while (attempts < 3 && !result) {
            if (attempts < 2) {
                result = killModule(modulePath, pid, thread, moduleStartedWithRoot, "", attempts + 2);
            } else {
                result = killModule(modulePath, pid, thread, moduleStartedWithRoot, "SIGKILL", attempts + 1);
            }

            attempts++;
        }
        return result;
    }

    private boolean stopModuleWithInterruptThread(Thread thread) {
        boolean result = false;
        int attempts = 0;

        try {
            while (attempts < 3 && !result) {
                if (thread != null && thread.isAlive()) {
                    thread.interrupt();
                    makeDelay(3);
                }

                if (thread != null) {
                    result = !thread.isAlive();
                }

                attempts++;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Kill with interrupt thread exception " + e.getMessage() + " " + e.getCause());
        }

        return result;
    }

    private String readPidFile(String path) {
        String pid = "";

        File file = new File(path);
        if (file.isFile()) {
            List<String> lines = FileOperations.readTextFileSynchronous(service, path);

            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    pid = line.trim();
                    break;
                }
            }
        }
        return pid;
    }

    @SuppressWarnings("deprecation")
    public static void forceCloseApp(PathVars pathVars) {
        ModulesStatus modulesStatus = ModulesStatus.getInstance();
        if (modulesStatus.isRootAvailable()) {

            String iptablesPath = pathVars.getIptablesPath();
            String ip6tablesPath = pathVars.getIp6tablesPath();
            String busyboxPath = pathVars.getBusyboxPath();

            modulesStatus.setUseModulesWithRoot(true);
            modulesStatus.setDnsCryptState(STOPPED);
            modulesStatus.setTorState(STOPPED);
            modulesStatus.setItpdState(STOPPED);

            final String[] commands = new String[]{
                    ip6tablesPath + "-D OUTPUT -j DROP 2> /dev/null || true",
                    ip6tablesPath + "-I OUTPUT -j DROP",
                    iptablesPath + "-t nat -F tordnscrypt_nat_output 2> /dev/null",
                    iptablesPath + "-t nat -D OUTPUT -j tordnscrypt_nat_output 2> /dev/null || true",
                    iptablesPath + "-F tordnscrypt 2> /dev/null",
                    iptablesPath + "-D OUTPUT -j tordnscrypt 2> /dev/null || true",
                    iptablesPath + "-t nat -F tordnscrypt_prerouting 2> /dev/null",
                    iptablesPath + "-F tordnscrypt_forward 2> /dev/null",
                    iptablesPath + "-t nat -D PREROUTING -j tordnscrypt_prerouting 2> /dev/null || true",
                    iptablesPath + "-D FORWARD -j tordnscrypt_forward 2> /dev/null || true",
                    busyboxPath + "killall -s SIGKILL libdnscrypt-proxy.so",
                    busyboxPath + "killall -s SIGKILL libtor.so",
                    busyboxPath + "killall -s SIGKILL libi2pd.so"
            };

            new Thread(() -> Shell.SU.run(commands)).start();
        }
    }
}
