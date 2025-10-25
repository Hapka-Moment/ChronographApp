package com.example.chronographapp;

import android.bluetooth.BluetoothSocket;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // UI элементы
    private TextView velocityText, energyText, rpmText, shotCountText, massText;
    private TextView connectionStatusText, connectionStatusToolbar;
    private Button connectButton, historyButton, settingsButton, massButton, resetButton;
    private FloatingActionButton fab;

    // Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private ConnectedThread connectedThread;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Данные
    private int shotCount = 0;
    private float currentMass = 0.25f;
    private List<Float> velocityHistory = new ArrayList<>();
    private List<Float> energyHistory = new ArrayList<>();

    // Handler для обновления UI из фоновых потоков
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация Toolbar
        setupToolbar();

        // Инициализация UI элементов
        initViews();

        // Настройка Bluetooth
        setupBluetooth();

        // Настройка обработчиков кликов
        setupClickListeners();

        // Показываем начальные значения
        updateUI();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Находим статус подключения в Toolbar
        connectionStatusToolbar = findViewById(R.id.connectionStatus);
        updateConnectionStatus(false);
    }

    private void initViews() {
        // Находим все View элементы
        velocityText = findViewById(R.id.velocityText);
        energyText = findViewById(R.id.energyText);
        rpmText = findViewById(R.id.rpmText);
        shotCountText = findViewById(R.id.shotCountText);
        massText = findViewById(R.id.massText);
        connectionStatusText = findViewById(R.id.connectionStatusText);

        connectButton = findViewById(R.id.connectButton);
        historyButton = findViewById(R.id.historyButton);
        settingsButton = findViewById(R.id.settingsButton);
        massButton = findViewById(R.id.massButton);
        resetButton = findViewById(R.id.resetButton);

        // Настройка FAB
        try {
            fab = findViewById(R.id.fab);
        } catch (Exception e) {
            // FAB не найден в layout - это нормально
            fab = null;
        }

    }

    private void setupBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Устройство не поддерживает Bluetooth
            connectionStatusText.setText("Bluetooth не поддерживается");
            connectButton.setEnabled(false);
            updateConnectionStatus(false);
        } else if (!bluetoothAdapter.isEnabled()) {
            // Bluetooth выключен
            connectionStatusText.setText("Bluetooth выключен");
            updateConnectionStatus(false);
        } else {
            // Bluetooth доступен
            connectionStatusText.setText("Готов к подключению");
            updateConnectionStatus(false);
        }
    }

    private void setupClickListeners() {
        // Кнопка подключения Bluetooth
        connectButton.setOnClickListener(v -> connectToBluetoothDevice());

        // Кнопка истории
        historyButton.setOnClickListener(v -> openHistoryActivity());

        // Кнопка настроек
        settingsButton.setOnClickListener(v -> {
            Toast.makeText(this, "Настройки в разработке", Toast.LENGTH_SHORT).show();
        });

        // Кнопка массы
        massButton.setOnClickListener(v -> openMassSettingsActivity());

        // Кнопка сброса
        resetButton.setOnClickListener(v -> resetCounter());

        // FAB кнопка
        if (fab != null) {
            fab.setOnClickListener(v -> openMassSettingsActivity());
        }
    }

    //region Bluetooth методы

    private void connectToBluetoothDevice() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Включите Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        // Пока просто эмулируем подключение для теста
        connectionStatusText.setText("Поиск устройств...");
        updateConnectionStatus(false);

        // Имитация подключения через 2 секунды
        mainHandler.postDelayed(() -> {
            connectionStatusText.setText("Подключено (тестовый режим)");
            connectButton.setText("Отключить");
            updateConnectionStatus(true);

            // Начинаем получать тестовые данные
            startTestDataStream();
        }, 2000);
    }

    private void startTestDataStream() {
        // Эмуляция получения данных от хронографа
        new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(3000); // Каждые 3 секунды

                    // Генерируем тестовые данные
                    float velocity = 150 + (float) (Math.random() * 50); // 150-200 м/с
                    float energy = (velocity * velocity * currentMass) / 2000; // E = (m*v^2)/2

                    // Обновляем UI в главном потоке
                    mainHandler.post(() -> {
                        onNewShotData(velocity, energy);
                    });
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void onNewShotData(float velocity, float energy) {
        // Увеличиваем счетчик выстрелов
        shotCount++;

        // Сохраняем в историю
        velocityHistory.add(velocity);
        energyHistory.add(energy);

        // Обновляем UI
        updateShotData(velocity, energy);

        // Показываем уведомление
        Toast.makeText(this,
                String.format("Выстрел #%d: %.1f м/с", shotCount, velocity),
                Toast.LENGTH_SHORT).show();
    }

    //endregion

    //region UI методы

    private void updateUI() {
        // Обновляем массу
        massText.setText(String.format("%.2f", currentMass));

        // Сбрасываем остальные поля
        velocityText.setText("---");
        energyText.setText("---");
        rpmText.setText("---");
        shotCountText.setText("0");
    }

    private void updateShotData(float velocity, float energy) {
        velocityText.setText(String.format("%.1f", velocity));
        energyText.setText(String.format("%.2f", energy));
        shotCountText.setText(String.valueOf(shotCount));

        // Расчет RPM (простой вариант - 1 выстрел в 3 секунды = 20 выстр/мин)
        float rpm = 20.0f;
        rpmText.setText(String.format("%.0f", rpm));
    }

    private void resetCounter() {
        shotCount = 0;
        velocityHistory.clear();
        energyHistory.clear();
        updateUI();
        Toast.makeText(this, "Счетчик сброшен", Toast.LENGTH_SHORT).show();
    }

    private void updateConnectionStatus(boolean connected) {
        if (connectionStatusToolbar != null) {
            if (connected) {
                connectionStatusToolbar.setText("●");
                connectionStatusToolbar.setTextColor(Color.GREEN);
                connectionStatusToolbar.setContentDescription("Подключено");
            } else {
                connectionStatusToolbar.setText("●");
                connectionStatusToolbar.setTextColor(Color.RED);
                connectionStatusToolbar.setContentDescription("Не подключено");
            }
        }
    }

    //endregion

    //region Navigation методы

    private void openHistoryActivity() {
        Intent intent = new Intent(this, HistoryActivity.class);

        // Передаем данные в HistoryActivity
        intent.putExtra("shot_count", shotCount);
        intent.putExtra("mass", currentMass);

        // Преобразуем списки в массивы для передачи
        float[] velocityArray = new float[velocityHistory.size()];
        float[] energyArray = new float[energyHistory.size()];
        for (int i = 0; i < velocityHistory.size(); i++) {
            velocityArray[i] = velocityHistory.get(i);
            energyArray[i] = energyHistory.get(i);
        }

        intent.putExtra("velocity_history", velocityArray);
        intent.putExtra("energy_history", energyArray);

        startActivity(intent);
    }

    private void openMassSettingsActivity() {
        Intent intent = new Intent(this, MassSettingActivity.class);
        intent.putExtra("current_mass", currentMass);
        startActivityForResult(intent, 1); // 1 - requestCode для onActivityResult
    }

    //endregion

    //region Menu методы

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Надуваем меню из XML
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_history) {
            openHistoryActivity();
            return true;
        } else if (id == R.id.action_mass) {
            openMassSettingsActivity();
            return true;
        } else if (id == R.id.action_connect) {
            connectToBluetoothDevice();
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("О программе")
                .setMessage("Хронограф v1.0\n\n" +
                        "Разработано для дипломной работы\n" +
                        "Функции:\n" +
                        "• Измерение скорости выстрела\n" +
                        "• Расчет энергии\n" +
                        "• Счетчик выстрелов\n" +
                        "• Настройка массы снаряда\n" +
                        "• Bluetooth подключение\n" +
                        "• История выстрелов")
                .setPositiveButton("OK", null)
                .show();
    }

    //endregion

    //region Activity методы

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            // Получаем новую массу из MassSettingActivity
            currentMass = data.getFloatExtra("new_mass", 0.25f);
            massText.setText(String.format("%.2f", currentMass));
            Toast.makeText(this, "Масса установлена: " + currentMass + "г", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Закрываем Bluetooth соединение при выходе
        if (connectedThread != null) {
            connectedThread.cancel();
        }
    }

    //endregion

    //region Bluetooth Thread класс

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes;

            while (true) {
                try {
                    // Читаем данные из Bluetooth
                    numBytes = mmInStream.read(mmBuffer);
                    String receivedData = new String(mmBuffer, 0, numBytes);

                    // Обрабатываем полученные данные в главном потоке
                    mainHandler.post(() -> processReceivedData(receivedData));

                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processReceivedData(String data) {
        // Здесь будет парсинг данных от Arduino
        // Пока просто выводим в лог
        System.out.println("Received: " + data);

        // Пример парсинга данных от хронографа
        if (data.contains("Speed: ")) {
            try {
                String speedStr = data.replace("Speed: ", "").trim();
                float velocity = Float.parseFloat(speedStr);
                float energy = (velocity * velocity * currentMass) / 2000;
                onNewShotData(velocity, energy);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    //endregion
}