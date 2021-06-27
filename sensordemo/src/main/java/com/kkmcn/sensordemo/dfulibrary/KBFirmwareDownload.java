package com.kkmcn.sensordemo.dfulibrary;

import android.app.Activity;

import com.kkmcn.sensordemo.Utils;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgBase;
import com.kkmcn.kbeaconlib2.KBException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class KBFirmwareDownload {

    public interface DownloadFirmwareInfoCallback {
        void onDownloadComplete(boolean bSuccess, HashMap<String, Object> firmwareInfo, KBException error);
    }

    public interface DownloadFirmwareDataCallback {
        void onDownloadComplete(boolean bSuccess, File file, KBException error);
    }

    private final static String DEFAULT_DOWN_DIRECTORY_NAME = "KBeaconFirmware";
    private final static String DEFAULT_DOWNLOAD_WEB_ADDRESS = "https://api.ieasygroup.com:8092/KBeaconFirmware/";

    public final static int ERR_NETWORK_DOWN_FILE_ERROR = 0x1001;
    public final static int ERR_CREATE_DIRECTORY_FAIL = 0x1002;

    private Activity mCtx;
    private String firmwareWebAddress;
    private String mDownloadFilePath;

    KBFirmwareDownload(Activity ctx)
    {
        mCtx = ctx;
        firmwareWebAddress = DEFAULT_DOWNLOAD_WEB_ADDRESS;
        mDownloadFilePath = mCtx.getFilesDir().getPath() +  "/" + DEFAULT_DOWN_DIRECTORY_NAME;
        makeSureFileDirectory();
    }

    KBFirmwareDownload(Activity ctx, String webPath, String fileDirectoryName)
    {
        mCtx = ctx;
        firmwareWebAddress = webPath;
        mDownloadFilePath = mCtx.getFilesDir().getPath() +  "/" + fileDirectoryName;
        makeSureFileDirectory();
    }

    private boolean makeSureFileDirectory()
    {
        if (mCtx == null){
            return false;
        }

        File newDir= new File(mDownloadFilePath);
        if (!newDir.exists()) {
            return newDir.mkdir();
        }

        return true;
    }

    public void downloadFirmwareInfo(String beaconModel, int nMaxDownloadTime, final DownloadFirmwareInfoCallback callback)
    {
        String urlStr = String.format("%s.json", beaconModel);
        this.downLoadFile(urlStr, nMaxDownloadTime, new DownloadFirmwareDataCallback() {
            @Override
            public void onDownloadComplete(boolean bSuccess, File file, KBException error) {
                final boolean bReadFileSuccess;
                final KBException downResult;
                HashMap<String, Object> jsonPara;
                if (bSuccess) {
                    String strJsonFile = Utils.ReadTxtFile(file);
                    jsonPara = new HashMap<>(10);
                    if (strJsonFile != null) {
                        KBCfgBase.JsonString2HashMap(strJsonFile, jsonPara);
                        bReadFileSuccess = true;
                        downResult = null;
                    } else {
                        bReadFileSuccess = false;
                        jsonPara = null;
                        downResult = null;
                    }
                } else {
                    bReadFileSuccess = false;
                    downResult = error;
                    jsonPara = null;
                }
                callback.onDownloadComplete(bReadFileSuccess, jsonPara, downResult);
            }
        });
    }

    public void downLoadFile(final String fileName, final int timeoutMS, final DownloadFirmwareDataCallback callback) {
        final String urlStr = String.format("%s%s", this.firmwareWebAddress, fileName);

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean bDownSuccess = false;
                File downloadFile = null;
                KBException downResult = null;

                try {
                    URL url = new URL(urlStr);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setReadTimeout(timeoutMS);
                    con.setConnectTimeout(timeoutMS);
                    con.setRequestProperty("Charset", "UTF-8");
                    con.setRequestMethod("GET");
                    int nResponse = con.getResponseCode();
                    if (nResponse == 200) {
                        InputStream is = con.getInputStream();
                        if (is != null) {
                            FileOutputStream fileOutputStream = null;
                            int nIndex = urlStr.lastIndexOf('/');
                            String strFileName = urlStr.substring(nIndex);

                            //make sure the directory
                            if (makeSureFileDirectory()) {
                                downloadFile = new File(mDownloadFilePath, strFileName);
                                fileOutputStream = new FileOutputStream(downloadFile);
                                byte[] buf = new byte[1024];
                                int ch;
                                while ((ch = is.read(buf)) != -1) {
                                    fileOutputStream.write(buf, 0, ch);
                                }
                                bDownSuccess = true;
                            }else{
                                downResult = new KBException(ERR_CREATE_DIRECTORY_FAIL, "create file directory failed");
                            }
                        }
                        else
                        {
                            downResult = new KBException(ERR_NETWORK_DOWN_FILE_ERROR, "Download file failed");
                        }
                    }
                    else
                    {
                        downResult = new KBException(ERR_NETWORK_DOWN_FILE_ERROR, con.getResponseMessage());
                    }
                } catch (final IOException e) {
                    e.printStackTrace();
                    downResult = new KBException(ERR_NETWORK_DOWN_FILE_ERROR, e.getLocalizedMessage());
                }

                final KBException fDownRslt = downResult;
                final boolean fDownSuccess = bDownSuccess;
                final File fDownFile = downloadFile;

                mCtx.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        callback.onDownloadComplete(fDownSuccess, fDownFile, fDownRslt);
                    }
                });
            }
        }).start();
    }
}
