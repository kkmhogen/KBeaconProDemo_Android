package com.kkmcn.sensordemo;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
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
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvEddyTLM;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvEddyUID;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvIBeacon;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgSensorBase;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgSensorCO2;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgSensorGEO;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgSensorHT;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgSensorLight;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgSensorPIR;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgSensorScan;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgSensorVOC;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgTrigger;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgTriggerAngle;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgTriggerMotion;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBSensorType;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBTimeRange;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBTriggerAction;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBTriggerAdvChgMode;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBTriggerType;
import com.kkmcn.kbeaconlib2.KBConnState;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBRecordAlarm;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBRecordBase;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBRecordDataRsp;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBRecordHumidity;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBRecordInfoRsp;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBRecordLight;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBRecordPIR;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBRecordVOC;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBSensorReadOption;
import com.kkmcn.kbeaconlib2.KBUtility;
import com.kkmcn.sensordemo.dfulibrary.KBeaconDFUActivity;
import com.kkmcn.sensordemo.recordhistory.CfgHTBeaconHistoryActivity;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgBase;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgCommon;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvKSensor;
import com.kkmcn.kbeaconlib2.KBConnectionEvent;
import com.kkmcn.kbeaconlib2.KBException;
import com.kkmcn.kbeaconlib2.KBeacon;
import com.kkmcn.kbeaconlib2.KBeaconsMgr;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import androidx.core.app.ActivityCompat;

