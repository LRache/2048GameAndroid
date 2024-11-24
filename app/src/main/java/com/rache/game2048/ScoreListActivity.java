package com.rache.game2048;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ScoreListActivity extends AppCompatActivity {

    ListView scoreListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score_list);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle(R.string.score_list_string);

        scoreListView = findViewById(R.id.saveScoreListView);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        int count = bundle.getInt("count");
        ArrayList<String> names = bundle.getStringArrayList("names");
        ArrayList<Integer> scores = bundle.getIntegerArrayList("scores");
        ArrayList<Integer> undoCounts = bundle.getIntegerArrayList("undoCounts");

        ArrayList<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            HashMap<String, Object> m = new HashMap<>();
            m.put("number", i + 1);
            m.put("name", names.get(i));
            m.put("score", getString(R.string.score_list_item_score_text, scores.get(i)));
            m.put("undoCount", getString(R.string.score_list_item_undo_text, undoCounts.get(i)));
            list.add(m);
        }

        SimpleAdapter adapter = new SimpleAdapter(this, list, R.layout.save_score_item,
                new String[]{"number", "score", "name", "undoCount"},
                new int[]{R.id.scoreListNumberItemTextView, R.id.scoreListItemTextView,
                        R.id.scoreListItemNameTextView, R.id.scoreListItemUndoCountTextView});
        scoreListView.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}