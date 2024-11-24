package com.rache.game2048;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class SentencesActivity extends AppCompatActivity {
    static String TAG = "SentenceActivity";

    TextView[] textViews = new TextView[14];
    Button birthdaySongButton;

    int animationProgress = 0;
    int animationProgressEnd = 14;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sentences);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setTitle(R.string.ff_toDLY_string);
        textViews[0] = findViewById(R.id.sentenceTextView0);
        textViews[1] = findViewById(R.id.sentenceTextView1);
        textViews[2] = findViewById(R.id.sentenceTextView2);
        textViews[3] = findViewById(R.id.sentenceTextView3);
        textViews[4] = findViewById(R.id.sentenceTextView4);
        textViews[5] = findViewById(R.id.sentenceTextView5);
        textViews[6] = findViewById(R.id.sentenceTextView6);
        textViews[7] = findViewById(R.id.sentenceTextView7);
        textViews[8] = findViewById(R.id.sentenceTextView8);
        textViews[9] = findViewById(R.id.sentenceTextView9);
        textViews[10] = findViewById(R.id.sentenceTextView10);
        textViews[11] = findViewById(R.id.sentenceTextView11);
        textViews[12] = findViewById(R.id.sentenceTextView12);
        textViews[13] = findViewById(R.id.sentenceTextView13);
        birthdaySongButton = findViewById(R.id.birthdaySongButton);

        for (int i = 0; i < animationProgressEnd; i++) {
            textViews[i].setAlpha(0.0f);
        }
        birthdaySongButton.setAlpha(0.0f);

        birthdaySongButton.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://www.bilibili.com/video/BV1t7411J7FT"));
            startActivity(intent);
        });

        @SuppressLint("HandlerLeak") Handler handler = new Handler() {
            @SuppressLint("HandlerLeak")
            @Override
            public void handleMessage(@NonNull Message msg) {
                TextView textView = textViews[msg.what];
                ValueAnimator valueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
                valueAnimator.setDuration(1000);
                valueAnimator.addUpdateListener(valueAnimator1 -> textView.setAlpha((float) valueAnimator1.getAnimatedValue()));
                valueAnimator.start();
                super.handleMessage(msg);
            }
        };

        @SuppressLint("HandlerLeak") Handler handler2 = new Handler() {
            @SuppressLint("HandlerLeak")
            @Override
            public void handleMessage(@NonNull Message msg) {
                ValueAnimator valueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
                valueAnimator.setDuration(1000);
                valueAnimator.addUpdateListener(valueAnimator1 -> birthdaySongButton.setAlpha((float) valueAnimator1.getAnimatedValue()));
                valueAnimator.start();
                super.handleMessage(msg);
            }
        };

        Timer animationTimer = new Timer();
        animationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (animationProgress == animationProgressEnd) {
                    handler2.sendEmptyMessage(0);
                    animationTimer.cancel();
                    return;
                }
                handler.sendEmptyMessage(animationProgress);
                animationProgress++;
            }
        }, 500, 1000);
    }
    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}