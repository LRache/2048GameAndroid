package com.rache.game2048;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.Random;
import java.util.TimeZone;
import java.util.Deque;
import java.lang.Math;
import java.util.Timer;
import java.util.TimerTask;

class SaveScoreListItem implements Comparable<SaveScoreListItem>{
    String name;
    int score;
    int undoCount;

    @Override
    public int compareTo(SaveScoreListItem saveScoreListItem) {
        return saveScoreListItem.score - score;
    }
}

public class MainActivity extends AppCompatActivity {

    static String TAG = "MainActivity";
    static String PROPERTY_VERSION = "1.01";

    public static class GameSave{
        String name;
        long time;
    }


    public static class CellPos {
        CellPos(int a, int b) {
            r = a;
            c = b;
        }
        int r;
        int c;
    }

    // FebFour Version
    public static class FfCellStep {
        FfCellStep(int a, int b, int t) {
            r = a;
            c = b;
            v = t;
        }
        int r, c, v;
    }

    public static class NumberStep {
        int score;
        int[][] numbers;
        NumberStep(int s, int[][] n) {
            numbers = new int[4][4];
            for (int i = 0; i < 4; i++) {
                System.arraycopy(n[i], 0, numbers[i], 0, 4);
            }
            score = s;
        }
    }

    protected ArrayList<GameSave> saves = new ArrayList<>();

    protected Button newGameButton;
    protected Button undoButton;
    protected TextView saveNameTextView;
    protected TextView scoreTextView;
    protected TextView undoCountTextView;
    protected GameArea gameArea;
    protected Random random;

    boolean moveFlag = true;
    float startX = 0;
    float startY = 0;
    float dxb = 0;
    float dyb = 0;

    int[][] numbers = new int[4][4];
    boolean[][] isSpan = new boolean[4][4];
    int score = 0;
    Deque<NumberStep> undoDeque = new ArrayDeque<>();
    int undoCount = 0;

    final int SELECT_SAVE_ACTIVITY_CODE = 0x00;

    String saveDirPath;
    String cacheDirPath;
    String currentName = "";
    File currentFile = null;
    File propertiesFile;
    Properties properties;
    boolean played = false;

    // FebFour Version
    FfCellStep[] ffCellSteps = new FfCellStep[12];
    int animationProgress = 0;
    int animationProgressEnd = 12;
    boolean playingBirthdayAnimation = false;
    Timer hpAnimationTimer;

    private byte[] intToBytes(int n) {
        byte[] b = new byte[4];
        b[0] = (byte) (n & 0xff);
        b[1] = (byte) (n >> 8 & 0xff);
        b[2] = (byte) (n >> 16 & 0xff);
        b[3] = (byte) (n >> 24 & 0xff);
        return b;
    }

