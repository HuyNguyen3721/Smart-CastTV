package com.google.android.gms.ads.ez.appupdate;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.ez.EzApplication;
import com.google.android.gms.ads.ez.R;
import com.google.android.gms.ads.ez.SharedPreferencesUtils;
import com.google.android.gms.ads.ez.remote.AppConfigs;
import com.google.android.gms.ads.ez.remote.RemoteKey;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;


public class AppUpdate {

    private static AppUpdate instance;
    private int typeUpdate = -1;
    private static int APP_UPDATE_REQUEST_CODE = 1991;
    private static String KEY_SKIP_UPDATE = "skip update";
    private AppUpdateManager appUpdateManager;
    int priorityUpdate = 0;

    private AppUpdate() {
    }

    public static AppUpdate getInstance() {
        if (instance == null) {
            synchronized (AppUpdate.class) {
                if (instance == null) {
                    instance = new AppUpdate();
                }
            }
        }
        return instance;
    }

    public void checkUpdateOnStore() {

        Activity currentActivity = EzApplication.getInstance().getCurrentActivity();
        appUpdateManager = AppUpdateManagerFactory.create(currentActivity);
        priorityUpdate = AppConfigs.getInt(RemoteKey.PRIORITY_UPDATE);

        InstallStateUpdatedListener installStateUpdatedListener = new InstallStateUpdatedListener() {
            @Override
            public void onStateUpdate(@NonNull InstallState installState) {
                Log.d("Appupdate", "onStateUpdate");
                if (installState.installStatus() == InstallStatus.DOWNLOADED) {
                    Log.d("Appupdate", "onStateUpdate_1");
                    showDialogUpdateSuccess();
                } else if (installState.installStatus() == InstallStatus.INSTALLED) {
                    Log.d("Appupdate", "onStateUpdate_2");
                    appUpdateManager.unregisterListener(this);
                } else {
                    Log.d("InstallState: ", installState.installStatus() + "");
                }
            }
        };

        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            Log.d("Appupdate", "successListener");
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                try {
                    Log.d("Appupdate", "successListener_1");
                    if (priorityUpdate == 2) {
                        Log.d("Appupdate", "successListener_2");
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE,
                                currentActivity,
                                APP_UPDATE_REQUEST_CODE
                        );
                    } else if (priorityUpdate == 1 && !validateSkip()) {
                        Log.d("Appupdate", "successListener_3");
                        appUpdateManager.registerListener(
                                installStateUpdatedListener
                        );
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.FLEXIBLE,
                                currentActivity,
                                APP_UPDATE_REQUEST_CODE
                        );
                    }
                } catch (IntentSender.SendIntentException ex) {
                    ex.printStackTrace();
                }
            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                //CHECK THIS if AppUpdateType.FLEXIBLE, otherwise you can skip
                if (priorityUpdate == 1) {
                    showDialogUpdateSuccess();
                }
            } else {
                Log.d("Appupdate", "checkForAppUpdateAvailability: something else");
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode == APP_UPDATE_REQUEST_CODE) {
            Log.d("Appupdate", "onActivityResult");
            if (resultCode != RESULT_OK) {
                Log.d("Appupdate", "onActivityResult_1");
                Activity currentActivity = EzApplication.getInstance().getCurrentActivity();
                if (currentActivity != null) {
                    if (priorityUpdate == 2) {
                        Log.d("Appupdate", "onActivityResult_2");
                        currentActivity.finishAffinity();
                    } else if (priorityUpdate == 1) {
                        addSkipUpdate(currentActivity);
                    }
                }
            } else {
                Log.d("Appupdate", "onActivityResult_3");
            }
        }
    }

    private void addSkipUpdate(Activity currentActivity) {
        try {
            int versionCode = currentActivity.getPackageManager().getPackageInfo(currentActivity.getPackageName(), 0).versionCode;
            String value = SharedPreferencesUtils.getTagString(currentActivity, KEY_SKIP_UPDATE, "");
            Log.d("Appupdate", "addSkipUpdate");
            if (TextUtils.isEmpty(value)) {
                Log.d("Appupdate", "addSkipUpdate_1");
                SharedPreferencesUtils.setTagString(currentActivity, KEY_SKIP_UPDATE, versionCode + "_1");
                return;
            }
            String[] data = value.split("_");
            if (data.length != 2) {
                Log.d("Appupdate", "addSkipUpdate_2");
                SharedPreferencesUtils.setTagString(currentActivity, KEY_SKIP_UPDATE, versionCode + "_1");
                return;
            }
            if (data[0].equals(String.valueOf(versionCode))) {
                try {
                    Log.d("Appupdate", "addSkipUpdate_3");
                    int countSkip = Integer.parseInt(data[1]);
                    SharedPreferencesUtils.setTagString(currentActivity, KEY_SKIP_UPDATE, versionCode + "_" + (countSkip + 1));
                } catch (Exception ex) {
                    Log.d("Appupdate", "addSkipUpdate_4");
                    SharedPreferencesUtils.setTagString(currentActivity, KEY_SKIP_UPDATE, versionCode + "_1");
                }
            } else {
                Log.d("Appupdate", "addSkipUpdate_5");
                SharedPreferencesUtils.setTagString(currentActivity, KEY_SKIP_UPDATE, versionCode + "_1");
            }
        } catch (PackageManager.NameNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    private boolean validateSkip() {
        Activity currentActivity = EzApplication.getInstance().getCurrentActivity();
        if (currentActivity != null) {
            Log.d("Appupdate", "validateSkip");
            String value = SharedPreferencesUtils.getTagString(currentActivity, KEY_SKIP_UPDATE, "");
            if (TextUtils.isEmpty(value)) {
                Log.d("Appupdate", "validateSkip_1");
                return false;
            }
            String[] data = value.split("_");
            if (data.length != 2) {
                Log.d("Appupdate", "validateSkip_2");
                return false;
            }
            try {
                int versionCode = currentActivity.getPackageManager().getPackageInfo(currentActivity.getPackageName(), 0).versionCode;
                if (data[0].equals(String.valueOf(versionCode))) {
                    try {
                        Log.d("Appupdate", "validateSkip_1");
                        int countSkip = Integer.parseInt(data[1]);
                        return countSkip > 2;
                    } catch (Exception ex) {
                        return false;
                    }
                } else {
                    return false;
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    public void callOnResume() {
        Activity currentActivity = EzApplication.getInstance().getCurrentActivity();
        Log.d("Appupdate", "callOnResume");
        if (appUpdateManager == null)
            return;
        Log.d("Appupdate", "callOnResume_1");
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            Log.d("Appupdate", "callOnResume_2");
            if (priorityUpdate == 2 && typeUpdate == AppUpdateType.IMMEDIATE) {
                Log.d("Appupdate", "callOnResume_3");
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    Log.d("Appupdate", "callOnResume_4");
                    showDialogUpdateSuccess();
                }
                try {
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        Log.d("Appupdate", "callOnResume_5");
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE,
                                currentActivity,
                                APP_UPDATE_REQUEST_CODE
                        );
                    }
                } catch (IntentSender.SendIntentException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void showDialogUpdateSuccess() {
        Log.d("Appupdate", "showDialogUpdateSuccess");
        Activity currentActivity = EzApplication.getInstance().getCurrentActivity();
        Snackbar snackbar = Snackbar.make(
                currentActivity.findViewById(android.R.id.content),
                currentActivity.getString(R.string.app_update_success),
                Snackbar.LENGTH_LONG
        );
        snackbar.setAction(currentActivity.getString(R.string.install), v -> {
            if (appUpdateManager != null) {
                appUpdateManager.completeUpdate();
            }
        });
        snackbar.addCallback(new Snackbar.Callback() {

            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                instance = null;
            }

            @Override
            public void onShown(Snackbar snackbar) {
            }
        });
        snackbar.show();
    }
}
