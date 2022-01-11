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

package pan.alexander.tordnscrypt.tor_fragment;

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

import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.ScaleGestureDetector;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import java.util.Arrays;

import pan.alexander.tordnscrypt.MainActivity;
import pan.alexander.tordnscrypt.R;
import pan.alexander.tordnscrypt.TopFragment;
import pan.alexander.tordnscrypt.dialogs.NotificationDialogFragment;
import pan.alexander.tordnscrypt.dialogs.NotificationHelper;
import pan.alexander.tordnscrypt.domain.CheckConnectionInteractor;
import pan.alexander.tordnscrypt.domain.TorInteractorInterface;
import pan.alexander.tordnscrypt.domain.check_connection.OnInternetConnectionCheckedListener;
import pan.alexander.tordnscrypt.domain.entities.LogDataModel;
import pan.alexander.tordnscrypt.domain.LogReaderInteractors;
import pan.alexander.tordnscrypt.domain.log_reader.tor.OnTorLogUpdatedListener;
import pan.alexander.tordnscrypt.modules.ModulesAux;
import pan.alexander.tordnscrypt.modules.ModulesKiller;
import pan.alexander.tordnscrypt.modules.ModulesRunner;
import pan.alexander.tordnscrypt.modules.ModulesStatus;
import pan.alexander.tordnscrypt.settings.PreferencesFastFragment;
import pan.alexander.tordnscrypt.utils.CachedExecutor;
import pan.alexander.tordnscrypt.utils.GetIPsJobService;
import pan.alexander.tordnscrypt.utils.PrefManager;
import pan.alexander.tordnscrypt.utils.TorRefreshIPsWork;
import pan.alexander.tordnscrypt.utils.Verifier;
import pan.alexander.tordnscrypt.utils.enums.ModuleState;
import pan.alexander.tordnscrypt.vpn.service.ServiceVPNHelper;

import static pan.alexander.tordnscrypt.TopFragment.TOP_BROADCAST;
import static pan.alexander.tordnscrypt.TopFragment.appVersion;
import static pan.alexander.tordnscrypt.TopFragment.wrongSign;
import static pan.alexander.tordnscrypt.utils.RootExecService.LOG_TAG;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.FAULT;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.RESTARTING;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.RUNNING;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.STARTING;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.STOPPED;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.STOPPING;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.UNDEFINED;
import static pan.alexander.tordnscrypt.utils.enums.OperationMode.ROOT_MODE;

