package com.kkmcn.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.kkmcn.sensordemo.R;


/**
 * Created by Tan on 2015/2/12.
 */
public class DotsMarquee extends LinearLayout {

    public static final String LOG_TAG = "DotsMarquee";
    public static final int DELAY_MILLIS = 500;
    private final int mDotMargin;
    private int mLimit = 0;
    private boolean mFlip = true;
    private static final int MSG_ADD_LIMIT = 0;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ADD_LIMIT:
                    mLimit++;
                    mLimit %= 6;
                    if (mLimit == 0) {
                        mFlip = !mFlip;
                    }
                    updateUi();
                    sendEmptyMessageDelayed(MSG_ADD_LIMIT, DELAY_MILLIS);
                    break;
            }
        }
    };

    private void updateUi() {
        for (int i = 0; i < 6; i++) {
            if (mFlip) {
                if (i <= mLimit) {
                    getChildAt(i).setSelected(true);
                } else {
                    getChildAt(i).setSelected(false);
                }
            } else {
                if (i <= mLimit) {
                    getChildAt(i).setSelected(false);
                } else {
                    getChildAt(i).setSelected(true);
                }
            }

        }
    }

    public DotsMarquee(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        mDotMargin = getResources().getDimensionPixelOffset(R.dimen.dot_marquee_padding);
        init();

    }

    private void init() {
        removeAllViews();
        for (int i = 0; i < 6; i++) {
            addOneDot();
        }
    }

    private void addOneDot() {
        ImageView iv = new ImageView(getContext());
        iv.setImageResource(R.drawable.marquee_dots);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(mDotMargin, 0, mDotMargin, 0);
        addView(iv, params);
    }


    public void startMarquee() {
        mHandler.removeMessages(MSG_ADD_LIMIT);
        mHandler.sendEmptyMessage(MSG_ADD_LIMIT);
    }

    public void stopMarquee() {
        mLimit = 0;
        mHandler.removeMessages(MSG_ADD_LIMIT);
    }
}
