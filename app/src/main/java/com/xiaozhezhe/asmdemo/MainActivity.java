package com.xiaozhezhe.asmdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String JAVA_STRING = "java const string";
    private static final String TAG = "MainActivityTag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, JAVA_STRING);
        TextView tvShowKtLog = (TextView) findViewById(R.id.tvShowKtLog);
        tvShowKtLog.setText(new KtStringTest().test());
    }
}