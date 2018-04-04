package com.bdev.burrows.vehiclediagnostics;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainMenu extends AppCompatActivity {
    private final int MY_PERMISSIONS_REQUEST_LOCATION = 1;
    private AlertDialog noBTDialog;
    private static TextView btText;
    private static ImageView btImage;
    public static boolean tryingToConnect = true;
    public static GettingConnection gettingConnection;
    private BluetoothAdapter bt;
    public static final int NO_BT = 2;
    public static Context context;
    private static MainReceived handler;
    protected static boolean arduinoReady;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main_menu);

        //Set up variables for handler and bluetooth
        gettingConnection = new GettingConnection();
        context = this.getApplicationContext();
        handler = new MainReceived();
        ((BTManager) this.getApplicationContext()).setHandler(handler);
        arduinoReady = ((BTManager) context.getApplicationContext()).arduinoStatus;
        bt = BluetoothAdapter.getDefaultAdapter();

        //Setting the variables for the UI
        btText = (TextView) findViewById(R.id.bttext);
        btText.setText("Bluetooth");
        btImage = (ImageButton) findViewById(R.id.btImage);
        btImage.setClickable(false);
        btText.setClickable(false);

        //Request permissions before attempting to pair.
        checkLocationPermission();

        //Dialog to close application because the device does not support bluetooth
        noBTDialog = new AlertDialog.Builder(MainMenu.this).create();
        noBTDialog.setTitle("Bluetooth not Supported");
        noBTDialog.setMessage("Unfortunately this device does not support bluetooth.");
        noBTDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });

        //Check if device supports Bluetooth, then if bluetooth is on and final check if a stream is open or not.
        if (bt == null) {
            noBTDialog.show();
        } else if(!bt.isEnabled()){
            String actionRequestEnable = BluetoothAdapter.ACTION_REQUEST_ENABLE;
            startActivityForResult(new Intent(actionRequestEnable), 0);
        } else if(!hasStream()){
            gettingConnection = new GettingConnection();
            gettingConnection.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else{
            write("m");
            tryingToConnect = false;
            btText.setText("Connected");
            btImage.setImageResource(R.drawable.bluetooth_light);
        }


    }

    //Check the result of the request to turn bluetooth on
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 0) {
            // Make sure the request was successful
            if (resultCode !=  RESULT_OK) {
                noBTDialog.setMessage("Unfortunately this application will not function without Bluetooth.");
                noBTDialog.show();
            } else {
                gettingConnection = new GettingConnection();
                gettingConnection.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            }
        }
    }

 //Methods to request user permission to use course location. This is required to use Bluetooth
 protected void checkLocationPermission() {
     if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
             != PackageManager.PERMISSION_GRANTED) {

         ActivityCompat.requestPermissions(this,
                 new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
     }
 }

 //Check the result of the request for location permissions
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Log.d("LOG", "Permission Denied");
                    noBTDialog.show();
                }
                break;
            }
        }
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////


