package com.example.mingyu.javaproject;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by melon on 2017-05-22.
 */

public class BTManager extends AppCompatActivity {
    static final int REQUEST_ENABLE_BT = 10;
    int mPariedDeviceCount = 0;
    Set<BluetoothDevice> mDevices;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice mRemoteDevice;
    BluetoothSocket mSocket = null;
    OutputStream mOutputStream = null;
    InputStream mInputStream = null;
    String mStrDelimiter = "\n";
    char mCharDelimiter =  '\n';



    Thread mWorkerThread = null;
    byte[] readBuffer;
    int readBufferPosition;


    EditText mEditReceive;
    Button mButtonReg;
    Button mButtonReservation;
    ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt);

        final DBManager dbManager = new DBManager(getApplicationContext(), "Bulb.db", null, 1);




        mImageView = (ImageView)findViewById(R.id.imageView);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendData("a");
            }
        });


        final AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("전등을 등록합니다.");
        alert.setMessage("이름");

        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String name = input.getText().toString();
                String address = mRemoteDevice.getAddress();
                Toast.makeText(getApplicationContext(), name + " " + address +" DB에 추가", Toast.LENGTH_LONG).show();
                try {
                    dbManager.insert("insert into BULB_LIST values(null, '" + name + "', '" + address + "');");
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "db Error", Toast.LENGTH_LONG).show();
                }
            }
        });
        alert.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });

        mButtonReg = (Button)findViewById(R.id.btn_register);

        mButtonReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alert.show();
            }
        });

        mButtonReservation = (Button)findViewById(R.id.btn_reservation);
        mButtonReservation.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(BTManager.this, ALActivity.class);
                i.putExtra("macadd",mRemoteDevice.getAddress());
                i.putExtra("macadd",mRemoteDevice.getAddress());
                startActivity(i);

            }
        });

        Intent i = getIntent();
        int con = i.getIntExtra("Connect",3);
        String macadd = i.getStringExtra("MacAddress");
        try {
            if (con == 0)
                checkBluetooth();
            else if (con == 1) {
                Toast.makeText(getApplicationContext(), macadd, Toast.LENGTH_LONG).show();
                connectToSelectedDeviceAdd(macadd);

            }
            else
                finish();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    BluetoothDevice getDeviceFromBondedListAdd(String macadd) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            mDevices = mBluetoothAdapter.getBondedDevices();
            BluetoothDevice selectedDevice = null;

            try {
                for (BluetoothDevice device : mDevices) {
                    if (macadd.equals(device.getAddress())) {
                        selectedDevice = device;
                        break;
                    }
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "getDeviceFromBondedList Error", Toast.LENGTH_LONG).show();
            }
            return selectedDevice;
    }

    void connectToSelectedDeviceAdd(String selectedDeviceAddress) {
            mRemoteDevice = getDeviceFromBondedListAdd(selectedDeviceAddress);
            UUID uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

            try {
                mSocket = mRemoteDevice.createRfcommSocketToServiceRecord(uuid);
                mSocket.connect();


                mOutputStream = mSocket.getOutputStream();

                beginListenForData();

            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "connectError", Toast.LENGTH_LONG).show();
                finish();
            }
    }


    BluetoothDevice getDeviceFromBondedList(String name) {
        BluetoothDevice selectedDevice = null;
        for(BluetoothDevice device : mDevices) {
            if(name.equals(device.getName())) {
                selectedDevice = device;
                break;
            }
        }
        return selectedDevice;
    }

    void sendData(String msg) {
        try{
            mOutputStream.write(msg.getBytes());  // 문자열 전송.
        }catch(Exception e) {
            Toast.makeText(getApplicationContext(), "데이터 전송중 오류가 발생", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    void connectToSelectedDevice(String selectedDeviceName) {
        mRemoteDevice = getDeviceFromBondedList(selectedDeviceName);
        UUID uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        try {

            mSocket = mRemoteDevice.createRfcommSocketToServiceRecord(uuid);
            mSocket.connect();


            mOutputStream = mSocket.getOutputStream();
            mInputStream = mSocket.getInputStream();

            sendData("Set");

        }catch(Exception e) {
            Toast.makeText(getApplicationContext(), "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    void beginListenForData() {
        final Handler handler = new Handler();

        readBufferPosition = 0;
        readBuffer = new byte[1024];

        // 문자열 수신 쓰레드.
        mWorkerThread = new Thread(new Runnable()
        {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        // InputStream.available() : 다른 스레드에서 blocking 하기 전까지 읽은 수 있는 문자열 개수를 반환함.
                        int byteAvailable = mInputStream.available();
                        if(byteAvailable > 0) {
                            byte[] packetBytes = new byte[byteAvailable];
                            // read(buf[]) : 입력스트림에서 buf[] 크기만큼 읽어서 저장 없을 경우에 -1 리턴.
                            mInputStream.read(packetBytes);
                            for(int i=0; i<byteAvailable; i++) {
                                byte b = packetBytes[i];
                                if(b == mCharDelimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    //  System.arraycopy(복사할 배열, 복사시작점, 복사된 배열, 붙이기 시작점, 복사할 개수)
                                    //  readBuffer 배열을 처음 부터 끝까지 encodedBytes 배열로 복사.
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);

                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable(){
                                        // 수신된 문자열 데이터에 대한 처리.
                                        @Override
                                        public void run() {
                                            // mStrDelimiter = '\n';
                                            mEditReceive.setText(mEditReceive.getText().toString() + data+ mStrDelimiter);
                                        }

                                    });
                                }
                                else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }

                    } catch (Exception e) {    // 데이터 수신 중 오류 발생.
                        Toast.makeText(getApplicationContext(), "데이터 수신 중 오류가 발생 했습니다.", Toast.LENGTH_LONG).show();
                        finish();            // App 종료.
                    }
                }
            }

        });

    }

    void selectDevice() {
        mDevices = mBluetoothAdapter.getBondedDevices();
        mPariedDeviceCount = mDevices.size();

        if(mPariedDeviceCount == 0 ) { // 페어링된 장치가 없는 경우.
            Toast.makeText(getApplicationContext(), "페어링된 장치가 없습니다.", Toast.LENGTH_LONG).show();
            finish(); // App 종료.
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("블루투스 장치 선택");

        List<String> listItems = new ArrayList<String>();
        for(BluetoothDevice device : mDevices) {
            listItems.add(device.getName());
        }
        listItems.add("취소");  // 취소 항목 추가.


        final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
        listItems.toArray(new CharSequence[listItems.size()]);

        builder.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {
                // TODO Auto-generated method stub
                if(item == mPariedDeviceCount) {
                    Toast.makeText(getApplicationContext(), "연결할 장치를 선택하지 않았습니다.", Toast.LENGTH_LONG).show();
                    finish();
                }
                else {
                    connectToSelectedDevice(items[item].toString());
                }
            }

        });

        builder.setCancelable(false);
        AlertDialog alert = builder.create();
        alert.show();
    }


    void checkBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null ) {  // 블루투스 미지원
            Toast.makeText(getApplicationContext(), "기기가 블루투스를 지원하지 않습니다.", Toast.LENGTH_LONG).show();
            finish();
        }
        else {
            if(!mBluetoothAdapter.isEnabled()) { // 블루투스 지원하며 비활성 상태인 경우.
                Toast.makeText(getApplicationContext(), "현재 블루투스가 비활성 상태입니다.", Toast.LENGTH_LONG).show();
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            else // 블루투스 지원하며 활성 상태인 경우.
                selectDevice();
        }
    }


    @Override
    protected void onDestroy() {
        try{
            mSocket.close();
        }catch(Exception e){}
        super.onDestroy();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK) {
                    selectDevice();
                }
                else if(resultCode == RESULT_CANCELED) {
                    Toast.makeText(getApplicationContext(), "블루투스를 사용할 수 없어 프로그램을 종료합니다", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        try {
            mSocket.close();
        }catch(Exception e){}
        super.onBackPressed();

    }

}
