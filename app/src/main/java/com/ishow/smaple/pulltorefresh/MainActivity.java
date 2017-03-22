package com.ishow.smaple.pulltorefresh;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ishow.pulltorefresh.PullToRefreshView;
import com.ishow.pulltorefresh.test.TestHeader;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TestHeader header = new TestHeader(this);
        header.setBackgroundColor(Color.GREEN);
        header.setText("Header");

        PullToRefreshView pullToRefreshView = (PullToRefreshView) findViewById(R.id.pulltorefresh);
        pullToRefreshView.setHeaderView(header);
    }
}
