package com.example.chronographapp;

import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        initViews();
        setupBluetooth();
        setupClickListeners();
        updateUI();
        updateConnectionStatus(false);
    }

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
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            updateConnectionStatus(false);
            if (connectionStatusText != null) {
                connectionStatusText.setText("Bluetooth не поддерживается");
            }
            if (connectionCard != null) {
                connectionCard.setEnabled(false);
            }
        } else if (!bluetoothAdapter.isEnabled()) {
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
    }

    private void setupClickListeners() {
        if (connectionCard != null) {
            connectionCard.setOnClickListener(v -> {
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

        // КНОПКА СБРОС - ИСПРАВЛЕНО!
        if (resetButton != null) {
            resetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetCounter();
                }
            });
        }
    }

    private void connectToBluetoothDevice() {
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
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(HC05_MAC_ADDRESS);
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();

                mainHandler.post(() -> {
                    updateConnectionStatus(true);
                    if (connectionCard != null) {
                        connectionCard.setEnabled(true);
                    }
                    Toast.makeText(MainActivity.this, "Подключено к HC-05", Toast.LENGTH_SHORT).show();
                });

                connectedThread = new ConnectedThread(bluetoothSocket);
                connectedThread.start();

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
            Toast.makeText(this,
                    String.format("Выстрел #%d: %.1f м/с", shotCount, velocity),
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void processReceivedData(String rawData) {
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

    // UI методы
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
                connectionStatusIcon.setColorFilter(getColor(R.color.status_connected));
                connectionStatusIcon.setImageResource(R.drawable.ic_bluetooth_connected);
            }
            if (connectionHintText != null) {
                connectionHintText.setText("ОТКЛЮЧИТЬ");
                connectionHintText.setTextColor(getColor(R.color.status_connected));
            }
        } else {
            if (connectionStatusText != null) {
                connectionStatusText.setText("Не подключено");
            }
            if (connectionStatusIcon != null) {
                connectionStatusIcon.setColorFilter(getColor(R.color.text_primary));
                connectionStatusIcon.setImageResource(R.drawable.ic_bluetooth);
            }
            if (connectionHintText != null) {
                connectionHintText.setText("ПОДКЛЮЧИТЬ");
                connectionHintText.setTextColor(getColor(R.color.glass_blue));
            }
        }
    }

    // Navigation
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

    // Menu
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("О программе")
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
    protected void onDestroy() {
        super.onDestroy();
        disconnectFromBluetoothDevice();
    }

    // Bluetooth Thread
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
                Log.e("Bluetooth", "Ошибка создания потоков", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes;

            while (true) {
                try {
                    numBytes = mmInStream.read(mmBuffer);
                    String receivedData = new String(mmBuffer, 0, numBytes);
                    mainHandler.post(() -> processReceivedData(receivedData));

                } catch (IOException e) {
                    Log.e("Bluetooth", "Поток чтения прерван", e);
                    mainHandler.post(() -> {
                        if (connectionStatusText != null) {
                            connectionStatusText.setText("Соединение разорвано");
                        }
                        if (connectionHintText != null) {
                            connectionHintText.setText("Нажмите для подключения");
                        }
                        updateConnectionStatus(false);
                        Toast.makeText(MainActivity.this,
                                "Соединение с HC-05 разорвано",
                                Toast.LENGTH_SHORT).show();
                    });
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                Log.d("Bluetooth", "Данные отправлены: " + new String(bytes));
            } catch (IOException e) {
                Log.e("Bluetooth", "Ошибка отправки данных", e);
            }
        }

        public void cancel() {
            try {
                if (mmInStream != null) mmInStream.close();
                if (mmOutStream != null) mmOutStream.close();
            } catch (IOException e) {
                Log.e("Bluetooth", "Ошибка закрытия потоков", e);
            }
        }
    }
}