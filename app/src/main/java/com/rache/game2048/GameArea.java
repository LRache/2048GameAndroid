package com.rache.game2048;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

public class GameArea extends View {
    private static class NumberSpawnAnimation {
        public int number = 0;
        public float x = 0, y = 0;
        public int row = 0, column = 0;
    }
    NumberSpawnAnimation[] spawnAnimations = new NumberSpawnAnimation[16];
    int spawnAnimationCount = 0;

    private static class NumberMoveAnimation {
        public int number = 0;
        public float x = 0, y = 0, xSpeed = 0, ySpeed = 0;
        public int fr = 0, fc = 0, tr = 0, tc = 0;
    }
    NumberMoveAnimation[] moveAnimations = new NumberMoveAnimation[16];
    int moveAnimationCount = 0;

    private int spawnAnimationProgress = 0;
    private float spawnAnimationSize;
    private float spawnAnimationSizeStep;
    private int moveAnimationProgress = 0;
    private final int moveAnimationProgressEnd = 10;

    protected Paint backgroundPaint = new Paint();
    private int frameSize = 800;
    public float cellSize = 50.0f;
    private float frameSep = 15;
    private float cellSep = 18;

    private final RectF[][] cellRects = new RectF[4][4];
    private final Paint[] cellBackgroundPaints = new Paint[13];
    public final Paint[] cellTextPaints = new Paint[19];
    public final String[] numberTexts = new String[19];
    private final float[][][] cellTextPos = new float[4][4][2];

    private final int[][] data = new int[4][4];

    boolean isRunningSpawnAnimation = false;
    boolean isRunningAnimation = false;
    int spawnAnimationTimerPeriod = 8;
    int moveAnimationTimerPeriod = 5;
    Timer animationTimer;
    TimerTask spawnAnimationTimerTask1;
    TimerTask spawnAnimationTimerTask2;
    TimerTask moveAnimationTimerTask;