//Onclick method to take the user to the DTC page
    public void diagnostics(View v){
        //Only allows user to navigate to next page if there is an open stream and the arduino is ready.
        if(!tryingToConnect) {
            if (hasStream() && arduinoReady) {
                Intent diagnostics = new Intent(MainMenu.this, Diagnostics.class);
                startActivity(diagnostics);
            } else {
                if(hasStream() && !arduinoReady){
                    Toast.makeText(getApplicationContext(), "The Arduino cannot find your vehicle.\nRestart the Arduino to try again.", Toast.LENGTH_SHORT).show();
                    write("m");
                } else {
                    Toast.makeText(getApplicationContext(), "Please ensure that the Arduino is in range and powered on.", Toast.LENGTH_SHORT).show();
                    tryingToConnect = true;
                    gettingConnection = new GettingConnection();
                    gettingConnection.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth is connecting.\n Please wait.", Toast.LENGTH_SHORT).show();
        }
    }

    //Onclick method to take the user to the dashboard page.
    public void dashboard(View v){
        //Only allows user to navigate to next page if there is an open stream and the arduino is ready.
        if(!tryingToConnect ) {
            if (hasStream() && arduinoReady) {
                Intent dash = new Intent(MainMenu.this, Dashboard.class);
                startActivity(dash);
            } else {
                if(hasStream() && !arduinoReady){
                    Toast.makeText(getApplicationContext(), "The Arduino cannot find your vehicle.\nRestart the Arduino to try again.", Toast.LENGTH_SHORT).show();
                    write("m");
                } else {
                    Toast.makeText(getApplicationContext(), "Please ensure that the Arduino is in range and powered on.", Toast.LENGTH_SHORT).show();
                    tryingToConnect = true;
                    gettingConnection = new GettingConnection();
                    gettingConnection.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        }else {
            Toast.makeText(getApplicationContext(), "Bluetooth is connecting.\n Please wait.", Toast.LENGTH_SHORT).show();
        }
    }

    //Onclick method for the user to close the application
    public void exitApp(View v){
        gettingConnection.cancel(true);
        ((BTManager)this.getApplicationContext()).terminate();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
        finish();
    }

    //Method to check if there is a communication thread open.
    public static boolean hasStream(){
        return ((BTManager) context.getApplicationContext()).steamReady();
    }

    //Method to connect bluetooth if not connected
    public static void connect(){
        ((BTManager) context.getApplicationContext()).pair();
    }

    //Method to write messages to the arduino
    public static void write(String msg){
        if(arduinoReady) {
            ((BTManager) context.getApplicationContext()).write(msg + 3);
        }else {
            ((BTManager) context.getApplicationContext()).write(msg + 3);
        }
    }


    //Connect to the arduino and update the UI accordingly.///////////////////////////////////////////////
    public static int counter = 0;
    public static class GettingConnection extends AsyncTask<Void, Void, Void> {
        //Set the UI before connecting
        @Override
        protected void onPreExecute() {
            btText.setText("Connecting   ");
            btText.setClickable(false);
            btImage.setClickable(false);
            connect();
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            //If the connection is not successful then allow the user to try again
            Log.d("LOG", hasStream() + " stream");
            if(!hasStream()){
                tryingToConnect = false;
                btText.setText("Click to Connect");
                btText.setClickable(true);
                btImage.setImageResource(R.drawable.bluetooth_ligh_white);
                btImage.setClickable(true);
                counter = 0;
                btImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        tryingToConnect = true;
                        gettingConnection = new GettingConnection();
                        gettingConnection.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                });
                //Connection was successful - display to user.
            } else {
                write("m");
                tryingToConnect = false;
                btText.setText("Connected");
                btImage.setImageResource(R.drawable.bluetooth_light);
            }
        }

        //Change the UI to show the user that a connection is being established
        @Override
        protected void onProgressUpdate(Void... values) {
            switch (counter % 3){
                case 0:
                    btText.setText("Connecting.  ");
                    btImage.setImageResource(R.drawable.bluetooth_light);
                    break;
                case 1:
                    btText.setText("Connecting.. ");
                    btImage.setImageResource(R.drawable.bluetooth_ligh_white);
                    break;
                case 2:
                    btText.setText("Connecting...");
                    btImage.setImageResource(R.drawable.bluetooth_light);
                    break;
                default:
                    btText.setText("Connecting...");
                    btImage.setImageResource(R.drawable.bluetooth_ligh_white);
                    break;
            }

        }

        //Try to connect to the arduino for 12 seconds or until a connection is established
        @Override
        protected Void doInBackground(Void... voids) {
            while((!hasStream()) && counter < 24  ){
                try {
                    Thread.sleep(500);
                    counter++;
                    publishProgress();
                } catch (Exception e){ Log.d("LOG", e.getMessage());}
            }
            return null;
        }
    }

    //Overriding the back button so that all threads and async tasks are closed.
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handler = null;
        gettingConnection.cancel(true);
        ((BTManager)this.getApplicationContext()).terminate();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler = new MainReceived();
    }

    //Handler for the main menu
    public static class MainReceived extends Handler {
        private final int RECEIVE_MESSAGE = 1;
        private static StringBuilder sb = new StringBuilder();

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                //RECEIVE_MESSAGE for the main menu only checks for the ready signal from the arduino
                case RECEIVE_MESSAGE:
                    byte[] readBuf = (byte[]) msg.obj;
                    String strIncom = new String(readBuf, 0, msg.arg1);           // create string from bytes array
                    sb.append(strIncom);                                                // append string
                    int endOfLineIndex = sb.indexOf("\r\n");                            // determine the end-of-line
                    if (endOfLineIndex > 0) {                                            // if end-of-line,
                        String sbprint = sb.substring(0, endOfLineIndex).trim();
                        sb.delete(0, sb.length());

                        if(sbprint.trim().equals("ready")){
                            ((BTManager) context.getApplicationContext()).setArduinoStatus(true);
                            arduinoReady = true;
                            Log.d("LOG","Ready");
                        } else if(sbprint.trim().equals("nocom")){
                            arduinoReady = false;

                        }
                    }
                    break;
                //Lets the main page know that the connection has been lost with the arduino
                case NO_BT:
                    btImage.setImageResource(R.drawable.bluetooth_ligh_white);
                    btText.setText("Click to Connect");
                    btImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            tryingToConnect = true;
                            gettingConnection = new GettingConnection();
                            gettingConnection.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                    });

                    break;
            }
        }
    }
}
