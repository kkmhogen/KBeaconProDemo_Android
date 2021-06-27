package com.kkmcn.sensordemo;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by qindachang on 2017/3/10.
 */
public class UploadDialog {
    private Context mContext;

    private Dialog mAppCompatDialog;
    private TextView mTvNotice;
    private TextView mTvPercent;
    private ProgressBar mPb;

    public UploadDialog(Context context) {
        mContext = context;
        init();
    }

    private void init() {
        mAppCompatDialog = new Dialog(mContext);
        mAppCompatDialog.setTitle(R.string.upload_config_paramaters);
        View mContentView = LayoutInflater.from(mContext).inflate(R.layout.dialog_update, null, false);
        mTvNotice = (TextView) mContentView.findViewById(R.id.tv_dialog_update_notice);
        mTvPercent = (TextView) mContentView.findViewById(R.id.tv_dialog_update_percent);
        mPb = (ProgressBar) mContentView.findViewById(R.id.progressBar_update);
        mAppCompatDialog.setContentView(mContentView);
        mAppCompatDialog.setCancelable(false);
    }

    private boolean mDlgShow = false;

    public void show() {
        mDlgShow = true;
        mAppCompatDialog.show();
        mPb.setProgress(0);
    }

    public void setNoticeText(String notice) {
        mTvNotice.setText(notice);
    }

    public void setPercentText(String text) {
        mTvPercent.setText(text);
    }

    public boolean isShowing(){
        return mDlgShow;
    }

    public void setPercent(int percent) {
        mPb.setProgress(percent);
    }

    public void dismiss() {
        mDlgShow = false;
        mAppCompatDialog.dismiss();
    }
}
