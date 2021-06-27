#KBeacon Android SDK Instruction DOC（English）

----
## 1. Introduction
With this SDK, you can scan and configure the KBeacon device. The SDK include follow main class:
* KBeaconsMgr: Global definition, responsible for scanning KBeacon devices advertisment packet, and monitoring the Bluetooth status of the system;  

* KBeacon: An instance of a KBeacon device, KBeaconsMgr creates an instance of KBeacon while it found a physical device. Each KBeacon instance has three properties: KBAdvPacketHandler, KBAuthHandler, KBCfgHandler.
* KBAdvPacketHandler: parsing advertisement packet. This attribute is valid during the scan phase.  
*	KBAuthHandler: Responsible for the authentication operation with the KBeacon device after the connection is established.  
*	KBCfgHandler：Responsible for configuring parameters related to KBeacon devices.  
* DFU Library: Responsible for KBeacon firmware update.
![avatar](https://github.com/kkmhogen/KBeaconProDemo_Android/blob/master/kbeacon_class_arc.png?raw=true)

**Scanning Stage**

in this stage, KBeaconsMgr will scan and parse the advertisement packet about KBeacon devices, and it will create "KBeacon" instance for every founded devices, developers can get all advertisements data by its allAdvPackets or getAdvPacketByType function.

**Connection Stage**

After a KBeacon connected, developer can make some changes of the device by modifyConfig.


## 2. Android demo
There are 3 projects in this SDK, if you plan to use KBeacon for iBeacon related applications. I suggest you develop based on iBeacondemo.  
If your application is based on the Eddystone protocol, I suggest you start from eddystonedemo.  
If you are using KBeacon for sensors, such as temperature, humidity, and motion monitoring, please start from sensordemo.

## 3. Import SDK to project
Development environment:  
Android Studio  
minSdkVersion 21

1. The kbeaconlib library can be found on maven repository. Add it to your application project by adding the following dependency in your build.gradle:

```Java
dependencies {
   …
   implementation 'com.kkmcn.kbeaconlib2:kbeaconlib2:1.0.6'
}
```

2. In your root project’s build.gradle file, make sure to include maven repository.
```Java
buildscript {
    repositories {
        mavenCentral()
    }
}
```

3. Add the Bluetooth permissions and the corresponding component registration under the AndroidManifest.xml file. As follows:

```Java
<uses-feature
    android:name="android.hardware.bluetooth_le" android:required="true" />
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```  

For android version > 10, if you want the app scanning KBeacons in background, please add:  

```Java
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

```

## 4. How to use SDK
### 4.1 Scanning device
1. Init KBeaconMgr instance in Activity, also your application should implementation the scanning callback.

```Java
@Override
public void onCreate(Bundle savedInstanceState) {
	//other code...
	//get KBeacon central manager instance
	mBeaconsMgr = KBeaconsMgr.sharedBeaconManager(this);
	if (mBeaconsMgr == null)
	{
	    toastShow("Make sure the phone supports BLE function");
	    return;
	}
	//other code...  
}  
```

2. Request permission:  
In Android-6.0 or later, Bluetooth scanning requires location permissions, so the app should request permission before start scanning like follows:
```Java
//for android6, the app need corse location permission for BLE scanning
if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_COARSE_LOCATION);
}
//for android10, the app need fine location permission for BLE scanning
if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
}
```

3. Start scanning
```Java
mBeaconsMgr.delegate = beaconMgrDeletate;
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
```

3. Before start scanning, the app should implementation KBeaconMgr delegate for get scanning result
```Java
KBeaconsMgr.KBeaconMgrDelegate beaconMgrDeletate = new KBeaconsMgr.KBeaconMgrDelegate()
    {
        //get advertisement packet during scanning callback
        public void onBeaconDiscovered(KBeacon[] beacons)
        {
            for (KBeacon beacon: beacons)
            {
                //get beacon adv common info
                Log.v(LOG_TAG, "beacon mac:" + beacon.getMac());
                Log.v(LOG_TAG, "beacon name:" + beacon.getName());
                Log.v(LOG_TAG,"beacon rssi:" + beacon.getRssi());

                //get adv packet
                for (KBAdvPacketBase advPacket : beacon.allAdvPackets())
                {
                    switch (advPacket.getAdvType())
                    {
                        case KBAdvType.KBAdvTypeIBeacon:
                        {
                            KBAdvPacketIBeacon advIBeacon = (KBAdvPacketIBeacon)advPacket;
                            Log.v(LOG_TAG,"iBeacon uuid:" + advIBeacon.getUuid());
                            Log.v(LOG_TAG,"iBeacon major:" + advIBeacon.getMajorID());
                            Log.v(LOG_TAG,"iBeacon minor:" + advIBeacon.getMinorID());
                            break;
                        }

                        case KBAdvType.KBAdvTypeEddyTLM:
                        {
                            KBAdvPacketEddyTLM advTLM = (KBAdvPacketEddyTLM)advPacket;
                            Log.v(LOG_TAG,"TLM battery:" + advTLM.getBatteryLevel());
                            Log.v(LOG_TAG,"TLM Temperature:" + advTLM.getTemperature());
                            Log.v(LOG_TAG,"TLM adv count:" + advTLM.getAdvCount());
                            break;
                        }
                    }
                }

                mBeaconsDictory.put(beacon.getMac(), beacon);
            }

            if (mBeaconsDictory.size() > 0) {
                mBeaconsArray = new KBeacon[mBeaconsDictory.size()];
                mBeaconsDictory.values().toArray(mBeaconsArray);
                mDevListAdapter.notifyDataSetChanged();
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
```

4. Handle KSensor data from advertisement packet
```Java
//get advertisement packet during scanning callback
public void onBeaconDiscovered(KBeacon[] beacons)
{
	KBAdvPacketSensor advSensor = (KBAdvPacketSensor)device.getAdvPacketByType(KBAdvType.Sensor);
	if (advSensor != null)
	{
	   KBAccSensorValue accPos = advSensor.getAccSensor();
	   if (accPos != null) {
	      strAccValue = String.format(Locale.ENGLISH, "x:%d; y:%d; z:%d",
	            accPos.xAis, accPos.yAis, accPos.zAis);
	   }
	}
}
```

5. Clean scanning result and stop scanning  
After start scanning, The KBeaconMgr will buffer all found KBeacon device. If the app wants to remove all buffered KBeacon device, the app can:  
```Java
mBeaconsMgr.clearBeacons();
```
If the app wants to stop scanning:
```Java
mBeaconsMgr. stopScanning();
```

### 4.2 Connect to device
 1. If the app wants to change the device parameters, then it need connect to the device.
 ```Java
mBeacon.connect(password, max_timeout, connectionDelegate);
 ```
* Password: device password, the default password is 0000000000000000
* max_timeout: max connection time, unit is milliseconds.
* connectionDelegate: connection callback.

2. The app can handle connection result by follow:
 ```Java
private KBeacon.ConnStateDelegate connectionDelegate = new KBeacon.ConnStateDelegate()
{
    public void onConnStateChange(KBeacon var1, int state, int nReason)
    {
        if (state == KBeacon.KBStateConnected)
        {
            Log.v(LOG_TAG, "device has connected");
            nDeviceLastState = state;
        }
        else if (state == KBeacon.KBStateConnecting)
        {
            Log.v(LOG_TAG, "device start connecting");
            nDeviceLastState = state;
        }
        else if (state == KBeacon.KBStateDisconnecting)
        {
            Log.v(LOG_TAG, "device start disconnecting");
            nDeviceLastState = state;
        }
        else if (state == KBeacon.KBStateDisconnected)
        {
            if (nReason == KBConnectionEvent.ConnAuthFail) {
                toastShow("password error");
            } else if (nReason == KBConnectionEvent.ConnTimeout) {
                toastShow("connection timeout");
            } else {
                toastShow("connection other error, reason:" + nReason);
            }

            nDeviceLastState = state;
            Log.e(LOG_TAG, "device has disconnected:" +  nReason);
        }
    }
};
 ```

3. Disconnect from the device.
 ```Java
mBeacon.disconnect();
 ```

### 4.3 Configure parameters
#### 4.3.1 Advertisement type
KBeacon devices can support broadcasting multiple type advertisement packets in parallel.  
For example, advertisement type was set to “iBeacon + TLM + System”, then the device will send advertisement packet like follow.   

|Slot No.|0|1|2|3|4|
|----|----|----|----|----|----|
|`Adv type`|iBeacon|TLM |System|None|None|
|`Adv Interval(ms)`|1022.5|8000.0|8000.0|NA|NA|
|`Tx power(dBm)`|0|4|-12|NA|NA|


**Notify:**  
  For the advertisement period, Apple has some suggestions that make the device more easily discovered by IOS phones. (The suggest value was: 152.5 ms; 211.25 ms; 318.75 ms; 417.5 ms; 546.25 ms; 760 ms; 852.5 ms; 1022.5 ms; 1285 ms). For more information, please refer to Section 3.5 in "Bluetooth Accessory Design Guidelines for Apple Products". The document link: https://developer.apple.com/accessories/Accessory-Design-Guidelines.pdf.

#### 4.3.2 Get device parameters
After the app connect to KBeacon success. The KBeacon will automatically read current parameters from KBeacon device. so the app can update UI and show the parameters to user after connection setup.  
 ```Java
private KBeacon.ConnStateDelegate connectionDelegate = new KBeacon.ConnStateDelegate()
{
    public void onConnStateChange(KBeacon var1, KBConnState state, int nReason)
    {
        if (state == KBConnState.Connected)
        {
            Log.v(LOG_TAG, "device has connected");
	          updateDeviceToView();
        }
    }
};

//update device's configuration to UI
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
        Log.v(LOG_TAG, "support accleration sensor:" + commonCfg.isSupportAccSensor());
        Log.v(LOG_TAG, "support humidify sensor:" + commonCfg.isSupportHumiditySensor());
        Log.v(LOG_TAG, "support max tx power:" + commonCfg.getMaxTxPower());
        Log.v(LOG_TAG, "support min tx power:" + commonCfg.getMinTxPower());

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
            mEditBeaconAdvPeriod.setText(String.valueOf(slot0Adv.getAdvPeriod()));
            mEditBeaconTxPower.setText(String.valueOf(slot0Adv.getTxPower()));
        }
    }
}
 ```

#### 4.3.3 Update advertisement parameters

After app connects to device success, the app can update parameters of device.

##### 4.3.3.1 Update common parameters
The app can modify the basic parameters of KBeacon through the KBCfgCommon class. The KBCfgCommon has follow parameters:

* name: device name, the device name must <= 18 character

* advType: beacon type, can be setting to iBeacon, KSesnor, Eddy TLM/UID/ etc.,

* advPeriod: advertisement period, the value can be set to 100~10000ms

* txPower: advertisement TX power, unit is dBm.

* autoAdvAfterPowerOn: if autoAdvAfterPowerOn was setting to true, the beacon always advertisement if it has battery. If this value was setting to false, the beacon will power off if long press button for 5 seconds.

* tlmAdvInterval: eddystone TLM advertisement interval. The default value is 10. The KBeacon will send 1 TLM advertisement every 10 advertisement packets

* refPower1Meters: the rx power at 1 meters

* advConnectable: is beacon advertisement can be connectable.  
  **Warning:**   
   if the app set the KBeacon to un-connectable, the app cannot connect to it again if it does not has button. If the device has button, the device can enter connect-able advertisement for 60 seconds when click on the button

* password: device password, the password length must >= 8 character and <= 16 character.  
 **Warning:**   
 Be sure to remember the new password, you won’t be able to connect to the device if you forget the new password.

 Example: Update common parameters
```Java
public void updateBeaconCommonPara()
{
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
```

##### 4.3.3.2 Update iBeacon parameters
The app can modify the basic parameters of KBeacon through the KBCfgIBeacon class. The KBCfgIBeacon has follow parameters:
uuid: iBeacon uuid
majorID: iBeacon major ID
minorID: iBeacon minor ID
please make sure the KBeacon advertisement type was set to iBeacon.

example: set the KBeacon to broadcasting iBeacon packet
```Java
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
```

example: update KBeacon to hybrid iBeacon/EddyTLM.  
Sometimes we need KBeacon broadcasting both iBeacon and TLM packet (battery level, Temperature, power on times
```Java
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
```

##### 4.3.3.3 Update Eddystone parameters
The app can modify the eddystone parameters of KBeacon through the KBCfgEddyURL and KBCfgEddyUID class.  
The KBCfgEddyURL has follow parameters:
* url: eddystone URL address

The KBCfgEddyUID has follow parameters:
* nid: namespace id about UID. It is 10 bytes length hex string value.
* sid: instance id about UID. It is 6 bytes length hex string value.

```Java
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
```

##### 4.3.3.4 Check if parameters are changed
Sometimes, in order to reduce the time for configuration, The app can only sending the modified parameters.

Example: checking if the parameters was changed, then send new parameters to device.
```Java
//read old parameters and sending modifiation parameters to device
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
```

#### 4.3.4 Update trigger parameters
 For some KBeacon device that has some motion sensor, temperature&humidity sensor, push button, etc., The application can config the KBeacon to monitor some trigger event. For example, button was pressed, the temperature is too high, or device was motion. The KBeacon can do some action when the trigger condition was met.

 |Trigger No.|0|1|2|3|4|
 |----|----|----|----|----|----|
 |`Type`|Btn single click|Btn double click |Motion|None|None|
 |`Action`|advertisement|advertisement|advertisement|NA|NA|
 |`Adv slot`|0|0|1|NA|NA|
 |`Para`|NA|NA|4|NA|NA|
 |`Adv duration`|10|10|30|NA|NA|

 The trigger advertisement has follow parameters:
 * Trigger No: Trigger instance number, the device supports up to 5 Triggers by default, the No is 0 ~ 4.
 * Trigger type: Trigger event type
 * Trigger action: Action when trigger event happened. For example: start broadcast, make a sound, or send a notification to the connected App.
 * Trigger Adv slot: When the Trigger event happened, which advertisement Slot  starts to broadcasting
 * Trigger parameters: For motion trigger, the parameter is acceleration sensitivity. For temperature above trigger, you can set to the temperature threshold.
 *	Trigger Adv duration: The advertisement duration when trigger event happened. Unit is second.  


 Example 1: Trigger only advertisment  
  &nbsp;&nbsp;The device usually does not broadcast by default, and we want to trigger the broadcast when the button is pressed.  
  &nbsp;&nbsp; 1. Setting slot 0 to iBeacon advertisement(adv period = 211.25ms, trigger only adv = true).  
  &nbsp;&nbsp; 2. Add a single button trigger(Trigger No = 0, Trigger type = Btn single click, Action = advertisement, Adv slot = 0, Adv duration = 20).  
	&nbsp;&nbsp;  
	![avatar](https://github.com/kkmhogen/KBeaconProDemo_Android/blob/master/only_adv_when_trigger.png?raw=true)

 Example 2:  Trigger advertisment
	&nbsp;For some scenario, we need to continuously monitor the KBeacon to ensure that the device was alive. The device usually broadcasting iBeacon1(UUID=xxx1) , and we want to trigger the broadcast iBeacon2(uuid=xxx2) when the button is pressed.   
  &nbsp;&nbsp; 1. Setting slot 0 to iBeacon advertisement(uuid=xxx1, adv period = 1280ms, trigger only adv = false).    
  &nbsp;&nbsp; 2. Setting slot 1 to iBeacon advertisement(uuid=xxx2, adv period = 211.25ms, trigger only adv = true).    
	&nbsp;We set an larger advertisement interval during alive advertisement and a short advertisement interval when trigger event happened, so we can achieve a balance between power consumption and triggers advertisement be easily detected.  
  &nbsp;&nbsp; 3. Add a single button trigger(Trigger No = 0, Trigger type = Btn single click, Action = advertisement, Adv slot = 1, Adv duration = 20).  
	 &nbsp;&nbsp;
 	![avatar](https://github.com/kkmhogen/KBeaconProDemo_Android/blob/master/always_adv_with_trigger.png?raw=true)




#### 4.3.4.1 Push button trigger
The push button trigger feature is used in some hospitals, nursing homes and other scenarios. When the user encounters some emergency event(SOS button), they can click the button and the KBeacon device will start broadcast or the KBeacon device send the click event to connected Android/IOS app.
The app can configure single click, double-click, triple-click, long-press the button trigger, oor a combination.

**Notify:**  
* By KBeacon's default setting, long press button used to power on and off. Clicking button used to force the KBeacon enter connectable broadcast advertisement. So when you enable the long-press button trigger, the long-press power off function will be disabled. When you turn on the single/double/triple click trigger, the function of clicking to enter connectable broadcast state will also be disabled. After you disable button trigger, the default function about long press or click button will take effect again.  
When you set multiple triggers to the same slot broadcast, you can turn on the Trigger content change mode. When different triggers are triggered, the content of UUID will change by UUID + trigger type.    
* iBeacon UUID for single click trigger = iBeacon UUID + 0x3
* iBeacon UUID for single double trigger = iBeacon UUID + 0x4
* iBeacon UUID for single triple trigger = iBeacon UUID + 0x5
* iBeacon UUID for single long press trigger = iBeacon UUID + 0x6

1. Enable or button trigger event to advertisement.  

```Java

// The device always broadcast UUID B9407F30-F5F8-466E-AFF9-25556B57FE67. When device detects button press, it triggers the broadcast of the iBeacon message(uuid=B9407F30-F5F8-466E-AFF9-25556B570001) in Slot1, and the iBeacon broadcast duration is 10 seconds.
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

    try {
        //set slot0 to always advertisement
        final KBCfgAdvIBeacon iBeaconAdv = new KBCfgAdvIBeacon();
        iBeaconAdv.setSlotIndex(0);  
        iBeaconAdv.setAdvPeriod(1280.0f);
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
    } catch (KBException excpt) {
        excpt.printStackTrace();
    }
}
```

2. Enable device send button trigger event to connected Andoird/IOS application  
In some scenarios, our app will always be connected to the KBeacon device. We need the app can receive a press notification event when the button is pressed.  

```Java
//implementation NotifyDataDelegate
public class DevicePannelActivity implements KBeacon.NotifyDataDelegate
{
  ...
}

//enable button trigger event to app that connected with the KBeacon
//the code was in DevicePannelActivity.java file that in sensordemo demo project
public void enableButtonTriggerEvent2App()
{
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

    //set trigger type
    KBCfgTrigger btnTriggerPara = new KBCfgTrigger(0, KBTriggerType.BtnSingleClick);
    btnTriggerPara.setTriggerAction(KBTriggerAction.Report2App | KBTriggerAction.Advertisement);

    //enable push button trigger
    mTriggerButtonApp.setEnabled(false);
    this.mBeacon.modifyConfig(btnTriggerPara, new KBeacon.ActionCallback() {
        public void onActionComplete(boolean bConfigSuccess, KBException error) {
            mTriggerButtonApp.setEnabled(true);
            if (bConfigSuccess) {
                //subscribe humidity notify
                if (!mBeacon.isSensorDataSubscribe(KBTriggerType.BtnSingleClick)) {
                    mBeacon.subscribeSensorDataNotify(KBTriggerType.BtnSingleClick, DevicePannelActivity.this, new KBeacon.ActionCallback() {
                        @Override
                        public void onActionComplete(boolean bConfigSuccess, KBException error) {
                            if (bConfigSuccess) {
                                Log.v(LOG_TAG, "subscribe button trigger event success");
                            } else {
                                Log.v(LOG_TAG, "subscribe button trigger event failed");
                            }
                        }
                    });
                }
            } else {
                toastShow("enable push button trigger error:" + error.errorCode);
            }
        }
    });
}

public void onNotifyDataReceived(KBeacon beacon, int nEventType, byte[] sensorData)
{
  //check if Temperature and humidity trigger event
  if (nEventType >= KBTriggerType.HTTempAbove
      && nEventType <= KBTriggerType.HTRealTimeReport)
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

```

3. The app can disable the button trigger  
```Java
//disable button trigger
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
```


#### 4.3.4.2 Motion trigger
The KBeacon can start broadcasting when it detects motion. Also the app can setting the sensitivity of motion detection.  
**Notify:**  
* When the KBeacon enable the motion trigger, the Acc feature(X, Y, and Z axis detected function) in the KSensor broadcast will be disabled.


Enabling motion trigger is similar to push button trigger, which will not be described in detail here.

1. Enable or motion trigger feature.  

```Java
// the iBeacon broadcast duration is 10 seconds.
public void enableMotionTrigger() {
    if (!mBeacon.isConnected()) {
        toastShow("Device is not connected");
        return;
    }

    //check device capability
    final KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getCommonCfg();
    if (oldCommonCfg != null && !oldCommonCfg.isSupportAccSensor())
    {
        toastShow("device is not support humidity");
        return;
    }

    try {
        //set trigger adv slot information
        final KBCfgAdvIBeacon triggerAdv = new KBCfgAdvIBeacon();
        triggerAdv.setSlotIndex(1);  //reuse previous slot
        triggerAdv.setAdvPeriod(211.25f);
        triggerAdv.setAdvMode(KBAdvMode.Legacy);
        triggerAdv.setTxPower(KBAdvTxPower.RADIO_0dBm);
        triggerAdv.setAdvConnectable(false);
        triggerAdv.setAdvTriggerOnly(true);  //this slot only advertisement when trigger event happened
        triggerAdv.setUuid("B9407F30-F5F8-466E-AFF9-25556B570002");
        triggerAdv.setMinorID(32);
        triggerAdv.setMinorID(10);

        //set trigger type
        KBCfgTrigger mtionTriggerPara = new KBCfgTrigger(0, KBTriggerType.AccMotion);
        mtionTriggerPara.setTriggerAdvSlot(1);
        mtionTriggerPara.setTriggerAction(KBTriggerAction.Advertisement); //set trigger advertisement enable
        mtionTriggerPara.setTriggerPara(5);  //set motion detection sensitive
        mtionTriggerPara.setTriggerAdvTime(10);  //set trigger adv duration to 20 seconds

        //enable push button trigger
        nEnableAccTrigger.setEnabled(false);
        ArrayList<KBCfgBase> cfgList = new ArrayList<>(2);
        cfgList.add(triggerAdv);
        cfgList.add(mtionTriggerPara);
        this.mBeacon.modifyConfig(cfgList, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                nEnableAccTrigger.setEnabled(true);
                if (bConfigSuccess) {
                    toastShow("enable motion trigger success");
                } else {
                    toastShow("enable motion trigger error:" + error.errorCode);
                }
            }
        });
    } catch (KBException excpt) {
        excpt.printStackTrace();
    }
}
```

#### 4.3.4.3 Temperature&Humidity trigger
The app can configure KBeacon to start broadcasting after detecting an abnormality humidity&temperature. For example, the temperature exceeds a specified threshold, or the temperature is below a certain threshold. Currently supports the following Trigger  
* HTTempAbove
* HTTempBelow
* HTHumidityAbove
* HTHumidityBelow
* HTRealTimeReport : This trigger will always happened regardless of the measured value of temperature and humidity. It is only used to report realtime humidity and temperature to APP.

1. Enable temperature&humidity trigger feature.  

```Java
public void enableTHTriggerEvtRpt2Adv() {
    if (!mBeacon.isConnected()) {
        toastShow("Device is not connected");
        return;
    }

    //check device capability
    final KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getCommonCfg();
    if (oldCommonCfg != null && !oldCommonCfg.isSupportHumiditySensor())
    {
        toastShow("device is not support humidity");
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
    thTriggerPara.setTriggerPara(70);  //trigger event when temperature > 70 Celsius
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
```
2. Report temperature&humidity to app realtime
```Java
//After enable realtime data to app, then the device will periodically send the temperature and humidity data to app whether it was changed or not.
    public void enableTHRealtimeTriggerRpt2App(){
        final KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getCommonCfg();

        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        if (oldCommonCfg != null && !oldCommonCfg.isSupportHumiditySensor())
        {
            toastShow("device is not support humidity");
            return;
        }

        KBCfgTrigger thTriggerPara = new KBCfgTrigger(0, KBTriggerType.HTRealTimeReport);
        thTriggerPara.setTriggerAction(KBTriggerAction.Report2App);

        //subscribe humidity notify
        mEnableTHRealtimeTrigger2App.setEnabled(false);
        this.mBeacon.modifyConfig(thTriggerPara, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mEnableTHRealtimeTrigger2App.setEnabled(true);
                if (bConfigSuccess) {
                    Log.v(LOG_TAG, "set temp&humidity trigger event report to app");

                    if (!mBeacon.isSensorDataSubscribe(KBTriggerType.HTRealTimeReport)) {
                        mBeacon.subscribeSensorDataNotify(KBTriggerType.HTRealTimeReport, DevicePannelActivity.this, new KBeacon.ActionCallback() {
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
```

#### 4.3.5 Setup Temperature&Humidity sensor parameters
If the device has some sensors, such as temperature and humidity sensors, we may need to set sensor parameters, such as the measurement period. Whether to save measurement records, etc.


#### 4.3.5.1 Config measure and log paramaters
```Java
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
```


#### 4.3.5.3 Read sensor history records
1. read history summary information.
```Java
KBHumidityDataMsg mSensorDataMsg = new KBHumidityDataMsg();
mSensorDataMsg.readSensorDataInfo(mBeacon,
        new KBSensorDataMsgBase.ReadSensorCallback()
        {
            @Override
            public void onReadComplete(boolean bConfigSuccess, Object obj, KBException error)
            {

                if (bConfigSuccess){
                  ReadHTSensorInfoRsp infRsp = (ReadHTSensorInfoRsp) obj;
                  mUtcOffset = UTCTime.getUTCTimeSeconds() - infRsp.readInfoUtcSeconds;
                  Log.v(LOG_TAG, "Total records in device:" + infRsp.totalRecordNumber);
                  Log.v(LOG_TAG, "Un read records in device:" + infRsp.unreadRecordNumber);
              }
            }
        }
    );
```

2.  Read log history records  
  The SDK provides the following three ways to read records.
  * KBSensorReadOption.NewRecord:  read history records and move next. After app reading records, the KBeacon device will move the pointer to the next unreaded record. If the app send read request again, the KBeacon device sends next unread records and move the pointer to next.

  * KBSensorReadOption.NormalOrder: Read records without pointer moving. The app can read records from old to recently. To read records in this way, the app must  specify the record no to be read.

  * KBSensorReadOption.ReverseOrder: Read records without pointer moving. The app can read records from recently to old. To read records in this way, the app must  specify the record no to be read.

   Example1: The app read the temperature and humidity records. Each time the records was read, the pointer will move to next.
```Java
    mSensorDataMsg.readSensorRecord(mBeacon,
        INVALID_DATA_RECORD_POS, //set to INVALID_DATA_RECORD_POS
        KBSensorReadOption.NewRecord,  //read direction type
        50,   //number of records the app want to read
        new KBSensorDataMsgBase.ReadSensorCallback()
        {
            @Override
            public void onReadComplete(boolean bConfigSuccess,  Object obj, KBException error) {
                if (bConfigSuccess)
                {
                  ReadHTSensorDataRsp dataRsp = (ReadHTSensorDataRsp) obj;
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
        }
);
```  

  Example2: The app read the temperature and humidity records without moving pointer.
  The device has 100 records sorted by time, the app want to reading 20 records and start from the No 99. The Kbeacon will send records #99 ~ #90 to app by reverse order.     
  If the app does not known the last record no, then the value can set to INVALID_DATA_RECORD_POS.
```Java
 mSensorDataMsg.readSensorRecord(mBeacon,
     INVALID_DATA_RECORD_POS,   // 99 or INVALID_DATA_RECORD_POS
     KBSensorReadOption.ReverseOrder,
     10,
     new KBSensorDataMsgBase.ReadSensorCallback()
     {
         @Override
         public void onReadComplete(boolean bConfigSuccess,  Object obj, KBException error) {
             if (bConfigSuccess)
             {

               ReadHTSensorDataRsp dataRsp = (ReadHTSensorDataRsp) obj;

               //the Kbeacon return records 99 ~ 80
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
     }
);
```  

 Example3: The app read the temperature and humidity records without moving pointer.
 The device has 100 records sorted by time, the app want to reading 20 records and start from No 10. The Kbeacon will send records #10 ~ #29 to app.  
```Java
 mSensorDataMsg.readSensorRecord(mBeacon,
     10,  //start read record no
     KBSensorReadOption.NormalOrder,   //read direction, from oldes to recent
     20,  //max read records number
     new KBSensorDataMsgBase.ReadSensorCallback()
     {
         @Override
         public void onReadComplete(boolean bConfigSuccess,  Object obj, KBException error) {
             if (bConfigSuccess)
             {
               ReadHTSensorDataRsp dataRsp = (ReadHTSensorDataRsp) obj;

               //the Kbeacon return records 10 ~ 29
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
     }
);
```
#### 4.3.6 Send command to device
After app connect to device success, the app can send command to device.  
All command message between app and KBeacon are JSON format. Our SDK provide Hash Map to encapsulate these JSON message.
#### 4.3.6.1 Ring device
 For some KBeacon device that has buzzer function. The app can ring device. For ring command, it has 5 parameters:
 * msg: msg type is 'ring'
 * ringTime: unit is ms. The KBeacon will start flash/alert for 'ringTime' millisecond  when receive this command.
 * ringType: 0x1:beep alert only; 0x2 led flash ; 0x0 turn off ring;
 * ledOn: optional parameters, unit is ms. The LED will flash at interval (ledOn + ledOff).  This parameters is valid when ringType set to 0x0 or 0x1.
 * ledOff: optional parameters, unit is ms. the LED will flash at interval (ledOn + ledOff).  This parameters is valid when ringType set to 0x0 or 0x1.  

  ```Java
 public void ringDevice() {
        if (!mBeacon.isConnected()) {
            return;
        }

        mDownloadButton.setEnabled(false);
        HashMap<String, Object> cmdPara = new HashMap<>(5);
        cmdPara.put("msg", "ring");
        cmdPara.put("ringTime", 20000);   //ring times, uint is ms
        cmdPara.put("ringType", 2);  //0x1:beep alert only; 0x2 led flash;
        cmdPara.put("ledOn", 200);   //valid when ringType set to 0x0 or 0x2
        cmdPara.put("ledOff", 1800); //valid when ringType set to 0x0 or 0x2
        mBeacon.sendCommand(cmdPara, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mDownloadButton.setEnabled(true);
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
```

#### 4.3.6.2 Reset configuration to default
 The app can use follow command to reset all configurations to default.
 * msg: message type is 'reset'

 ```Java
    public void resetParameters() {
        if (!mBeacon.isConnected()) {
            return;
        }

        HashMap<String, Object> cmdPara = new HashMap<>(5);
        cmdPara.put("msg", "admin");
        cmdPara.put("stype", "reset");
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
```

#### 4.3.7 Error cause in configurations/command
 App may get errors during the configuration. The KBException has follow values.
 * KBErrorCode.CfgReadNull: Device return null parameters
 * KBErrorCode.CfgBusy: device is busy, please make sure last configuration complete
 * KBErrorCode.CfgFailed: device return failed.
 * KBErrorCode.CfgTimeout: configuration timeout
 * KBErrorCode.CfgInputInvalid: input parameters data not in valid range
 * KBErrorCode.CfgStateError: device is not in connected state
 * KBErrorCode.CfgNotSupport: device does not support the parameters

 ```Java
{
    ...other code
    mBeacon.sendCommand(cmdPara, new KBeacon.ActionCallback()
    {
        @Override
        public void onActionComplete(boolean bConfigSuccess, KBException error)
        {
            if (!bConfigSuccess)
            {
                if (error.errorCode == KBErrorCode.CfgBusy)
                {
                    toastShow("Another configruation is not complete");
                }
                else if (error.errorCode == KBErrorCode.CfgFailed)
                {
                    toastShow("Device return failed");
                }
                else if (error.errorCode == KBErrorCode.CfgTimeout)
                {
                    toastShow("send parameters to device timeout");
                }
                else if (error.errorCode == KBErrorCode.CfgInputInvalid)
                {
                    toastShow("Input parameters invalid");
                }
                else if (error.errorCode == KBErrorCode.CfgStateError)
                {
                    toastShow("Please make sure the device was connected");
                }
                else if (error.errorCode == KBErrorCode.CfgNotSupport)
                {
                    toastShow("Device does not support the parameters");
                }
                else
                {
                    toastShow("config failed for error:" + error.errorCode);
                }
            }
        }
    });
}
 ```  

## 5. DFU
Through the DFU function, you can upgrade the firmware of the device. Our DFU function is based on Nordic's DFU library. In order to make it easier for you to integrate the DFU function, We add the DFU function into ibeacondemo demo project for your reference. The Demo about DFU includes the following class:
* KBeaconDFUActivity: DFU UI activity and procedure about how to download latest firmware.
* KBFirmwareDownload: Responsible for download the JSON or firmware from KKM clouds.
* DFUService: This DFU service that implementation Nordic's DFU library.
* NotificationActivity: During the upgrade, a notification will pop up, click on the notification to enter this activity.
![avatar](https://github.com/kkmhogen/KBeaconProDemo_Android/blob/master/kbeacon_dfu_arc.png?raw=true)

### 5.1 Add DFU function to the application.
1. The DFU library need download the latest firmware from KKM cloud server. So you need add follow permission into AndroidManifest.xml
 ```
<uses-permission android:name="android.permission.INTERNET" />
 ```
2. The DFU Demo using nordic DFU library for update. So we need add follow dependency.
 ```
implementation 'no.nordicsemi.android:dfu:1.10.3'
 ```

3. Start DFU activity  
 ```Java
 public void onClick(View v)
{
    switch (v.getId()) {
        case R.id.dfuDevice:
            if (mBeacon.isConnected()) {
                final Intent intent = new Intent(this, KBeaconDFUActivity.class);
                intent.putExtra(KBeaconDFUActivity.DEVICE_MAC_ADDRESS, mBeacon.getMac());
                startActivityForResult(intent, 1);
            }
            break;
        }
}
```
If you want to known more details about getting the Device's latest firmware from KKM cloud, or deploied the latest firmware on you cloud. Please contact KKM sales(sales@kkmcn.com) and she/he will send you a detail document.

 Also for more detail nordic DFU library, please refer to
https://github.com/NordicSemiconductor/Android-DFU-Library

## 6. Special instructions

> 1. AndroidManifest.xml of SDK has declared to access Bluetooth permissions.
> 2. After connecting to the device successfully, we suggest delay 1 second to sending configure data, otherwise the device may not return data normally.
> 3. If you app need running in background, we suggest that sending and receiving data should be executed in the "Service". There will be a certain delay when the device returns data, and you can broadcast data to the "Activity" after receiving in the "Service".

## 7. Change log
*  2021.6.20 V1.41 Support slot mode advertisement
*  2021.1.30 V1.31 Support button and motion trigger event in connected state
* 2020.11.11 v1.30 Support temperature and humidity sensor. Remove AAR library, please download library from JCenter.
* 2020.3.1 v1.23 change the adv period type from integer to float.
* 2020.1.16 v1.22 add button trigger.
* 2019.12.16 v1.21 add android10 permission.
* 2019.10.28 v1.2 add beep function.
* 2019.10.11 v1.1 add KSesnor function.
* 2019.2.1 v1.0 first version.
