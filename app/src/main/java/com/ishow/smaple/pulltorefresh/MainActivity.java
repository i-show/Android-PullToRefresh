package com.ishow.smaple.pulltorefresh;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.ishow.pulltorefresh.OnPullToRefreshListener;
import com.ishow.pulltorefresh.PullToRefreshView;
import com.ishow.pulltorefresh.classic.ClassicHeader;
import com.ishow.pulltorefresh.recycleview.LoadMoreAdapter;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ClassicHeader header = new ClassicHeader(this);

        final Test2Adapter adapter = new Test2Adapter(this);
        adapter.setData(getData(adapter));
        LoadMoreAdapter wrapper = new LoadMoreAdapter(this, adapter);
        GridLayoutManager manager = new GridLayoutManager(this, 2);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.pull_to_refresh_scroll_view);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(wrapper);

        final PullToRefreshView pullToRefreshView = (PullToRefreshView) findViewById(R.id.pulltorefresh);
        pullToRefreshView.setHeader(header);
        pullToRefreshView.setFooter(wrapper);
        pullToRefreshView.setOnPullToRefreshListener(new OnPullToRefreshListener() {
            @Override
            public void onRefresh(PullToRefreshView v) {
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pullToRefreshView.setRefreshSuccess();
                        pullToRefreshView.setLoadMoreNormal();
                        adapter.setData(getData(null));
                    }
                }, 600);
            }

            @Override
            public void onLoadMore(PullToRefreshView v) {
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (adapter.getItemCount() >= 80) {
                            pullToRefreshView.setLoadMoreEnd();
                        } else {
                            pullToRefreshView.setLoadMoreSuccess();
                        }
                        adapter.plusData(getData(adapter));
                    }
                }, 600);
            }
        });


    }


    private List<String> getData(RecyclerView.Adapter adapter) {
        List<String> list = new ArrayList<>();

        int size = 0;
        if (adapter != null) {
            size = adapter.getItemCount();
        }
        for (int i = 0; i < 30; i++) {
            int index = size + i;
            list.add("postion " + index);
        }
        return list;
    }

}
