package com.kbeacon.eddystone;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;


import androidx.core.app.ActivityCompat;

import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvType;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBAdvMode;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBAdvTxPower;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvBase;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvEddyTLM;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvEddyUID;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvEddyURL;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgBase;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgCommon;
import com.kkmcn.kbeaconlib2.KBConnState;
import com.kkmcn.kbeaconlib2.KBConnectionEvent;
import com.kkmcn.kbeaconlib2.KBException;
import com.kkmcn.kbeaconlib2.KBeacon;
import com.kkmcn.kbeaconlib2.KBeaconsMgr;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class DevicePannelActivity extends AppBaseActivity implements View.OnClickListener{

    public final static String DEVICE_MAC_ADDRESS = "DEVICE_MAC_ADDRESS";
    private final static String LOG_TAG = "DevicePannel";

    public final static String DEFAULT_PASSWORD = "0000000000000000";   //16 zero ascii

    private KBeaconsMgr mBeaconMgr;
    private String mDeviceAddress;
    private KBeacon mBeacon;
    private final static int PERMISSION_CONNECT = 20;
    //uiview
    private CheckBox mCheckBoxURL, mCheckboxUID, mCheckboxTLM;
    private TextView mBeaconModel;
    private TextView mBeaconVersion;
    private EditText mEditEddyURL;
    private EditText mEditEddyNID;
    private EditText mEditEddySID;
    private EditText mEditBeaconName;
    private Button mDownloadButton;
    private Button mCommandButton;
    private LinearLayout mUrlLayout, mUidLayout, mSettingViewLayout;

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

        setContentView(R.layout.device_pannel);
        mSettingViewLayout = (LinearLayout)findViewById(R.id.beaconConnSetting) ;
        mSettingViewLayout.setVisibility(View.GONE);

        mCheckBoxURL = (CheckBox) findViewById(R.id.checkBoxUrl);

        mCheckBoxURL.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    mUrlLayout.setVisibility(View.VISIBLE);
                }else{
                    mUrlLayout.setVisibility(View.GONE);
                }
            }
        });

        mCheckboxTLM = (CheckBox) findViewById(R.id.checkBoxTLM);
        mCheckboxUID = (CheckBox) findViewById(R.id.checkBoxUID);
        mCheckboxUID.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    mUidLayout.setVisibility(View.VISIBLE);
                }else{
                    mUidLayout.setVisibility(View.GONE);
                }
            }
        });

        mBeaconVersion = (TextView) findViewById(R.id.beaconVersion);
        mBeaconModel = (TextView) findViewById(R.id.beaconModle);

        mUrlLayout = (LinearLayout)findViewById(R.id.eddy_url_layout);
        mUrlLayout.setVisibility(View.GONE);
        mEditEddyURL = (EditText)findViewById(R.id.editEddyURL);
        mEditEddyURL.addTextChangedListener(new TextChangeWatcher(mEditEddyURL));
        mEditEddyURL.setText("https://www.google.com/");


        mUidLayout = (LinearLayout)findViewById(R.id.eddy_uid_layout);
        mUidLayout.setVisibility(View.GONE);
        mEditEddyNID = (EditText)findViewById(R.id.editEddyNid);
        mEditEddyNID.addTextChangedListener(new TextChangeWatcher(mEditEddyNID));
        mEditEddyNID.setText("0x00112233445566778899");

        mEditEddySID = (EditText)findViewById(R.id.editEddySid);
        mEditEddySID.addTextChangedListener(new TextChangeWatcher(mEditEddySID));
        mEditEddySID.setText("0x001122334455");

        mEditBeaconName = (EditText)findViewById(R.id.editBeaconname);
        mEditBeaconName.addTextChangedListener(new TextChangeWatcher(mEditBeaconName));

        mDownloadButton = (Button) findViewById(R.id.buttonSaveData);
        mDownloadButton.setEnabled(false);
        mDownloadButton.setOnClickListener(this);

        mCommandButton = (Button)findViewById(R.id.buttonCommand);
        mCommandButton.setOnClickListener(this);
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
        }
        else if (mBeacon.getState() == KBConnState.Connecting)
        {
            menu.findItem(R.id.menu_connect).setEnabled(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_connecting).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        else
        {
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
        if (v.getId() == R.id.buttonSaveData) {
            updateViewToDevice();
        }
       else if (v.getId() == R.id.buttonCommand) {
            ringDevice();
        }
    }

    public class TextChangeWatcher implements TextWatcher
    {
        private View mEditText;

        TextChangeWatcher(View parent)
        {
            mEditText = parent;
            mEditText.setTag(0);
        }

        public void beforeTextChanged(CharSequence var1, int var2, int var3, int var4)
        {

        }

        public void onTextChanged(CharSequence var1, int var2, int var3, int var4)
        {
            mEditText.setTag(1);
        }

        public void afterTextChanged(Editable var1)
        {

        }

    }

    public void simpleUpdateDeviceTest() {
        if (!mBeacon.isConnected()) {
            return;
        }

        KBCfgAdvEddyUID eddyUIDCfg = new KBCfgAdvEddyUID();
        eddyUIDCfg.setSlotIndex(0);
        eddyUIDCfg.setAdvMode(KBAdvMode.Legacy);
        eddyUIDCfg.setAdvPeriod(1280.0f);
        eddyUIDCfg.setTxPower(KBAdvTxPower.RADIO_Neg4dBm);
        eddyUIDCfg.setNid("0x00112233445566778899");
        eddyUIDCfg.setSid("0x001122334455");

        mBeacon.modifyConfig(eddyUIDCfg, (bConfigSuccess, error) -> {
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

    //read user input and download to KBeacon device
    void updateViewToDevice()
    {
        if (!mBeacon.isConnected())
        {
            return;
        }

        ArrayList<KBCfgBase> cfgList = new ArrayList<>(4);

        //device name
        String strDeviceName = mEditBeaconName.getText().toString();
        if (!strDeviceName.isEmpty() && strDeviceName.length() < KBCfgCommon.MAX_NAME_LENGTH) {
            KBCfgCommon cfgComm = new KBCfgCommon();
            cfgComm.setName(strDeviceName);
            cfgList.add(cfgComm);
        }

        if (mCheckboxUID.isChecked()){
            KBCfgAdvEddyUID eddyUIDCfg = new KBCfgAdvEddyUID();
            eddyUIDCfg.setSlotIndex(0);  //slot 0 adv
            eddyUIDCfg.setAdvMode(KBAdvMode.Legacy);
            eddyUIDCfg.setAdvPeriod(1280.0f);
            eddyUIDCfg.setTxPower(KBAdvTxPower.RADIO_0dBm);

            //nid and sid
            String strNewNID = mEditEddyNID.getText().toString();
            String strNewSID = mEditEddySID.getText().toString();
            eddyUIDCfg.setNid(strNewNID);
            eddyUIDCfg.setSid(strNewSID);
            cfgList.add(eddyUIDCfg);
        }

        if (mCheckBoxURL.isChecked()){
            KBCfgAdvEddyURL urlAdv = new KBCfgAdvEddyURL();
            urlAdv.setSlotIndex(1);  //slot 1 adv
            urlAdv.setAdvMode(KBAdvMode.Legacy);
            urlAdv.setAdvPeriod(1280.0f);

            String strUrl = mEditEddyURL.getText().toString();
            urlAdv.setUrl(strUrl);
            urlAdv.setTxPower(KBAdvTxPower.RADIO_Neg4dBm);
            cfgList.add(urlAdv);
        }

        if (mCheckboxTLM.isChecked()){
            KBCfgAdvEddyTLM eddyTLMCfg = new KBCfgAdvEddyTLM();
            eddyTLMCfg.setSlotIndex(2);  //slot 2 adv
            eddyTLMCfg.setAdvMode(KBAdvMode.Legacy);
            eddyTLMCfg.setAdvPeriod(8000f);
            eddyTLMCfg.setTxPower(KBAdvTxPower.RADIO_Neg4dBm);
            cfgList.add(eddyTLMCfg);
        }

        mDownloadButton.setEnabled(false);
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
                    toastShow("config failed for error:" + error.errorCode);
                }
            }
        });
    }

    public void stopRingDevice() {
        if (!mBeacon.isConnected()) {
            return;
        }

        KBCfgCommon cfgCommon = (KBCfgCommon)mBeacon.getCommonCfg();
        if (cfgCommon != null && !cfgCommon.isSupportBeep())
        {
            Log.e(LOG_TAG, "device does not support ring feature");
            return;
        }

        mDownloadButton.setEnabled(false);
        JSONObject cmdPara = new JSONObject();
        try {
            cmdPara.put("msg", "ring");
            cmdPara.put("ringType", 0);
        }
        catch (JSONException except)
        {
            except.printStackTrace();
            return;
        }

        mBeacon.sendCommand(cmdPara, (bConfigSuccess, error) -> {
            mDownloadButton.setEnabled(true);
            if (bConfigSuccess)
            {
                toastShow("send command to beacon success");
            }
            else
            {
                toastShow("send command to beacon error:" + error.errorCode);
            }
        });
    }

    public void ringDevice() {
        if (!mBeacon.isConnected()) {
            return;
        }

        KBCfgCommon cfgCommon = (KBCfgCommon)mBeacon.getCommonCfg();
        if (!cfgCommon.isSupportBeep())
        {
            Log.e(LOG_TAG, "device does not support ring feature");
            return;
        }

        mDownloadButton.setEnabled(false);
        JSONObject cmdPara = new JSONObject();
        try {
            cmdPara.put("msg", "ring");
            cmdPara.put("ringTime", 20000);   //ring times, uint is ms
            cmdPara.put("ringType", 2 + 1);  //0x1:beep alert; 0x2 led flash;
            cmdPara.put("ledOn", 200);   //valid when ringType set to 0x0 or 0x2
            cmdPara.put("ledOff", 1800); //valid when ringType set to 0x0 or 0x2
        }
        catch (JSONException except)
        {
            except.printStackTrace();
            return;
        }

        mBeacon.sendCommand(cmdPara, (bConfigSuccess, error) -> {
            mDownloadButton.setEnabled(true);
            if (bConfigSuccess)
            {
                toastShow("send command to beacon success");
            }
            else
            {
                toastShow("send command to beacon error:" + error.errorCode);
            }
        });
    }

    //update device's configuration  to UI
    public void updateDeviceToView()
    {
        boolean isTLMEnable, isUIDEnable, isUrlEnable;
        KBCfgCommon commonCfg = (KBCfgCommon) mBeacon.getCommonCfg();
        if (commonCfg != null) {

            //print basic capibility
            Log.v(LOG_TAG, "support iBeacon:" + commonCfg.isSupportIBeacon());
            Log.v(LOG_TAG, "support eddy url:" + commonCfg.isSupportEddyURL());
            Log.v(LOG_TAG, "support eddy tlm:" + commonCfg.isSupportEddyTLM());
            Log.v(LOG_TAG, "support eddy uid:" + commonCfg.isSupportEddyUID());
            Log.v(LOG_TAG, "support ksensor:" + commonCfg.isSupportKBSensor());
            Log.v(LOG_TAG, "beacon has button:" + commonCfg.isSupportButton());
            Log.v(LOG_TAG, "beacon can beep:" + commonCfg.isSupportBeep());
            Log.v(LOG_TAG, "support accleration sensor:" + commonCfg.isSupportAccSensor());
            Log.v(LOG_TAG, "support humidify sensor:" + commonCfg.isSupportHumiditySensor());
            Log.v(LOG_TAG, "support max tx power:" + commonCfg.getMaxTxPower());
            Log.v(LOG_TAG, "support min tx power:" + commonCfg.getMinTxPower());

            //get support trigger
            Log.v(LOG_TAG, "support trigger" + commonCfg.getTrigCapability());

            //device model
            mBeaconModel.setText(commonCfg.getModel());

            //device version
            mBeaconVersion.setText(commonCfg.getVersion());

            //beacon name
            mEditBeaconName.setText(String.valueOf(commonCfg.getName()));

            //check if Eddy UID advertisement enable
            ArrayList<KBCfgAdvBase> cfgUID = mBeacon.getSlotCfgByAdvType(KBAdvType.EddyUID);
            mCheckboxUID.setChecked(cfgUID != null);
            mUidLayout.setVisibility(cfgUID != null? View.VISIBLE : View.GONE );
            if (cfgUID != null) {
                KBCfgAdvEddyUID uidAdv = (KBCfgAdvEddyUID)cfgUID.get(0);
                mEditEddyNID.setText(uidAdv.getNid());
                mEditEddySID.setText(uidAdv.getSid());
            }

            //check if Eddy URL advertisement enable
            ArrayList<KBCfgAdvBase> cfgURL = mBeacon.getSlotCfgByAdvType(KBAdvType.EddyURL);
            mCheckBoxURL.setChecked(cfgURL != null);
            mUrlLayout.setVisibility(cfgURL != null? View.VISIBLE: View.GONE);
            if (cfgURL != null) {
                KBCfgAdvEddyURL urlAdv = (KBCfgAdvEddyURL)cfgURL.get(0);
                mEditEddyURL.setText(urlAdv.getUrl());
            }

            //check if Eddy TLM advertisement enable
            ArrayList<KBCfgAdvBase> cfgTLM = mBeacon.getSlotCfgByAdvType(KBAdvType.EddyTLM);
            mCheckboxTLM.setChecked(cfgTLM != null);
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
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)
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
                mBeacon.connect(DEFAULT_PASSWORD,
                        20 * 1000,
                        connectionDelegate);
            } else {
                toastShow("The app need ble connection permission for start ble scanning");
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.menu_connect){
            if (check2RequestPermission()) {
                mBeacon.connect(DEFAULT_PASSWORD, 20*1000, connectionDelegate);
                invalidateOptionsMenu();
            }
        }
        else if(id == R.id.menu_disconnect){
            mBeacon.disconnect();
            invalidateOptionsMenu();
        }

        return super.onOptionsItemSelected(item);
    }

    private KBeacon.ConnStateDelegate connectionDelegate = new KBeacon.ConnStateDelegate()
    {
        @Override
        public void onConnStateChange(KBeacon beacon, KBConnState state, int nReason) {
            if (state == KBConnState.Connected)
            {
                Log.v(LOG_TAG, "device has connected");
                invalidateOptionsMenu();

                mDownloadButton.setEnabled(true);
                mSettingViewLayout.setVisibility(View.VISIBLE);

                updateDeviceToView();
            }
            else if (state == KBConnState.Connecting)
            {
                Log.v(LOG_TAG, "device start connecting");
                invalidateOptionsMenu();

            }
            else if (state == KBConnState.Disconnecting)
            {
                Log.v(LOG_TAG, "device start disconnecting");
                invalidateOptionsMenu();
            }
            else if (state == KBConnState.Disconnected)
            {
                if (nReason == KBConnectionEvent.ConnAuthFail) {
                    toastShow("password error");
                } else if (nReason == KBConnectionEvent.ConnTimeout) {
                    toastShow("connection timeout");
                } else {
                    toastShow("connection other error, reason:" + nReason);
                }

                mDownloadButton.setEnabled(false);
                mSettingViewLayout.setVisibility(View.GONE);
                Log.e(LOG_TAG, "device has disconnected:" +  nReason);
                invalidateOptionsMenu();
            }
        }
    };
}
