package com.example.myapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.UUID;

public class BlueControl extends AppCompatActivity {
    ImageButton btnTb1, btnTb2, btnDis;
    TextView txt1, txtMAC;

    String address = null;
    private String mode = "SIMULATION";

    private ProgressDialog progress;
    private BluetoothAdapter myBluetooth = null;
    private BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    int flaglamp1 = 0;
    int flaglamp2 = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        address = intent.getStringExtra(MainActivity.EXTRA_ADDRESS);
        mode = intent.getStringExtra("MODE");

        setContentView(R.layout.activity_blue);

        btnTb1 = findViewById(R.id.btnTb1);
        btnTb2 = findViewById(R.id.btnTb2);
        txt1 = findViewById(R.id.textV1);
        txtMAC = findViewById(R.id.textViewMAC);
        btnDis = findViewById(R.id.btnDisc);

        txtMAC.setText("Address: " + address);

        if ("REAL".equals(mode)) {
            new ConnectBT().execute();
        } else {
            msg("Chế độ mô phỏng");
            initializeSimulationState();
        }

        btnTb1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("REAL".equals(mode)) {
                    sendControlSignal("1", "A");
                } else {
                    toggleLamp1Simulation();
                }
            }
        });

        btnTb2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("REAL".equals(mode)) {
                    sendControlSignal("7", "G");
                } else {
                    toggleLamp2Simulation();
                }
            }
        });

        btnDis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("REAL".equals(mode)) {
                    disconnect();
                } else {
                    finish();
                }
            }
        });
    }

    private void initializeSimulationState() {
        btnTb1.setBackgroundResource(R.drawable.tbloff);
        btnTb2.setBackgroundResource(R.drawable.tb2off);
        txt1.setText("Thiết bị đang tắt (Mô phỏng)");
    }

    private void toggleLamp1Simulation() {
        if (flaglamp1 == 0) {
            flaglamp1 = 1;
            btnTb1.setBackgroundResource(R.drawable.tblon);
            txt1.setText("Mô phỏng: Thiết bị 1 Bật");
        } else {
            flaglamp1 = 0;
            btnTb1.setBackgroundResource(R.drawable.tbloff);
            txt1.setText("Mô phỏng: Thiết bị 1 Tắt");
        }
    }

    private void toggleLamp2Simulation() {
        if (flaglamp2 == 0) {
            flaglamp2 = 1;
            btnTb2.setBackgroundResource(R.drawable.tb2on);
            txt1.setText("Mô phỏng: Thiết bị 2 Bật");
        } else {
            flaglamp2 = 0;
            btnTb2.setBackgroundResource(R.drawable.tb2off);
            txt1.setText("Mô phỏng: Thiết bị 2 Tắt");
        }
    }

    private void sendControlSignal(String onSignal, String offSignal) {
        if (btSocket != null && isBtConnected) {
            try {

                if (("1".equals(onSignal) && flaglamp1 == 0) || ("7".equals(onSignal) && flaglamp2 == 0) ) {
                     btSocket.getOutputStream().write(onSignal.getBytes());
                     if("1".equals(onSignal)) flaglamp1 = 1;
                     if("7".equals(onSignal)) flaglamp2 = 1;

                } else {
                    btSocket.getOutputStream().write(offSignal.getBytes());
                    if("A".equals(offSignal)) flaglamp1 = 0;
                    if("G".equals(offSignal)) flaglamp2 = 0;
                }
            } catch (IOException e) {
                msg("Lỗi gửi tín hiệu");
            }
        }
    }

    private void disconnect() {
        if (btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException e) {
                msg("Lỗi ngắt kết nối");
            }
        }
        finish();
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean connectSuccess = true;

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(BlueControl.this, "Đang kết nối...", "Vui lòng đợi.");
        }

        @Override
        protected Void doInBackground(Void... devices) {
            try {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    if (ActivityCompat.checkSelfPermission(BlueControl.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return null;
                    }
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            } catch (IOException e) {
                connectSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (!connectSuccess) {
                msg("Kết nối thất bại.");
                finish();
            } else {
                msg("Đã kết nối.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }
}
