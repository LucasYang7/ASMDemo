package com.xiaozhezhe.asmdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivityTag";
    private static final String JAVA_STATIC_STRING = "java static const string";
    private final String JAVA_STRING = "java const string";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, JAVA_STRING);
        TextView tvShowKtLog = (TextView) findViewById(R.id.tvShowKtLog);
        tvShowKtLog.setText(new KtStringTest().test());
        Log.d(TAG, "Log Local String");
        String testLocalString = "testLocalString";
        Log.d(TAG, testLocalString);
    }
}