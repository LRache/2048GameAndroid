package com.rache.game2048;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class SelectSaveActivity extends AppCompatActivity {

    static String TAG = "SelectSaveActivity";

    protected ListView saveListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_save);
        setTitle(R.string.select_save_text);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        saveListView  = findViewById(R.id.saveListView);
        initListView();

        saveListView.setOnItemClickListener((adapterView, view, i, l) -> {
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putInt("index", i);
            intent.putExtras(bundle);
            setResult(0x01, intent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        back();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            back();
        }
        return super.onOptionsItemSelected(item);
    }

    public void back(){
        Intent intent = new Intent();
        setResult(0x00, intent);
        finish();
    }

    protected void initListView() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        ArrayList<String> names = bundle.getStringArrayList("names");
        ArrayList<String> times = bundle.getStringArrayList("times");
        int count = bundle.getInt("count");

        ArrayList<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            HashMap<String, Object> m = new HashMap<>();
            m.put("name", names.get(i));
            m.put("time", times.get(i));
            list.add(m);
        }

        SimpleAdapter adapter = new SimpleAdapter(this, list, R.layout.save_item,
                new String[]{"name", "time"}, new int[]{R.id.saveItemNameTextView, R.id.saveItemScoreTextView});
        saveListView.setAdapter(adapter);
    }
}