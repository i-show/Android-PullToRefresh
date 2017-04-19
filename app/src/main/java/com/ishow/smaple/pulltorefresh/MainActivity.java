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

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(wrapper);

        final PullToRefreshView pullToRefreshView = (PullToRefreshView) findViewById(R.id.pulltorefresh);
        pullToRefreshView.setHeader(header);
        pullToRefreshView.setFooter(wrapper);
        pullToRefreshView.setOnPullToRefreshListener(new OnPullToRefreshListener() {
            @Override
            public void onRefresh(View v) {
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pullToRefreshView.setRefreshSuccess();
                        pullToRefreshView.setLoadMoreNormal();
                        adapter.setData(getData(null));
                    }
                }, 3000);
            }

            @Override
            public void onLoadMore(View v) {
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        adapter.plusData(getData(adapter));
                        if (adapter.getItemCount() >= 80) {
                            pullToRefreshView.setLoadMoreEnd();
                        } else {
                            pullToRefreshView.setLoadMoreSuccess();
                        }
                    }
                }, 3000);
            }
        });


        View swipeTest = findViewById(R.id.swipe_test);
        swipeTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SwipeActivity.class);
                startActivity(intent);
            }
        });

    }


    private List<String> getData(RecyclerView.Adapter adapter) {
        List<String> list = new ArrayList<>();

        int size = 0;
        if (adapter != null) {
            size = adapter.getItemCount();
        }
        for (int i = 0; i < 14; i++) {
            int index = size + i;
            list.add("postion " + index);
        }
        return list;
    }

}
