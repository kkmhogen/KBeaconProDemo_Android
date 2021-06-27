package com.kkmcn.sensordemo.recordhistory;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBHumidityRecord;

public interface HTSensorDataInterface {

    public int size();

    public KBHumidityRecord get(int nIndex);
}