    private final String TAG = "GameArea";

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
       @SuppressLint("HandlerLeak")
       public void handleMessage(Message msg) {
           invalidate();
       }
   };

    public GameArea(Context context) {
        super(context);
        init();
    }

    public GameArea(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameArea(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @SuppressLint("HandlerLeak")
    protected void init() {

        for (int i = 0; i < 13; i++) {
            cellBackgroundPaints[i] = new Paint();
        }
        for (int i = 0; i < 19; i++) {
            cellTextPaints[i] = new Paint();
        }
        Resources res = getResources();

        cellBackgroundPaints[0].setColor(res.getColor(R.color.cell_bg_color_empty));
        cellBackgroundPaints[1].setColor(res.getColor(R.color.cell_bg_color_2));
        cellBackgroundPaints[2].setColor(res.getColor(R.color.cell_bg_color_4));
        cellBackgroundPaints[3].setColor(res.getColor(R.color.cell_bg_color_8));
        cellBackgroundPaints[4].setColor(res.getColor(R.color.cell_bg_color_16));
        cellBackgroundPaints[5].setColor(res.getColor(R.color.cell_bg_color_32));
        cellBackgroundPaints[6].setColor(res.getColor(R.color.cell_bg_color_64));
        cellBackgroundPaints[7].setColor(res.getColor(R.color.cell_bg_color_128));
        cellBackgroundPaints[8].setColor(res.getColor(R.color.cell_bg_color_256));
        cellBackgroundPaints[9].setColor(res.getColor(R.color.cell_bg_color_512));
        cellBackgroundPaints[10].setColor(res.getColor(R.color.cell_bg_color_1024));
        cellBackgroundPaints[11].setColor(res.getColor(R.color.cell_bg_color_2048));
        cellBackgroundPaints[12].setColor(res.getColor(R.color.cell_bg_color_other));

        for (int i = 1; i < 3; i++) cellTextPaints[i].setColor(res.getColor(R.color.cell_text_color_2_4));
        for (int i = 3; i < 19; i++) cellTextPaints[i].setColor(res.getColor(R.color.cell_text_color_other));
        for (int i = 1; i < 19; i++) cellTextPaints[i].setTextAlign(Paint.Align.CENTER);

        initNumberTexts();

        backgroundPaint.setColor(getResources().getColor(R.color.game_area_bg));

        for (int i = 0; i < 19; i++) {
            cellTextPaints[i].setAntiAlias(true);
        }
    }

    public void initNumberTexts() {
        Resources res = getResources();
        numberTexts[1] = res.getString(R.string.string_2);
        numberTexts[2] = res.getString(R.string.string_4);
        numberTexts[3] = res.getString(R.string.string_8);
        numberTexts[4] = res.getString(R.string.string_16);
        numberTexts[5] = res.getString(R.string.string_32);
        numberTexts[6] = res.getString(R.string.string_64);
        numberTexts[7] = res.getString(R.string.string_128);
        numberTexts[8] = res.getString(R.string.string_256);
        numberTexts[9] = res.getString(R.string.string_512);
        numberTexts[10] = res.getString(R.string.string_1024);
        numberTexts[11] = res.getString(R.string.string_2048);
        numberTexts[12] = res.getString(R.string.string_4096);
        numberTexts[13] = res.getString(R.string.string_8192);
        numberTexts[14] = res.getString(R.string.string_16384);
        numberTexts[15] = res.getString(R.string.string_32768);
        numberTexts[16] = res.getString(R.string.string_65536);
        numberTexts[17] = res.getString(R.string.string_131072);
        numberTexts[18] = res.getString(R.string.string_undefined);
    }

    public void setFontSize() {
        for (int i = 1; i < 4; i++) cellTextPaints[i].setTextSize(cellSize * 0.625f);
        for (int i = 4; i < 7; i++) cellTextPaints[i].setTextSize(cellSize * 0.50f);
        for (int i = 7; i < 10; i++) cellTextPaints[i].setTextSize(cellSize *0.40f);
        for (int i = 10; i < 14; i++) cellTextPaints[i].setTextSize(cellSize * 0.32f);
        for (int i = 14; i < 17; i++) cellTextPaints[i].setTextSize(cellSize * 0.30f);
        cellTextPaints[17].setTextSize(cellSize * 0.27f);
        cellTextPaints[18].setTextSize(cellSize * 0.16f);
    }

    public void setFrameSize(int s) {
        frameSize = s;

        cellSep = (float) frameSize / 30;
        cellSize = (float) frameSize / 5;
        frameSep = cellSep * 1.5f;
        spawnAnimationSizeStep = cellSize * 0.004f;
        setFontSize();

        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                cellRects[row][column] = new RectF(frameSep + (cellSize + cellSep) * column,
                        frameSep + (cellSize + cellSep) * row,
                        frameSep + (cellSize + cellSep) * column + cellSize,
                        frameSep + (cellSize + cellSep) * row + cellSize);
                cellTextPos[row][column][0] = frameSep + (cellSize + cellSep) * column + cellSize / 2;
                if (data[row][column] != 0)
                {
                    Paint.FontMetrics fontMetrics = cellTextPaints[data[row][column]].getFontMetrics();
                    cellTextPos[row][column][1] = frameSep + (cellSize + cellSep) * row + cellSize / 2 - fontMetrics.top / 2 - fontMetrics.bottom / 2;
                }
            }
        }
    }

    public void clear() {
        stopAllAnimation();
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                data[r][c] = 0;
            }
        }
        invalidate();
    }

    private void drawTextCenter(String text, float x, float y, float height, float width, Paint paint, Canvas canvas) {
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float x_ = x + width / 2;
        float y_ = y + height / 2 - fontMetrics.top / 2 - fontMetrics.bottom / 2;
        canvas.drawText(text, x_, y_, paint);
    }

    public void setNumber(int row, int column, int number) {
        data[row][column] = number;
        if (cellRects[row][column] != null) {
            Paint.FontMetrics fontMetrics = cellTextPaints[number].getFontMetrics();
            cellTextPos[row][column][1] = cellRects[row][column].centerY() + (fontMetrics.bottom - fontMetrics.top)/2 - fontMetrics.bottom;
        }
    }

    public void addSpawnAnimation(int row, int column, int number) {
        NumberSpawnAnimation animation = new NumberSpawnAnimation();
        animation.x = frameSep + (cellSize + cellSep) * column;
        animation.y = frameSep + (cellSize + cellSep) * row;
        animation.row = row;
        animation.column = column;
        animation.number = number;
        spawnAnimations[spawnAnimationCount++] = animation;
    }

    public void addMoveAnimation(int fr, int fc, int tr, int tc, int number) {
        NumberMoveAnimation animation = new NumberMoveAnimation();
        animation.number = number;
        animation.x = frameSep + (cellSize + cellSep) * fc;
        animation.y = frameSep + (cellSize + cellSep) * fr;
        animation.xSpeed = (cellSize + cellSep) * (tc - fc) / moveAnimationProgressEnd;
        animation.ySpeed = (cellSize + cellSep) * (tr - fr) / moveAnimationProgressEnd;
        animation.fr = fr;
        animation.fc = fc;
        animation.tr = tr;
        animation.tc = tc;
        moveAnimations[moveAnimationCount++] = animation;
    }

    public void startSpawnAnimation() {
        Log.i(TAG, "startSpawnAnimation: ");
        if (spawnAnimationCount == 0) {
            isRunningAnimation = false;
            return;
        }
        spawnAnimationProgress = 0;
        spawnAnimationSize = 0;
        isRunningSpawnAnimation = true;

        spawnAnimationTimerTask1 = new TimerTask() {
            @Override
            public void run() {
                spawnAnimationTimerTask1_timeout();
            }
        };
        if (animationTimer != null) animationTimer.schedule(spawnAnimationTimerTask1, 0, spawnAnimationTimerPeriod);
    }

    public void startMoveAnimation() {
        Log.i(TAG, "startMoveAnimation: " + moveAnimationCount);
        if (moveAnimationCount == 0) {
            startSpawnAnimation();
            return;
        }
        moveAnimationProgress = 0;
        for (int i = 0; i < moveAnimationCount; ++i) {
            NumberMoveAnimation animation = moveAnimations[i];
            data[animation.fr][animation.fc] = 0;
        }

        moveAnimationTimerTask = new TimerTask() {
            @Override
            public void run() {
                moveAnimationTimerTask_timeout();
            }
        };
        if (animationTimer != null) animationTimer.schedule(moveAnimationTimerTask, 0, moveAnimationTimerPeriod);
    }

    public void startAllAnimation() {
        animationTimer = new Timer();
        isRunningAnimation = true;
        startMoveAnimation();
    }

    public void stopAllAnimation() {
        if (animationTimer != null) {
            animationTimer.cancel();
            animationTimer.purge();
            animationTimer = null;
        }
        isRunningSpawnAnimation = false;
        for (int i = 0; i < moveAnimationCount; ++i) {
            NumberMoveAnimation animation = moveAnimations[i];
            setNumber(animation.tr, animation.tc, animation.number);
        }
        for (int i = 0; i < spawnAnimationCount; ++i) {
            NumberSpawnAnimation animation = spawnAnimations[i];
            setNumber(animation.row, animation.column, animation.number);
        }
        moveAnimationCount = 0;
        spawnAnimationCount = 0;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int frameRadius = 15;
        canvas.drawRoundRect(0, 0, frameSize, frameSize, frameRadius, frameRadius, backgroundPaint);

        int cellRadius = 10;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                int n = data[r][c];
                if (n > 11)
                    canvas.drawRoundRect(cellRects[r][c], cellRadius, cellRadius, cellBackgroundPaints[12]);
                else
                    canvas.drawRoundRect(cellRects[r][c], cellRadius, cellRadius, cellBackgroundPaints[n]);
                if (n != 0) {
                    canvas.drawText(numberTexts[n], cellTextPos[r][c][0], cellTextPos[r][c][1], cellTextPaints[n]);
                }
            }
        }

        if (isRunningSpawnAnimation) {
            for (int i = 0; i < spawnAnimationCount; ++i) {
                NumberSpawnAnimation animation = spawnAnimations[i];
                int n = animation.number;
                if (n > 17) n = 18;
                float x = animation.x - spawnAnimationSize;
                float y = animation.y - spawnAnimationSize;
                if (n > 11)
                    canvas.drawRoundRect(x, y,
                            animation.x + cellSize + spawnAnimationSize, animation.y + cellSize + spawnAnimationSize,
                            cellRadius, cellRadius, cellBackgroundPaints[12]);
                else canvas.drawRoundRect(x, y,
                        animation.x + cellSize + spawnAnimationSize, animation.y + cellSize + spawnAnimationSize,
                        cellRadius, cellRadius, cellBackgroundPaints[n]);
                drawTextCenter(numberTexts[n], x, y, spawnAnimationSize * 2 + cellSize, spawnAnimationSize * 2 + cellSize, cellTextPaints[n], canvas);
            }
            for (int i = 0; i < moveAnimationCount; ++i) {
                NumberMoveAnimation animation = moveAnimations[i];
                int n = animation.number;
                if (n > 17) n = 18;
                if (n > 11) canvas.drawRoundRect(animation.x, animation.y, animation.x + cellSize,
                        animation.y + cellSize, cellRadius, cellRadius, cellBackgroundPaints[12]);
                else canvas.drawRoundRect(animation.x, animation.y, animation.x + cellSize,
                        animation.y + cellSize, cellRadius, cellRadius, cellBackgroundPaints[n]);
                drawTextCenter(numberTexts[n], animation.x, animation.y, cellSize, cellSize, cellTextPaints[n], canvas);
            }
        }
    }

    protected void spawnAnimationTimerTask1_timeout() {
        spawnAnimationSize += spawnAnimationSizeStep;
        spawnAnimationProgress++;
        handler.sendEmptyMessage(0);
        int spawnAnimationProgressEnd = 15;
        if (spawnAnimationProgress == spawnAnimationProgressEnd) {
            spawnAnimationTimerTask1.cancel();

            spawnAnimationTimerTask2 = new TimerTask() {
                @Override
                public void run() {
                    spawnAnimationTimerTask2_timeout();
                }
            };
            if (animationTimer != null) animationTimer.schedule(spawnAnimationTimerTask2, 0, spawnAnimationTimerPeriod);
        }
    }

    protected void spawnAnimationTimerTask2_timeout() {
        spawnAnimationSize -= spawnAnimationSizeStep;
        spawnAnimationProgress--;
        if (spawnAnimationProgress == 0) {
            spawnAnimationTimerTask2.cancel();
            for (int i = 0; i < spawnAnimationCount; ++i) {
                NumberSpawnAnimation animation = spawnAnimations[i];
                setNumber(animation.row, animation.column, animation.number);
            }
            isRunningSpawnAnimation = false;
            isRunningAnimation = false;
            spawnAnimationCount = 0;
        }
        handler.sendEmptyMessage(0);
    }

    protected void moveAnimationTimerTask_timeout() {
        moveAnimationProgress++;
        for (int i = 0; i < moveAnimationCount; ++i) {
            NumberMoveAnimation animation = moveAnimations[i];
            animation.x += animation.xSpeed;
            animation.y += animation.ySpeed;
        }
        if (moveAnimationProgress == moveAnimationProgressEnd) {
            moveAnimationTimerTask.cancel();
            for (int i = 0; i < moveAnimationCount; ++i) {
                NumberMoveAnimation animation = moveAnimations[i];
                setNumber(animation.tr, animation.tc, animation.number);
            }
            moveAnimationCount = 0;
            startSpawnAnimation();
        }
        handler.sendEmptyMessage(0);
    }
}
