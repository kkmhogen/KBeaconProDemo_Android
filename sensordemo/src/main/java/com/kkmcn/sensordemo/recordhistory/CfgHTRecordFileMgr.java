package com.kkmcn.sensordemo.recordhistory;

import android.content.Context;
import android.util.Log;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBRecordBase;
import com.kkmcn.kbeaconlib2.KBSensorHistoryData.KBRecordHumidity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CfgHTRecordFileMgr implements HTSensorDataInterface {

    private final String LOG_TAG = "CfgHTRecordFileMgr";
    private String RECORD_FILE_NAME_PREFEX = "_ht_sensor_record.txt";
    private String RECORD_FILE_NAME = "_ht_sensor_record.txt";
    private static SimpleDateFormat mLogFileFmt = new SimpleDateFormat("yyyy&MM&dd HH&mm&ss");// 日志文件格式
    private static final SimpleDateFormat mRecordTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Context mCtx;

    private List<KBRecordBase> mMovingRecordList = new ArrayList<>(500);//表示首次加载的list

    public List<KBRecordBase> getRecordList()
    {
        return mMovingRecordList;
    }


    public int size()
    {
        return mMovingRecordList.size();
    }

    public KBRecordBase get(int nIndex)
    {
        int nMaxIndex = mMovingRecordList.size() - 1;
        int nReverseIndex =  nMaxIndex - nIndex;
        return mMovingRecordList.get(nReverseIndex);
    }

    public void initHistoryRecord(String strMac, Context ctx)
    {
        mCtx = ctx;
        String strMacAddress = strMac.replace(":", "").toLowerCase();
        RECORD_FILE_NAME = strMacAddress + RECORD_FILE_NAME_PREFEX ;
        File recordFile = new File(mCtx.getFilesDir(), RECORD_FILE_NAME);

        try {
            InputStream instream = new FileInputStream(recordFile);
            InputStreamReader inputreader = new InputStreamReader(instream);
            BufferedReader buffreader = new BufferedReader(inputreader);
            String line;
            while (( line = buffreader.readLine()) != null) {
                String[]strRecordArray =  line.split("\t");
                if (strRecordArray.length != 3)
                {
                    continue;
                }
                KBRecordHumidity record = new KBRecordHumidity();
                try {
                    record.utcTime = Long.valueOf(strRecordArray[0]);
                    record.temperature = Float.valueOf(strRecordArray[1]);
                    record.humidity = Float.valueOf(strRecordArray[2]);
                }
                catch (Exception excpt)
                {
                    excpt.printStackTrace();
                    continue;
                }

                mMovingRecordList.add(record);
            }
            instream.close();
        }
        catch (java.io.FileNotFoundException e)
        {
            Log.d(LOG_TAG, "The File doesn't not exist.");
        }
        catch (IOException e)
        {
            Log.d(LOG_TAG, e.getMessage());
        }
    }

    public void clearHistoryRecord() {
        mMovingRecordList.clear();

        File recordFile = new File(mCtx.getFilesDir(), RECORD_FILE_NAME);
        if (recordFile.exists()) {
            if (recordFile.isFile()) {
                recordFile.delete();
            }
        }
    }

    public void appendRecord(List<KBRecordBase> recordList)
    {
        mMovingRecordList.addAll(recordList);

        File recordFile = new File(mCtx.getFilesDir(), RECORD_FILE_NAME);
        try {
            FileWriter filerWriter = new FileWriter(recordFile, true);//后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖
            BufferedWriter bufWriter = new BufferedWriter(filerWriter);

            for (KBRecordBase sensorRecord : recordList) {
                KBRecordHumidity record = (KBRecordHumidity)sensorRecord;
                String recordLine = "" + record.utcTime + "\t"
                        + record.temperature + "\t"
                        + record.humidity + "\n";
                bufWriter.write(recordLine);
            }

            bufWriter.close();
            filerWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void appendRecord(KBRecordHumidity record)
    {
        mMovingRecordList.add(record);

        File recordFile = new File(mCtx.getFilesDir(), RECORD_FILE_NAME);
        try {
            FileWriter filerWriter = new FileWriter(recordFile, true);//后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖
            BufferedWriter bufWriter = new BufferedWriter(filerWriter);

            String recordLine = "" + record.utcTime + "\t"
                    + record.temperature + "\t"
                    + record.humidity;
            bufWriter.write(recordLine);

            bufWriter.close();
            filerWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public File writeHistoryToFile()
    {
        File appDir = mCtx.getFilesDir();
        if (appDir == null || mMovingRecordList.size() == 0){
            return null;
        }

        String strLogFileDirectory = appDir.getPath() + "history";
        File newDir= new File(strLogFileDirectory);
        if (!newDir.exists()) {
            if (!newDir.mkdir()){
                return null;
            }
        }

        //生成文件名
        Date nowtime = new Date();
        String fileNamePrefex = mLogFileFmt.format(nowtime);
        String strLogFilePath = strLogFileDirectory + fileNamePrefex + ".txt";
        File file = new File(strLogFilePath);

        //每次写入时，都换行写
        try {
            FileWriter filerWriter = new FileWriter(file, true);//后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖
            BufferedWriter bufWriter = new BufferedWriter(filerWriter);

            for (int i = 0; i < mMovingRecordList.size(); i++)
            {
                KBRecordHumidity historyData = (KBRecordHumidity)mMovingRecordList.get(i);

                String strNearbyUtcTime = mRecordTimeFormat.format(historyData.utcTime * 1000);
                String strWriteLine =
                        strNearbyUtcTime + "\t" +
                        historyData.temperature + "\t" +
                        historyData.humidity;

                bufWriter.write(strWriteLine);
            }
            bufWriter.close();
            filerWriter.close();

        } catch (Exception e) {
            Log.e(LOG_TAG, "Error on write File.");
            return null;
        }

        return file;
    }

    public String writeHistoryToString()
    {
        File appDir = mCtx.getFilesDir();
        if (appDir == null || mMovingRecordList.size() == 0){
            return null;
        }

        final StringBuilder strBuilder = new StringBuilder(1024*20);
        String strWriteLine = "UTC" + "\t" +
                "Temperature" + "\t" +
                "Humidity" + "\n";
        strBuilder.append(strWriteLine);

        for (int i = 0; i < mMovingRecordList.size(); i++)
        {
            KBRecordHumidity historyData = (KBRecordHumidity)mMovingRecordList.get(i);

            String strNearbyUtcTime = mRecordTimeFormat.format(historyData.utcTime * 1000);
            strWriteLine = strNearbyUtcTime + "\t" +
                            historyData.temperature + "\t" +
                            historyData.humidity + "\n";

            strBuilder.append(strWriteLine);
        }

        return strBuilder.toString();
    }
}
