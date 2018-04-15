package com.bdev.burrows.vehiclediagnostics;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Burrows on 2018-02-16.
 */

public class BTManager extends Application {
    private BluetoothAdapter bluetooth;
    private UUID uuid;
    private BluetoothSocket btSocket = null;
    private IntentFilter filtera;
    protected boolean isConnected, hasStream = false, arduinoStatus;
    private String address;
    public ConnectingThread connectingThread;
    private android.os.Handler handler;
    private Connection connection;

    //Name of the bluetooth module.
    private final String arduinoName = "ArduinoBT";


    @Override
    public void onCreate() {
        super.onCreate();
        bluetooth = BluetoothAdapter.getDefaultAdapter();
        uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        isConnected = false;

        connection = new Connection();
        arduinoStatus = false;

    }


    //Method to set the handler to the specific activity. So that is can change the ui based on BT data recievec
    public void setHandler(android.os.Handler h) {
        handler = h;
        if(connectingThread != null) {
            connectingThread.setHandler(handler);
        }
    }

    public void setArduinoStatus(boolean r){
        arduinoStatus = r;
    }

    //Returns true if there is an open stream from arduino to android device
    public boolean steamReady(){
        return hasStream;
    }

    //Write function is used in the UI activity in order to write to the ArduinoBT
    public void write(String s){
        if(connectingThread != null)
            connectingThread.write(s);
    }


    //Broadcast receiver to monitor bt connection
    private final BroadcastReceiver bluetoothState = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String stateExtra = BluetoothAdapter.EXTRA_STATE;
            int currentState = intent.getIntExtra(stateExtra, -1);
            switch(currentState){
                case(BluetoothAdapter.STATE_OFF):{
                    hasStream = false;
                    break;

                }

            }
        }
    };



    //Broadcast receiver to discover nearby devices and display them to the screen.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("LOG", action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName() == null ? "No name found" : device.getName();

                //The arduino is not yet connected and it is discovered in the area.
                if (deviceName.equals(arduinoName) && !isConnected) {

                    address = device.getAddress();
                    bluetooth.cancelDiscovery();

                    //Async Task that is capable of running in parallel to others
                     connection = new Connection();
                     connection.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                } else if(deviceName.equals(arduinoName)){
                    //The arduino is in range and is already paired.
                    //Try to open a communication stream.
                    address = device.getAddress();
                    isConnected = true;
                    bluetooth.cancelDiscovery();

                    try {
                        btSocket = device.createInsecureRfcommSocketToServiceRecord(uuid);
                        btSocket.connect();
                    } catch (IOException e){}
                    if(btSocket == null){
                        hasStream = false;
                    } else {
                        connectingThread = new ConnectingThread(btSocket, handler);
                        connectingThread.start();
                        unregisterReceiver(mReceiver);
                    }
                }
            }
        }

    };


    //Start discovery so that the Arduino can be found and paired with.
    public void pair(){
        if(bluetooth != null && bluetooth.isEnabled() && !hasStream) {
            connection.cancel(true);
            //Check if it is already paired. Can called method btPoweredOn instead
            pairedDevicesList();
            try {
                unregisterReceiver(mReceiver);
            }catch (Exception e){
                Log.d("LOG", e.toString());
            }

            try {
                unregisterReceiver(bluetoothState);
            }catch (Exception e){
                Log.d("LOG", e.toString());
            }

            //Scan the area to check if the device is in the area and pair if necessary
            bluetooth.startDiscovery();
            filtera = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filtera.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
            registerReceiver(mReceiver, filtera);

            String actionStateChanged = BluetoothAdapter.ACTION_STATE_CHANGED;
            IntentFilter filter = new IntentFilter(actionStateChanged);
            registerReceiver(bluetoothState, filter);
        }
    }


    //Check if the arduino is already paired.
    private void pairedDevicesList() {
        Set<BluetoothDevice> paired = bluetooth.getBondedDevices();

        if (paired.size() > 0) {
            for (BluetoothDevice bt : paired) {
                if(bt.getName().equals(arduinoName)){
                    isConnected = true;
                }
            }
        }

    }

    //Method to close the bluetooth thread and close all broadcasts.
    public void terminate(){
        connection.cancel(true);
        if(connectingThread != null && connectingThread.isAlive()){
            connectingThread.cancel();
        }
        connectingThread = null;
        isConnected = false;
        handler = null;

        try {
            unregisterReceiver(mReceiver);
        }catch (Exception e){
            Log.d("LOG", e.toString());
        }

        try {
            unregisterReceiver(bluetoothState);
        }catch (Exception e){
            Log.d("LOG", e.toString());
        }
    }

    //Asynchronous task to pair to the arduino.
    public class Connection extends AsyncTask<Void, Void, Void> {
        private boolean success = true;

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try {
                if (btSocket == null || !isConnected) {
                    Log.d("LOG", "socket null or not connected");
                    BluetoothDevice divice = bluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = divice.createInsecureRfcommSocketToServiceRecord(uuid);//create a RFCOMM (SPP) connection
                    bluetooth.cancelDiscovery();
                    btSocket.connect();//start connection
                }
            } catch (IOException e) {
                //Failed to pair with the device
                success = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!success) {
                isConnected = false;
            } else {
                //Toast.makeText(getApplicationContext(), "Connected.", Toast.LENGTH_SHORT).show();
                isConnected = true;

                //Not tested
                connectingThread = new ConnectingThread(btSocket,handler);
                connectingThread.start();

            }
        }
    }

    // Connection thread is used to listen to/send messages to/from the arduino device
    public final class ConnectingThread extends Thread {
        private final InputStream arduinoInStream;
        private final OutputStream arduinoOutStream;
        private final int RECEIVE_MESSAGE = 1;
        private final int NO_BT = 2;
        private android.os.Handler handler;
        public BluetoothSocket socket;

        public ConnectingThread(BluetoothSocket sock, android.os.Handler h ) {
            socket = sock;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            handler = h;
            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                hasStream = true;
            } catch (IOException e) {
                Log.d("LOG", "Stream not opened");
            }

            arduinoInStream = tmpIn;
            arduinoOutStream = tmpOut;
        }


        //Run command listens for incoming data
        public void run() {
            byte[] buffer = new byte[256];
            int bytes; // bytes returned from read()

            // List to the input stream until exception occurs
                while (true) {
                    try {
                        // Read from the InputStream
                        bytes = arduinoInStream.read(buffer);
                        handler.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();// Send to message queue Handler

                        //Create a new byte array over the old one to ensure no errors.
                        buffer = new byte[256];

                    } catch (IOException e) {
                        handler.obtainMessage(NO_BT).sendToTarget();
                        hasStream = false;
                        break;
                    }
                }

        }


        // Call this to send data to arduino
        public void write(String message) {
            byte[] msgBuffer = message.getBytes();
            try {
                arduinoOutStream.write(msgBuffer);
                arduinoOutStream.flush();
            } catch (IOException e) {
                Log.d("LOG", "...Error data send: " + e.getMessage() + "...");
                hasStream = false;

            }
        }

        public void cancel(){
            try {
                socket.close();
            } catch (IOException e){}
        }

        //Method so we can change the handler based on the current activity
        public void setHandler(android.os.Handler han){
            handler = han;
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

}
