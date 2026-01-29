package com.example.chronographapp;

import android.Manifest;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // UI элементы
    private TextView velocityText, energyText, rpmText, shotCountText, massText;
    private TextView connectionStatusText, deviceNameText, connectionHintText;
    private ImageView connectionStatusIcon;
    private Button historyButton, massButton, resetButton;
    private CardView connectionCard;

    // Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private ConnectedThread connectedThread;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String HC05_MAC_ADDRESS = "00:18:E4:34:EF:18";

    // Данные
    private int shotCount = 0;
    private float currentMass = 0.25f;
    private List<Float> velocityHistory = new ArrayList<>();
    private List<Float> energyHistory = new ArrayList<>();
    private StringBuilder dataBuffer = new StringBuilder();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // Разрешения
    private static final int PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        initViews();

        // Проверяем разрешения при запуске
        checkPermissionsOnStart();

        setupBluetooth();
        setupClickListeners();
        updateUI();
        updateConnectionStatus(false);
    }

    // ============ МЕТОДЫ ДЛЯ РАЗРЕШЕНИЙ ============

    private void checkPermissionsOnStart() {
        if (!hasBluetoothPermissions()) {
            showPermissionDialog();
        }
    }

    private boolean hasBluetoothPermissions() {
        // Для Android 12 и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean hasConnect = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            boolean hasScan = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            return hasConnect && hasScan;
        }
        // Для Android 6-11
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        // Для старых версий Android (до 6)
        return true;
    }

    private void showPermissionDialog() {
        String message;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            message = "Для работы с Bluetooth хронографом нужны разрешения на доступ к Bluetooth";
        } else {
            message = "Для поиска Bluetooth устройств нужно разрешение на доступ к местоположению";
        }

        new AlertDialog.Builder(this)
                .setTitle("Нужны разрешения")
                .setMessage(message)
                .setPositiveButton("Дать разрешения", (dialog, which) -> {
                    requestBluetoothPermissions();
                })
                .setNegativeButton("Позже", (dialog, which) -> {
                    Toast.makeText(MainActivity.this,
                            "Без разрешений Bluetooth не будет работать",
                            Toast.LENGTH_LONG).show();
                })
                .show();
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                    },
                    PERMISSION_REQUEST_CODE
            );
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-11
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    PERMISSION_REQUEST_CODE
            );
        }
        // Для Android 5 и ниже ничего не делаем
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, "Разрешения получены", Toast.LENGTH_SHORT).show();
                // Обновляем Bluetooth
                setupBluetooth();
            } else {
                Toast.makeText(this,
                        "Без разрешений Bluetooth не будет работать",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean checkPermissionsBeforeBluetooth() {
        if (!hasBluetoothPermissions()) {
            Toast.makeText(this,
                    "Сначала дайте разрешения для Bluetooth",
                    Toast.LENGTH_SHORT).show();
            showPermissionDialog();
            return false;
        }
        return true;
    }

    // ============ КОНЕЦ МЕТОДОВ РАЗРЕШЕНИЙ ============

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void initViews() {
        velocityText = findViewById(R.id.velocityText);
        energyText = findViewById(R.id.energyText);
        rpmText = findViewById(R.id.rpmText);
        shotCountText = findViewById(R.id.shotCountText);
        massText = findViewById(R.id.massText);
        connectionStatusText = findViewById(R.id.connectionStatusText);
        connectionStatusIcon = findViewById(R.id.connectionStatusIcon);
        deviceNameText = findViewById(R.id.deviceNameText);
        connectionHintText = findViewById(R.id.connectionHintText);
        connectionCard = findViewById(R.id.connectionCard);
        historyButton = findViewById(R.id.historyButton);
        massButton = findViewById(R.id.massButton);
        resetButton = findViewById(R.id.resetButton);
    }

    private void setupBluetooth() {
        // Проверяем разрешения перед настройкой Bluetooth
        if (!checkPermissionsBeforeBluetooth()) {
            return;
        }

        try {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (bluetoothAdapter == null) {
                updateConnectionStatus(false);
                if (connectionStatusText != null) {
                    connectionStatusText.setText("Bluetooth не поддерживается");
                }
                if (connectionCard != null) {
                    connectionCard.setEnabled(false);
                }
                return;
            }

            // Проверяем разрешение на доступ к состоянию Bluetooth
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    updateConnectionStatus(false);
                    if (connectionStatusText != null) {
                        connectionStatusText.setText("Нет разрешения Bluetooth");
                    }
                    return;
                }
            }

            if (!bluetoothAdapter.isEnabled()) {
                updateConnectionStatus(false);
                if (connectionStatusText != null) {
                    connectionStatusText.setText("Bluetooth выключен");
                }
            } else {
                updateConnectionStatus(false);
                if (connectionStatusText != null) {
                    connectionStatusText.setText("Не подключено");
                }
                if (connectionHintText != null) {
                    connectionHintText.setText("Нажмите для подключения");
                }
            }
        } catch (SecurityException e) {
            Log.e("Bluetooth", "SecurityException в setupBluetooth", e);
            updateConnectionStatus(false);
            if (connectionStatusText != null) {
                connectionStatusText.setText("Ошибка разрешений Bluetooth");
            }
        }
    }

    private void setupClickListeners() {
        if (connectionCard != null) {
            connectionCard.setOnClickListener(v -> {
                // Проверяем разрешения перед подключением
                if (!checkPermissionsBeforeBluetooth()) {
                    return;
                }

                if (connectedThread != null && bluetoothSocket != null && bluetoothSocket.isConnected()) {
                    disconnectFromBluetoothDevice();
                } else {
                    connectToBluetoothDevice();
                }
            });
        }

        if (historyButton != null) {
            historyButton.setOnClickListener(v -> openHistoryActivity());
        }

        if (massButton != null) {
            massButton.setOnClickListener(v -> openMassSettingsActivity());
        }

        if (resetButton != null) {
            resetButton.setOnClickListener(v -> resetCounter());
        }
    }

    private void connectToBluetoothDevice() {
        // Проверяем разрешения
        if (!checkPermissionsBeforeBluetooth()) {
            return;
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Включите Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        updateConnectionText("Подключение...");
        if (connectionCard != null) {
            connectionCard.setEnabled(false);
        }

        new Thread(() -> {
            try {
                // Проверяем разрешения в потоке
                MainActivity activity = MainActivity.this;
                if (!activity.hasBluetoothPermissions()) {
                    mainHandler.post(() -> {
                        Toast.makeText(activity,
                                "Нет разрешений для Bluetooth",
                                Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Получаем устройство с проверкой разрешений
                BluetoothDevice device;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Для Android 12+ проверяем разрешение BLUETOOTH_CONNECT
                    if (ContextCompat.checkSelfPermission(activity,
                            Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        mainHandler.post(() -> {
                            Toast.makeText(activity,
                                    "Нет разрешения на подключение Bluetooth",
                                    Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                    device = bluetoothAdapter.getRemoteDevice(HC05_MAC_ADDRESS);
                } else {
                    device = bluetoothAdapter.getRemoteDevice(HC05_MAC_ADDRESS);
                }

                // Создаем и подключаем сокет
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();

                mainHandler.post(() -> {
                    updateConnectionStatus(true);
                    if (connectionCard != null) {
                        connectionCard.setEnabled(true);
                    }
                    Toast.makeText(MainActivity.this, "Подключено к HC-05", Toast.LENGTH_SHORT).show();
                });

                connectedThread = new ConnectedThread(bluetoothSocket, this);
                connectedThread.start();

            } catch (SecurityException e) {
                mainHandler.post(() -> {
                    updateConnectionStatus(false);
                    if (connectionCard != null) {
                        connectionCard.setEnabled(true);
                    }
                    Toast.makeText(MainActivity.this,
                            "Ошибка безопасности: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e("Bluetooth", "SecurityException", e);
                });
            } catch (IOException e) {
                mainHandler.post(() -> {
                    updateConnectionStatus(false);
                    if (connectionCard != null) {
                        connectionCard.setEnabled(true);
                    }
                    Toast.makeText(MainActivity.this,
                            "Не удалось подключиться: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e("Bluetooth", "Ошибка подключения", e);
                });
            } catch (IllegalArgumentException e) {
                mainHandler.post(() -> {
                    updateConnectionStatus(false);
                    if (connectionCard != null) {
                        connectionCard.setEnabled(true);
                    }
                    Toast.makeText(MainActivity.this,
                            "Проверьте MAC-адрес HC-05",
                            Toast.LENGTH_LONG).show();
                    Log.e("Bluetooth", "Неверный MAC-адрес", e);
                });
            }
        }).start();
    }

    private void disconnectFromBluetoothDevice() {
        try {
            if (connectedThread != null) {
                connectedThread.cancel();
                connectedThread = null;
            }

            if (bluetoothSocket != null) {
                try {
                    bluetoothSocket.close();
                } catch (IOException e) {
                    Log.e("Bluetooth", "Ошибка закрытия сокета", e);
                }
                bluetoothSocket = null;
            }

            updateConnectionStatus(false);
            Toast.makeText(this, "Отключено от HC-05", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Log.e("Bluetooth", "SecurityException при отключении", e);
            Toast.makeText(this, "Ошибка при отключении: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void sendCommandToArduino(String command) {
        if (connectedThread != null && bluetoothSocket != null && bluetoothSocket.isConnected()) {
            connectedThread.write((command + "\n").getBytes());
            Log.d("Bluetooth", "Отправлена команда: " + command);
        } else {
            mainHandler.post(() -> {
                Toast.makeText(this, "Не подключено к Arduino", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void onNewShotData(float velocity, float energy) {
        shotCount++;
        velocityHistory.add(velocity);
        energyHistory.add(energy);

        mainHandler.post(() -> {
            updateShotData(velocity, energy);
            Toast.makeText(MainActivity.this,
                    String.format("Выстрел #%d: %.1f м/с", shotCount, velocity),
                    Toast.LENGTH_SHORT).show();
        });
    }

    public void processReceivedData(String rawData) {
        dataBuffer.append(rawData);
        String bufferContent = dataBuffer.toString();
        Log.d("Bluetooth", "Получено: " + bufferContent);

        if (bufferContent.contains("Shot #") &&
                bufferContent.contains("Speed: ") &&
                bufferContent.contains("Energy: ")) {

            try {
                int shotIndex = bufferContent.indexOf("Shot #") + 6;
                int shotEndLine = bufferContent.indexOf("\n", shotIndex);
                if (shotEndLine == -1) return;

                String shotStr = bufferContent.substring(shotIndex, shotEndLine).trim();
                int shotNumber = Integer.parseInt(shotStr);

                int speedIndex = bufferContent.indexOf("Speed: ") + 7;
                int speedEndLine = bufferContent.indexOf("\n", speedIndex);
                if (speedEndLine == -1) return;

                String speedStr = bufferContent.substring(speedIndex, speedEndLine).trim();
                float velocity = Float.parseFloat(speedStr);

                int energyIndex = bufferContent.indexOf("Energy: ") + 8;
                int energyEndLine = bufferContent.indexOf("\n", energyIndex);
                if (energyEndLine == -1) energyEndLine = bufferContent.length();

                String energyStr = bufferContent.substring(energyIndex, energyEndLine).trim();
                float energy = Float.parseFloat(energyStr);

                onNewShotData(velocity, energy);

            } catch (Exception e) {
                Log.e("Bluetooth", "Ошибка парсинга данных: " + bufferContent, e);
            } finally {
                dataBuffer.setLength(0);
            }
        }
    }

    private void updateUI() {
        if (massText != null) {
            massText.setText(String.format(Locale.getDefault(), "%.2f", currentMass));
        }

        if (velocityText != null) {
            velocityText.setText("---");
        }
        if (energyText != null) {
            energyText.setText("---");
        }
        if (rpmText != null) {
            rpmText.setText("---");
        }
        if (shotCountText != null) {
            shotCountText.setText("0");
        }
    }

    private void updateShotData(float velocity, float energy) {
        if (velocityText != null) {
            velocityText.setText(String.format(Locale.getDefault(), "%.1f", velocity));
        }
        if (energyText != null) {
            energyText.setText(String.format(Locale.getDefault(), "%.2f", energy));
        }
        if (shotCountText != null) {
            shotCountText.setText(String.valueOf(shotCount));
        }

        if (velocityHistory.size() >= 2) {
            float rpm = 20.0f;
            if (rpmText != null) {
                rpmText.setText(String.format(Locale.getDefault(), "%.0f", rpm));
            }
        }
    }

    private void resetCounter() {
        shotCount = 0;
        velocityHistory.clear();
        energyHistory.clear();
        updateUI();
        Toast.makeText(this, "Счетчик сброшен", Toast.LENGTH_SHORT).show();
    }

    private void updateConnectionText(String text) {
        if (connectionStatusText != null) {
            connectionStatusText.setText(text);
        }
        if (connectionHintText != null) {
            connectionHintText.setText(text);
        }
    }

    private void updateConnectionStatus(boolean connected) {
        if (connected) {
            if (connectionStatusText != null) {
                connectionStatusText.setText("Подключено к HC-05");
            }
            if (connectionStatusIcon != null) {
                connectionStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_connected));
                connectionStatusIcon.setImageResource(R.drawable.ic_bluetooth_connected);
            }
            if (connectionHintText != null) {
                connectionHintText.setText("ОТКЛЮЧИТЬ");
                connectionHintText.setTextColor(ContextCompat.getColor(this, R.color.status_connected));
            }
        } else {
            if (connectionStatusText != null) {
                connectionStatusText.setText("Не подключено");
            }
            if (connectionStatusIcon != null) {
                connectionStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_primary));
                connectionStatusIcon.setImageResource(R.drawable.ic_bluetooth);
            }
            if (connectionHintText != null) {
                connectionHintText.setText("ПОДКЛЮЧИТЬ");
                connectionHintText.setTextColor(ContextCompat.getColor(this, R.color.glass_blue));
            }
        }
    }

    private void openHistoryActivity() {
        Intent intent = new Intent(this, HistoryActivity.class);
        intent.putExtra("shot_count", shotCount);
        intent.putExtra("mass", currentMass);

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
        startActivityForResult(intent, 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_history) {
            openHistoryActivity();
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("О программе")
                .setMessage("Хронограф v1.0\n\n" +
                        "Разработано для дипломной работы\n" +
                        "Функции:\n" +
                        "• Измерение скорости выстрела\n" +
                        "• Расчет энергии\n" +
                        "• Счетчик выстрелов\n" +
                        "• Настройка массы снаряда\n" +
                        "• Bluetooth подключение к HC-05\n" +
                        "• История выстрелов")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            currentMass = data.getFloatExtra("new_mass", 0.25f);
            if (massText != null) {
                massText.setText(String.format(Locale.getDefault(), "%.2f", currentMass));
            }

            String massCommand = String.format(Locale.US, "MASS:%.2f", currentMass);
            sendCommandToArduino(massCommand);

            Toast.makeText(this, "Масса установлена: " + currentMass + "г", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("shotCount", shotCount);
        outState.putFloat("currentMass", currentMass);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        shotCount = savedInstanceState.getInt("shotCount", 0);
        currentMass = savedInstanceState.getFloat("currentMass", 0.25f);
        if (shotCountText != null) {
            shotCountText.setText(String.valueOf(shotCount));
        }
        if (massText != null) {
            massText.setText(String.format(Locale.getDefault(), "%.2f", currentMass));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectedThread != null) {
            connectedThread.cancel();
        }
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e("Bluetooth", "Ошибка закрытия сокета", e);
            }
        }
    }

    private static class ConnectedThread extends Thread {
        private final WeakReference<MainActivity> activityRef;
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private volatile boolean isRunning = true;

        public ConnectedThread(BluetoothSocket socket, MainActivity activity) {
            this.socket = socket;
            this.activityRef = new WeakReference<>(activity);

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("Bluetooth", "Ошибка создания потоков", e);
            }

            this.inputStream = tmpIn;
            this.outputStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int numBytes;

            while (isRunning) {
                try {
                    numBytes = inputStream.read(buffer);
                    if (numBytes > 0) {
                        final String receivedData = new String(buffer, 0, numBytes);

                        MainActivity activity = activityRef.get();
                        if (activity != null) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                activity.processReceivedData(receivedData);
                            });
                        }
                    }
                } catch (IOException e) {
                    if (isRunning) {
                        Log.e("Bluetooth", "Поток чтения прерван", e);
                        MainActivity activity = activityRef.get();
                        if (activity != null) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                activity.updateConnectionStatus(false);
                                Toast.makeText(activity,
                                        "Соединение разорвано",
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
                Log.d("Bluetooth", "Данные отправлены: " + new String(bytes));
            } catch (IOException e) {
                Log.e("Bluetooth", "Ошибка отправки данных", e);
            }
        }

        public void cancel() {
            isRunning = false;
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                Log.e("Bluetooth", "Ошибка закрытия потоков", e);
            }
        }
    }
}