public class DevicePannelActivity extends AppBaseActivity implements View.OnClickListener,
        KBeacon.ConnStateDelegate, KBeacon.NotifyDataDelegate{

    public final static String DEVICE_MAC_ADDRESS = "DEVICE_MAC_ADDRESS";
    private final static String LOG_TAG = "DevicePanel";

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
//            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_connecting).setVisible(false);
            menu.findItem(R.id.menu_connecting).setActionView(null);
        }
        return true;
    }

    @Override
    public void onClick(View v)
    {
        int id = v.getId();
        if (id == R.id.readBtnTriggerPara) {
            readButtonTriggerPara();
        }else if (id == R.id.enableBtnAdvTrigger) {
            enableButtonTriggerEvent2Adv();
        }else if (id == R.id.enableBtnAppTrigger) {
            enableButtonTriggerEvent2App();
        }else if (id == R.id.enableAccTrigger) {
            //acc trigger
            enableMotionTrigger();
        }else if (id == R.id.disableAccAppTrigger) {
            disableMotionTrigger();
        }else if (id == R.id.enableAccAdvertisement) {
            //ksensor advertisement
            enableAdvTypeIncludeAccXYZ();
        }else if (id == R.id.enableTHAdvertisement) {
            enableAdvTypeIncludeAccTH();
        }else if (id == R.id.viewTHDataHistory) {
            if (mBeacon.isConnected()) {
                KBCfgCommon commCfg = mBeacon.getCommonCfg();
                if (commCfg != null && commCfg.isSupportHumiditySensor()) {
                    Intent intent = new Intent(this, CfgHTBeaconHistoryActivity.class);
                    intent.putExtra(CfgHTBeaconHistoryActivity.DEVICE_MAC_ADDRESS, mBeacon.getMac());   //field type
                    startActivityForResult(intent, 1);
                } else {
                    toastShow("not support humidity sensor");
                }
            } else {
                toastShow("device not connected");
            }
        }else if (id == R.id.enableTHChangeTriggerEvtRpt2Adv) {
            //T&H trigger
            enableTHTriggerEvtRpt2Adv();
        }else if (id == R.id.enableTHChangeTriggerEvtRpt2App) {
            enableTHTriggerEvtRpt2App();
        }else if (id == R.id.enablePeriodicallyTHDataToApp) {
            enableTHPeriodicallyTriggerRpt2App();
        }else if (id == R.id.dfuDevice) {
            //DFU service
            if (mBeacon.isConnected()) {
                final Intent intent = new Intent(this, KBeaconDFUActivity.class);
                intent.putExtra(KBeaconDFUActivity.DEVICE_MAC_ADDRESS, mBeacon.getMac());
                startActivityForResult(intent, 1);
            }
        }else if (id == R.id.ringDevice) {
           ringDevice();
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
                String strSoftVersion = cfgCommon.getVersion();
                this.mBeaconModel.setText(cfgCommon.getModel() + "," + strSoftVersion);
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
        btnTriggerPara.setTriggerAdvTxPower(KBAdvTxPower.RADIO_Pos4dBm);

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
                Log.i(LOG_TAG, "enable app trigger result:" + bConfigSuccess);
            }
        });

        //subscribe all notify
        mBeacon.subscribeSensorDataNotify(null, DevicePannelActivity.this, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                Log.i(LOG_TAG, "subscribe app trigger event result:" + bConfigSuccess);
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
            toastShow("The device does not support button trigger");
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
            toastShow("The device does not motion trigger");
            return;
        }

        // set trigger adv slot information
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
        thTriggerPara.setTriggerPara(80);

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

    // When the beacon detects light level > x, the device will record the event
    public void enableLightTrigger() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        final KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getCommonCfg();
        if (oldCommonCfg != null && !oldCommonCfg.isSupportLightSensor())
        {
            toastShow("device does not support light sensor");
            return;
        }

        //enable light trigger
        KBCfgTrigger lightTriggerPara = new KBCfgTrigger(0, KBTriggerType.LightLUXAbove);

        //Save the Light event to memory flash and report it to the APP at the same time
        lightTriggerPara.setTriggerAction(KBTriggerAction.Record | KBTriggerAction.Report2App);

        //If light level > 500 lx, then record the event and report event to app
        lightTriggerPara.setTriggerPara(500);

        this.mBeacon.modifyConfig(lightTriggerPara, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                if (bConfigSuccess) {
                    toastShow("enable light trigger success");
                } else {
                    toastShow("enable light trigger error:" + error.errorCode);
                }
            }
        });
    }

    public void settingChannelMask(){
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check capability
        final KBCfgCommon cfgCommon = mBeacon.getCommonCfg();
        if (cfgCommon != null && !cfgCommon.isSupportAdvChannelMask())
        {
            toastShow("device does not support channel mask");
            return;
        }

        KBCfgAdvIBeacon iBeaconAdv = new KBCfgAdvIBeacon();
        iBeaconAdv.setSlotIndex(0);

        //disable advertisement on channel 38 and 39
        iBeaconAdv.setAdvChanelMask(KBCfgAdvBase.ADV_CH_38_DISABLE_MASK | KBCfgAdvBase.ADV_CH_39_DISABLE_MASK);

        iBeaconAdv.setAdvPeriod(1022.5f);
        iBeaconAdv.setTxPower(0);
        iBeaconAdv.setUuid("B9407F30-F5F8-466E-AFF9-25556B570003");
        iBeaconAdv.setMajorID(10);
        iBeaconAdv.setMinorID(155);

        mBeacon.modifyConfig(iBeaconAdv, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mRingButton.setEnabled(true);
                if (bConfigSuccess)
                {
                    toastShow("modify adv channel mask success");
                }
                else
                {
                    toastShow("modify adv channel mask failed:" + error.errorCode);
                }
            }
        });
    }

    //set light sensor measure parameters
    public void setLightSensorMeasureParameters()
    {
        if (!mBeacon.isConnected())
        {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        KBCfgCommon oldCommonCfg = mBeacon.getCommonCfg();
        if (oldCommonCfg != null && !oldCommonCfg.isSupportLightSensor())
        {
            toastShow("Device does not supported light sensor");
            return;
        }

        KBCfgSensorLight sensorLightPara = new KBCfgSensorLight();
        //enable light measure log
        sensorLightPara.setLogEnable(true);

        //unit is second, set measure interval to 5 seconds
        sensorLightPara.setMeasureInterval(5);

        //unit is 1 lx, if abs(current light level - last saved light level) > 20, then log new record
        sensorLightPara.setLogChangeThreshold(20);

        //enable sensor advertisement
        mBeacon.modifyConfig(sensorLightPara, new KBeacon.ActionCallback() {
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

    //read light sensor history records
    public void readLightHistoryRecordExample()
    {
        mBeacon.readSensorRecord(KBSensorType.Light,
                KBRecordDataRsp.INVALID_DATA_RECORD_POS, //set to INVALID_DATA_RECORD_POS
                KBSensorReadOption.NewRecord,  //read direction type
                100,   //number of records the app want to read
                (bConfigSuccess, dataRsp, error) -> {
                    if (bConfigSuccess)
                    {
                        for (KBRecordBase sensorRecord: dataRsp.readDataRspList)
                        {
                            KBRecordLight record = (KBRecordLight)sensorRecord;
                            Log.v(LOG_TAG, "Light utc time:" + record.utcTime);
                            Log.v(LOG_TAG, "Light level:" + record.lightLevel);
                        }
                        if (dataRsp.readDataNextPos == KBRecordDataRsp.INVALID_DATA_RECORD_POS)
                        {
                            Log.v(LOG_TAG, "Read data complete");
                        }
                    }
                });
    }
	
    //set parking sensor measure parameters
    public void setParkingSensorMeasureParameters()
    {
        if (!mBeacon.isConnected())
        {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        KBCfgCommon oldCommonCfg = mBeacon.getCommonCfg();
        if (oldCommonCfg != null && !oldCommonCfg.isSupportGEOSensor())
        {
            toastShow("Device does not supported parking sensors");
            return;
        }

        KBCfgSensorGEO sensorGeoPara = new KBCfgSensorGEO();

        //Set the geomagnetic offset value of the parking space occupancy relative to the idle parking space
        //unit is mg
        sensorGeoPara.setParkingThreshold(2000);

        //If the setting continuously detects geomagnetic changes for more than 50 seconds,
        //the device will generate a parking space occupancy event. the Delay unit is 10 seconds
        sensorGeoPara.setParkingDelay(5);

        //enable sensor advertisement
        mBeacon.modifyConfig(sensorGeoPara, new KBeacon.ActionCallback() {
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


    //force sensor calibration
    public void geomagneticCalibration() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }
        if (!mBeacon.isConnected())
        {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        KBCfgCommon oldCommonCfg = mBeacon.getCommonCfg();
        if (oldCommonCfg != null && !oldCommonCfg.isSupportGEOSensor())
        {
            toastShow("Device does not supported parking sensors");
            return;
        }

        KBCfgSensorGEO sensorGeoPara = new KBCfgSensorGEO();

        //If this parameter is set to true, the sensor initiates the mag sensor calibration
        sensorGeoPara.setCalibration(true);

        //enable sensor advertisement
        mBeacon.modifyConfig(sensorGeoPara, (bConfigSuccess, error) -> {
            if (bConfigSuccess)
            {
                toastShow("config data to beacon success");
            }
        });
    }

      //set parking idle
    //Parking sensors need to be marked before use. That is, when there is no parking, we need to set
    // the sensor to idle. The sensor detects if a vehicle is parked based on the status of the marker.
    public void setParkingIdleParameters()
    {
        if (!mBeacon.isConnected())
        {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        KBCfgCommon oldCommonCfg = mBeacon.getCommonCfg();
        if (oldCommonCfg != null && !oldCommonCfg.isSupportGEOSensor())
        {
            toastShow("Device does not supported parking sensors");
            return;
        }

        KBCfgSensorGEO sensorGeoPara = new KBCfgSensorGEO();

        //If this parameter is set to true, the sensor initiates the measurement
        // and sets the current state to the idle parking state.
        sensorGeoPara.setParkingTag(true);

        //enable sensor advertisement
        mBeacon.modifyConfig(sensorGeoPara, (bConfigSuccess, error) -> {
            if (bConfigSuccess)
            {
                toastShow("config data to beacon success");
            }
        });
    }


    //read battery percent when connected
    public void readBatteryPercent()
    {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //get battery percent(the SDK will read battery level after authentication)
        final KBCfgCommon commPara = mBeacon.getCommonCfg();
        if (commPara != null)
        {
            Log.v(LOG_TAG, "old battery percent:" + commPara.getBatteryPercent());
        }

        //read new battery percent from device again
        mBeacon.readCommonConfig((result, jsonObject, error) -> {
            //read complete
            if (result && jsonObject != null) {
                final KBCfgCommon newCommPara = mBeacon.getCommonCfg();
                Log.v(LOG_TAG, "new battery percent:" + newCommPara.getBatteryPercent());

                //--------also the JSON object contain battery percent
                //jsonObject.getInt("btPt")
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
        sensorHTPara.setMeasureInterval(2);

        //unit is 0.1%, if abs(current humidity - last saved humidity) > 3, then save new record
        sensorHTPara.setHumidityLogThreshold(30);

        //unit is 0.1 Celsius, if abs(current temperature - last saved temperature) > 0.5, then save new record
        sensorHTPara.setTemperatureLogThreshold(5);

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

    //update CO2 sensor parameters
    public void setCO2SensorMeasureParameters()
    {
        if (!mBeacon.isConnected())
        {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        KBCfgCommon oldCommonCfg = mBeacon.getCommonCfg();
        if (oldCommonCfg != null && !oldCommonCfg.isSupportCO2Sensor())
        {
            toastShow("Device does not supported CO2 sensor");
            return;
        }

        KBCfgSensorCO2 sensorCO2Para = new KBCfgSensorCO2();
        //enable light measure log
        sensorCO2Para.setLogEnable(true);

        //unit is second, set measure interval to 120 seconds
        sensorCO2Para.setMeasureInterval(120);

        //unit is 1 ppm, if abs(current CO2 level - last saved CO2 level) > 20, then new record created
        sensorCO2Para.setLogCO2SaveThreshold(20);

        //enable sensor advertisement
        mBeacon.modifyConfig(sensorCO2Para, (bConfigSuccess, error) -> {
            if (bConfigSuccess)
            {
                toastShow("config data to beacon success");
            }
            else
            {
                toastShow("config failed for error:" + error.errorCode);
            }
        });
    }

    public void otherSensorParameters()
    {
        //pir sensor
        KBCfgSensorPIR pirSensor = new KBCfgSensorPIR();
        pirSensor.setLogBackoffTime(30);

        //voc sensor
        KBCfgSensorVOC vocSensor = new KBCfgSensorVOC();
        vocSensor.setMeasureInterval(40);
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
        JSONObject cmdPara = new JSONObject();
        try {
            cmdPara.put("msg", "ring");
            cmdPara.put("ringTime", 20000);   //ring times, uint is ms

            // 0: stop ring
            // 0x1: Beep
            // 0x2: LED flash
            // 0x4: vibration
            int ringType = 0x2;   //LED flash default

            //check if need beep
            if (cfgCommon != null && !cfgCommon.isSupportBeep())
            {
                ringType = ringType | 0x1;
            }
            cmdPara.put("ringType", ringType);  //beep and LED flash
            cmdPara.put("ledOn", 100);   //valid when ringType set to 0x2/0x4
            cmdPara.put("ledOff", 900); //valid when ringType set to 0x2/0x4
        }catch (JSONException exception)
        {
            exception.printStackTrace();
            return;
        }

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
        mBeacon.readSensorDataInfo(KBSensorType.Alarm, new KBeacon.ReadSensorInfoCallback() {
            @Override
            public void onReadComplete(boolean b, KBRecordInfoRsp infRsp, KBException e) {
                if (b){
                    Log.v(LOG_TAG, "Total records:" + infRsp.totalRecordNumber);
                    Log.v(LOG_TAG, "Unread records:" + infRsp.unreadRecordNumber);
                    Log.v(LOG_TAG, "Device clock:" + infRsp.readInfoUtcSeconds);
                }
            }
        });
    }

    /*
    Example1: The app read un-read history records in KBeacon device.
    Each time the records was read, the unread pointer in the KBeacon will move to next.
    * */
    public void readTempHistoryRecordExample()
    {
        mBeacon.readSensorRecord(KBSensorType.HTHumidity,
            KBRecordDataRsp.INVALID_DATA_RECORD_POS, //set to INVALID_DATA_RECORD_POS
            KBSensorReadOption.NewRecord,  //read direction type
            100,   //number of records the app want to read
                (bSuccess, dataRsp, error) -> {
                    if (bSuccess)
                    {
                        for (KBRecordBase sensorRecord: dataRsp.readDataRspList)
                        {
                            KBRecordHumidity record = (KBRecordHumidity)sensorRecord;
                            Log.v(LOG_TAG, "record utc time:" + record.utcTime);
                            Log.v(LOG_TAG, "record temperature:" + record.temperature);
                            Log.v(LOG_TAG, "record humidity:" + record.humidity);
                        }
                        if (dataRsp.readDataNextPos == KBRecordDataRsp.INVALID_DATA_RECORD_POS)
                        {
                            Log.v(LOG_TAG, "Read data complete");
                        }
                    }
                });
    }

    //reverse read record example
    private long mNextReadReverseIndex = KBRecordDataRsp.INVALID_DATA_RECORD_POS;
    private int mTotalReverseReadIndex = 0;
    public void readTempHistoryRecordReverseExample()
    {
        mBeacon.readSensorRecord(KBSensorType.HTHumidity,
                mNextReadReverseIndex, //read from last pos
                KBSensorReadOption.ReverseOrder,  //read direction type
                50,   //number of records the app want to read
                (bSuccess, dataRsp, error) -> {
                    if (bSuccess)
                    {
                        mNextReadReverseIndex = dataRsp.readDataNextPos;
                        for (KBRecordBase sensorRecord: dataRsp.readDataRspList)
                        {
                            KBRecordHumidity record = (KBRecordHumidity)sensorRecord;
                            Log.v(LOG_TAG, mTotalReverseReadIndex
                                    +": utc time:" + record.utcTime
                                    + ",temperature:" + record.temperature
                                    + ",humidity:" + record.humidity);
                            mTotalReverseReadIndex++;
                        }
                        if (dataRsp.readDataNextPos == KBRecordDataRsp.INVALID_DATA_RECORD_POS)
                        {
                            Log.v(LOG_TAG, "Read data complete");
                        }
                        else
                        {
                            Log.v(LOG_TAG, "next read position:" + dataRsp.readDataNextPos);
                        }
                    }
                });
    }

    //reverse read record example
    private long mNextReadNormalIndex = 10;
    private int mTotalReadNormalIndex = 0;
    public void readTempHistoryRecordNormalExample()
    {
        mBeacon.readSensorRecord(KBSensorType.HTHumidity,
                mNextReadNormalIndex, //read from last pos
                KBSensorReadOption.NormalOrder,  //read direction type
                10,   //number of records the app want to read
                (bSuccess, dataRsp, error) -> {
                    if (bSuccess)
                    {
                        mNextReadNormalIndex = dataRsp.readDataNextPos;
                        for (KBRecordBase sensorRecord: dataRsp.readDataRspList)
                        {
                            KBRecordHumidity record = (KBRecordHumidity)sensorRecord;
                            Log.v(LOG_TAG, mTotalReadNormalIndex
                                    +": utc time:" + record.utcTime
                                    + ",temperature:" + record.temperature
                                    + ",humidity:" + record.humidity);
                            mTotalReadNormalIndex++;
                        }
                        if (dataRsp.readDataNextPos == KBRecordDataRsp.INVALID_DATA_RECORD_POS)
                        {
                            Log.v(LOG_TAG, "Read data complete");
                        }
                        else
                        {
                            Log.v(LOG_TAG, "next read position:" + dataRsp.readDataNextPos);
                        }
                    }
                    else
                    {
                        Log.e(LOG_TAG, "read data failed:" + error.errorCode);
                    }
                });
    }

    //read door open/close history records
    public void readCutoffHistoryRecordExample()
    {
        mBeacon.readSensorRecord(KBSensorType.Alarm,
                KBRecordDataRsp.INVALID_DATA_RECORD_POS, //set to INVALID_DATA_RECORD_POS
                KBSensorReadOption.NewRecord,  //read direction type
                100,   //number of records the app want to read
                (bSuccess, dataRsp, error) -> {
                    if (bSuccess)
                    {
                        for (KBRecordBase sensorRecord: dataRsp.readDataRspList)
                        {
                            KBRecordAlarm record = (KBRecordAlarm)sensorRecord;
                            Log.v(LOG_TAG, "record utc time:" + record.utcTime);
                            Log.v(LOG_TAG, "record cut off Flag:" + record.alarmStatus);
                        }
                        if (dataRsp.readDataNextPos == KBRecordDataRsp.INVALID_DATA_RECORD_POS)
                        {
                            Log.v(LOG_TAG, "Read data complete");
                        }
                    }
                });
    }

    //read door PIR detection history records
    public void readPIRHistoryRecordExample()
    {
        mBeacon.readSensorRecord(KBSensorType.PIR,
                KBRecordDataRsp.INVALID_DATA_RECORD_POS, //set to INVALID_DATA_RECORD_POS
                KBSensorReadOption.NewRecord,  //read direction type
                100,   //number of records the app want to read
                (bSuccess, dataRsp, error) -> {
                    if (bSuccess)
                    {
                        for (KBRecordBase sensorRecord: dataRsp.readDataRspList)
                        {
                            KBRecordPIR record = (KBRecordPIR)sensorRecord;
                            Log.v(LOG_TAG, "record utc time:" + record.utcTime);
                            Log.v(LOG_TAG, "record pir indication:" + record.pirIndication);
                        }
                        if (dataRsp.readDataNextPos == KBRecordDataRsp.INVALID_DATA_RECORD_POS)
                        {
                            Log.v(LOG_TAG, "Read data complete");
                        }
                    }
                });
    }

    public void powerOffDevice() {
        if (!mBeacon.isConnected()) {
            return;
        }

        JSONObject cmdPara = new JSONObject();
        try {
            cmdPara.put("msg", "admin");
            cmdPara.put("stype", "pwroff");
        }
        catch (JSONException except)
        {
            except.printStackTrace();
            return;
        }

        mBeacon.sendCommand(cmdPara, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                if (bConfigSuccess)
                {
                    toastShow("send power off command to beacon success");
                }
                else
                {
                    toastShow("send power pff command to beacon error:" + error.errorCode);
                }
            }
        });
    }

    public void resetParameters() {
        if (!mBeacon.isConnected()) {
            return;
        }

        JSONObject cmdPara = new JSONObject();
        try {
            cmdPara.put("msg", "admin");
            cmdPara.put("stype", "reset");
        }
        catch (JSONException except)
        {
            except.printStackTrace();
            return;
        }

        mBeacon.sendCommand(cmdPara, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                if (bConfigSuccess)
                {
                    //disconnect with device to make sure the new parameters take effect
                    mBeacon.disconnect();
                    toastShow("send reset command to beacon success");
                }
                else
                {
                    toastShow("send reset command to beacon error:" + error.errorCode);
                }
            }
        });
    }



    //Example4: read VOC sensor history records
    public void readVOCHistoryRecordExample()
    {
        mBeacon.readSensorRecord(KBSensorType.Light,
                KBRecordDataRsp.INVALID_DATA_RECORD_POS, //set to INVALID_DATA_RECORD_POS
                KBSensorReadOption.NewRecord,  //read direction type
                100,   //number of records the app want to read
                (bConfigSuccess, dataRsp, error) -> {
                    if (bConfigSuccess)
                    {
                        for (KBRecordBase sensorRecord: dataRsp.readDataRspList)
                        {
                            KBRecordVOC record = (KBRecordVOC)sensorRecord;
                            Log.v(LOG_TAG, "VOC utc time:" + record.utcTime);
                            Log.v(LOG_TAG, "VOC index:" + record.vocIndex);
                        }
                        if (dataRsp.readDataNextPos == KBRecordDataRsp.INVALID_DATA_RECORD_POS)
                        {
                            Log.v(LOG_TAG, "Read data complete");
                        }
                    }
                });
    }

    //set tilt angle trigger
    public void enableTiltAngleTrigger()
    {
        //check capability
        final KBCfgCommon cfgCommon = (KBCfgCommon)mBeacon.getCommonCfg();
        if (cfgCommon != null && !cfgCommon.isSupportTrigger(KBTriggerType.AccAngle))
        {
            Log.e(LOG_TAG, "device does not support acc tilt angle trigger");
            return;
        }

        //set tilt angle trigger
        KBCfgTriggerAngle angleTrigger = new KBCfgTriggerAngle();
        angleTrigger.setTriggerAction(KBTriggerAction.Advertisement | KBTriggerAction.Report2App);
        angleTrigger.setTriggerAdvSlot(0);

        //set trigger angle
        angleTrigger.setTriggerPara(45);        //set below angle threshold
        angleTrigger.setAboveAngle(90);         //set above angle threshold
        angleTrigger.setReportingInterval(1);   //set repeat report interval to 1 minutes

        mBeacon.modifyConfig(angleTrigger,
                (bConfigSuccess, error) -> {
                    if (bConfigSuccess)
                    {
                        Log.v(LOG_TAG, "Enable angle trigger success");
                    }
                    else
                    {
                        Log.v(LOG_TAG, "Enable angle trigger failed");
                    }
                });
    }

    //set repeater scanning
    public void enableRepeaterScanner()
    {
        //check capability
        final KBCfgCommon cfgCommon = mBeacon.getCommonCfg();
        if (!cfgCommon.isSupportTrigger(KBTriggerType.PeriodicallyEvent)
                || !cfgCommon.isSupportScanSensor())
        {
            toastShow("device does not support repeat scanning");
            return;
        }

        //set scanner parameters
        KBCfgSensorScan scanPara = new KBCfgSensorScan();
        scanPara.setScanDuration(100); //set scan duration 1seconds, unit is 10 ms
        scanPara.setScanMode(KBAdvMode.Legacy); //only scan BLE4.0 legacy advertisement
        scanPara.setScanRssi(-80); //Scan devices with signals greater than -80dBm
        //The maximum number of peripheral devices during each scan
        // When the number of devices scanned exceed 20, then stop scanning.
        scanPara.setScanMax(20);

        // Set a Trigger to periodically trigger scanning
        KBCfgTrigger periodicTrigger = new KBCfgTrigger(0, KBTriggerType.PeriodicallyEvent);

        //When a trigger occurs, it triggers a BLE scan and carries the scanned parameters in the broadcast.
        periodicTrigger.setTriggerAction(KBTriggerAction.BLEScan | KBTriggerAction.Advertisement);
        periodicTrigger.setTriggerAdvSlot(0);
        periodicTrigger.setTriggerAdvPeriod(500f);
        periodicTrigger.setTriggerAdvTime(10);
        periodicTrigger.setTriggerAdvTxPower(0);

        //When a trigger occurs, change the UUID to carry the MAC address of the scanned peripheral device.
        periodicTrigger.setTriggerAdvChangeMode(KBTriggerAdvChgMode.KBTriggerAdvChangeModeUUID);

        //Set to start scanning every 60 seconds, unit is ms
        periodicTrigger.setTriggerPara(60*1000);

        ArrayList<KBCfgBase> triggerPara = new ArrayList<>(2);
        triggerPara.add(scanPara);
        triggerPara.add(periodicTrigger);

        mBeacon.modifyConfig(triggerPara,
                (bConfigSuccess, error) -> {
                    if (bConfigSuccess)
                    {
                        toastShow("Enable periodic scanning success");
                    }
                    else
                    {
                        toastShow("Enable periodic scanning failed");
                    }
                });
    }
}
