package com.xuexuan.androidaop;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import will.github.com.xuexuan.androidaop.R;

public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
