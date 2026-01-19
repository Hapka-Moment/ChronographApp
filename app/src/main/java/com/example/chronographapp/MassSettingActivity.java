package com.example.chronographapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;

public class MassSettingActivity extends AppCompatActivity {

    private int[] digits = new int[4];
    private TextView[] digitViews = new TextView[4];
    private TextView finalMassText;
    private TextView currentMassText;
    private int selectedDigit = 0;
    private float currentMass = 0.25f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mass_setting);

        setupToolbar();
        getIntentData();
        initViews();
        setupInitialValues();
        setupDigitButtons();
        setupDigitClickListeners();
        setupBackPressedHandler();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Настройка массы");
        }
    }

    private void getIntentData() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            currentMass = extras.getFloat("current_mass", 0.25f);
        }
    }

    private void initViews() {
        digitViews[0] = findViewById(R.id.massDigit1);
        digitViews[1] = findViewById(R.id.massDigit2);
        digitViews[2] = findViewById(R.id.massDigit3);
        digitViews[3] = findViewById(R.id.massDigit4);
        finalMassText = findViewById(R.id.finalMassText);
        currentMassText = findViewById(R.id.currentMassText);
    }

    private void setupInitialValues() {
        parseMassToDigits(currentMass);
        updateCurrentMassDisplay();
        updateDigitDisplay();
        setSelectedDigit(0);
    }

    private void parseMassToDigits(float mass) {
        int massInt = Math.round(mass * 100);
        digits[0] = (massInt / 1000) % 10;
        digits[1] = (massInt / 100) % 10;
        digits[2] = (massInt / 10) % 10;
        digits[3] = massInt % 10;

        if (mass < 10.0f) {
            digits[0] = 0;
        }
        if (mass < 1.0f) {
            digits[0] = 0;
            digits[1] = 0;
        }
        if (mass < 0.01f) {
            digits[0] = 0;
            digits[1] = 0;
            digits[2] = 0;
            digits[3] = 1;
        }
    }

    private void updateDigitDisplay() {
        for (int i = 0; i < digits.length; i++) {
            digitViews[i].setText(String.valueOf(digits[i]));
        }

        float mass = calculateMassFromDigits();
        finalMassText.setText(String.format("Масса: %.2f грамм", mass));
    }

    private float calculateMassFromDigits() {
        return digits[0] * 10 + digits[1] + digits[2] * 0.1f + digits[3] * 0.01f;
    }

    private void setupDigitButtons() {
        setupDigitButton(0, R.id.massDigit1Up, R.id.massDigit1Down);
        setupDigitButton(1, R.id.massDigit2Up, R.id.massDigit2Down);
        setupDigitButton(2, R.id.massDigit3Up, R.id.massDigit3Down);
        setupDigitButton(3, R.id.massDigit4Up, R.id.massDigit4Down);
    }

    private void setupDigitButton(final int digitIndex, int upButtonId, int downButtonId) {
        MaterialButton upButton = findViewById(upButtonId);
        if (upButton != null) {
            upButton.setOnClickListener(v -> {
                increaseDigit(digitIndex);
                updateDigitDisplay();
            });
        }

        MaterialButton downButton = findViewById(downButtonId);
        if (downButton != null) {
            downButton.setOnClickListener(v -> {
                decreaseDigit(digitIndex);
                updateDigitDisplay();
            });
        }
    }

    private void setupDigitClickListeners() {
        for (int i = 0; i < digitViews.length; i++) {
            final int digitIndex = i;
            digitViews[i].setOnClickListener(v -> setSelectedDigit(digitIndex));
        }
    }

    private void increaseDigit(int digitIndex) {
        digits[digitIndex]++;

        if (digits[digitIndex] > 9) {
            digits[digitIndex] = 0;
            if (digitIndex > 0) {
                increaseDigit(digitIndex - 1);
            }
        }

        float newMass = calculateMassFromDigits();
        if (newMass < 0.01f) {
            digits[0] = 0;
            digits[1] = 0;
            digits[2] = 0;
            digits[3] = 1;
        }
        if (newMass > 99.99f) {
            digits[0] = 9;
            digits[1] = 9;
            digits[2] = 9;
            digits[3] = 9;
        }
    }

    private void decreaseDigit(int digitIndex) {
        digits[digitIndex]--;

        if (digits[digitIndex] < 0) {
            digits[digitIndex] = 9;
            if (digitIndex > 0) {
                decreaseDigit(digitIndex - 1);
            }
        }

        float newMass = calculateMassFromDigits();
        if (newMass < 0.01f) {
            digits[0] = 0;
            digits[1] = 0;
            digits[2] = 0;
            digits[3] = 1;
        }
    }

    private void setSelectedDigit(int digitIndex) {
        for (TextView digitView : digitViews) {
            digitView.setBackgroundResource(R.drawable.circle_background_blue);
        }

        digitViews[digitIndex].setBackgroundResource(R.drawable.circle_background_indicator);
        selectedDigit = digitIndex;

        showDigitHint(digitIndex);
    }

    private void showDigitHint(int digitIndex) {
        String[] hints = {
                "Десятки граммов (0-9)",
                "Единицы граммов (0-9)",
                "Десятые доли (0-9)",
                "Сотые доли (0-9)"
        };

        if (digitIndex >= 0 && digitIndex < hints.length) {
            Toast.makeText(this, hints[digitIndex], Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCurrentMassDisplay() {
        if (currentMassText != null) {
            currentMassText.setText(String.format("Текущая масса: %.2f грамм", currentMass));
        }
    }

    public void onCancelClick(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }

    public void onResetClick(View view) {
        parseMassToDigits(0.25f);
        updateDigitDisplay();
        setSelectedDigit(0);
        Toast.makeText(this, "Масса сброшена к 0.25г", Toast.LENGTH_SHORT).show();
    }

    public void onApplyClick(View view) {
        float newMass = calculateMassFromDigits();

        if (newMass < 0.01f || newMass > 99.99f) {
            Toast.makeText(this, "Масса должна быть от 0.01г до 99.99г", Toast.LENGTH_LONG).show();
            return;
        }

        currentMass = newMass;
        updateCurrentMassDisplay();

        Toast.makeText(this,
                String.format("Масса применена: %.2fг", newMass),
                Toast.LENGTH_SHORT).show();
    }

    public void onSaveClick(View view) {
        float newMass = calculateMassFromDigits();

        if (newMass < 0.01f) {
            Toast.makeText(this, "Масса должна быть не менее 0.01г", Toast.LENGTH_LONG).show();
            return;
        }

        if (newMass > 99.99f) {
            Toast.makeText(this, "Масса должна быть не более 99.99г", Toast.LENGTH_LONG).show();
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("new_mass", newMass);
        setResult(RESULT_OK, resultIntent);

        Toast.makeText(this,
                String.format("Масса установлена: %.2fг", newMass),
                Toast.LENGTH_SHORT).show();

        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            handleBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPressed();
            }
        });
    }

    private void handleBackPressed() {
        float newMass = calculateMassFromDigits();
        if (Math.abs(newMass - currentMass) > 0.001f) {
            showUnsavedChangesDialog();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void showUnsavedChangesDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Несохраненные изменения")
                .setMessage("У вас есть несохраненные изменения массы. Сохранить?")
                .setPositiveButton("Сохранить", (dialog, which) -> onSaveClick(null))
                .setNegativeButton("Не сохранять", (dialog, which) -> {
                    setResult(RESULT_CANCELED);
                    finish();
                })
                .setNeutralButton("Отмена", null)
                .show();
    }
}