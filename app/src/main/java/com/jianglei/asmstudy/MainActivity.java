package com.jianglei.asmstudy;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.jianglei.testlibrary.CustomThread;
import com.jianglei.testlibrary.TestUtils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int i=10;
        TestUtils.test();
        new Thread().run();
        new Thread().run();
    }
}
