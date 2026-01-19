package com.example.chronographapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ShotHistoryAdapter adapter;
    private TextView statsText;
    private View emptyState;

    private List<ShotData> shotList = new ArrayList<>();
    private int totalShots = 0;
    private float currentMass = 0.25f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        setupToolbar();
        initViews();
        getIntentData();
        setupRecyclerView();
        updateStatistics();
        checkEmptyState();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("История выстрелов");
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.shotsRecyclerView);
        statsText = findViewById(R.id.statsText);
        emptyState = findViewById(R.id.emptyState);

        Button exportButton = findViewById(R.id.exportButton);
        Button clearButton = findViewById(R.id.clearButton);

        if (exportButton != null) {
            exportButton.setOnClickListener(v -> exportData());
        }

        if (clearButton != null) {
            clearButton.setOnClickListener(v -> clearHistory());
        }
    }

    private void getIntentData() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            totalShots = extras.getInt("shot_count", 0);
            currentMass = extras.getFloat("mass", 0.25f);

            float[] velocityArray = extras.getFloatArray("velocity_history");
            float[] energyArray = extras.getFloatArray("energy_history");

            if (velocityArray != null && energyArray != null && velocityArray.length > 0) {
                createShotListFromArrays(velocityArray, energyArray);
            }
        }
    }

    private void createShotListFromArrays(float[] velocityArray, float[] energyArray) {
        shotList.clear();
        for (int i = 0; i < velocityArray.length; i++) {
            String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(new Date(System.currentTimeMillis() - (velocityArray.length - i) * 3000));

            ShotData shot = new ShotData(
                    i + 1,
                    velocityArray[i],
                    energyArray[i],
                    timestamp
            );
            shotList.add(shot);
        }
        totalShots = shotList.size();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ShotHistoryAdapter(shotList);
        recyclerView.setAdapter(adapter);

        adapter.setOnShotClickListener(new ShotHistoryAdapter.OnShotClickListener() {
            @Override
            public void onShotClick(int position, ShotData shot) {
                showShotDetailsDialog(shot);
            }

            @Override
            public void onShotLongClick(int position, ShotData shot) {
                showShotActionsDialog(position, shot);
            }
        });
    }

    private void showShotDetailsDialog(ShotData shot) {
        new AlertDialog.Builder(this)
                .setTitle(String.format("Выстрел #%d", shot.getShotNumber()))
                .setMessage(String.format(
                        "Скорость: %.1f м/с\n" +
                                "Энергия: %.2f Дж\n" +
                                "Время: %s\n" +
                                "Масса: %.2f г",
                        shot.getVelocity(), shot.getEnergy(), shot.getTimestamp(), currentMass))
                .setPositiveButton("OK", null)
                .show();
    }

    private void showShotActionsDialog(int position, ShotData shot) {
        String[] actions = {"Удалить", "Поделиться", "Отмена"};

        new AlertDialog.Builder(this)
                .setTitle("Действия с выстрелом")
                .setItems(actions, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            adapter.removeShot(position);
                            updateStatistics();
                            checkEmptyState();
                            Toast.makeText(HistoryActivity.this, "Выстрел удален", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            shareShotData(shot);
                            break;
                    }
                })
                .show();
    }

    private void shareShotData(ShotData shot) {
        String shareText = String.format(
                "Выстрел #%d: скорость %.1f м/с, энергия %.2f Дж. Время: %s",
                shot.getShotNumber(), shot.getVelocity(), shot.getEnergy(), shot.getTimestamp());

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Поделиться данными выстрела"));
    }

    private void updateStatistics() {
        if (shotList.isEmpty()) {
            statsText.setText("Нет данных о выстрелах");
            return;
        }

        float maxVelocity = 0;
        float minVelocity = Float.MAX_VALUE;
        float totalVelocity = 0;
        float maxEnergy = 0;
        float minEnergy = Float.MAX_VALUE;
        float totalEnergy = 0;

        for (ShotData shot : shotList) {
            float velocity = shot.getVelocity();
            float energy = shot.getEnergy();

            if (velocity > maxVelocity) maxVelocity = velocity;
            if (velocity < minVelocity) minVelocity = velocity;
            totalVelocity += velocity;

            if (energy > maxEnergy) maxEnergy = energy;
            if (energy < minEnergy) minEnergy = energy;
            totalEnergy += energy;
        }

        float avgVelocity = totalVelocity / shotList.size();
        float avgEnergy = totalEnergy / shotList.size();

        String stats = String.format(Locale.getDefault(),
                "Всего: %d | Скорость: макс %.1f/мин %.1f/ср %.1f м/с | Энергия: макс %.2f/ср %.2f Дж",
                shotList.size(), maxVelocity, minVelocity, avgVelocity, maxEnergy, avgEnergy);

        statsText.setText(stats);
    }

    private void checkEmptyState() {
        if (emptyState != null) {
            if (shotList.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void exportData() {
        if (shotList.isEmpty()) {
            Toast.makeText(this, "Нет данных для экспорта", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Номер;Время;Скорость (м/с);Энергия (Дж)\n");

        for (ShotData shot : shotList) {
            csv.append(shot.getShotNumber()).append(";")
                    .append(shot.getTimestamp()).append(";")
                    .append(String.format(Locale.getDefault(), "%.1f", shot.getVelocity())).append(";")
                    .append(String.format(Locale.getDefault(), "%.2f", shot.getEnergy())).append("\n");
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Данные хронографа");
        shareIntent.putExtra(Intent.EXTRA_TEXT, csv.toString());
        startActivity(Intent.createChooser(shareIntent, "Экспорт данных"));

        Toast.makeText(this,
                "Экспортировано " + shotList.size() + " записей",
                Toast.LENGTH_SHORT).show();
    }

    private void clearHistory() {
        if (shotList.isEmpty()) {
            Toast.makeText(this, "История уже пуста", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Очистка истории")
                .setMessage("Вы уверены, что хотите очистить всю историю выстрелов?")
                .setPositiveButton("Очистить", (dialog, which) -> {
                    adapter.clearData();
                    updateStatistics();
                    checkEmptyState();
                    Toast.makeText(HistoryActivity.this, "История очищена", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}