public class TorFragmentPresenter implements TorFragmentPresenterInterface,
        OnTorLogUpdatedListener, OnInternetConnectionCheckedListener {

    public TorFragmentView view;

    private final int mJobId = PreferencesFastFragment.mJobId;
    private int refreshPeriodHours = 12;

    private final ModulesStatus modulesStatus = ModulesStatus.getInstance();
    private ModuleState fixedModuleState = STOPPED;
    private Context context;

    private volatile boolean torLogAutoScroll = true;

    private ScaleGestureDetector scaleGestureDetector;

    private TorInteractorInterface torInteractor;
    private volatile LogDataModel savedLogData = null;
    private volatile int savedLinesLength;
    private boolean fixedTorReady;
    private boolean fixedTorError;

    private CheckConnectionInteractor checkConnectionInteractor;

    public TorFragmentPresenter(TorFragmentView view) {
        this.view = view;
    }

    public void onStart() {
        if (!isActive()) {
            return;
        }

        context = view.getFragmentActivity();

        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(context);
        String refreshPeriod = shPref.getString("pref_fast_site_refresh_interval", "12");
        if (refreshPeriod != null) {
            refreshPeriodHours = Integer.parseInt(refreshPeriod);
        }

        if (isTorInstalled()) {
            setTorInstalled(true);

            ModuleState currentModuleState = modulesStatus.getTorState();

            if (currentModuleState == RUNNING || ModulesAux.isTorSavedStateRunning(context)) {

                if (isTorReady()) {
                    setTorRunning();
                    setTorProgressBarIndeterminate(false);
                    setFixedReadyState(true);
                    setFixedErrorState(false);
                } else {
                    setTorStarting();
                    setTorProgressBarIndeterminate(true);
                }

            } else if (currentModuleState == STARTING || currentModuleState == RESTARTING) {
                setTorStarting();
                setTorProgressBarIndeterminate(true);
            } else if (currentModuleState == STOPPING) {
                setTorStopping();
                setTorProgressBarIndeterminate(true);
            } else if (currentModuleState == FAULT) {
                setTorSomethingWrong();
                setTorProgressBarIndeterminate(false);
            } else if (currentModuleState == STOPPED) {
                setTorProgressBarIndeterminate(false);
                setTorStopped();
                //modulesStatus.setTorState(STOPPED);
            }

            if (currentModuleState != STOPPED
                    && currentModuleState != FAULT) {
                displayLog();
            }
        } else {
            setTorInstalled(false);
        }

        registerZoomGestureDetector();
    }

    public void onStop() {

        if (view == null) {
            return;
        }

        if (!view.getFragmentActivity().isChangingConfigurations()) {
            stopDisplayLog();

            fixedModuleState = STOPPED;
            torLogAutoScroll = true;
            scaleGestureDetector = null;
            torInteractor = null;
            savedLogData = null;
            savedLinesLength = 0;
            fixedTorReady = false;
            fixedTorError = false;
            checkConnectionInteractor = null;
        }

        view = null;
    }

    @Override
    public boolean isTorInstalled() {
        return new PrefManager(context).getBoolPref("Tor Installed");
    }

    private void setTorInstalled(boolean installed) {
        if (!isActive()) {
            return;
        }

        if (installed) {
            setTorStartButtonEnabled(true);
        } else {
            view.setTorStatus(R.string.tvTorNotInstalled, R.color.textModuleStatusColorAlert);
        }
    }

    private void setTorStarting(int percents) {
        if (isActive()) {
            view.setTorStatus(context.getText(R.string.tvTorConnecting) + " " + percents + "%", R.color.textModuleStatusColorStarting);
        }
    }

    private void setTorStarting() {
        if (isActive()) {
            view.setTorStatus(R.string.tvTorStarting, R.color.textModuleStatusColorStarting);
        }
    }

    private void setTorRunning() {
        if (isActive()) {
            view.setTorStatus(R.string.tvTorRunning, R.color.textModuleStatusColorRunning);
            view.setStartButtonText(R.string.btnTorStop);
        }
    }

    private void setTorStopping() {
        if (isActive()) {
            view.setTorStatus(R.string.tvTorStopping, R.color.textModuleStatusColorStopping);
        }
    }

    @Override
    public void setTorStopped() {
        if (!isActive()) {
            return;
        }

        stopRefreshTorUnlockIPs();

        view.setTorStatus(R.string.tvTorStop, R.color.textModuleStatusColorStopped);
        view.setStartButtonText(R.string.btnTorStart);
        view.setTorLogViewText();

        setFixedReadyState(false);
        setFixedErrorState(false);

        showNewTorIdentityIcon(false);
    }

    @Override
    public void setTorSomethingWrong() {
        if (isActive()) {
            view.setTorStatus(R.string.wrong, R.color.textModuleStatusColorAlert);
            modulesStatus.setTorState(FAULT);
        }
    }

    @Override
    public void refreshTorState() {

        if (!isActive()) {
            return;
        }

        ModuleState currentModuleState = modulesStatus.getTorState();

        if ((currentModuleState.equals(fixedModuleState)) && currentModuleState != STOPPED) {
            return;
        }

        if (currentModuleState == RUNNING || currentModuleState == STARTING) {

            if (isFixedReadyState()) {
                setTorRunning();
                setTorProgressBarIndeterminate(false);
            } else {
                setTorStarting();
                setTorProgressBarIndeterminate(true);
            }

            ServiceVPNHelper.prepareVPNServiceIfRequired(view.getFragmentActivity(), modulesStatus);

            setTorStartButtonEnabled(true);

            ModulesAux.saveTorStateRunning(context, true);

            view.setStartButtonText(R.string.btnTorStop);
        } else if (currentModuleState == RESTARTING) {
            setTorStarting();
            setTorProgressBarIndeterminate(true);
            setFixedReadyState(false);
        } else if (currentModuleState == STOPPING) {
            setTorStopping();
            setTorProgressBarIndeterminate(true);
        } else if (currentModuleState == STOPPED) {
            stopDisplayLog();

            if (ModulesAux.isTorSavedStateRunning(context)) {
                setTorStoppedBySystem();
            } else {
                setTorStopped();
            }

            setTorProgressBarIndeterminate(false);

            ModulesAux.saveTorStateRunning(context, false);

            setTorStartButtonEnabled(true);
        }

        fixedModuleState = currentModuleState;
    }

    private void setTorStoppedBySystem() {

        setTorStopped();

        if (isActive()) {

            modulesStatus.setTorState(STOPPED);

            ModulesAux.requestModulesStatusUpdate(context);

            FragmentManager fragmentManager = view.getFragmentFragmentManager();
            if (fragmentManager != null) {
                DialogFragment notification = NotificationDialogFragment.newInstance(R.string.helper_tor_stopped);
                notification.show(fragmentManager, "NotificationDialogFragment");
            }

            Log.e(LOG_TAG, context.getString(R.string.helper_tor_stopped));
        }

    }

    @Override
    public synchronized void displayLog() {

        if (torInteractor == null) {
            torInteractor = LogReaderInteractors.Companion.getInteractor();
        }

        torInteractor.addOnTorLogUpdatedListener(this);

        savedLogData = null;

        savedLinesLength = 0;

        if (checkConnectionInteractor == null) {
            checkConnectionInteractor = new CheckConnectionInteractor();
            checkConnectionInteractor.setListener(this);
        }
    }

    @Override
    public void stopDisplayLog() {
        if (torInteractor != null) {
            torInteractor.removeOnTorLogUpdatedListener(this);
        }

        savedLogData = null;

        savedLinesLength = 0;

        if (checkConnectionInteractor != null) {
            checkConnectionInteractor.removeListener();
            checkConnectionInteractor = null;
        }
    }

    @Override
    public void onTorLogUpdated(@NonNull LogDataModel torLogData) {
        final String lastLines = torLogData.getLines();

        int linesLength = lastLines.length();

        if (torLogData.equals(savedLogData) && savedLinesLength == linesLength) {
            return;
        }

        if (lastLines.isEmpty()) {
            return;
        }

        Spanned htmlText = Html.fromHtml(lastLines);

        if (!isActive() || htmlText == null) {
            return;
        }

        view.getFragmentActivity().runOnUiThread(() -> {

            if (!isActive()) {
                return;
            }

            if (savedLinesLength != linesLength && torLogAutoScroll) {
                view.setTorLogViewText(htmlText);
                view.scrollTorLogViewToBottom();

                savedLinesLength = linesLength;
            }

            if (torLogData.equals(savedLogData)) {
                return;
            }

            savedLogData = torLogData;

            if (isFixedReadyState() && !isTorReady() && !isUseModulesWithRoot()) {
                setFixedReadyState(false);
            }

            torStartingSuccessfully(torLogData);

            if (torLogData.getStartedWithError()) {
                torStartingWithError(torLogData);
            }

            refreshTorState();

        });
    }

    private void torStartingSuccessfully(LogDataModel logData) {

        if (isFixedReadyState() || !isActive()) {
            return;
        }

        int percents = logData.getPercents();

        if (!logData.getStartedSuccessfully()) {

            if (percents < 0) {
                return;
            }

            if (modulesStatus.getTorState() == STOPPED || modulesStatus.getTorState() == STOPPING) {
                return;
            }

            setTorProgressBarPercents(percents);

            setTorStarting(percents);

        } else if (modulesStatus.getTorState() == RUNNING) {

            setTorProgressBarIndeterminate(false);

            setTorRunning();

            displayLog();

            showNewTorIdentityIcon(true);

            checkInternetAvailable();
        }
    }

    private void torStartingWithError(LogDataModel logData) {
        if (isFixedReadyState() || isFixedErrorState() || !isActive()) {
            return;
        }

        FragmentManager fragmentManager = view.getFragmentFragmentManager();
        if (fragmentManager == null) {
            return;
        }

        Log.e(LOG_TAG, "Problem bootstrapping Tor: " + logData.getLines());

        int percents = logData.getPercents();

        NotificationHelper notificationHelper;
        if (percents <= 5) {
            notificationHelper = NotificationHelper.setHelperMessage(
                    context, context.getString(R.string.helper_dnscrypt_no_internet), "helper_dnscrypt_no_internet");
        } else {
            notificationHelper = NotificationHelper.setHelperMessage(
                    context, context.getString(R.string.helper_tor_use_bridges), "helper_tor_use_bridges");
        }
        if (notificationHelper != null) {
            notificationHelper.show(fragmentManager, NotificationHelper.TAG_HELPER);
        }

        setFixedErrorState(true);
    }

    private void checkInternetAvailable() {
        if (isActive() && checkConnectionInteractor != null && !checkConnectionInteractor.isChecking()) {
            checkConnectionInteractor.checkConnection("https://www.torproject.org/", true);
        }
    }

    @Override
    public void onConnectionChecked(boolean available) {
        Log.i(LOG_TAG, "Tor connection is checked. " + (available ? "Tor ready." : "Tor not ready."));

        if (!available) {
            return;
        }

        setFixedReadyState(true);
        setFixedErrorState(false);

        startRefreshTorUnlockIPs();

        /////////////////Check Updates///////////////////////////////////////////////
        if (isActive() && view.getFragmentActivity() instanceof MainActivity) {
            checkInvizibleUpdates((MainActivity) view.getFragmentActivity());
        }
    }

    private void checkInvizibleUpdates(MainActivity activity) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean throughTorUpdate = sharedPreferences.getBoolean("pref_fast through_tor_update", false);
        boolean autoUpdate = sharedPreferences.getBoolean("pref_fast_auto_update", true)
                && !appVersion.startsWith("l") && !appVersion.endsWith("p") && !appVersion.startsWith("f");

        String lastUpdateResult = new PrefManager(context).getStrPref("LastUpdateResult");

        if (autoUpdate &&
                (throughTorUpdate || lastUpdateResult.isEmpty()
                        || lastUpdateResult.equals(context.getString(R.string.update_check_warning_menu)))) {
            FragmentManager fragmentManager = activity.getSupportFragmentManager();
            TopFragment topFragment = (TopFragment) fragmentManager.findFragmentByTag("topFragmentTAG");
            if (topFragment != null) {
                topFragment.checkUpdates(activity);
            }
        }
    }

    private void startRefreshTorUnlockIPs() {
        if (!isActive()) {
            return;
        }

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP || refreshPeriodHours == 0) {
            TorRefreshIPsWork torRefreshIPsWork = new TorRefreshIPsWork(context, null);
            torRefreshIPsWork.refreshIPs();
        } else {
            ComponentName jobService = new ComponentName(context, GetIPsJobService.class);
            JobInfo.Builder getIPsJobBuilder;
            getIPsJobBuilder = new JobInfo.Builder(mJobId, jobService);
            getIPsJobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
            getIPsJobBuilder.setPeriodic(refreshPeriodHours * 60 * 60 * 1000);

            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            if (jobScheduler != null) {
                jobScheduler.schedule(getIPsJobBuilder.build());
            }
        }
    }

    private void stopRefreshTorUnlockIPs() {

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP || refreshPeriodHours == 0) {
            return;
        }

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            jobScheduler.cancel(mJobId);
        }
    }

    @Override
    public void setTorInstalling() {
        if (isActive()) {
            view.setTorStatus(R.string.tvTorInstalling, R.color.textModuleStatusColorInstalling);
        }
    }

    @Override
    public void setTorInstalled() {
        if (isActive()) {
            view.setTorStatus(R.string.tvTorInstalled, R.color.textModuleStatusColorInstalled);
        }
    }

    @Override
    public void setTorStartButtonEnabled(boolean enabled) {
        if (isActive()) {
            view.setTorStartButtonEnabled(enabled);
        }
    }

    @Override
    public void setTorProgressBarIndeterminate(boolean indeterminate) {
        if (isActive()) {
            view.setTorProgressBarIndeterminate(indeterminate);

            if (indeterminate) {
                view.setTorProgressBarProgress(100);
            } else {
                view.setTorProgressBarProgress(0);
            }
        }
    }

    private void setTorProgressBarPercents(int percents) {
        view.setTorProgressBarIndeterminate(false);
        view.setTorProgressBarProgress(percents);
    }

    private boolean isTorReady() {
        return modulesStatus.isTorReady();
    }

    public void startButtonOnClick() {

        if (!isActive()) {
            return;
        }

        Activity activity = view.getFragmentActivity();

        if (activity instanceof MainActivity && ((MainActivity) activity).childLockActive) {
            Toast.makeText(activity, activity.getText(R.string.action_mode_dialog_locked), Toast.LENGTH_LONG).show();
            return;
        }


        setTorStartButtonEnabled(false);

        if (!isActive()) {
            return;
        }

        CachedExecutor.INSTANCE.getExecutorService().submit(() -> {

            if (!isActive() || activity == null) {
                return;
            }

            FragmentManager fragmentManager = view.getFragmentFragmentManager();
            if (fragmentManager == null) {
                return;
            }

            try {
                Verifier verifier = new Verifier(activity);
                String appSign = verifier.getApkSignatureZip();
                String appSignAlt = verifier.getApkSignature();
                if (!verifier.decryptStr(wrongSign, appSign, appSignAlt).equals(TOP_BROADCAST)) {
                }

            } catch (Exception e) {
                Log.e(LOG_TAG, "TorRunFragment fault " + e.getMessage() + " " + e.getCause() + System.lineSeparator() +
                        Arrays.toString(e.getStackTrace()));
            }
        });

        if (modulesStatus.getTorState() != RUNNING) {

            if (modulesStatus.isContextUIDUpdateRequested()
                    || modulesStatus.getTorState() == UNDEFINED) {
                Toast.makeText(context, R.string.please_wait, Toast.LENGTH_SHORT).show();
                setTorStartButtonEnabled(true);
                return;
            }

            setTorStarting();

            runTor();

            displayLog();
        } else if (modulesStatus.getTorState() == RUNNING) {

            stopRefreshTorUnlockIPs();

            setTorStopping();
            stopTor();
        }

        setTorProgressBarIndeterminate(true);

    }

    private void runTor() {
        if (isActive()) {
            if (!modulesStatus.isDnsCryptReady()) {
                allowSystemDNS();
            }
            ModulesRunner.runTor(context);
        }
    }

    private void stopTor() {
        if (isActive()) {
            ModulesKiller.stopTor(context);
        }
    }

    public void torLogAutoScrollingAllowed(boolean allowed) {
        torLogAutoScroll = allowed;
    }

    private void showNewTorIdentityIcon(boolean show) {
        if (isActive() && view.getFragmentActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) view.getFragmentActivity();
            mainActivity.showNewTorIdentityIcon(show);
        }
    }

    private void registerZoomGestureDetector() {

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
                setLogsTextSize(scaleGestureDetector.getScaleFactor());
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
            }
        });
    }

    private void setLogsTextSize(float scale) {
        float logsTextSizeMin = context.getResources().getDimension(R.dimen.fragment_log_text_size);
        float logsTextSize = (float) Math.max(logsTextSizeMin, Math.min(TopFragment.logsTextSize * scale, logsTextSizeMin * 1.5));
        TopFragment.logsTextSize = logsTextSize;

        if (view != null) {
            view.setLogsTextSize(logsTextSize);
        }
    }

    public ScaleGestureDetector getScaleGestureDetector() {
        return scaleGestureDetector;
    }

    @Override
    public synchronized boolean isActive() {

        if (view != null) {
            Activity activity = view.getFragmentActivity();
            if (activity != null) {
                return !activity.isFinishing();
            }
        }

        return false;
    }

    private void allowSystemDNS() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if ((!modulesStatus.isRootAvailable() || !modulesStatus.isUseModulesWithRoot())
                && !sharedPreferences.getBoolean("ignore_system_dns", false)) {
            modulesStatus.setSystemDNSAllowed(true);
        }
    }

    private synchronized boolean isFixedReadyState() {
        return fixedTorReady;
    }

    private synchronized void setFixedReadyState(boolean ready) {
        this.fixedTorReady = ready;
    }

    private synchronized boolean isFixedErrorState() {
        return fixedTorError;
    }

    private synchronized void setFixedErrorState(boolean error) {
        this.fixedTorError = error;
    }

    private boolean isUseModulesWithRoot() {
        return modulesStatus.isUseModulesWithRoot() && modulesStatus.getMode() == ROOT_MODE;
    }
}
