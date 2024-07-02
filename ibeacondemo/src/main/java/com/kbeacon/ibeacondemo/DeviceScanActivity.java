/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kbeacon.ibeacondemo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAccSensorValue;
import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvPacketBase;
import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvPacketEddyTLM;
import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvPacketEddyUID;
import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvPacketEddyURL;
import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvPacketIBeacon;
import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvPacketSensor;
import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvPacketSystem;
import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvType;
import com.kkmcn.kbeaconlib2.KBeacon;
import com.kkmcn.kbeaconlib2.KBeaconsMgr;
import java.util.HashMap;
import java.util.Locale;

import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends AppBaseActivity implements AdapterView.OnItemClickListener,
        KBeaconsMgr.KBeaconMgrDelegate, LeDeviceListAdapter.ListDataSource{
	private final static String TAG = "Beacon.ScanAct";//DeviceScanActivity.class.getSimpleName();

    private ListView mListView;
    private LeDeviceListAdapter mDevListAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int mScanFailedContinueNum = 0;

    private static String LOG_TAG = "DeviceScanActivity";

    private final static int  MAX_ERROR_SCAN_NUMBER = 2;
    private HashMap<String, KBeacon> mBeaconsDictory;
    private KBeacon[] mBeaconsArray;
    private KBeaconsMgr mBeaconsMgr;

    private static final int PERMISSION_COARSE_LOCATION = 22;
    private static final int PERMISSION_FINE_LOCATION = 23;
    private static final int PERMISSION_SCAN = 24;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (mBeaconsMgr.isScanning()) {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);

        } else {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
        setTitle(R.string.device_list);

        mBeaconsDictory = new HashMap<>(50);

        mBeaconsMgr = KBeaconsMgr.sharedBeaconManager(this);
        if (mBeaconsMgr == null)
        {
            toastShow("make sure the phone has support ble funtion");
            finish();
            return;
        }
        mBeaconsMgr.delegate = this;
        //mBeaconsMgr.delegate = beaconMgrExample;
        mBeaconsMgr.setScanMinRssiFilter(-40);
        mBeaconsMgr.setScanMode(KBeaconsMgr.SCAN_MODE_LOW_LATENCY);

        mListView = (ListView) findViewById(R.id.listview);
        mDevListAdapter = new LeDeviceListAdapter(this, getApplicationContext());
        mListView.setAdapter(mDevListAdapter);
        mListView.setOnItemClickListener(this);

        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_container);
        //设置刷新时动画的颜色，可以设置4个
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_red_light, android.R.color.holo_orange_light, android.R.color.holo_green_light);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                // TODO Auto-generated method stub
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        swipeRefreshLayout.setRefreshing(false);
                        if (mScanFailedContinueNum >= MAX_ERROR_SCAN_NUMBER) {
                            mScanFailedContinueNum = 0;
                            new AlertDialog.Builder(DeviceScanActivity.this)
                                    .setTitle(R.string.common_error_title)
                                    .setMessage(R.string.bluetooth_error_need_reboot)
                                    .setPositiveButton(R.string.Dialog_OK, null)
                                    .show();
                        }else{
                            clearAllData();
                            mDevListAdapter.notifyDataSetChanged();
                        }
                    }
                }, 500);
            }
        });
    }

    public void clearAllData()
    {
        mBeaconsDictory.clear();
        mBeaconsArray = null;
        mBeaconsMgr.clearBeacons();
    }

    public void onBeaconDiscovered(KBeacon[] beacons)
    {
        for (KBeacon pBeacons: beacons)
        {
            mBeaconsDictory.put(pBeacons.getMac(), pBeacons);
        }
        if (mBeaconsDictory.size() > 0) {
            mBeaconsArray = new KBeacon[mBeaconsDictory.size()];
            mBeaconsDictory.values().toArray(mBeaconsArray);
            mDevListAdapter.notifyDataSetChanged();
        }
    }

    //example for print all scanned packet
    KBeaconsMgr.KBeaconMgrDelegate beaconMgrExample = new KBeaconsMgr.KBeaconMgrDelegate() {
        //get advertisement packet during scanning callback
        public void onBeaconDiscovered(KBeacon[] beacons) {
            for (KBeacon beacon : beacons) {
                //get beacon adv common info
                Log.v(LOG_TAG, "beacon mac:" + beacon.getMac());
                Log.v(LOG_TAG, "beacon name:" + beacon.getName());
                Log.v(LOG_TAG, "beacon rssi:" + beacon.getRssi());

                //get adv packet
                for (KBAdvPacketBase advPacket : beacon.allAdvPackets()) {
                    switch (advPacket.getAdvType()) {
                        case KBAdvType.IBeacon: {
                            KBAdvPacketIBeacon advIBeacon = (KBAdvPacketIBeacon) advPacket;
                            Log.v(LOG_TAG, "iBeacon uuid:" + advIBeacon.getUuid());
                            Log.v(LOG_TAG, "iBeacon major:" + advIBeacon.getMajorID());
                            Log.v(LOG_TAG, "iBeacon minor:" + advIBeacon.getMinorID());
                            break;
                        }

                        case KBAdvType.EddyTLM: {
                            KBAdvPacketEddyTLM advTLM = (KBAdvPacketEddyTLM) advPacket;
                            Log.v(LOG_TAG, "TLM battery:" + advTLM.getBatteryLevel());
                            Log.v(LOG_TAG, "TLM Temperature:" + advTLM.getTemperature());
                            Log.v(LOG_TAG, "TLM adv count:" + advTLM.getAdvCount());
                            break;
                        }

                        case KBAdvType.Sensor: {
                            KBAdvPacketSensor advSensor = (KBAdvPacketSensor) advPacket;
                            Log.v(LOG_TAG, "Sensor battery:" + advSensor.getBatteryLevel());
                            Log.v(LOG_TAG, "Sensor temp:" + advSensor.getTemperature());

                            //device that has acc sensor
                            KBAccSensorValue accPos = advSensor.getAccSensor();
                            if (accPos != null) {
                                String strAccValue = String.format(Locale.ENGLISH, "x:%d; y:%d; z:%d",
                                        accPos.xAis, accPos.yAis, accPos.zAis);
                                Log.v(LOG_TAG, "Sensor Acc:" + strAccValue);
                            }

                            //device that has humidity sensor
                            if (advSensor.getHumidity() != null) {
                                Log.v(LOG_TAG, "Sensor humidity:" + advSensor.getHumidity());
                            }

                            //device that has cutoff sensor
                            if (advSensor.getWatchCutoff() != null) {
                                Log.v(LOG_TAG, "cutoff flag:" + advSensor.getWatchCutoff());
                            }

                            //device that has PIR sensor
                            if (advSensor.getPirIndication() != null) {
                                Log.v(LOG_TAG, "pir indication:" + advSensor.getPirIndication());
                            }
                            break;
                        }

                        case KBAdvType.EddyUID: {
                            KBAdvPacketEddyUID advUID = (KBAdvPacketEddyUID) advPacket;
                            Log.v(LOG_TAG, "UID Nid:" + advUID.getNid());
                            Log.v(LOG_TAG, "UID Sid:" + advUID.getSid());
                            break;
                        }

                        case KBAdvType.EddyURL: {
                            KBAdvPacketEddyURL advURL = (KBAdvPacketEddyURL) advPacket;
                            Log.v(LOG_TAG, "URL:" + advURL.getUrl());
                            break;
                        }

                        case KBAdvType.System: {
                            KBAdvPacketSystem advSystem = (KBAdvPacketSystem) advPacket;
                            Log.v(LOG_TAG, "System mac:" + advSystem.getMacAddress());
                            Log.v(LOG_TAG, "System model:" + advSystem.getModel());
                            Log.v(LOG_TAG, "System batt:" + advSystem.getBatteryPercent());
                            Log.v(LOG_TAG, "System ver:" + advSystem.getVersion());
                            break;
                        }

                        default:
                            break;
                    }
                }

                //clear all buffered packet
                beacon.removeAdvPacket();
            }
        }

        public void onCentralBleStateChang(int nNewState)
        {
            if (nNewState == KBeaconsMgr.BLEStatePowerOff)
            {
                Log.e(LOG_TAG, "BLE function is power off");
            }
            else if (nNewState == KBeaconsMgr.BLEStatePowerOn)
            {
                Log.e(LOG_TAG, "BLE function is power on");
            }
        }

        public void onScanFailed(int errorCode)
        {
            Log.e(LOG_TAG, "Start N scan failed：" + errorCode);
            if (mScanFailedContinueNum >= MAX_ERROR_SCAN_NUMBER){
                toastShow("scan encount error, error time:" + mScanFailedContinueNum);
            }
            mScanFailedContinueNum++;
        }
    };

    public void onCentralBleStateChang(int nNewState)
    {
        Log.e(TAG, "centralBleStateChang：" + nNewState);
    }

    public void onScanFailed(int errorCode)
    {
        Log.e(TAG, "Start N scan failed：" + errorCode);
        if (mScanFailedContinueNum >= MAX_ERROR_SCAN_NUMBER){
            toastShow("scan encount error, error time:" + mScanFailedContinueNum);
        }
        mScanFailedContinueNum++;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.menu_scan){
            handleStartScan();
            invalidateOptionsMenu();
        }
        else if(id == R.id.menu_stop){
            mBeaconsMgr.stopScanning();
            invalidateOptionsMenu();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Log.e(TAG, "click id:" + id );
        KBeacon beacon = getBeaconDevice(position);
        if (beacon != null) {
            final Intent intent = new Intent(DeviceScanActivity.this, DevicePannelActivity.class);
            intent.putExtra(DevicePannelActivity.DEVICE_MAC_ADDRESS, beacon.getMac());
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mBeaconsMgr.stopScanning();
        invalidateOptionsMenu();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mBeaconsMgr.clearBeacons();
    }

    private void handlePeriodChk(){
        long currTick = System.currentTimeMillis();
    }

    private void handleStartScan(){
        if (!checkBluetoothPermitAllowed())
        {
            toastShow("BLE scanning need location permission");
            return;
        }

        //mBeaconsMgr.setScanMinRssiFilter(-60);
        int nStartScan = mBeaconsMgr.startScanning();
        if (nStartScan == 0)
        {
            Log.v(TAG, "start scan success");
        }
        else if (nStartScan == KBeaconsMgr.SCAN_ERROR_BLE_NOT_ENABLE) {
            toastShow("BLE function is not enable");
        }
        else if (nStartScan == KBeaconsMgr.SCAN_ERROR_NO_PERMISSION) {
            toastShow("BLE scanning has no location permission");
        }
        else
        {
            toastShow("BLE scanning unknown error");
        }
    }
    private boolean checkBluetoothPermitAllowed() {
        boolean bHasPermission = true;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_FINE_LOCATION);
            bHasPermission = false;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_COARSE_LOCATION);
            bHasPermission = false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN},
                        PERMISSION_SCAN);
                bHasPermission = false;
            }
        }

        return bHasPermission;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_SCAN){
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                toastShow("The app need ble scanning permission for start ble scanning");
            }
        }
        if (requestCode == PERMISSION_COARSE_LOCATION){
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                toastShow("The app need coarse location permission for start ble scanning");
            }
        }
        if (requestCode == PERMISSION_FINE_LOCATION){
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                toastShow("The app need fine location permission for start ble scanning");
            }
        }
    }


    public KBeacon getBeaconDevice(int nIndex)
    {
        if (mBeaconsArray != null && mBeaconsArray.length > nIndex)
        {
            return mBeaconsArray[nIndex];
        }
        else
        {
            return null;
        }
    }

    public int getCount()
    {
        if (mBeaconsArray == null){
            return 0;
        }else{
            return mBeaconsArray.length;
        }
    }
}