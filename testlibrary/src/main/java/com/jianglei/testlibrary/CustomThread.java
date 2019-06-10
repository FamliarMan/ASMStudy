package com.jianglei.testlibrary;

import android.util.Log;

/**
 * @author longyi created on 19-6-10
 */
public class CustomThread extends Thread {
    @Override
    public void run() {
        super.run();
        Log.d("longyi","你正在使用的是CustomThread");
    }
}
