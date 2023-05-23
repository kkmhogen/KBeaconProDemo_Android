package com.kkmcn.sensordemo.dfulibrary;

import android.app.Activity;
import com.kkmcn.kbeaconlib2.KBErrorCode;
import com.kkmcn.kbeaconlib2.KBException;
import com.kkmcn.sensordemo.Utils;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;


public class KBFirmwareDownload {

    public interface DownloadFirmwareInfoCallback {
        void onDownloadComplete(boolean bSuccess, JSONObject firmwareInfo, KBException error);
    }

    public interface DownloadFirmwareDataCallback {
        void onDownloadComplete(boolean bSuccess, File file, KBException error);
    }

    private Activity mCtx;
    private KBHttpDownload mHttpDownload;

    public KBFirmwareDownload(Activity ctx)
    {
        mCtx = ctx;
        mHttpDownload = new KBHttpDownload(ctx);
    }

    public void downloadFirmwareData(String hexFileName, final DownloadFirmwareDataCallback callback) {
        mHttpDownload.downLoadFile(hexFileName, 60 * 1000, (bSuccess, file, error) -> {
            if (bSuccess) {
                callback.onDownloadComplete(true, file, null);
            } else {
                callback.onDownloadComplete(false, null, error);
            }
        });
    }

    public void downloadFirmwareInfo(String beaconModel, int nMaxDownloadTime, final DownloadFirmwareInfoCallback callback)
    {
        String urlStr = String.format("%s.json", beaconModel);
        mHttpDownload.downLoadFile(urlStr, nMaxDownloadTime, (bSuccess, file, error) -> {
            boolean bReadFileSuccess = false;
            KBException downResult = null;
            JSONObject jsonPara = null;
            if (bSuccess) {
                String strJsonFile = Utils.ReadTxtFile(file);
                if (strJsonFile != null) {
                    try {
                        jsonPara = new JSONObject(strJsonFile);
                        bReadFileSuccess = true;
                    } catch (JSONException except) {
                        except.printStackTrace();
                    }
                }
                if (strJsonFile == null || jsonPara == null)
                {
                    downResult = new KBException(KBErrorCode.CfgJSONError, "parse cloud json file failed");
                }
            }
            callback.onDownloadComplete(bReadFileSuccess, jsonPara, downResult);
        });
    }

    public boolean isFirmwareFileExist(String strFirmwareFileName)
    {
        String strFilePath = mHttpDownload.getDownloadFilePath() + strFirmwareFileName;
        File file=new File(strFilePath);
        return file.exists();
    }

    public File getFirmwareFile(String strFirmwareFileName)
    {
        String strFilePath = mHttpDownload.getDownloadFilePath() + strFirmwareFileName;
        File firmwareFile = new File(strFilePath);
        return firmwareFile;
    }
}
