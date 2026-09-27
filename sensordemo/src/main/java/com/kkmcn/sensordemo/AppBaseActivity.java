package com.kkmcn.sensordemo;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Created by hogen on 2018/9/22.
 */

public class AppBaseActivity extends AppCompatActivity {

    protected ActionBar mActionBar;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        mActionBar = getSupportActionBar();
        if(mActionBar!=null){
            mActionBar.setElevation(1);
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setDisplayShowHomeEnabled(false);
        }
    }

    protected void setupEdgeToEdgeWithInsets() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        View contentView = findViewById(android.R.id.content);
        if (contentView == null){
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(contentView,
                (view, insets) -> {
                    androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );
                    return insets;
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 是否触发按键为back键
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void toastShow(String strMsg)
    {
        Toast toast=Toast.makeText(this, strMsg, Toast.LENGTH_SHORT);

        toast.setGravity(Gravity.CENTER, 0, 0);

        toast.show();
    }

}