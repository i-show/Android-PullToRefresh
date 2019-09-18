package com.ishow.smaple.pulltorefresh;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ishow.pulltorefresh.IPullToRefreshHeader;
import com.ishow.pulltorefresh.OnPullToRefreshListener;
import com.ishow.pulltorefresh.PullToRefreshView;
import com.ishow.pulltorefresh.headers.classic.ClassicHeader;
import com.ishow.pulltorefresh.headers.google.GoogleStyleHeader;
import com.ishow.pulltorefresh.recycleview.LoadMoreAdapter;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "yhy";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //transparentStatusBar();
        setContentView(R.layout.activity_main);

        IPullToRefreshHeader header = new GoogleStyleHeader(this);

        final Test2Adapter adapter = new Test2Adapter(this);
        adapter.setData(getData(adapter));
        LoadMoreAdapter wrapper = new LoadMoreAdapter(this, adapter);
        LinearLayoutManager manager = new LinearLayoutManager(this);

        RecyclerView recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(wrapper);

        final PullToRefreshView pullToRefreshView = findViewById(R.id.pull2refresh);
        pullToRefreshView.setHeader(header);
        pullToRefreshView.setFooter(wrapper);
        pullToRefreshView.setOnPullToRefreshListener(new OnPullToRefreshListener() {
            @Override
            public void onRefresh(PullToRefreshView v) {
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pullToRefreshView.setRefreshSuccess();
                        adapter.setData(getData(null));
                    }
                }, 1000);
            }

            @Override
            public void onLoadMore(PullToRefreshView v) {
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (adapter.getItemCount() >= 30) {
                            pullToRefreshView.setLoadMoreFailed();
                        } else {
                            pullToRefreshView.setLoadMoreSuccess();
                            adapter.plusData(getData(adapter));
                        }

                    }
                }, 800);
            }
        });
    }


    private List<String> getData(RecyclerView.Adapter adapter) {
        List<String> list = new ArrayList<>();

        int size = 0;
        if (adapter != null) {
            size = adapter.getItemCount();
        }
        for (int i = 0; i < 15; i++) {
            int index = size + i;
            list.add("postion " + index);
        }
        return list;
    }

    private void transparentStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }


}
