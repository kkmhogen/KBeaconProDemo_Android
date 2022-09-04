package com.kkmcn.sensordemo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.kkmcn.kbeaconlib2.ByteConvert;
import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvType;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBAdvMode;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBAdvTxPower;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvBase;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvIBeacon;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgSensorBase;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgSensorHT;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgTrigger;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgTriggerMotion;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBSensorType;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBTimeRange;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBTriggerAction;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBTriggerAdvChgMode;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBTriggerType;
import com.kkmcn.kbeaconlib2.KBConnState;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBCutoffDataMsg;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBCutoffRecord;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBHumidityDataMsg;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBHumidityRecord;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBPIRDataMsg;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBPIRRecord;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBSensorDataMsgBase;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBSensorReadOption;
import com.kkmcn.kbeaconlib2.KBUtility;
import com.kkmcn.kbeaconlib2.UTCTime;
import com.kkmcn.sensordemo.dfulibrary.KBeaconDFUActivity;
import com.kkmcn.sensordemo.recordhistory.CfgHTBeaconHistoryActivity;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgBase;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgCommon;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvKSensor;
import com.kkmcn.kbeaconlib2.KBConnectionEvent;
import com.kkmcn.kbeaconlib2.KBException;
import com.kkmcn.kbeaconlib2.KBeacon;
import com.kkmcn.kbeaconlib2.KBeaconsMgr;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import androidx.core.app.ActivityCompat;

import static com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBSensorDataMsgBase.INVALID_DATA_RECORD_POS;

