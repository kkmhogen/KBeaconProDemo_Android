package com.kbeacon.ibeacondemo;

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

import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvType;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBAdvMode;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBAdvTxPower;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvBase;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvEBeacon;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvEddyTLM;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvEddyUID;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvEddyURL;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvIBeacon;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgBase;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgCommon;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgTrigger;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBTriggerAction;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBTriggerType;
import com.kkmcn.kbeaconlib2.KBConnPara;
import com.kkmcn.kbeaconlib2.KBConnState;
import com.kkmcn.kbeaconlib2.KBConnectionEvent;
import com.kkmcn.kbeaconlib2.KBErrorCode;
import com.kkmcn.kbeaconlib2.KBException;
import com.kkmcn.kbeaconlib2.KBUtility;
import com.kkmcn.kbeaconlib2.KBeacon;
import com.kkmcn.kbeaconlib2.KBeaconsMgr;
import com.kbeacon.ibeacondemo.R;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

public class DevicePannelActivity extends AppBaseActivity implements View.OnClickListener, KBeacon.ConnStateDelegate{

    public final static String DEVICE_MAC_ADDRESS = "DEVICE_MAC_ADDRESS";
    private final static String LOG_TAG = "DevicePannel";
    public final static String DEFAULT_PASSWORD = "0000000000000000";   //16 zero ascii

    private final static boolean READ_DEFAULT_PARAMETERS = true;

    private final static int PERMISSION_CONNECT = 20;

    private KBeaconsMgr mBeaconMgr;
    private String mDeviceAddress;
    private KBeacon mBeacon;

    //uiview
    private TextView mBeaconType, mBeaconStatus;
    private TextView mBeaconModel;
    private EditText mEditBeaconUUID;
    private EditText mEditBeaconMajor;
    private EditText mEditBeaconMinor;
    private EditText mEditBeaconAdvPeriod;
    private EditText mEditBeaconPassword;
    private EditText mEditBeaconTxPower;
    private EditText mEditBeaconName;
    private Button mDownloadButton, mTriggerButton, mResetButton;
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
        mBeaconType = (TextView) findViewById(R.id.beaconType);
        mBeaconModel = (TextView) findViewById(R.id.beaconModle);
        mEditBeaconUUID = (EditText)findViewById(R.id.editIBeaconUUID);
        mEditBeaconMajor = (EditText)findViewById(R.id.editIBeaconMajor);
        mEditBeaconMinor = (EditText)findViewById(R.id.editIBeaconMinor);
        mEditBeaconAdvPeriod = (EditText)findViewById(R.id.editBeaconAdvPeriod);
        mEditBeaconTxPower = (EditText)findViewById(R.id.editBeaconTxPower);
        mEditBeaconName = (EditText)findViewById(R.id.editBeaconname);
        mDownloadButton = (Button) findViewById(R.id.buttonSaveData);
        mEditBeaconPassword = (EditText)findViewById(R.id.editPassword);
        mDownloadButton.setEnabled(false);
        mDownloadButton.setOnClickListener(this);

