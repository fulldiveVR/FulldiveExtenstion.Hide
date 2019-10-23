package pan.alexander.tordnscrypt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Arrays;

import pan.alexander.tordnscrypt.utils.PrefManager;
import pan.alexander.tordnscrypt.utils.RootCommands;
import pan.alexander.tordnscrypt.utils.RootExecService;
import pan.alexander.tordnscrypt.utils.ZipUtil.ZipFileManager;
import pan.alexander.tordnscrypt.utils.fileOperations.FileOperations;

import static pan.alexander.tordnscrypt.utils.RootExecService.LOG_TAG;

public class HelpActivityReceiver extends BroadcastReceiver {
    private Handler mHandler;
    private String appDataDir;
    private String info;
    private String pathToSaveLogs;
    private DialogInterface progressDialog;

    public HelpActivityReceiver(Handler mHandler, String appDataDir, String pathToSaveLogs) {
        this.mHandler = mHandler;
        this.appDataDir = appDataDir;
        this.pathToSaveLogs = pathToSaveLogs;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(LOG_TAG, "BackupActivity onReceive");

        if (!isBroadcastMatch(intent)) {
            return;
        }

        RootCommands comResult = (RootCommands) intent.getSerializableExtra("CommandsResult");

        if (comResult.getCommands().length == 0) {
            closeProgressBar();
            showSomethingWrongToast(context);
            return;
        }

        Thread thread = new Thread(saveLogs(context, comResult));
        thread.start();
    }

    Runnable saveLogs(final Context context, final RootCommands comResult) {
        return new Runnable() {
            @Override
            public void run() {
                deleteRootExecLog(context);

                if (isRootMethodWroteLogs(comResult)) {
                    saveLogsMethodOne(context);
                } else {
                    saveLogsMethodTwo(context);
                }

                if (isLogsExist()) {
                    FileOperations.moveBinaryFile(context, appDataDir
                            + "/logs", "InvizibleLogs.txt", pathToSaveLogs, "InvizibleLogs.txt");
                } else {
                    closeProgressBar();
                    showSomethingWrongToast(context);
                    Log.e(LOG_TAG, "Collect logs alternative method fault");
                }
            }
        };
    }

    private void saveLogsMethodOne(Context context) {
        try {
            ZipFileManager zipFileManager = new ZipFileManager(appDataDir + "/logs/InvizibleLogs.txt");
            zipFileManager.createZip(appDataDir + "/logs_dir");
            FileOperations.deleteDirSynchronous(context,appDataDir + "/logs_dir");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Create zip file for first method failed  " + e.getMessage() + " " + e.getCause());
        }
    }

    private void saveLogsMethodTwo(Context context) {
        Log.e(LOG_TAG, "Collect logs first method fault");
        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            StringBuilder log = new StringBuilder();
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    log.append(line);
                    log.append(System.lineSeparator());
                }
            }

            try (FileWriter out = new FileWriter(appDataDir + "/logs/InvizibleLogs.txt")) {
                out.write(log.toString());

                if (info != null) {
                    out.write(info);
                }
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, "Collect logs alternative method fault " + e.getMessage() + " " + e.getCause());
            showSomethingWrongToast(context);
        }
    }

    private void deleteRootExecLog(Context context) {
        if (new PrefManager(context).getBoolPref("swRootCommandsLog")) {
            FileOperations.deleteFileSynchronous(context, appDataDir + "/logs", "RootExec.log");
            Log.e(LOG_TAG, "deleteFile function fault " + appDataDir + "logs/RootExec.log");
        }
    }

    private boolean isBroadcastMatch(Intent intent) {
        if (intent == null) {
            return false;
        }

        String action = intent.getAction();

        if ((action == null) || (action.equals(""))) {
            return false;
        }

        if (!action.equals(RootExecService.COMMAND_RESULT)) {
            return false;
        }

        return intent.getIntExtra("Mark", 0) == RootExecService.HelpActivityMark;
    }

    private void closeProgressBar() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null)
                    progressDialog.dismiss();
            }
        });
    }

    private void showSomethingWrongToast(final Context context) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, R.string.wrong, Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isRootMethodWroteLogs(RootCommands comResult) {
        File invizibleLogs = new File(appDataDir + "/logs_dir");

        return Arrays.toString(comResult.getCommands()).contains("Logs Saved")
                && invizibleLogs.exists()
                && invizibleLogs.list().length > 0;
    }

    private boolean isLogsExist() {
        File invizibleLogs = new File(appDataDir + "/logs/InvizibleLogs.txt");
        return invizibleLogs.exists();
    }

    public void setPathToSaveLogs(String pathToSaveLogs) {
        this.pathToSaveLogs = pathToSaveLogs;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public void setProgressDialog(DialogInterface progressDialog) {
        this.progressDialog = progressDialog;
    }
}