public class DevicePannelActivity extends AppBaseActivity implements View.OnClickListener,
        KBeacon.ConnStateDelegate, KBeacon.NotifyDataDelegate{

    public final static String DEVICE_MAC_ADDRESS = "DEVICE_MAC_ADDRESS";
    private final static String LOG_TAG = "DevicePannel";

    public final static String DEFAULT_PASSWORD = "0000000000000000";   //16 zero ascii
    private static SimpleDateFormat mUtcTimeFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 日志文件格式
    private static final int PERMISSION_CONNECT = 25;
    private KBeaconsMgr mBeaconMgr;
    private String mDeviceAddress;
    private KBeacon mBeacon;

    //uiview
    private TextView mAdvType, mBeaconStatus;
    private TextView mBeaconModel;

    //button trigger
    private Button mReadButtonTrigger, mTriggerButtonAdv, mTriggerButtonApp;

    //acc trigger
    private Button mEnableAccTrigger, mDisableAccTrigger;

    //KSensor advertisement
    private Button nEnableTHData2Adv, nEnableAxisData2Adv, mViewTHDataHistory;

    //TH trigger
    private Button nEnableTHTrigger2Adv, nEnableTHTrigger2App, mEnablePeriodicallyTrigger2App;

    private Button mRingButton;
    private String mNewPassword;
    SharePreferenceMgr mPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(DEVICE_MAC_ADDRESS);
        mBeaconMgr = KBeaconsMgr.sharedBeaconManager(this);
        mBeacon = mBeaconMgr.getBeacon(mDeviceAddress);
        if (mBeacon == null){
            toastShow("device is not exist");
            finish();
        }

        mPref = SharePreferenceMgr.shareInstance(this);
        setContentView(R.layout.device_pannel);
        mBeaconStatus = (TextView)findViewById(R.id.connection_states);
        mAdvType = (TextView) findViewById(R.id.beaconAdvType);
        mBeaconModel = (TextView) findViewById(R.id.beaconModle);

        //button trigger
        mReadButtonTrigger = findViewById(R.id.readBtnTriggerPara);
        mReadButtonTrigger.setOnClickListener(this);
        mTriggerButtonAdv = (Button) findViewById(R.id.enableBtnAdvTrigger);
        mTriggerButtonAdv.setOnClickListener(this);
        mTriggerButtonApp = (Button) findViewById(R.id.enableBtnAppTrigger);
        mTriggerButtonApp.setOnClickListener(this);

        //acc trigger
        mEnableAccTrigger = findViewById(R.id.enableAccTrigger);
        mEnableAccTrigger.setOnClickListener(this);
        mDisableAccTrigger = findViewById(R.id.disableAccAppTrigger);
        mDisableAccTrigger.setOnClickListener(this);

        //KSensor advertisement
        nEnableTHData2Adv = (Button) findViewById(R.id.enableTHAdvertisement);
        nEnableTHData2Adv.setOnClickListener(this);
        nEnableAxisData2Adv = findViewById(R.id.enableAccAdvertisement);
        nEnableAxisData2Adv.setOnClickListener(this);
        mViewTHDataHistory = findViewById(R.id.viewTHDataHistory); //view temperature and humidity data history
        mViewTHDataHistory.setOnClickListener(this);

        //TH trigger
        nEnableTHTrigger2Adv = findViewById(R.id.enableTHChangeTriggerEvtRpt2Adv);
        nEnableTHTrigger2Adv.setOnClickListener(this);
        nEnableTHTrigger2App = findViewById(R.id.enableTHChangeTriggerEvtRpt2App);
        nEnableTHTrigger2App.setOnClickListener(this);

        //send temperature humidity data to app
        mEnablePeriodicallyTrigger2App = findViewById(R.id.enablePeriodicallyTHDataToApp);
        mEnablePeriodicallyTrigger2App.setOnClickListener(this);
        mRingButton = (Button) findViewById(R.id.ringDevice);
        mRingButton.setOnClickListener(this);

        findViewById(R.id.dfuDevice).setOnClickListener(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_connect, menu);
        if (mBeacon.getState() == KBConnState.Connected)
        {
            menu.findItem(R.id.menu_connect).setEnabled(true);
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            menu.findItem(R.id.menu_connecting).setVisible(false);
            menu.findItem(R.id.menu_connecting).setActionView(null);
            mBeaconStatus.setText("Connected");
        }
        else if (mBeacon.getState() == KBConnState.Connecting)
        {
            mBeaconStatus.setText("Connecting");
            menu.findItem(R.id.menu_connect).setEnabled(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_connecting).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        else
        {
            mBeaconStatus.setText("Disconnected");
            menu.findItem(R.id.menu_connect).setEnabled(true);
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_connecting).setVisible(false);
            menu.findItem(R.id.menu_connecting).setActionView(null);
        }
        return true;
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.readBtnTriggerPara:
                readButtonTriggerPara();
                break;

            case R.id.enableBtnAdvTrigger:
                enableButtonTriggerEvent2Adv();
                break;

            case R.id.enableBtnAppTrigger:
                enableButtonTriggerEvent2App();
                break;


            //acc trigger
            case R.id.enableAccTrigger:
                enableMotionTrigger();
                break;

            case R.id.disableAccAppTrigger:
                disableMotionTrigger();
                break;

            //ksensor advertisement
            case R.id.enableAccAdvertisement:
                enableAdvTypeIncludeAccXYZ();
                break;

            case R.id.enableTHAdvertisement:
                enableAdvTypeIncludeAccTH();
                break;

            case R.id.viewTHDataHistory:
                if (mBeacon.isConnected()) {
                    Intent intent = new Intent(this, CfgHTBeaconHistoryActivity.class);
                    intent.putExtra(CfgHTBeaconHistoryActivity.DEVICE_MAC_ADDRESS, mBeacon.getMac());   //field type
                    startActivityForResult(intent, 1);
                }
                break;

            //T&H trigger
            case R.id.enableTHChangeTriggerEvtRpt2Adv:
                enableTHTriggerEvtRpt2Adv();
                break;

            case R.id.enableTHChangeTriggerEvtRpt2App:
                enableTHTriggerEvtRpt2App();
                break;

            case R.id.enablePeriodicallyTHDataToApp:
                enableTHPeriodicallyTriggerRpt2App();
                break;

            //DFU service
            case R.id.dfuDevice:
                if (mBeacon.isConnected()) {
                    final Intent intent = new Intent(this, KBeaconDFUActivity.class);
                    intent.putExtra(KBeaconDFUActivity.DEVICE_MAC_ADDRESS, mBeacon.getMac());
                    startActivityForResult(intent, 1);
                }
                break;

            case R.id.ringDevice:
                ringDevice();
                break;
            default:
                break;
        }
    }

    private KBConnState nDeviceConnState = KBConnState.Disconnected;

    public void onConnStateChange(KBeacon beacon, KBConnState state, int nReason)
    {
        if (state == KBConnState.Connected)
        {
            Log.v(LOG_TAG, "device has connected");
            invalidateOptionsMenu();

            KBCfgCommon cfgCommon = mBeacon.getCommonCfg();
            if (cfgCommon != null) {
                this.mBeaconModel.setText(cfgCommon.getModel());
                ArrayList<KBCfgAdvBase> slotCfg = mBeacon.getSlotCfgList();
                String strAdvType = "";
                for (KBCfgAdvBase adv : slotCfg)
                {
                    strAdvType = strAdvType + "|" + KBAdvType.getAdvTypeString(adv.getAdvType());
                }
                mAdvType.setText(strAdvType);
            }

            nDeviceConnState = state;
        }
        else if (state == KBConnState.Connecting)
        {
            Log.v(LOG_TAG, "device start connecting");
            invalidateOptionsMenu();

            nDeviceConnState = state;
        }
        else if (state == KBConnState.Disconnecting) {
            Log.e(LOG_TAG, "connection error, now disconnecting");
        }
        else
        {
            if (nDeviceConnState == KBConnState.Connecting)
            {
                if (nReason == KBConnectionEvent.ConnAuthFail)
                {
                    final EditText inputServer = new EditText(this);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.auth_error_title));
                    builder.setView(inputServer);
                    builder.setNegativeButton(R.string.Dialog_Cancel, null);
                    builder.setPositiveButton(R.string.Dialog_OK, null);
                    final AlertDialog alertDialog = builder.create();
                    alertDialog.show();

                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String strNewPassword = inputServer.getText().toString().trim();
                            if (strNewPassword.length() < 8|| strNewPassword.length() > 16)
                            {
                                Toast.makeText(DevicePannelActivity.this,
                                        R.string.connect_error_auth_format,
                                        Toast.LENGTH_SHORT).show();
                            }else {
                                mPref.setPassword(mDeviceAddress, strNewPassword);
                                alertDialog.dismiss();
                            }
                        }
                    });
                }
                else
                {
                    toastShow("connect to device failed, reason:" + nReason);
                }
            }

            Log.e(LOG_TAG, "device has disconnected:" +  nReason);
            invalidateOptionsMenu();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        /*
        if (mBeacon.getState() == KBConnState.Connected
            || mBeacon.getState() == KBConnState.Connecting){
            mBeacon.disconnect();
            invalidateOptionsMenu();
        }
        */
    }

    public boolean checkPermission()
    {
        boolean bHasPermission = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        PERMISSION_CONNECT);
                bHasPermission = false;
            }
        }
        return bHasPermission;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mBeacon.connect(mPref.getPassword(mDeviceAddress),
                        20 * 1000,
                        this);
            } else {
                toastShow("The app need ble connection permission for start ble scanning");
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.menu_connect){
            //connect and sync the UTC time to device
            if (checkPermission())
            {
                mBeacon.connect(mPref.getPassword(mDeviceAddress),
                        20 * 1000,
                        this);
            }
            invalidateOptionsMenu();
        }
        else if(id == R.id.menu_disconnect){
            mBeacon.disconnect();
            invalidateOptionsMenu();
        }
        else if(id == android.R.id.home){
            mBeacon.disconnect();
        }

        return super.onOptionsItemSelected(item);
    }


    public void onNotifyDataReceived(KBeacon beacon, int nEventType, byte[] sensorData)
    {
        //check if Temperature and humidity trigger event
        if (nEventType >= KBTriggerType.HTTempAbove
            && nEventType <= KBTriggerType.HTHumidityBelow)
        {
            int nDataIndex = 0;
            long nUTCTime = ByteConvert.bytesToLong(sensorData, 0);
            nDataIndex += 4;

            Float Temperature = KBUtility.signedBytes2Float(sensorData[nDataIndex], sensorData[nDataIndex+1]);
            nDataIndex += 2;

            Float humidity = KBUtility.signedBytes2Float(sensorData[nDataIndex], sensorData[nDataIndex+1]);
            nDataIndex += 2;

            Log.v(LOG_TAG, "Receive trigger notify event:" + nEventType + ", temperature:" + Temperature + ", humidity:" + humidity);
        }
        else
        {
            Log.v(LOG_TAG, "Receive other trigger notify event:" + nEventType);
        }
    }

    public void readButtonTriggerPara() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //read push button trigger parameters
        this.mBeacon.readTriggerConfig(KBTriggerType.BtnSingleClick, new KBeacon.ReadConfigCallback() {
            public void onReadComplete(boolean bConfigSuccess, JSONObject paraDicts, KBException error) {
                if (bConfigSuccess) {
                    KBCfgTrigger btnCfg = mBeacon.getTriggerCfg(KBTriggerType.BtnSingleClick);
                    if (btnCfg == null ){
                        toastShow("button trigger is not enabled");
                        return;
                    }

                    Log.v(LOG_TAG, "read trigger type:" + btnCfg.getTriggerType());
                    if (btnCfg.getTriggerAction() > 0)
                    {
                        //button trigger adv mode
                        if (btnCfg.getTriggerAdvChgMode() == KBTriggerAdvChgMode.KBTriggerAdvChangeModeUUID)
                        {
                            Log.v(LOG_TAG, "device advertisement uuid is difference when trigger event happened");
                        }

                        //button trigger adv duration, unit is sec
                        Log.v(LOG_TAG, "BTN trigger slot:" + btnCfg.getTriggerAdvSlot());
                        Log.v(LOG_TAG, "BTN trigger action:" + btnCfg.getTriggerAction());
                        Log.v(LOG_TAG, "BTN trigger adv duration:" + btnCfg.getTriggerAdvTime());
                    }
                    else
                    {
                        Log.v(LOG_TAG, "trigger type:" + btnCfg.getTriggerType() + " is off");
                    }
                }
            }
        });
    }

    //The following example is that the beacon usually broadcasts the iBeacon message in Slot0.
    // When it detects the watchband was cutoff, it triggers the broadcast of the iBeacon with UUID + 7, and
    // the iBeacon broadcast duration is 10 seconds.
    public void enableCutoffTriggerEvent2Adv() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        final KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getCommonCfg();
        if (oldCommonCfg != null && !oldCommonCfg.isSupportTrigger(KBTriggerType.Cutoff))
        {
            toastShow("device does not support cutoff alarm");
            return;
        }

        //set slot0 to default alive advertisement
        final KBCfgAdvIBeacon iBeaconAdv = new KBCfgAdvIBeacon();
        iBeaconAdv.setSlotIndex(0);  //reuse previous slot
        iBeaconAdv.setAdvPeriod(1280f);
        iBeaconAdv.setAdvMode(KBAdvMode.Legacy);
        iBeaconAdv.setTxPower(KBAdvTxPower.RADIO_Neg4dBm);
        iBeaconAdv.setAdvConnectable(true);
        iBeaconAdv.setAdvTriggerOnly(false);  //always advertisement
        iBeaconAdv.setUuid("B9407F30-F5F8-466E-AFF9-25556B57FE61");
        iBeaconAdv.setMajorID(12);
        iBeaconAdv.setMinorID(10);

        //set trigger type
        KBCfgTrigger cutoffTriggerPara = new KBCfgTrigger(0, KBTriggerType.Cutoff);
        cutoffTriggerPara.setTriggerAdvChangeMode(1);   //change the UUID when trigger event happened
        cutoffTriggerPara.setTriggerAction(KBTriggerAction.Advertisement);
        cutoffTriggerPara.setTriggerAdvSlot(0);
        cutoffTriggerPara.setTriggerAdvTime(20);

        //enable cutoff trigger
        ArrayList<KBCfgBase> cfgList = new ArrayList<>(2);
        cfgList.add(iBeaconAdv);
        cfgList.add(cutoffTriggerPara);
        this.mBeacon.modifyConfig(cfgList, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                if (bConfigSuccess) {
                    toastShow("enable cut off trigger success");
                } else {
                    toastShow("enable cut off trigger error:" + error.errorCode);
                }
            }
        });
    }

    //The following example is that the beacon  broadcasts the iBeacon message in Slot0.
    // When it detects button press, it triggers the UUID, adv interval, TX power change in slot 0,
    // the iBeacon broadcast duration is 10 seconds.
    public void enableButtonTriggerEvent2Adv() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        final KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getCommonCfg();
        if (oldCommonCfg != null && !oldCommonCfg.isSupportButton())
        {
            toastShow("device does not support humidity");
            return;
        }

        //set slot0 to default alive advertisement
        final KBCfgAdvIBeacon iBeaconAdv = new KBCfgAdvIBeacon();
        iBeaconAdv.setSlotIndex(0);  //reuse previous slot
        iBeaconAdv.setAdvPeriod(2560.0f);
        iBeaconAdv.setAdvMode(KBAdvMode.Legacy);
        iBeaconAdv.setTxPower(KBAdvTxPower.RADIO_Neg4dBm);
        iBeaconAdv.setAdvConnectable(true);
        iBeaconAdv.setAdvTriggerOnly(false);  //always advertisement
        iBeaconAdv.setUuid("B9407F30-F5F8-466E-AFF9-25556B57FE67");
        iBeaconAdv.setMajorID(12);
        iBeaconAdv.setMinorID(10);

        //set trigger type
        KBCfgTrigger btnTriggerPara = new KBCfgTrigger(0, KBTriggerType.BtnSingleClick);
        btnTriggerPara.setTriggerAdvChangeMode(1); //change the UUID when trigger happened
        btnTriggerPara.setTriggerAction(KBTriggerAction.Advertisement);
        btnTriggerPara.setTriggerAdvSlot(0);
        btnTriggerPara.setTriggerAdvTime(10);

        //option trigger para, If the following two parameters are omitted,
        // the trigger broadcast interval is 2560.0ms and the transmit power is -4dBm.
        btnTriggerPara.setTriggerAdvPeriod(200.0f);
        btnTriggerPara.setTriggerTxPower(KBAdvTxPower.RADIO_Pos4dBm);

        //enable push button trigger
        mTriggerButtonAdv.setEnabled(false);
        ArrayList<KBCfgBase> cfgList = new ArrayList<>(2);
        cfgList.add(iBeaconAdv);
        cfgList.add(btnTriggerPara);
        this.mBeacon.modifyConfig(cfgList, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mTriggerButtonAdv.setEnabled(true);
                if (bConfigSuccess) {
                    toastShow("enable push button trigger success");
                } else {
                    toastShow("enable push button trigger error:" + error.errorCode);
                }
            }
        });
    }

    public void enableButtonTriggerEvent2App() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        final int nTriggerType = KBTriggerType.BtnSingleClick;
        final KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getCommonCfg();
        if (oldCommonCfg != null && !oldCommonCfg.isSupportTrigger(nTriggerType))
        {
            toastShow("Th device does not support humidity");
            return;
        }

        //set trigger type
        KBCfgTrigger btnTriggerPara = new KBCfgTrigger(0,
                nTriggerType);
        btnTriggerPara.setTriggerAction(KBTriggerAction.Report2App);

        //enable push button trigger
        mTriggerButtonApp.setEnabled(false);
        this.mBeacon.modifyConfig(btnTriggerPara, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mTriggerButtonApp.setEnabled(true);
                if (bConfigSuccess) {
                    //subscribe all notify
                    mBeacon.subscribeSensorDataNotify(null, DevicePannelActivity.this, new KBeacon.ActionCallback() {
                        @Override
                        public void onActionComplete(boolean bConfigSuccess, KBException error) {
                            if (bConfigSuccess) {
                                toastShow("subscribe button trigger event success");
                            } else {
                                toastShow("subscribe button trigger event failed");
                            }
                        }
                    });
                }
            }
        });
    }

    public void disableButtonTrigger() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        final KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getCommonCfg();
        if (oldCommonCfg != null && !oldCommonCfg.isSupportButton())
        {
            toastShow("The device does not support humidity");
            return;
        }


        //turn off trigger 0
        KBCfgTrigger btnTriggerPara = new KBCfgTrigger(0, KBTriggerType.TriggerNull);
        //disable push button trigger
        this.mBeacon.modifyConfig(btnTriggerPara, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                if (bConfigSuccess) {
                    toastShow("disable push button trigger success");
                } else {
                    toastShow("disable push button trigger error:" + error.errorCode);
                }
            }
        });
    }

    // When the beacon detects motion event, it triggers the broadcast of the iBeacon message in Slot0, and
    // the iBeacon broadcast duration is 60 seconds.
    public void enableMotionTrigger() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        final KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getCommonCfg();
        if (oldCommonCfg != null && !oldCommonCfg.isSupportAccSensor())
        {
            toastShow("The device does not support humidity");
            return;
        }

        //set trigger adv slot information
        final KBCfgAdvIBeacon triggerAdv = new KBCfgAdvIBeacon();
        triggerAdv.setSlotIndex(0);  //reuse previous slot
        triggerAdv.setAdvPeriod(200f);
        triggerAdv.setAdvMode(KBAdvMode.Legacy);
        triggerAdv.setTxPower(KBAdvTxPower.RADIO_0dBm);
        triggerAdv.setAdvConnectable(true);
        triggerAdv.setAdvTriggerOnly(true);  //this slot only advertisement when trigger event happened
        triggerAdv.setUuid("B9407F30-F5F8-466E-AFF9-25556B570002");
        triggerAdv.setMinorID(32);
        triggerAdv.setMinorID(10);

        //set trigger type
        KBCfgTriggerMotion motionTriggerPara = new KBCfgTriggerMotion();
        motionTriggerPara.setTriggerType(KBTriggerType.AccMotion);
        motionTriggerPara.setTriggerIndex(0);
        motionTriggerPara.setTriggerAdvSlot(0);
        motionTriggerPara.setTriggerAction(KBTriggerAction.Advertisement); //set trigger advertisement enable
        motionTriggerPara.setTriggerAdvTime(60);  //set trigger adv duration to 60 seconds

        //set acc motion para
        motionTriggerPara.setTriggerPara(5);  //set motion sensitive, unit is 16mg
        motionTriggerPara.setAccODR(KBCfgTriggerMotion.ACC_ODR_25_HZ);
        motionTriggerPara.setWakeupDuration(3);

        //enable motion trigger
        mEnableAccTrigger.setEnabled(false);
        ArrayList<KBCfgBase> cfgList = new ArrayList<>(2);
        cfgList.add(triggerAdv);
        cfgList.add(motionTriggerPara);
        this.mBeacon.modifyConfig(cfgList, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mEnableAccTrigger.setEnabled(true);
                if (bConfigSuccess) {
                    toastShow("enable motion trigger success");
                } else {
                    toastShow("enable motion trigger error:" + error.errorCode);
                }
            }
        });
    }

    //disable motion trigger
    public void disableMotionTrigger() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        final KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getCommonCfg();
        if (oldCommonCfg != null && !oldCommonCfg.isSupportAccSensor())
        {
            toastShow("The device does not support humidity");
            return;
        }

        //set trigger type
        KBCfgTrigger mtionTriggerPara = new KBCfgTrigger(0, KBTriggerType.TriggerNull);

        //enable push button trigger
        mDisableAccTrigger.setEnabled(false);
        this.mBeacon.modifyConfig(mtionTriggerPara, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mDisableAccTrigger.setEnabled(true);
                if (bConfigSuccess) {
                    toastShow("enable motion trigger success");
                } else {
                    toastShow("enable motion trigger error:" + error.errorCode);
                }
            }
        });
    }

    //Please make sure the app does not enable any trigger's advertisement mode to KBTriggerAdvOnlyMode
    //If the app set some trigger advertisement mode to KBTriggerAdvOnlyMode, then the device only start advertisement when trigger event happened.
    //when this function enabled, then the device will include the realtime temperature and humidity data in advertisement
    public void enableAdvTypeIncludeAccTH()
    {
        final KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getCommonCfg();
        final KBCfgAdvKSensor oldSensorCfg = mBeacon.getKSensorAdvCfg();

        if (!mBeacon.isConnected())
        {
            toastShow("Device is not connected");
            return;
        }

        if (oldCommonCfg != null && !oldCommonCfg.isSupportHumiditySensor())
        {
            toastShow("The device does not support humidity");
            return;
        }

        //enable ksensor advertisement
        KBCfgAdvKSensor sensorCfg = new KBCfgAdvKSensor();
        int nSlotIndex = 0;
        if (oldSensorCfg != null){
            nSlotIndex = oldSensorCfg.getSlotIndex();
        }
        sensorCfg.setSlotIndex(nSlotIndex);  //reuse previous slot
        sensorCfg.setAdvPeriod(1000f);
        sensorCfg.setAdvMode(KBAdvMode.Legacy);
        sensorCfg.setTxPower(KBAdvTxPower.RADIO_0dBm);
        sensorCfg.setAdvConnectable(true);
        sensorCfg.setAdvTriggerOnly(false);  //always advertisement
        sensorCfg.setHtSensorInclude(true);

        nEnableTHData2Adv.setEnabled(false);
        mBeacon.modifyConfig(sensorCfg, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                nEnableTHData2Adv.setEnabled(true);
                if (bConfigSuccess) {
                    Log.v(LOG_TAG, "enable humidity advertisement success");
                }
            }
        });
    }

    //The following example is to set the device to broadcast KSensor in Slot0,
    // and the broadcast message contains the X, Y, and Z axis information of the motion sensor.
    public void enableAdvTypeIncludeAccXYZ()
    {
        if (!mBeacon.isConnected())
        {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        KBCfgCommon oldCommonCfg = mBeacon.getCommonCfg();
        if (oldCommonCfg != null && !oldCommonCfg.isSupportAccSensor())
        {
            toastShow("The device does not supported acc sensor");
            return;
        }

        try {
            //set trigger adv slot information
            final KBCfgAdvKSensor oldSensorCfg = mBeacon.getKSensorAdvCfg();
            int nSlotIndex = 0;
            if (oldSensorCfg != null){
                nSlotIndex = oldSensorCfg.getSlotIndex();
            }
            KBCfgAdvKSensor sensorAdvCfg = new KBCfgAdvKSensor();
            sensorAdvCfg.setSlotIndex(nSlotIndex);  //reuse previous slot
            sensorAdvCfg.setAdvPeriod(1000f);
            sensorAdvCfg.setAdvMode(KBAdvMode.Legacy);
            sensorAdvCfg.setTxPower(KBAdvTxPower.RADIO_0dBm);
            sensorAdvCfg.setAdvConnectable(true);
            sensorAdvCfg.setAdvTriggerOnly(false);
            sensorAdvCfg.setAxisSensorInclude(true);
            sensorAdvCfg.setHtSensorInclude(false);

            nEnableAxisData2Adv.setEnabled(false);
            mBeacon.modifyConfig(sensorAdvCfg, new KBeacon.ActionCallback() {
                @Override
                public void onActionComplete(boolean bConfigSuccess, KBException error) {
                    nEnableAxisData2Adv.setEnabled(true);
                    if (bConfigSuccess){
                        Log.v(LOG_TAG, "enable acc advertisement success");
                    }else{
                        Log.v(LOG_TAG, "enable acc advertisement failed");
                    }
                }
            });
        }
        catch (Exception excpt)
        {
            Log.v(LOG_TAG, "config acc advertisement failed");
        }
    }


    //The device will start broadcasting when temperature&humidity trigger event happened
    //for example, the humidity > 70%
    public void enableTHTriggerEvtRpt2Adv() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        final KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getCommonCfg();
        if (oldCommonCfg != null && !oldCommonCfg.isSupportHumiditySensor())
        {
            toastShow("The device does not support humidity");
            return;
        }

        //set slot 1 to trigger adv information
        final KBCfgAdvIBeacon triggerAdv = new KBCfgAdvIBeacon();
        triggerAdv.setSlotIndex(1);
        triggerAdv.setAdvPeriod(211.25f);
        triggerAdv.setAdvMode(KBAdvMode.Legacy);
        triggerAdv.setTxPower(KBAdvTxPower.RADIO_Pos4dBm);
        triggerAdv.setAdvConnectable(false);
        triggerAdv.setAdvTriggerOnly(true);  //always advertisement
        triggerAdv.setUuid("B9407F30-F5F8-466E-AFF9-25556B570003");
        triggerAdv.setMajorID(1);
        triggerAdv.setMinorID(1);

        //set trigger information
        KBCfgTrigger thTriggerPara = new KBCfgTrigger(0,
                KBTriggerType.HTHumidityAbove);
        thTriggerPara.setTriggerAction(KBTriggerAction.Advertisement);  //set trigger advertisement enable
        thTriggerPara.setTriggerAdvSlot(1);
        thTriggerPara.setTriggerPara(70);
        thTriggerPara.setTriggerAdvTime(15);  //set trigger adv duration to 15 seconds

        //enable push button trigger
        nEnableTHTrigger2Adv.setEnabled(false);
        ArrayList<KBCfgBase> cfgList = new ArrayList<>(2);
        cfgList.add(triggerAdv);
        cfgList.add(thTriggerPara);
        this.mBeacon.modifyConfig(cfgList, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                nEnableTHTrigger2Adv.setEnabled(true);
                if (bConfigSuccess) {
                    toastShow("enable temp&humidity trigger success");
                } else {
                    toastShow("enable temp&humidity trigger error:" + error.errorCode);
                }
            }
        });
    }

    //the device will send event to app when temperature&humidity trigger event happened
    //for example, the humidity > 50%
    //the app must subscribe the notification event if it want receive the event
    public void enableTHTriggerEvtRpt2App(){
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        final KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getCommonCfg();
        if (oldCommonCfg != null && !oldCommonCfg.isSupportHumiditySensor())
        {
            toastShow("device does not support humidity");
            return;
        }

        KBCfgTrigger thTriggerPara = new KBCfgTrigger(0,
                KBTriggerType.HTHumidityAbove);
        thTriggerPara.setTriggerAction(KBTriggerAction.Report2App);  //set trigger event that report to app
        thTriggerPara.setTriggerPara(70);

        nEnableTHTrigger2App.setEnabled(false);
        this.mBeacon.modifyConfig(thTriggerPara, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                nEnableTHTrigger2App.setEnabled(true);
                if (bConfigSuccess) {
                    toastShow("enable temp&humidity trigger success");

                    //subscribe humidity notify
                    if (!mBeacon.isSensorDataSubscribe(KBTriggerType.HTHumidityAbove)) {
                        mBeacon.subscribeSensorDataNotify(KBTriggerType.HTHumidityAbove, DevicePannelActivity.this, new KBeacon.ActionCallback() {
                            @Override
                            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                                if (bConfigSuccess) {
                                    Log.v(LOG_TAG, "subscribe temperature and humidity data success");
                                } else {
                                    Log.v(LOG_TAG, "subscribe temperature and humidity data failed");
                                }
                            }
                        });
                    }
                } else {
                    toastShow("enable temp&humidity error:" + error.errorCode);
                }
            }
        });
    }

    //After enable realtime data to app, then the device will periodically send the temperature and humidity data to app whether it was changed or not.
    public void enableTHPeriodicallyTriggerRpt2App(){
        final KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getCommonCfg();

        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        if (oldCommonCfg != null && !oldCommonCfg.isSupportHumiditySensor())
        {
            toastShow("device does not support humidity");
            return;
        }

        KBCfgTrigger thTriggerPara = new KBCfgTrigger(0, KBTriggerType.HTHumidityPeriodically);
        thTriggerPara.setTriggerAction(KBTriggerAction.Report2App);
        thTriggerPara.setTriggerPara(30);//report to app every 30 second

        //subscribe humidity notify
        mEnablePeriodicallyTrigger2App.setEnabled(false);
        this.mBeacon.modifyConfig(thTriggerPara, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mEnablePeriodicallyTrigger2App.setEnabled(true);
                if (bConfigSuccess) {
                    Log.v(LOG_TAG, "set temp&humidity trigger event report to app");

                    if (!mBeacon.isSensorDataSubscribe(KBTriggerType.HTHumidityPeriodically)) {
                        mBeacon.subscribeSensorDataNotify(KBTriggerType.HTHumidityPeriodically, DevicePannelActivity.this, new KBeacon.ActionCallback() {
                            @Override
                            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                                if (bConfigSuccess) {
                                    Log.v(LOG_TAG, "subscribe temperature and humidity data success");
                                } else {
                                    Log.v(LOG_TAG, "subscribe temperature and humidity data failed");
                                }
                            }
                        });
                    }

                } else {
                    toastShow("enable temp&humidity error:" + error.errorCode);
                }
            }
        });
    }

    // When the beacon detects PIR event, the device will record the event
    public void enablePIRTrigger() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        final KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getCommonCfg();
        if (oldCommonCfg != null && !oldCommonCfg.isSupportPIRSensor())
        {
            toastShow("device does not support PIR sensor");
            return;
        }

        //enable PIR trigger
        KBCfgTrigger pirTriggerPara = new KBCfgTrigger(0, KBTriggerType.PIRBodyInfraredDetected);

        //Save the PIR event to memory flash and report it to the APP at the same time
        pirTriggerPara.setTriggerAction(KBTriggerAction.Record | KBTriggerAction.Report2App);

        //If the human infrared is repeatedly detected within 30 seconds, it will no longer be record/reported.
        pirTriggerPara.setTriggerPara(30);

        this.mBeacon.modifyConfig(pirTriggerPara, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                if (bConfigSuccess) {
                    toastShow("enable PIR trigger success");
                } else {
                    toastShow("enable PIR trigger error:" + error.errorCode);
                }
            }
        });
    }

    //set disable period parameters
    public void setPIRDisablePeriod() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        final KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getCommonCfg();
        if (oldCommonCfg != null && !oldCommonCfg.isSupportPIRSensor())
        {
            toastShow("device does not support PIR sensor");
            return;
        }

        //enable PIR trigger
        KBCfgSensorBase sensorPara = new KBCfgSensorBase();
        sensorPara.setSensorType(KBSensorType.PIR);
        KBTimeRange disablePeriod = new KBTimeRange();
        disablePeriod.localStartHour = 8;
        disablePeriod.localStartMinute = 0;
        disablePeriod.localEndHour = 20;
        disablePeriod.localEndMinute = 0;
        sensorPara.setDisablePeriod0(disablePeriod);

        this.mBeacon.modifyConfig(sensorPara, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                if (bConfigSuccess) {
                    toastShow("Modify para success");
                } else {
                    toastShow("Modify para error:" + error.errorCode);
                }
            }
        });
    }

    //The following example assumes that the setting temperature and humidity parameters
    public void setTHSensorMeasureParameters()
    {
        if (!mBeacon.isConnected())
        {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        KBCfgCommon oldCommonCfg = mBeacon.getCommonCfg();
        if (oldCommonCfg != null && !oldCommonCfg.isSupportHumiditySensor())
        {
            toastShow("Device does not supported ht sensor");
            return;
        }

        //set trigger adv slot information
        KBCfgSensorHT sensorHTPara = new KBCfgSensorHT();
        //enable humidity sensor
        sensorHTPara.setLogEnable(true);

        //unit is second, set measure temperature and humidity interval
        sensorHTPara.setSensorHtMeasureInterval(2);

        //unit is 0.1%, if abs(current humidity - last saved humidity) > 3, then save new record
        sensorHTPara.setHumidityChangeThreshold(30);

        //unit is 0.1 Celsius, if abs(current temperature - last saved temperature) > 0.5, then save new record
        sensorHTPara.setTemperatureChangeThreshold(5);

        //enable sensor advertisement
        mBeacon.modifyConfig(sensorHTPara, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                if (bConfigSuccess)
                {
                    toastShow("config data to beacon success");
                }
                else
                {
                    toastShow("config failed for error:" + error.errorCode);
                }
            }
        });
    }

    //ring device
    public void ringDevice() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check capability
        final KBCfgCommon cfgCommon = (KBCfgCommon)mBeacon.getCommonCfg();
        if (cfgCommon != null && !cfgCommon.isSupportBeep())
        {
            toastShow("device does not support ring feature");
            return;
        }

        mRingButton.setEnabled(false);
        HashMap<String, Object> cmdPara = new HashMap<>(5);
        cmdPara.put("msg", "ring");
        cmdPara.put("ringTime", 20000);   //ring times, uint is ms
        cmdPara.put("ringType", 0x1);  //0x0:led flash only; 0x1:beep alert only;
        cmdPara.put("ledOn", 200);   //valid when ringType set to 0x0 or 0x2
        cmdPara.put("ledOff", 1800); //valid when ringType set to 0x0 or 0x2
        mRingButton.setEnabled(false);
        mBeacon.sendCommand(cmdPara, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mRingButton.setEnabled(true);
                if (bConfigSuccess)
                {
                    toastShow("send command to beacon success");
                }
                else
                {
                    toastShow("send command to beacon error:" + error.errorCode);
                }
            }
        });
    }

    public void readCutoffHistoryInfoExample()
    {
        KBCutoffDataMsg cutOffMsg = new KBCutoffDataMsg();
        cutOffMsg.readSensorDataInfo(mBeacon, new KBSensorDataMsgBase.ReadSensorCallback() {
            @Override
            public void onReadComplete(boolean b, Object o, KBException e) {
                if (b){
                    KBSensorDataMsgBase.ReadSensorInfoRsp infRsp = (KBSensorDataMsgBase.ReadSensorInfoRsp) o;
                    Log.v(LOG_TAG, "Total records:" + infRsp.totalRecordNumber);
                    Log.v(LOG_TAG, "Unread records:" + infRsp.unreadRecordNumber);
                }
            }
        });
    }

    public void readTempHistoryRecordExample()
    {
        KBHumidityDataMsg htDataMsg = new KBHumidityDataMsg();
        htDataMsg.readSensorRecord(mBeacon,
            INVALID_DATA_RECORD_POS, //set to INVALID_DATA_RECORD_POS
            KBSensorReadOption.NewRecord,  //read direction type
            100,   //number of records the app want to read
            new KBSensorDataMsgBase.ReadSensorCallback()
            {
                @Override
                public void onReadComplete(boolean bConfigSuccess,  Object obj, KBException error) {
                    if (bConfigSuccess)
                    {
                        KBHumidityDataMsg.ReadHTSensorDataRsp dataRsp = (KBHumidityDataMsg.ReadHTSensorDataRsp) obj;
                        for (KBHumidityRecord record: dataRsp.readDataRspList)
                        {
                            Log.v(LOG_TAG, "record utc time:" + record.mUtcTime);
                            Log.v(LOG_TAG, "record temperature:" + record.mTemperature);
                            Log.v(LOG_TAG, "record humidity:" + record.mHumidity);
                        }
                        if (dataRsp.readDataNextPos == INVALID_DATA_RECORD_POS)
                        {
                            Log.v(LOG_TAG, "Read data complete");
                        }
                    }
                }
            });
    }

    //read door cutoff history records
    public void readCutoffHistoryRecordExample()
    {
        KBCutoffDataMsg cutoffDataMsg = new KBCutoffDataMsg();
        cutoffDataMsg.readSensorRecord(mBeacon,
                INVALID_DATA_RECORD_POS, //set to INVALID_DATA_RECORD_POS
                KBSensorReadOption.NewRecord,  //read direction type
                100,   //number of records the app want to read
                new KBSensorDataMsgBase.ReadSensorCallback()
                {
                    @Override
                    public void onReadComplete(boolean bConfigSuccess,  Object obj, KBException error) {
                        if (bConfigSuccess)
                        {
                            KBCutoffDataMsg.ReadDoorSensorDataRsp dataRsp = (KBCutoffDataMsg.ReadDoorSensorDataRsp) obj;
                            for (KBCutoffRecord record: dataRsp.readDataRspList)
                            {
                                Log.v(LOG_TAG, "record utc time:" + record.mUtcTime);
                                Log.v(LOG_TAG, "record cut off Flag:" + record.mCutoffFlag);
                            }
                            if (dataRsp.readDataNextPos == INVALID_DATA_RECORD_POS)
                            {
                                Log.v(LOG_TAG, "Read data complete");
                            }
                        }
                    }
                });
    }

    //read door PIR detection history records
    public void readPIRHistoryRecordExample()
    {
        KBPIRDataMsg pirDataMsg = new KBPIRDataMsg();
        pirDataMsg.readSensorRecord(mBeacon,
                INVALID_DATA_RECORD_POS, //set to INVALID_DATA_RECORD_POS
                KBSensorReadOption.NewRecord,  //read direction type
                100,   //number of records the app want to read
                new KBSensorDataMsgBase.ReadSensorCallback()
                {
                    @Override
                    public void onReadComplete(boolean bConfigSuccess,  Object obj, KBException error) {
                        if (bConfigSuccess)
                        {
                            KBPIRDataMsg.ReadPIRSensorDataRsp dataRsp = (KBPIRDataMsg.ReadPIRSensorDataRsp) obj;
                            for (KBPIRRecord record: dataRsp.readDataRspList)
                            {
                                Log.v(LOG_TAG, "record utc time:" + record.mUtcTime);
                                Log.v(LOG_TAG, "record pir indication:" + record.mPirIndication);
                            }
                            if (dataRsp.readDataNextPos == INVALID_DATA_RECORD_POS)
                            {
                                Log.v(LOG_TAG, "Read data complete");
                            }
                        }
                    }
                });
    }
}
