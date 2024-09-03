package com.kkmcn.sensordemo.dfulibrary;

import android.app.Activity;

import com.kkmcn.kbeaconlib2.KBException;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class KBHttpDownload {
    private final static String DEFAULT_DOWNLOAD_WEB_ADDRESS = "https://download.kkmiot.com:8093/KBeaconFirmware/";
    private final static String DEFAULT_DOWN_DIRECTORY_NAME = "KBeaconFirmware";

    public final static int ERR_NETWORK_DOWN_FILE_ERROR = 0x1001;
    public final static int ERR_CREATE_DIRECTORY_FAIL = 0x1002;

    public String downloadFilePath;
    public String webPathAddress;
    public Activity mCtx;

    public interface DownloadCallback {
        void onDownloadComplete(boolean bSuccess, File file, KBException error);
    }

    private KBHttpDownload()
    {

    }

    public String getDownloadFilePath() {
        return downloadFilePath;
    }

    public String getWebPathAddress() {
        return webPathAddress;
    }

    public KBHttpDownload(Activity ctx)
    {
        mCtx = ctx;
        webPathAddress = DEFAULT_DOWNLOAD_WEB_ADDRESS;
        downloadFilePath = mCtx.getFilesDir().getPath() +  "/" + DEFAULT_DOWN_DIRECTORY_NAME + "/";
        makeSureFileDirectory();
    }

    public KBHttpDownload(Activity ctx, String savePath, String webPath)
    {
        mCtx = ctx;
        downloadFilePath = savePath;
        webPathAddress = webPath;
        makeSureFileDirectory();
    }

    private boolean makeSureFileDirectory()
    {
        if (mCtx == null){
            return false;
        }

        File newDir= new File(downloadFilePath);
        if (!newDir.exists()) {
            return newDir.mkdir();
        }

        return true;
    }

    public void downLoadFile(final String fileName, final int timeoutMS, final DownloadCallback callback) {
        final String urlStr = String.format("%s%s", this.webPathAddress, fileName);

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
                                downloadFile = new File(downloadFilePath, strFileName);
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