        mResetButton = (Button) findViewById(R.id.resetConfigruation);
        mResetButton.setOnClickListener(this);
        findViewById(R.id.beacon2TLM).setOnClickListener(this);
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
        int id = v.getId();
        if (id == R.id.buttonSaveData) {
            updateViewToDevice();
        }else if (id == R.id.enableBtnTrigger) {
            enableButtonTrigger();
        }else if (id == R.id.beacon2TLM) {
            updateKBeaconToIBeaconAndTLM();
        }else if (id == R.id.resetConfigruation) {
            resetParameters();
        }
    }

    //example, update common para
    public void updateBeaconCommonPara() {
        if (!mBeacon.isConnected()) {
            return;
        }

        //change parameters
        KBCfgCommon newCommomCfg = new KBCfgCommon();

        //set device name
        newCommomCfg.setName("KBeaconDemo");

        //set device to always power on
        //the autoAdvAfterPowerOn is enable, the device will not allowed power off by long press button
        newCommomCfg.setAlwaysPowerOn(true);

        //the password length must >=8 bytes and <= 16 bytes
        //Be sure to remember your new password, if you forget it, you won’t be able to connect to it.
        //newCommomCfg.setPassword("123456789");

        ArrayList<KBCfgBase> cfgList = new ArrayList<>(1);
        cfgList.add(newCommomCfg);
        mBeacon.modifyConfig(cfgList, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mDownloadButton.setEnabled(true);
                if (bConfigSuccess)
                {
                    toastShow("config data to beacon success");
                }
                else
                {
                    if (error.errorCode == KBErrorCode.CfgBusy) {
                        Log.e(LOG_TAG, "Device was busy, Maybe another configuration is not complete");
                    }else if (error.errorCode == KBErrorCode.CfgTimeout){
                        Log.e(LOG_TAG, "Sending parameters to device timeout");
                    }

                    toastShow("config failed for error:" + error.errorCode);
                }
            }
        });
    }

    //example: set device broadcasting iBeacon packet at SLOT0
    void updateKBeaconToCodePhyAdvertisement()
    {
        if (!mBeacon.isConnected())
        {
            Log.v(LOG_TAG, "device was disconnected");
            return;
        }

        //check if KBeacon support long range or 2Mbps feature
        KBCfgCommon cfgCommon = mBeacon.getCommonCfg();
        if (cfgCommon == null || !cfgCommon.isSupportBLELongRangeAdv()){
            Log.v(LOG_TAG, "device does not support long range feature");
            return;
        }

        //check if your phone can support code phy, this step is optional.
        // Warning: If your phone does not support code phy, and you enable the device’s code phy broadcast,
        // your phone will not be able to scan the EddyTLM signal.
        if (!mBeaconMgr.isLeCodedPhySupported()){
            Log.e(LOG_TAG, "You phone does not support long range feature, and you device will not scan the KSensor advertisement");
        }

        //set the device to connectable.
        KBCfgAdvIBeacon iBeaconCfg = new KBCfgAdvIBeacon();
        iBeaconCfg.setSlotIndex(0);
        iBeaconCfg.setAdvMode(KBAdvMode.Legacy);
        iBeaconCfg.setAdvConnectable(true);
        iBeaconCfg.setAdvTriggerOnly(false);
        iBeaconCfg.setAdvPeriod(1280.0f);
        iBeaconCfg.setTxPower(KBAdvTxPower.RADIO_0dBm);
        iBeaconCfg.setUuid("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0");
        iBeaconCfg.setMajorID(645);
        iBeaconCfg.setMinorID(741);

        //set slot 1 for advertisement long range advertisement
        KBCfgAdvEddyTLM tlmAdvCfg = new KBCfgAdvEddyTLM();
        tlmAdvCfg.setSlotIndex(1);
        tlmAdvCfg.setAdvMode(KBAdvMode.LongRange);
        tlmAdvCfg.setAdvConnectable(true);
        tlmAdvCfg.setAdvTriggerOnly(false);
        tlmAdvCfg.setAdvPeriod(5000.0f);
        tlmAdvCfg.setTxPower(KBAdvTxPower.RADIO_Pos4dBm);

        ArrayList<KBCfgBase> cfgList = new ArrayList<>(2);
        cfgList.add(iBeaconCfg);
        cfgList.add(tlmAdvCfg);

        mBeacon.modifyConfig(cfgList, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                if (bConfigSuccess)
                {
                    toastShow("Enable iBeacon and Long range advertisement success");
                }
                else
                {
                    toastShow("Enable iBeacon and Long range advertisement failed:" + error.errorCode);
                }
            }
        });
    }

    //example: set device broadcasting iBeacon packet at SLOT0
    void updateKBeaconToIBeacon()
    {
        if (!mBeacon.isConnected())
        {
            return;
        }

        KBCfgAdvIBeacon iBeaconCfg = new KBCfgAdvIBeacon();

        //slot index
        iBeaconCfg.setSlotIndex(0);
        iBeaconCfg.setAdvMode(KBAdvMode.Legacy);

        //set the device to connectable.
        iBeaconCfg.setAdvConnectable(true);

        //always advertisement
        iBeaconCfg.setAdvTriggerOnly(false);

        //adv period and tx power
        iBeaconCfg.setAdvPeriod(1280.0f);
        iBeaconCfg.setTxPower(KBAdvTxPower.RADIO_Neg4dBm);

        //iBeacon para
        iBeaconCfg.setUuid("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0");
        iBeaconCfg.setMajorID(645);
        iBeaconCfg.setMinorID(741);

        mBeacon.modifyConfig(iBeaconCfg, new KBeacon.ActionCallback() {
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

    /**
     *  Example: Beacon broadcasts 5 seconds every 2 minutes in Slot1.
     *  The advertisement interval is 1 second in advertisement period.
     *  That is, the Beacon sleeps for 115 seconds and then broadcasts for 5 seconds.
     */
    void setSlot0PeriodicIBeaconAdv()
    {
        if (!mBeacon.isConnected())
        {
            Log.v(LOG_TAG, "device was disconnected");
            return;
        }

        //check if KBeacon support long range or 2Mbps feature
        KBCfgCommon cfgCommon = mBeacon.getCommonCfg();
        if (cfgCommon == null || !cfgCommon.isSupportIBeacon()){
            Log.v(LOG_TAG, "device does not support iBeacon advertisement");
            return;
        }

        if (!cfgCommon.isSupportTrigger(KBTriggerType.PeriodicallyEvent)){
            Log.v(LOG_TAG, "device does not support Periodically Event");
            return;
        }

        // setting slot1 parameters.
        KBCfgAdvIBeacon periodicAdv = new KBCfgAdvIBeacon();
        periodicAdv.setSlotIndex(1);
        //set adv period, unit is ms
        periodicAdv.setAdvPeriod(1000f);
        periodicAdv.setTxPower(KBAdvTxPower.RADIO_0dBm);
        periodicAdv.setUuid("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0");

        //This parameter is very important, indicating that slot1 does
        // not broadcast by default and only broadcasts when triggered by a Trigger.
        periodicAdv.setAdvTriggerOnly(true);

        //add periodically trigger
        KBCfgTrigger periodicTrigger = new KBCfgTrigger(0, KBTriggerType.PeriodicallyEvent);
        periodicTrigger.setTriggerAction(KBTriggerAction.Advertisement);
        periodicTrigger.setTriggerAdvSlot(1);  //trigger slot 1 advertisement
        periodicTrigger.setTriggerAdvTime(5); //set adv duration to 5 seconds

        //set trigger period, unit is ms
        periodicTrigger.setTriggerPara(120*1000);

        ArrayList<KBCfgBase> cfgList = new ArrayList<>(2);
        cfgList.add(periodicAdv);
        cfgList.add(periodicTrigger);
        mBeacon.modifyConfig(cfgList, (bConfigSuccess, error) -> {
            if (bConfigSuccess)
            {
                toastShow("Enable periodically advertisement success");
            }
            else
            {
                toastShow("Enable periodically advertisement failed:" + error.errorCode);
            }
        });
    }

    //example: set device broadcasting encrypt UUID
    void setSlot0AdvEncrypt()
    {
        if (!mBeacon.isConnected())
        {
            Log.v(LOG_TAG, "device was disconnected");
            return;
        }

        //check if KBeacon support long range or 2Mbps feature
        KBCfgCommon cfgCommon = mBeacon.getCommonCfg();
        if (cfgCommon == null || !cfgCommon.isSupportEBeacon()){
            Log.v(LOG_TAG, "device does not support encrypt advertisement");
            return;
        }

        //set basic parameters.
        KBCfgAdvEBeacon encAdv = new KBCfgAdvEBeacon();
        encAdv.setSlotIndex(0);
        encAdv.setAdvPeriod(1000f);
        encAdv.setTxPower(KBAdvTxPower.RADIO_0dBm);

        //set the UUID that to be encrypt
        encAdv.setUuid("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0");

        //Set the AES KEY to change every 5 seconds.
        encAdv.setInterval(5);

        //set aes type to 0(ECB)
        encAdv.setAesType(KBCfgAdvEBeacon.AES_ECB_TYPE);

        mBeacon.modifyConfig(encAdv, (bConfigSuccess, error) -> {
            if (bConfigSuccess)
            {
                toastShow("Enable encrypt advertisement success");
            }
            else
            {
                toastShow("Enable encrypt advertisement failed:" + error.errorCode);
            }
        });
    }

    //example: check if parameters changed
    void updateModifyParaToDevice()
    {
        if (!mBeacon.isConnected())
        {
            return;
        }

        //First we get the current configuration of SLOT0, and then we only need to send the parameters that modified.
        KBCfgAdvBase oldCfgPara = mBeacon.getSlotCfg(0);
        if (oldCfgPara != null && oldCfgPara.getAdvType() == KBAdvType.IBeacon)
        {
            KBCfgAdvIBeacon oldIBeaconPara = (KBCfgAdvIBeacon) oldCfgPara;
            boolean bModification = false;
            KBCfgAdvIBeacon iBeaconCfg = new KBCfgAdvIBeacon();
            iBeaconCfg.setSlotIndex(0);  //must be parameters

            if (oldIBeaconPara.getAdvMode() != KBAdvMode.Legacy){
                iBeaconCfg.setAdvMode(KBAdvMode.Legacy);
                bModification = true;
            }

            if (!oldIBeaconPara.isAdvConnectable()){
                iBeaconCfg.setAdvConnectable(true);
                bModification = true;
            }

            if (oldIBeaconPara.isAdvTriggerOnly()){
                iBeaconCfg.setAdvTriggerOnly(false);
                bModification = true;
            }

            if (oldIBeaconPara.getAdvPeriod() != 1280.0f){
                iBeaconCfg.setAdvPeriod(1280.0f);
                bModification = true;
            }

            if (oldIBeaconPara.getTxPower() != KBAdvTxPower.RADIO_Neg4dBm){
                iBeaconCfg.setTxPower(KBAdvTxPower.RADIO_Neg4dBm);
                bModification = true;
            }

            if (oldIBeaconPara.getUuid().equals("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0")){
                iBeaconCfg.setUuid("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0");
                bModification = true;
            }

            if (oldIBeaconPara.getMajorID() != 645){
                iBeaconCfg.setMinorID(645);
                bModification = true;
            }

            if (oldIBeaconPara.getMinorID() != 741){
                iBeaconCfg.setMinorID(741);
                bModification = true;
            }


            //send parameters to device
            if (bModification) {
                mBeacon.modifyConfig(iBeaconCfg, new KBeacon.ActionCallback() {
                    @Override
                    public void onActionComplete(boolean bConfigSuccess, KBException error) {
                        if (bConfigSuccess) {
                            toastShow("config data to beacon success");
                        } else {
                            toastShow("config failed for error:" + error.errorCode);
                        }
                    }
                });
            }else{
                Log.v(LOG_TAG, "Parameters not change, not need to sending");
            }
        }
        else
        {
            updateKBeaconToIBeacon();
        }
    }

    //example: update KBeacon to broadcasting iBeacon at SLOT0 and EddyTLM at SLOT1
    //sometimes we need KBeacon broadcasting both iBeacon and TLM packet (battery level, Temperature, power on times
    void updateKBeaconToIBeaconAndTLM()
    {
        if (!mBeacon.isConnected())
        {
            return;
        }

        //iBeacon paramaters
        KBCfgAdvIBeacon iBeaconCfg = new KBCfgAdvIBeacon();
        iBeaconCfg.setSlotIndex(0);
        iBeaconCfg.setAdvMode(KBAdvMode.Legacy);
        iBeaconCfg.setTxPower(KBAdvTxPower.RADIO_Neg8dBm);
        iBeaconCfg.setAdvPeriod(1280.0f);
        iBeaconCfg.setAdvTriggerOnly(false);  //always advertisement
        iBeaconCfg.setUuid("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0");
        iBeaconCfg.setMajorID(6545);
        iBeaconCfg.setMinorID(1458);

        //TLM parameters
        KBCfgAdvEddyTLM tlmCfg = new KBCfgAdvEddyTLM();
        tlmCfg.setSlotIndex(1);
        tlmCfg.setAdvMode(KBAdvMode.Legacy);
        tlmCfg.setTxPower(KBAdvTxPower.RADIO_0dBm);
        tlmCfg.setAdvPeriod(8000.0f);
        tlmCfg.setAdvTriggerOnly(false);  //always advertisement

        //modify
        ArrayList<KBCfgBase> cfgList = new ArrayList<>(2);
        cfgList.add(iBeaconCfg);
        cfgList.add(tlmCfg);
        mBeacon.modifyConfig(cfgList, new KBeacon.ActionCallback() {
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

    //example: set device broadcasting URL packet at SLOT0
    void updateKBeaconToURL()
    {
        if (!mBeacon.isConnected())
        {
            return;
        }

        KBCfgAdvEddyURL urlCfg = new KBCfgAdvEddyURL();
        urlCfg.setSlotIndex(0);
        urlCfg.setAdvMode(KBAdvMode.Legacy);
        urlCfg.setAdvConnectable(true);
        urlCfg.setAdvTriggerOnly(false);
        urlCfg.setAdvPeriod(1280.0f);
        urlCfg.setTxPower(KBAdvTxPower.RADIO_Neg4dBm);

        //URL para
        urlCfg.setUrl("https://www.google.com/");

        //send parameters to device
        mBeacon.modifyConfig(urlCfg, new KBeacon.ActionCallback() {
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

    //example: set device broadcasting UID packet at SLOT0
    void updateKBeaconToUID()
    {
        if (!mBeacon.isConnected())
        {
            return;
        }

        KBCfgAdvEddyUID uidCfg = new KBCfgAdvEddyUID();
        uidCfg.setSlotIndex(0);
        uidCfg.setAdvMode(KBAdvMode.Legacy);
        uidCfg.setAdvConnectable(true);
        uidCfg.setAdvTriggerOnly(false);
        uidCfg.setAdvPeriod(1280.0f);
        uidCfg.setTxPower(KBAdvTxPower.RADIO_Neg4dBm);

        //UID para
        uidCfg.setNid("0x00010203040506070809");
        uidCfg.setSid("0x010203040506");

        //send parameters to device
        mBeacon.modifyConfig(uidCfg, new KBeacon.ActionCallback() {
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

    // The device always broadcast UUID B9407F30-F5F8-466E-AFF9-25556B57FE67. When device detects button press,
    // it triggers the broadcast of the iBeacon message(uuid=B9407F30-F5F8-466E-AFF9-25556B570001) in Slot1,
    // and the iBeacon broadcast duration is 10 seconds.
    public void enableButtonTrigger() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        final KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getCommonCfg();
        if (oldCommonCfg != null && !oldCommonCfg.isSupportButton())
        {
            toastShow("device is not support humidity");
            return;
        }

        //set slot0 to always advertisement
        final KBCfgAdvIBeacon iBeaconAdv = new KBCfgAdvIBeacon();
        iBeaconAdv.setSlotIndex(0);  //reuse previous slot
        iBeaconAdv.setAdvPeriod(1280f);
        iBeaconAdv.setAdvMode(KBAdvMode.Legacy);
        iBeaconAdv.setTxPower(KBAdvTxPower.RADIO_Neg4dBm);
        iBeaconAdv.setAdvConnectable(true);
        iBeaconAdv.setAdvTriggerOnly(false);  //always advertisement
        iBeaconAdv.setUuid("B9407F30-F5F8-466E-AFF9-25556B57FE67");
        iBeaconAdv.setMajorID(12);
        iBeaconAdv.setMinorID(10);

        //set slot 1 to trigger adv information
        final KBCfgAdvIBeacon triggerAdv = new KBCfgAdvIBeacon();
        triggerAdv.setSlotIndex(1);
        triggerAdv.setAdvPeriod(211.25f);
        triggerAdv.setAdvMode(KBAdvMode.Legacy);
        triggerAdv.setTxPower(KBAdvTxPower.RADIO_Pos4dBm);
        triggerAdv.setAdvConnectable(false);
        triggerAdv.setAdvTriggerOnly(true);  //trigger only advertisement
        triggerAdv.setUuid("B9407F30-F5F8-466E-AFF9-25556B570001");
        triggerAdv.setMajorID(1);
        triggerAdv.setMinorID(1);

        //set trigger type
        KBCfgTrigger btnTriggerPara = new KBCfgTrigger(0, KBTriggerType.BtnSingleClick);
        btnTriggerPara.setTriggerAdvChangeMode(0);
        btnTriggerPara.setTriggerAction(KBTriggerAction.Advertisement);
        btnTriggerPara.setTriggerAdvSlot(1);
        btnTriggerPara.setTriggerAdvTime(10);

        //enable push button trigger
        mTriggerButton.setEnabled(false);
        ArrayList<KBCfgBase> cfgList = new ArrayList<>(2);
        cfgList.add(iBeaconAdv);
        cfgList.add(triggerAdv);
        cfgList.add(btnTriggerPara);
        this.mBeacon.modifyConfig(cfgList, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mTriggerButton.setEnabled(true);
                if (bConfigSuccess) {
                    toastShow("enable push button trigger success");
                } else {
                    toastShow("enable push button trigger error:" + error.errorCode);
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
            toastShow("device is not support humidity");
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


    void updateViewToDevice()
    {
        if (!mBeacon.isConnected())
        {
            return;
        }
        KBCfgCommon oldCommonCfg = mBeacon.getCommonCfg();
        KBCfgAdvIBeacon iBeaconCfg = new KBCfgAdvIBeacon();

        //slot index
        iBeaconCfg.setSlotIndex(0);
        iBeaconCfg.setAdvMode(KBAdvMode.Legacy);

        //set the device to un-connectable or connectable.
        // Warning: if the app set the KBeacon to un-connectable, the app cannot connect to the device.
        //When the device enters the unconnectable mode, you can enter it in the following ways:
        //1. If the Button Trigger is not enabled, you can press the button of the device and the device will enter the connectable broadcast for 30 seconds.
        //2. When the device is powered on again, the device will enter the connectable broadcast within 30 seconds after it is powered on.
        iBeaconCfg.setAdvConnectable(true);

        //When enabled, this slot does not broadcast by default, and it only broadcasts when the Trigger event is triggered.
        iBeaconCfg.setAdvTriggerOnly(false); //always advertisement

        //adv period, check if user change adv period
        String strAdvPeriod = mEditBeaconAdvPeriod.getText().toString();
        if (Utils.isPositiveInteger(strAdvPeriod)) {
            Float newAdvPeriod = Float.valueOf(strAdvPeriod);
            iBeaconCfg.setAdvPeriod(newAdvPeriod);
        }

        //tx power, check if user change tx power
        String strTxPower = mEditBeaconTxPower.getText().toString();
        if (Utils.isPositiveInteger(strTxPower) || Utils.isMinusInteger(strTxPower)) {
            Integer newTxPower = Integer.valueOf(strTxPower);
            if (newTxPower > oldCommonCfg.getMaxTxPower() || newTxPower < oldCommonCfg.getMinTxPower()) {
                toastShow("tx power not valid");
                return;
            }
            iBeaconCfg.setTxPower(newTxPower);
        }

        //iBeacon data
        String uuid = mEditBeaconUUID.getText().toString();
        if (KBUtility.isUUIDString(uuid)) {
            iBeaconCfg.setUuid(uuid);
        }else{
            toastShow("UUID not valid");
            return;
        }

        //iBeacon major id data
        String strMajorID = mEditBeaconMajor.getText().toString();
        if (Utils.isPositiveInteger(strMajorID))
        {
            Integer majorID = Integer.valueOf(strMajorID);
            iBeaconCfg.setMajorID(majorID);
        }

        //iBeacon major id data
        String strMinorID = mEditBeaconMinor.getText().toString();
        if (Utils.isPositiveInteger(strMinorID))
        {
            Integer minorID = Integer.valueOf(strMinorID);
            iBeaconCfg.setMinorID(minorID);
        }

        mDownloadButton.setEnabled(false);
        mBeacon.modifyConfig(iBeaconCfg, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mDownloadButton.setEnabled(true);
                if (bConfigSuccess)
                {
                    toastShow("config data to beacon success");
                }
                else
                {
                    if (error.errorCode == KBErrorCode.CfgBusy)
                    {
                        toastShow("Another configruation is not complete");
                    }
                    else
                    {
                        toastShow("config failed for error:" + error.errorCode);
                    }
                }
            }
        });
    }

    //example: reset all parameters to default
    public void resetParameters() {
        if (!mBeacon.isConnected()) {
            return;
        }

        mDownloadButton.setEnabled(false);
        JSONObject cmdPara = new JSONObject();
        try {
            cmdPara.put("msg", "admin");
            cmdPara.put("stype", "reset");
        }catch (JSONException except)
        {
            except.printStackTrace();
            return;
        }
        mResetButton.setEnabled(false);
        mBeacon.sendCommand(cmdPara, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mResetButton.setEnabled(true);
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

    public void updateDeviceToView()
    {
        KBCfgCommon commonCfg = mBeacon.getCommonCfg();
        KBCfgAdvBase slot0Adv = mBeacon.getSlotCfg(0);

        if (commonCfg != null) {
            //print basic capibility
            Log.v(LOG_TAG, "support iBeacon:" + commonCfg.isSupportIBeacon());
            Log.v(LOG_TAG, "support eddy url:" + commonCfg.isSupportEddyURL());
            Log.v(LOG_TAG, "support eddy tlm:" + commonCfg.isSupportEddyTLM());
            Log.v(LOG_TAG, "support eddy uid:" + commonCfg.isSupportEddyUID());
            Log.v(LOG_TAG, "support ksensor:" + commonCfg.isSupportKBSensor());
            Log.v(LOG_TAG, "beacon has button:" + commonCfg.isSupportButton());
            Log.v(LOG_TAG, "beacon can beep:" + commonCfg.isSupportBeep());
            Log.v(LOG_TAG, "support acceleration sensor:" + commonCfg.isSupportAccSensor());
            Log.v(LOG_TAG, "support humidity sensor:" + commonCfg.isSupportHumiditySensor());
            Log.v(LOG_TAG, "support PIR sensor:" + commonCfg.isSupportPIRSensor());
            Log.v(LOG_TAG, "support CO2 sensor:" + commonCfg.isSupportCO2Sensor());
            Log.v(LOG_TAG, "support light sensor:" + commonCfg.isSupportLightSensor());
            Log.v(LOG_TAG, "support VOC sensor:" + commonCfg.isSupportVOCSensor());
            Log.v(LOG_TAG, "support max tx power:" + commonCfg.getMaxTxPower());
            Log.v(LOG_TAG, "support min tx power:" + commonCfg.getMinTxPower());
            Log.v(LOG_TAG, "device battery:" + commonCfg.getBatteryPercent());

            //slot adv type list
            ArrayList<KBCfgAdvBase> advArrays = mBeacon.getSlotCfgList();
            String strAdvArrays = "";
            if (advArrays != null) {
                for (KBCfgAdvBase adv : advArrays) {
                    strAdvArrays = strAdvArrays + "Slot:" + adv.getSlotIndex() +
                            ":" + KBAdvType.getAdvTypeString(adv.getAdvType()) + "|";
                }
            }
            mBeaconType.setText(strAdvArrays);

            //device model
            mBeaconModel.setText(commonCfg.getModel());

            //device name
            mEditBeaconName.setText(String.valueOf(commonCfg.getName()));

            //slot 0 parameters
            ArrayList<KBCfgAdvBase> allIBeaconAdvs = mBeacon.getSlotCfgByAdvType(KBAdvType.IBeacon);
            if (allIBeaconAdvs != null) {
                KBCfgAdvIBeacon iBeaconPara = (KBCfgAdvIBeacon)allIBeaconAdvs.get(0);
                mEditBeaconUUID.setText(iBeaconPara.getUuid());
                mEditBeaconMajor.setText(String.valueOf(iBeaconPara.getMajorID()));
                mEditBeaconMinor.setText(String.valueOf(iBeaconPara.getMinorID()));

                if (slot0Adv != null) {
                    mEditBeaconAdvPeriod.setText(String.valueOf(slot0Adv.getAdvPeriod()));
                    mEditBeaconTxPower.setText(String.valueOf(slot0Adv.getTxPower()));
                }
            }
        }
    }


    private KBConnState nDeviceConnState = KBConnState.Disconnected;

    public void onConnStateChange(KBeacon beacon, KBConnState state, int nReason)
    {
        if (state == KBConnState.Connected)
        {
            Log.v(LOG_TAG, "device has connected");
            invalidateOptionsMenu();

            mDownloadButton.setEnabled(true);

            updateDeviceToView();

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

            mDownloadButton.setEnabled(false);
            Log.e(LOG_TAG, "device has disconnected:" +  nReason);
            invalidateOptionsMenu();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mBeacon.getState() == KBConnState.Connected
            || mBeacon.getState() == KBConnState.Connecting){
            mBeacon.disconnect();
            invalidateOptionsMenu();
        }
    }


    public boolean check2RequestPermission()
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

            if (DevicePannelActivity.READ_DEFAULT_PARAMETERS)
            {
                //connect to device with default parameters
                if (check2RequestPermission()) {
                    mBeacon.connect(mPref.getPassword(mDeviceAddress), 20 * 1000, this);
                }
            }
            else
            {
                //connect to device with specified parameters
                //When the app is connected to the KBeacon device, the app can specify which the configuration parameters to be read,
                //The parameter that can be read include: common parameters, advertisement parameters, trigger parameters, and sensor parameters
                KBConnPara connPara = new KBConnPara();
                connPara.syncUtcTime = true;
                connPara.readCommPara = true;
                connPara.readSlotPara = true;
                connPara.readTriggerPara = false;
                connPara.readSensorPara = false;
                mBeacon.connectEnhanced(mPref.getPassword(mDeviceAddress), 20 * 1000,
                        connPara,
                        this);
            }

            invalidateOptionsMenu();
        }
        else if(id == R.id.menu_disconnect){
            mBeacon.disconnect();
            invalidateOptionsMenu();
        }

        return super.onOptionsItemSelected(item);
    }
}
