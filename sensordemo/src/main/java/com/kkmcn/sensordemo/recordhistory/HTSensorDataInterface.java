package com.kkmcn.sensordemo.recordhistory;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBRecordBase;

public interface HTSensorDataInterface {

    public int size();

    public KBRecordBase get(int nIndex);
}
