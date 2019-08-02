package com.matra.aswitch;

import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.Switch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Set;
import java.util.UUID;
import android.os.Handler;
import java.util.logging.LogRecord;

public class MainActivity extends AppCompatActivity {

    protected static final String TAG = "TAG";
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket bluetoothSocket;
    BluetoothDevice bluetoothDevice;
    OutputStream outputStream;
    InputStream inputStream;
    Thread thread;

    byte [] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;

    TextView lblPrinterName;
    EditText textBox;

    private Switch btnSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnSwitch = (Switch)findViewById(R.id.btnSwitch);
        Button btnConnect = (Button) findViewById(R.id.btnConnect);
        Button btnDis = (Button) findViewById(R.id.btnDis);
        Button btnPrint = (Button) findViewById(R.id.btnPrint);
        textBox = (EditText) findViewById(R.id.txtText);
        lblPrinterName = (TextView) findViewById(R.id.lblPrinterName);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    FindBluetoothDevice();
                    openBluetoothPrinter();

                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        });
        /*btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View mView) {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter == null) {
                    Toast.makeText(MainActivity.this, "No Bluetooth Adapter", Toast.LENGTH_SHORT).show();
                } else {
                    if (!bluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(
                                BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent,
                                REQUEST_ENABLE_BT);
                    }
                    else {
                        ListPairedDevices();
                        Intent connectIntent = new Intent(MainActivity.this,
                                DeviceListActivity.class);
                        startActivityForResult(connectIntent,
                                REQUEST_CONNECT_DEVICE);
                    }
                }
            }
        });*/

        btnDis.setOnClickListener(new View.OnClickListener() {
            public void onClick(View mView) {
                Thread t = new Thread() {
                    public void run() {
                        try {
                            OutputStream os = bluetoothSocket.getOutputStream();
                            String BILL = "";

                            BILL = "       Measurement Result       \n\n";

                            BILL = BILL + "No.Seri       :"+" "+"BIEON-123\n";
                            BILL = BILL + "Operator      :"+" "+"Jon Thor\n";
                            BILL = BILL + "Tanggal       :"+" "+"04-07-2019\n";
                            BILL = BILL + "Sampel        :"+" "+"Refina\n\n";
                            BILL = BILL + "NaCl          :"+" "+"99.60"+"%\n";
                            BILL = BILL + "Whiteness     :"+" "+"98.90"+"%\n";
                            BILL = BILL + "Water Content :"+" "+"2.45"+"%\n\n";
                            BILL = BILL + "================================\n\n";

                            os.write(BILL.getBytes());

                        } catch (Exception e) {
                            Log.e("MainActivity", "Exe ", e);
                        }
                    }
                };
                t.start();
            }
        });

        btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    printData();
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        });
        //reference bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //check apakah bluetooth tersedia atau tidak
        if(bluetoothAdapter == null){
            Toast.makeText(this, "Device tidak suppor", Toast.LENGTH_SHORT).show();
        }
        else{
            //check jika bluetooth tersedia apakah dalam status hidup/mati saat aplikasi dibuka
            if(bluetoothAdapter.isEnabled()){
                btnSwitch.setChecked(true);
                Toast.makeText(this, "Bluetooth ON", Toast.LENGTH_SHORT).show();
            }else{
                btnSwitch.setChecked(false);
                Toast.makeText(this, "Bluetooth OFF", Toast.LENGTH_SHORT).show();
            }
        }
        //switch listener
        btnSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(btnSwitch.isChecked()){
                    //mengatur bluetooth ON
                    bluetoothAdapter.enable();
                    Toast.makeText(getApplicationContext(), "Bluetooth ON", Toast.LENGTH_SHORT).show();
                }else {
                    //mengatur blutooth off
                    try{
                        disconnectBT();
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                    bluetoothAdapter.disable();
                    Toast.makeText(getApplicationContext(), "Bluetooth OFF", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void ListPairedDevices() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice mDevice : pairedDevices) {
                Log.v(TAG, "PairedDevices: " + mDevice.getName() + "  "
                        + mDevice.getAddress());
            }
        }
    }

    void disconnectBT() throws IOException{
        try {
            stopWorker=true;
            outputStream.close();
            inputStream.close();
            bluetoothSocket.close();
            lblPrinterName.setText("Printer Disconnected.");
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    void printData() throws  IOException{
        try{
            String msg = textBox.getText().toString();
            msg+="\n";
            outputStream.write(msg.getBytes());
            lblPrinterName.setText("Printing Text...");
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    void openBluetoothPrinter() throws IOException{
        try{

            //Standard uuid from string //
            UUID uuidSting = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            bluetoothSocket=bluetoothDevice.createRfcommSocketToServiceRecord(uuidSting);
            bluetoothSocket.connect();
            outputStream=bluetoothSocket.getOutputStream();
            inputStream=bluetoothSocket.getInputStream();

            beginListenData();

        }catch (Exception ex){

        }
    }

    void beginListenData() {
        try{

            final Handler handler =new Handler();
            final byte delimiter=10;
            stopWorker =false;
            readBufferPosition=0;
            readBuffer = new byte[1024];

            thread=new Thread(new Runnable() {
                @Override
                public void run() {

                    while (!Thread.currentThread().isInterrupted() && !stopWorker){
                        try{
                            int byteAvailable = inputStream.available();
                            if(byteAvailable>0){
                                byte[] packetByte = new byte[byteAvailable];
                                inputStream.read(packetByte);

                                for(int i=0; i<byteAvailable; i++){
                                    byte b = packetByte[i];
                                    if(b==delimiter){
                                        byte[] encodedByte = new byte[readBufferPosition];
                                        System.arraycopy(
                                                readBuffer,0,
                                                encodedByte,0,
                                                encodedByte.length
                                        );
                                        final String data = new String(encodedByte,"US-ASCII");
                                        readBufferPosition=0;
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                lblPrinterName.setText(data);
                                            }
                                        });
                                    }else{
                                        readBuffer[readBufferPosition++]=b;
                                    }
                                }
                            }
                        }catch(Exception ex){
                            stopWorker=true;
                        }
                    }

                }
            });

            thread.start();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    void FindBluetoothDevice() {
        try{

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if(bluetoothAdapter==null){
                lblPrinterName.setText("No Bluetooth Adapter found");
            }
            if(bluetoothAdapter.isEnabled()){
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBT,0);
            }

            Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();

            if(pairedDevice.size()>0){
                for(BluetoothDevice pairedDev:pairedDevice){

                    // My Bluetoth printer name is BIEON-Printer
                    if(pairedDev.getName().equals("BIEON-Printer")){
                        bluetoothDevice=pairedDev;
                        lblPrinterName.setText("Bluetooth Printer Attached: "+pairedDev.getName());
                        Toast.makeText(getApplicationContext(), "BIEON Connected", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            }

            lblPrinterName.setText("Bluetooth Printer Attached");
            Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
}
