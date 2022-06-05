package com.kbeacon.ibeacondemo.dfulibrary;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kbeacon.ibeacondemo.AppBaseActivity;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgBase;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgCommon;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgType;
import com.kkmcn.kbeaconlib2.KBConnState;
import com.kkmcn.kbeaconlib2.KBException;
import com.kkmcn.kbeaconlib2.KBeacon;
import com.kkmcn.kbeaconlib2.KBeaconsMgr;
import com.kbeacon.ibeacondemo.R;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

import androidx.annotation.NonNull;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceController;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class KBeaconDFUActivity extends AppBaseActivity implements KBeacon.ConnStateDelegate {

    private static String LOG_TAG = "CfgNBDFU";
    public static String DEVICE_MAC_ADDRESS = "DEVICE_MAC_ADDRESS";
    private boolean mInDfuState = false;
    private KBFirmwareDownload firmwareDownload;
    private DfuServiceController controller;
    private TextView mUpdateStatusLabel, mUpdateNotesLabel, mNewVersionLabel;
    private ProgressBar mProgressBar;
    private KBeacon.ConnStateDelegate mPrivousDelegation;
    private DfuServiceInitiator starter;
    private ProgressDialog mProgressDialog;
    private KBeacon mBeacon;

    private String mDestFirmwareFileName;
    private boolean mFoundNewVersion = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cfg_beacon_dfu);

        final Intent intent = getIntent();
        String mMacAddress = intent.getStringExtra(DEVICE_MAC_ADDRESS);
        if (mMacAddress == null) {
            finish();
            return;
        }
        KBeaconsMgr mBluetoothMgr = KBeaconsMgr.sharedBeaconManager(this);
        mBeacon = mBluetoothMgr.getBeacon(mMacAddress);

        mUpdateStatusLabel = (TextView) findViewById(R.id.textStatusDescription);
        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);

        mNewVersionLabel = (TextView) findViewById(R.id.releaseNotesTitle);
        mUpdateNotesLabel = (TextView) findViewById(R.id.releaseNotes);

        mInDfuState = false;
        firmwareDownload = new KBFirmwareDownload(this);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setTitle(getString(R.string.DEVICE_CHECK_UPDATE));
        mProgressDialog.setIndeterminate(false);//设置进度条是否为不明确
        mProgressDialog.setCancelable(false);//设置进度条是否可以按退回键取消
        mProgressDialog.show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DfuServiceInitiator.createDfuNotificationChannel(this);
        }

        this.downloadFirmwareInfo();
    }

    private final DfuProgressListener dfuProgressListener = new DfuProgressListenerAdapter() {
        @Override
        public void onDeviceConnecting(final String deviceAddress) {
            mUpdateStatusLabel.setText(R.string.dfu_status_connecting);
        }

        @Override
        public void onDfuProcessStarting(final String deviceAddress) {
            mUpdateStatusLabel.setText(R.string.dfu_status_starting);
        }

        public void onDeviceConnected(@NonNull final String deviceAddress) {
            // empty default implementation
            mUpdateStatusLabel.setText(R.string.UPDATE_CONNECTED);
        }

        public void onDeviceDisconnecting(@NonNull final String deviceAddress) {
            // empty default implementation
            mUpdateStatusLabel.setText(R.string.dfu_status_disconnecting);
        }

        public void onDeviceDisconned(@NonNull final String deviceAddress) {
            // empty default implementation
            mUpdateStatusLabel.setText(R.string.UPDATE_DISCONNECTED);
        }

        @Override
        public void onProgressChanged(@NonNull final String deviceAddress, final int percent,
                                      final float speed, final float avgSpeed,
                                      final int currentPart, final int partsTotal) {
            mProgressBar.setProgress(percent);
            mUpdateStatusLabel.setText(R.string.UPDATE_UPLOADING);
        }

        @Override
        public void onDfuCompleted(@NonNull final String deviceAddress) {
            // empty default implementation
            mUpdateStatusLabel.setText(R.string.UPDATE_COMPLETE);
            mInDfuState = false;
            if (mProgressDialog.isShowing()){
                mProgressDialog.dismiss();
            }
            dfuComplete(getString(R.string.UPDATE_COMPLETE));
        }

        @Override
        public void onDfuAborted(@NonNull final String deviceAddress) {
            // empty default implementation
            mUpdateStatusLabel.setText(R.string.UPDATE_ABORTED);
            mInDfuState = false;
            if (mProgressDialog.isShowing()){
                mProgressDialog.dismiss();
            }
            dfuComplete(getString(R.string.UPDATE_ABORTED));
        }

        @Override
        public void onError(@NonNull final String deviceAddress,
                            final int error, final int errorType, final String message) {
            // empty default implementation
            mUpdateStatusLabel.setText(R.string.UPDATE_ABORTED);
            mInDfuState = false;
            if (mProgressDialog.isShowing()){
                mProgressDialog.dismiss();
            }
            dfuComplete(message);
        }
    };

    private void dfuComplete(String strDesc)
    {
        if (mProgressDialog.isShowing()){
            mProgressDialog.dismiss();
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.DEVICE_DFU_TITLE)
                .setMessage(strDesc)
                .setPositiveButton(R.string.Dialog_OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        KBeaconDFUActivity.this.finish();
                    }
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        DfuServiceListenerHelper.registerProgressListener(this, dfuProgressListener);
    }

    @Override
    protected void onPause() {
        super.onPause();

        DfuServiceListenerHelper.unregisterProgressListener(this, dfuProgressListener);
    }

    public void onConnStateChange(KBeacon beacon, KBConnState state, int nReason)
    {
        if (state == KBConnState.Disconnected)
        {
            if (mInDfuState)
            {
                Log.v(LOG_TAG, "Disconnection for DFU");
            }
        }
    }


    private void updateFirmware() {

        if (firmwareDownload.isFirmwareFileExist(mDestFirmwareFileName))
        {
            mInDfuState = true;
            starter = new DfuServiceInitiator(KBeaconDFUActivity.this.mBeacon.getMac())
                    .setDeviceName(KBeaconDFUActivity.this.mBeacon.getName())
                    .setKeepBond(false);
            starter.setPrepareDataObjectDelay(300L);
            File firmwareFile = firmwareDownload.getFirmwareFile(mDestFirmwareFileName);
            starter.setZip(null, firmwareFile.getPath());
            controller = starter.start(KBeaconDFUActivity.this, DFUService.class);
        }
        else
        {
            firmwareDownload.downLoadFile(mDestFirmwareFileName,
                    50 * 1000,
                    new KBFirmwareDownload.DownloadFirmwareDataCallback() {
                        @Override
                        public void onDownloadComplete(boolean bSuccess, File file, KBException error) {
                            if (bSuccess) {
                                mInDfuState = true;

                                starter = new DfuServiceInitiator(KBeaconDFUActivity.this.mBeacon.getMac())
                                        .setDeviceName(KBeaconDFUActivity.this.mBeacon.getName())
                                        .setKeepBond(false);

                                starter.setPrepareDataObjectDelay(300L);
                                starter.setZip(null, file.getPath());

                                controller = starter.start(KBeaconDFUActivity.this, DFUService.class);
                            } else {
                                if (mProgressDialog.isShowing()) {
                                    mProgressDialog.dismiss();
                                }

                                mUpdateStatusLabel.setText(R.string.UPDATE_NETWORK_FAIL);
                                dfuComplete(getString(R.string.UPDATE_NETWORK_FAIL));
                            }
                        }
                    });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_update, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_update:
                if (!mInDfuState) {
                    if (mFoundNewVersion) {
                        makeSureUpdateSelection();
                    } else {
                        toastShow(getString(R.string.UPDATE_NOT_FOUND_NEW_VERSION));
                    }
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private void makeSureUpdateSelection( ) {
        new AlertDialog.Builder(KBeaconDFUActivity.this)
                .setTitle(R.string.DEVICE_DFU_TITLE)
                .setMessage(R.string.DFU_VERSION_MAKE_SURE)
                .setPositiveButton(R.string.Dialog_OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mProgressBar.setProgress(0);
                        mInDfuState = true;

                        //update
                        mPrivousDelegation = mBeacon.getConnStateDelegate();
                        mBeacon.setConnStateDelegate(KBeaconDFUActivity.this);
                        updateFirmware();

                        mProgressDialog.setTitle(getString(R.string.UPDATE_STARTED));
                        mProgressDialog.show();
                    }
                })
                .setNegativeButton(R.string.Dialog_Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }


    private void downloadFirmwareInfo() {
        mProgressDialog.show();
        mUpdateNotesLabel.setText(R.string.DEVICE_CHECK_UPDATE);

        final KBCfgCommon cfgCommon = (KBCfgCommon) mBeacon.getCommonCfg();
        firmwareDownload.downloadFirmwareInfo(cfgCommon.getModel(), 10* 1000, new KBFirmwareDownload.DownloadFirmwareInfoCallback() {
            @Override
            public void onDownloadComplete(boolean bSuccess, HashMap<String, Object> firmwareInfo, KBException error) {
                if (mProgressDialog.isShowing()){
                    mProgressDialog.hide();
                }

                if (bSuccess) {
                    if (firmwareInfo == null)
                    {
                        dfuComplete(getString(R.string.NB_network_cloud_server_error));
                        return;
                    }

                    JSONArray firmwareVerList = (JSONArray)firmwareInfo.get(mBeacon.hardwareVersion());
                    if (firmwareVerList == null){
                        dfuComplete(getString(R.string.NB_network_file_not_exist));
                        return;
                    }

                    String currVerDigital = cfgCommon.getVersion().substring(1);
                    StringBuilder versionNotes = new StringBuilder();
                    for (int i = 0; i < firmwareVerList.length(); i++) {
                        JSONObject object;
                        try
                        {
                            object = (JSONObject)firmwareVerList.get(i);
                        }
                        catch (JSONException excpt)
                        {
                            excpt.printStackTrace();
                            dfuComplete(getString(R.string.NB_network_cloud_server_error));
                            return;
                        }

                        HashMap<String, Object> verInfo = new HashMap<>(10);
                        KBCfgBase.JsonObject2HashMap(object, verInfo);
                        String destVersion = (String) verInfo.get("appVersion");
                        if (destVersion == null)
                        {
                            dfuComplete(getString(R.string.NB_network_cloud_server_error));
                            return;
                        }

                        String remoteVerDigital = destVersion.substring(1);
                        if (Float.valueOf(currVerDigital) < Float.valueOf(remoteVerDigital)) {

                            String appFileName = (String) verInfo.get("appFileName");
                            if (appFileName == null)
                            {
                                dfuComplete(getString(R.string.NB_network_cloud_server_error));
                                return;
                            }

                            String releaseNotes = (String) verInfo.get("note");
                            if (releaseNotes != null)
                            {
                                versionNotes.append(releaseNotes);
                                versionNotes.append("\n");
                            }

                            if (i == firmwareVerList.length() - 1)
                            {
                                mDestFirmwareFileName = appFileName;

                                //found new version
                                mUpdateStatusLabel.setText(R.string.UPDATE_FOUND_NEW_VERSION);
                                mNewVersionLabel.setText(destVersion);
                                mUpdateNotesLabel.setText(versionNotes.toString());
                                mFoundNewVersion = true;
                                String strDesc = String.format(getString(R.string.DFU_FOUND_NEW_VERSION), destVersion);
                                toastShow(strDesc);
                                return;
                            }
                        }
                    }
                    dfuComplete(getString(R.string.DEVICE_LATEST_VERSION));
                }
                else{
                    if (error.errorCode == KBFirmwareDownload.ERR_NETWORK_DOWN_FILE_ERROR)
                    {
                        dfuComplete(getString(R.string.UPDATE_NETWORK_FAIL) + error.getMessage());
                    }
                    else
                    {
                        dfuComplete(error.getMessage());
                    }
                }
            }
        });
    }
}