    private int bytesToInt(byte[] b) {
        int n = 0;
        for(int i = 0;i<4;i++){
            n += (b[i] & 0xff) << (i*8);
        }
        return n;
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        newGameButton = findViewById(R.id.newGameButton);
        undoButton = findViewById(R.id.undoButton);
        saveNameTextView = findViewById(R.id.saveNameTextView);
        scoreTextView = findViewById(R.id.scoreTextView);
        undoCountTextView = findViewById(R.id.undoCountTextView);
        gameArea = findViewById(R.id.gameArea);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        gameArea.setFrameSize(metrics.widthPixels - 60);
        dxb = dyb = (metrics.widthPixels - 60) * 0.12f;

        random = new Random(System.currentTimeMillis());

        newGameButton.setOnClickListener(view -> {
            if (played) {
                AlertDialog.Builder builder = getAskSaveDialogBuilder();
                builder.setNegativeButton(R.string.not_save_string, (dialogInterface, i) -> newGame()).
                        setPositiveButton(R.string.do_save_string, (dialogInterface, i) -> onSaveFile_withNewGame()).
                        setNeutralButton(R.string.cancel_string, (dialogInterface, i) -> {}).create().show();
            } else {
                newGame();
            }
        });
        undoButton.setOnClickListener(view -> undo());

        cacheDirPath = getExternalCacheDir().getAbsolutePath();
        saveDirPath = cacheDirPath + "/saves";
        propertiesFile = new File(cacheDirPath, "properties.properties");
        properties = new Properties();

        File filedir = new File(saveDirPath);
        if (!filedir.isDirectory()) {
            if(!filedir.mkdir()) {
                Toast.makeText(this, R.string.create_dir_failed, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.create_dir_successfully, Toast.LENGTH_SHORT).show();
            }
            initProperties();
            newGame();
        } else {
            loadProperties();
            if(loadAutoSave()) {
                newGame();
            }
        }

        // FebFour Version

        TextView titleTextView = findViewById(R.id.gameTitleTextView);
        SpannableString spannableString = new SpannableString(getResources().getString(R.string.title_string));
        spannableString.setSpan(new AbsoluteSizeSpan(20, true), 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new AbsoluteSizeSpan(20, true), 3, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        titleTextView.setText(spannableString);

        Button hbButton = findViewById(R.id.happyBirthdayButton);
        hbButton.setOnClickListener(view -> showBirthday());
        Button toDLYButton = findViewById(R.id.toDLYButton);
        toDLYButton.setOnClickListener(view -> {
             Intent intent = new Intent(MainActivity.this, SentencesActivity.class);
             startActivity(intent);
        });

        ffCellSteps[0] = new FfCellStep(0, 0, 1);
        ffCellSteps[1] = new FfCellStep(0, 1, 2);
        ffCellSteps[2] = new FfCellStep(0, 2, 3);
        ffCellSteps[3] = new FfCellStep(0, 3, 3);
        ffCellSteps[4] = new FfCellStep(1, 0, 4);
        ffCellSteps[5] = new FfCellStep(1, 1, 5);
        ffCellSteps[6] = new FfCellStep(1, 2, 6);
        ffCellSteps[7] = new FfCellStep(2, 0, 7);
        ffCellSteps[8] = new FfCellStep(2, 1, 8);
        ffCellSteps[9] = new FfCellStep(2, 2, 9);
        ffCellSteps[10] = new FfCellStep(2, 3, 10);
        ffCellSteps[11] = new FfCellStep(3, 3, 11);
    }

    private void initProperties() {
        properties.setProperty("version", PROPERTY_VERSION);
        properties.setProperty("current", "");
        properties.setProperty("played", Boolean.toString(false));

        currentFile = null;
        currentName = "";
        played = false;
    }

    private void loadProperties() {
        if (!propertiesFile.isFile()) {
            initProperties();
            newGame();
        } else {
            try {
                FileReader reader = new FileReader(propertiesFile);
                properties.load(reader);
                reader.close();

                String version = properties.getProperty("version");
                if (version == null) {
                    initProperties();
                }else if (!version.equals(PROPERTY_VERSION)) {
                    initProperties();
                } else {
                    String fn = properties.getProperty("current");
                    String playedString = properties.getProperty("played");

                    if (fn != null) {
                        if (fn.isEmpty()) {
                            if (loadAutoSave()) {
                                newGame();
                            }
                        } else {
                            currentFile = new File(saveDirPath, fn + ".2048Game");
                            currentName = fn;
                            saveNameTextView.setText(fn);
                        }
                    } else {
                        currentFile = null;
                        currentName = "";
                    }

                    if (playedString != null) {
                        played = Boolean.parseBoolean(playedString);
                    } else {
                        played = false;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                initProperties();
            }
        }
    }

    private boolean loadAutoSave() {
        File file = new File(saveDirPath, "auto_save");
        if (file.isFile()) {
            if (!loadFile(file)) {
                currentFile = null;
                currentName = "";
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onStop() {
        writeFile(new File(saveDirPath, "auto_save"));

        properties.setProperty("current", currentName);
        properties.setProperty("played", Boolean.toString(played));
        try {
            FileWriter writer = new FileWriter(propertiesFile);
            properties.store(writer, "2048Game properties");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.saveMenuItem) {
            onSaveFile();
        } else if (itemId == R.id.openMenuItem){
            onSelectSave();
        } else if (itemId == R.id.aboutMenuItem) {
            showAbout();
        } else if (itemId == R.id.renameSaveMenuItem) {
            final EditText inputServer = new EditText(this);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.input_new_save_name_text).setView(inputServer);
            builder.setNegativeButton(R.string.cancel_string, (dialog, which) -> dialog.dismiss());

            builder.setPositiveButton(R.string.ok_string, (dialog, which) -> {
                String name = inputServer.getText().toString();
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, R.string.save_name_cannot_be_empty_text, Toast.LENGTH_SHORT).show();
                } else {
                    renameSave(name);
                }
            });
            builder.create().show();
        } else if (itemId == R.id.deleteSaveMenuItem) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.delete_save_string).setMessage(R.string.ensure_delete_save_text);
            builder.setNegativeButton(R.string.cancel_string, (dialog, which) -> dialog.dismiss());
            builder.setPositiveButton(R.string.ok_string, (dialog, which) -> deleteSave());
            builder.create().show();
        } else if (itemId == R.id.scoreListMenuItem) {
            showScoreList();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_SAVE_ACTIVITY_CODE) {
            if (resultCode == 0x01) {
                Bundle bundle = data.getExtras();
                loadSave(bundle.getInt("index"));
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (playingBirthdayAnimation) return super.onTouchEvent(event);
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            startX = event.getX();
            startY = event.getY();
            moveFlag = true;
        }
        else if (moveFlag) {
            if (action == MotionEvent.ACTION_MOVE) {
                float dx = event.getX() - startX;
                float dy = event.getY() - startY;
                float dxa = Math.abs(dx);
                float dya = Math.abs(dy);
                if (dxa > dya) {
                    if (dxa >= dxb) {
                        moveFlag = false;
                        if (dx < 0) left();
                        else right();
                    }
                } else {
                    if (dya >= dyb) {
                        moveFlag = false;
                        if (dy < 0) up();
                        else down();
                    }
                }
            }
        }

        return super.onTouchEvent(event);
    }

    public void onSaveFile() {
        if (playingBirthdayAnimation) return;
        if (currentFile == null) {
            final EditText inputServer = new EditText(MainActivity.this);
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.input_name_text).setView(inputServer);
            builder.setNegativeButton(R.string.cancel_string, (dialog, which) -> dialog.dismiss());

            builder.setPositiveButton(R.string.ok_string, (dialog, which) -> {
                String name = inputServer.getText().toString();
                createFile(name, name + ".2048Game");
            });
            builder.create().show();
        } else {
            if (writeFile(currentFile)) {
                Toast.makeText(this, R.string.save_successfully_text, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.save_failed_text, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onSaveFile_withNewGame() {
        if (playingBirthdayAnimation) return;
        if (currentFile == null) {
            final EditText inputServer = new EditText(MainActivity.this);
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.input_name_text).setView(inputServer);
            builder.setNegativeButton(R.string.cancel_string, (dialog, which) -> {
                dialog.dismiss();
                newGame();
            }).setPositiveButton(R.string.ok_string, (dialog, which) -> {
                String name = inputServer.getText().toString();
                createFile(name, name + ".2048Game");
                newGame();
            });
            builder.create().show();
        } else {
            if (writeFile(currentFile)) {
                Toast.makeText(this, R.string.save_successfully_text, Toast.LENGTH_SHORT).show();
                newGame();
            } else {
                Toast.makeText(this, R.string.save_failed_text, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onSaveFile_withLoadSave(File f, String n) {
        if (playingBirthdayAnimation) return;
        if (currentFile == null) {
            final EditText inputServer = new EditText(MainActivity.this);
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.input_name_text).setView(inputServer);
            builder.setNegativeButton(R.string.cancel_string, (dialog, which) -> {
                dialog.dismiss();
                basicLoadSave(f, n);
            });

            builder.setPositiveButton(R.string.ok_string, (dialog, which) -> {
                String name = inputServer.getText().toString();
                createFile(name, name + ".2048Game");
                basicLoadSave(f, n);
            });
            builder.create().show();
        } else {
            if (writeFile(currentFile)) {
                Toast.makeText(this, R.string.save_successfully_text, Toast.LENGTH_SHORT).show();
                basicLoadSave(f, n);
            } else {
                Toast.makeText(this, R.string.save_failed_text, Toast.LENGTH_SHORT).show();
            }
        }
    }

    void createFile(String name, String filename) {
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.save_name_cannot_be_empty_text, Toast.LENGTH_SHORT).show();
            return;
        }
        currentFile = new File(saveDirPath, filename);
        currentName = name;
        saveNameTextView.setText(name);
        writeFile(currentFile);
        Toast.makeText(this, R.string.save_successfully_text, Toast.LENGTH_SHORT).show();
    }

    public void updateSaveList() {
        File dirfile = new File(saveDirPath);
        if (!dirfile.isDirectory()) {
            if (!dirfile.mkdir()) {
                return;
            }
        }
        
        String[] filenames = dirfile.list();
        if (filenames != null) {
            saves.clear();
            for (String filename : filenames) {
                if (filename.endsWith(".2048Game")) {
                    File f = new File(saveDirPath, filename);
                    GameSave save = new GameSave();
                    save.name= filename.substring(0, filename.length() - 9);
                    save.time = f.lastModified();
                    saves.add(save);
                }
            }
        }
    }

    public boolean loadFile(File file){
        if (!file.isFile()) {
            Toast.makeText(this, R.string.load_save_failed_text + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            FileInputStream reader = new FileInputStream(file);

            byte[] scoreByte = new byte[4];
            byte[] undoCountByte = new byte[4];
            int result1 = reader.read(scoreByte);
            if (result1 != 4) {
                Toast.makeText(this, R.string.load_error_0_text, Toast.LENGTH_SHORT).show();
                return false;
            }
            int result2 = reader.read(undoCountByte);
            if (result2 != 4) {
                Toast.makeText(this, R.string.load_error_1_text, Toast.LENGTH_SHORT).show();
                return false;
            }

            clear();
            gameArea.clear();
            setScore(bytesToInt(scoreByte));
            undoCount = bytesToInt(undoCountByte);
            undoCountTextView.setText(getString(R.string.undo_count_string, undoCount));

            for (int i = 0; i < 4; i++) {
                byte[] data = new byte[4];
                if (reader.read(data) == -1) {
                    Toast.makeText(this, R.string.load_error_2_text, Toast.LENGTH_SHORT).show();
                    return false;
                }
                for (int j = 0; j < 4; j++) {
                    numbers[i][j] = data[j];
                    if (data[j] != 0) {
                        gameArea.addSpawnAnimation(i, j, data[j]);
                    }
                }
            }
            gameArea.startAllAnimation();
            reader.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean writeFile(File file){
        try {
            FileOutputStream writer = new FileOutputStream(file);
            writer.write(intToBytes(score));
            writer.write(intToBytes(undoCount));
            for (int i = 0; i < 4; i++) {
                byte[] data = new byte[4];
                for (int j = 0; j < 4; j++) {
                    data[j] = (byte) numbers[i][j];
                }
                writer.write(data);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void onSelectSave() {
        if (playingBirthdayAnimation) return;
        ArrayList<String> times = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(TimeZone.getDefault().getID()));

        updateSaveList();
        for (GameSave s: saves) {
            times.add(simpleDateFormat.format(s.time));
            names.add(s.name);
        }

        Intent intent = new Intent(this, SelectSaveActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("count", saves.size());
        bundle.putStringArrayList("names", names);
        bundle.putStringArrayList("times", times);
        intent.putExtras(bundle);
        
        startActivityForResult(intent, SELECT_SAVE_ACTIVITY_CODE);
    }

    public boolean basicLoadSave(File f, String n) {
        if(loadFile(f)) {
            currentFile = f;
            currentName = n;
            saveNameTextView.setText(n);
            played = false;
            return true;
        }
        return false;
    }

    public void loadSave(int index) {
        GameSave save = saves.get(index);
        if (played) {
            AlertDialog.Builder builder = getAskSaveDialogBuilder();
            builder.setNegativeButton(R.string.not_save_string, (dialogInterface, i) -> onSaveFile_withLoadSave(new File(saveDirPath, save.name + ".2048Game"), save.name)).setPositiveButton(R.string.do_save_string, (dialogInterface, i) -> onSaveFile_withLoadSave(new File(saveDirPath, save.name + ".2048Game"), save.name)).setNeutralButton(R.string.cancel_string, (dialogInterface, i) -> {

            }).create().show();
        } else {
            if(basicLoadSave(new File(saveDirPath, save.name + ".2048Game"), save.name)){
                Toast.makeText(this, getResources().getText(R.string.load_save_successfully_text) + save.name, Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, R.string.load_save_failed_text, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void randomSpawn() {
        ArrayList<CellPos> emptyCells = new ArrayList<>();
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                if (numbers[row][column] == 0) emptyCells.add(new CellPos(row, column));
            }
        }
        if (emptyCells.isEmpty()) return;
        int randomIndex = random.nextInt(emptyCells.size());
        int randomNumber;
        if (random.nextInt(5) == 4) randomNumber= 2;
        else randomNumber = 1;
        CellPos cell = emptyCells.get(randomIndex);
        numbers[cell.r][cell.c] = randomNumber;
        gameArea.addSpawnAnimation(cell.r, cell.c, randomNumber);
    }

    public void clear() {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                numbers[r][c] = 0;
            }
        }
        gameArea.clear();
    }

    public void newGame() {
        if (playingBirthdayAnimation) return;
        saveNameTextView.setText(R.string.new_game_string);
        setScore(0);
        clear();
        played = false;
        currentFile = null;
        currentName = "";
        undoDeque.clear();
        undoCount = 0;
        undoCountTextView.setText(getString(R.string.undo_count_string, undoCount));

        randomSpawn();
        randomSpawn();
        gameArea.startAllAnimation();
    }

    protected void pushStep(NumberStep step) {
        if (undoDeque.size() >= 64) {
            undoDeque.removeLast();
        }
        undoDeque.push(step);
    }

    public void undo(){
        if (undoDeque.isEmpty()) {
            Toast.makeText(this, R.string.cannot_undo_text, Toast.LENGTH_SHORT).show();
        } else {
            NumberStep step = undoDeque.pop();
            setScore(step.score);
            undoCount++;
            undoCountTextView.setText(getString(R.string.undo_count_string, undoCount));
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    numbers[i][j] = step.numbers[i][j];
                    gameArea.setNumber(i, j, step.numbers[i][j]);
                }
            }
            gameArea.invalidate();
        }
    }

    protected void setScore(int s) {
        score = s;
        scoreTextView.setText(String.valueOf(s));
    }

    protected void initSpan() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                isSpan[i][j] = false;
            }
        }
    }

    protected void moveNumber(int fr, int fc, int tr, int tc) {
        int n = numbers[fr][fc];
        numbers[tr][tc] = n;
        numbers[fr][fc] = 0;
        gameArea.addMoveAnimation(fr, fc, tr, tc, n);
    }

    protected boolean trySpan(int fr, int fc, int tr, int tc) {
        if (isSpan[tr][tc]) return false;
        if (numbers[fr][fc] == numbers[tr][tc]) {
            gameArea.addMoveAnimation(fr, fc, tr, tc, numbers[fr][fc]);
            int n = ++numbers[tr][tc];
            numbers[fr][fc] = 0;
            gameArea.addSpawnAnimation(tr, tc, n);
            isSpan[tr][tc] = true;
            setScore(score + (1 << n));
            return true;
        }
        return false;
    }

    public void up() {
        gameArea.stopAllAnimation();
        initSpan();
        NumberStep step = new NumberStep(score, numbers);

        boolean flag = false;
        for (int row = 1; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                if (numbers[row][column] == 0) continue;

                int tr = row - 1;
                for (; tr >= 0; --tr) if (numbers[tr][column] != 0) break;

                if (tr == -1) {
                    flag = true;
                    moveNumber(row, column, 0, column);
                } else if (trySpan(row, column, tr, column)) {
                    flag = true;
                } else {
                    tr++;
                    if (tr != row) {
                        moveNumber(row, column, tr, column);
                        flag = true;
                    }
                }
            }
        }
        if (flag) {
            pushStep(step);
            randomSpawn();
            gameArea.startAllAnimation();
            played = true;
        }
    }

    public void down() {
        gameArea.stopAllAnimation();
        initSpan();
        NumberStep step = new NumberStep(score, numbers);

        boolean flag = false;
        for (int row = 2; row >= 0; row--) {
            for (int column = 0; column < 4; column++) {
                if (numbers[row][column] == 0) continue;

                int tr = row + 1;
                for (; tr < 4; ++tr) if (numbers[tr][column] != 0) break;

                if (tr == 4) {
                    flag = true;
                    moveNumber(row, column, 3, column);
                } else if (trySpan(row, column, tr, column)) {
                    flag = true;
                } else {
                    tr--;
                    if (tr != row) {
                        moveNumber(row, column, tr, column);
                        flag = true;
                    }
                }
            }
        }
        if (flag) {
            pushStep(step);
            randomSpawn();
            gameArea.startAllAnimation();
            played = true;
        }
    }

    public void left() {
        gameArea.stopAllAnimation();
        initSpan();
        NumberStep step = new NumberStep(score, numbers);

        boolean flag = false;
        for (int column = 1; column < 4; ++column) {
            for (int row = 0; row < 4; ++row) {
                if (numbers[row][column] == 0) continue;

                int tc = column - 1;
                for (; tc >= 0; --tc) if (numbers[row][tc] != 0) break;

                if (tc == -1) {
                    moveNumber(row, column, row, 0);
                    flag = true;
                } else if (trySpan(row, column, row, tc)) {
                    flag = true;
                } else {
                    tc++;
                    if (tc != column) {
                        moveNumber(row, column, row, tc);
                        flag = true;
                    }
                }
            }
        }
        if (flag) {
            pushStep(step);
            randomSpawn();
            gameArea.startAllAnimation();
            played = true;
        }
    }

    public void right() {
        gameArea.stopAllAnimation();
        initSpan();
        NumberStep step = new NumberStep(score, numbers);

        boolean flag = false;
        for (int column = 2; column >= 0; --column) {
            for (int row = 0; row < 4; ++row) {
                if (numbers[row][column] == 0) continue;

                int tc = column + 1;
                for (; tc < 4; ++tc) if (numbers[row][tc] != 0) break;
                if (tc == 4) {
                    moveNumber(row, column, row, 3);
                    flag = true;
                } else if (trySpan(row, column, row, tc)) {
                    flag = true;
                } else {
                    tc--;
                    if (tc != column) {
                        moveNumber(row, column, row, tc);
                        flag = true;
                    }
                }
            }
        }
        if (flag) {
            pushStep(step);
            randomSpawn();
            gameArea.startAllAnimation();
            played = true;
        }
    }

    public void showAbout() {
        Resources r = getResources();
        String aboutString = r.getString(R.string.game_name) + '\n' +
                "作者：" + r.getString(R.string.author_name) + "\n" +
                r.getString(R.string.version_name);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.about_string).setMessage(aboutString);
        builder.create().show();

    }

    public void renameSave(String newName) {
        if (currentFile == null | currentName.isEmpty()) {
            Toast.makeText(this, R.string.rename_failed_not_select_text, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!currentFile.isFile()) {
            Toast.makeText(this, R.string.rename_failed_text, Toast.LENGTH_SHORT).show();
            return;
        }
        File newFile = new File(saveDirPath, newName + ".2048Game");
        if (!currentFile.renameTo(newFile)) {
            Toast.makeText(this, R.string.rename_failed_text, Toast.LENGTH_SHORT).show();
        } else {
            currentName = newName;
            saveNameTextView.setText(newName);
            currentFile = newFile;
            Toast.makeText(this, R.string.rename_successfully_text, Toast.LENGTH_SHORT).show();
        }
    }

    public void deleteSave() {
        if (currentFile == null | currentName.isEmpty()) {
            Toast.makeText(this, R.string.delete_failed_not_select_text, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!currentFile.isFile()) {
            Toast.makeText(this, R.string.delete_failed_text, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!currentFile.delete()) {
            Toast.makeText(this, R.string.delete_failed_text, Toast.LENGTH_SHORT).show();
        } else {
            newGame();
            Toast.makeText(this, R.string.delete_successfully_text, Toast.LENGTH_SHORT).show();
        }
    }

    public void showScoreList() {
        ArrayList<SaveScoreListItem> items = new ArrayList<>();
        updateSaveList();
        for (GameSave s: saves) {
            String saveName = s.name;
            try {
                FileInputStream reader = new FileInputStream(new File(saveDirPath, saveName + ".2048Game"));
                byte[] b1 = new byte[4], b2 = new byte[4];
                if (reader.read(b1) != 4 | reader.read(b2) != 4) {
                    reader.close();
                    continue;
                }
                reader.close();

                SaveScoreListItem item = new SaveScoreListItem();
                item.score = bytesToInt(b1);
                item.undoCount = bytesToInt(b2);
                item.name = saveName;
                items.add(item);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(items);

        int count = Math.min(5, items.size());
        ArrayList<String> names = new ArrayList<>();
        ArrayList<Integer> scores = new ArrayList<>();
        ArrayList<Integer> undoCounts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SaveScoreListItem item = items.get(i);
            names.add(item.name);
            scores.add(item.score);
            undoCounts.add(item.undoCount);
        }

        Intent intent = new Intent(this, ScoreListActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("count", count);
        bundle.putStringArrayList("names", names);
        bundle.putIntegerArrayList("scores", scores);
        bundle.putIntegerArrayList("undoCounts", undoCounts);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public AlertDialog.Builder getAskSaveDialogBuilder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.save_text).setMessage(R.string.ask_save_text);
        return builder;
    }

    // FebFour Version
    public void showBirthday() {
        if (!playingBirthdayAnimation) {
            gameArea.clear();
            playingBirthdayAnimation = true;

            Resources res = getResources();
            gameArea.numberTexts[1] = res.getString(R.string.ff_string_2);
            gameArea.numberTexts[2] = res.getString(R.string.ff_string_4);
            gameArea.numberTexts[3] = res.getString(R.string.ff_string_8);
            gameArea.numberTexts[4] = res.getString(R.string.ff_string_16);
            gameArea.numberTexts[5] = res.getString(R.string.ff_string_32);
            gameArea.numberTexts[6] = res.getString(R.string.ff_string_64);
            gameArea.numberTexts[7] = res.getString(R.string.ff_string_128);
            gameArea.numberTexts[8] = res.getString(R.string.ff_string_256);
            gameArea.numberTexts[9] = res.getString(R.string.ff_string_512);
            gameArea.numberTexts[10] = res.getString(R.string.ff_string_1024);
            gameArea.numberTexts[11] = res.getString(R.string.ff_string_2048);

            scoreTextView.setText(R.string.ff_birthday_string);
            saveNameTextView.setText(R.string.happyBirthday_string);

            for (int i = 1; i < 11; i++) {
                gameArea.cellTextPaints[i].setTextSize(gameArea.cellSize * 0.625f);
            }
            gameArea.cellTextPaints[11].setTextSize(gameArea.cellSize * 0.20f);

            animationProgress = 0;
            hpAnimationTimer = new Timer();
            hpAnimationTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    FfCellStep step = ffCellSteps[animationProgress];
                    gameArea.addSpawnAnimation(step.r, step.c, step.v);
                    gameArea.startAllAnimation();
                    animationProgress++;
                    if (animationProgress == animationProgressEnd) {
                        hpAnimationTimer.cancel();
                    }
                }
            }, 0, 350);
        }
        else {
            hpAnimationTimer.cancel();
            gameArea.initNumberTexts();
            gameArea.setFontSize();
            gameArea.clear();
            setScore(score);
            if (currentName.isEmpty()) saveNameTextView.setText(R.string.new_game_string);
            else saveNameTextView.setText(currentName);
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    gameArea.setNumber(r, c, numbers[r][c]);
                }
            }
            gameArea.invalidate();
            playingBirthdayAnimation = false;
        }
    }
}
