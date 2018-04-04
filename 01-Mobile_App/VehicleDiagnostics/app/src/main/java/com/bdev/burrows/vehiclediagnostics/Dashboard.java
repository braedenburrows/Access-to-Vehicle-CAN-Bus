package com.bdev.burrows.vehiclediagnostics;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.anastr.speedviewlib.PointerSpeedometer;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.ArrayList;


public class Dashboard extends AppCompatActivity {
    public static PointerSpeedometer pointerSpeedometer;

    public static final int RECIEVE_MESSAGE = 1;
    public static final int NO_BT = 2;
    private static StringBuilder sb = new StringBuilder();
    private static Context context;
    private static  DashboardReceived handler;
    private static DecimalFormat df = new DecimalFormat("#.00");

    private static ArrayList<Integer> fuelValues;


    //Spinners to control user input for refresh rate and number of iterations before displaying data
    private Spinner refrestRate;
    private  Spinner iterations;
    private ArrayAdapter<CharSequence> iterationAdapter;
    private ArrayAdapter<CharSequence> refreshAdapter;
    private static TextView rpmTv, throttleTv, fuelTv, batteryTv;
    public static AppCompatActivity act;

    //Variables to hold the values for refresh rate and iteration numbers
    private static int iterationValue;
    private static int refreshTime;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_dashboard);

        //Activity and context variables to hold the current activity and context for the handler.
        act = Dashboard.this;
        context = getApplicationContext();

        //Set up the UI textviews elements
        rpmTv = (TextView) findViewById(R.id.rpm);
        throttleTv = (TextView) findViewById(R.id.throttle);
        fuelTv = (TextView) findViewById(R.id.fuel);
        batteryTv = (TextView) findViewById(R.id.battery);

        rpmTv.setText("0");
        throttleTv.setText("0%");
        fuelTv.setText("0%");
        batteryTv.setText("0");


        //Setting values for variables
        iterationValue = 10;
        refreshTime = 500;

        //Set up the arraylist to hold the fuel values sent from the arduino
        fuelValues = new ArrayList<>();

  //Set the speedometer settings. ////////////////////////////////////////////////////////////////////////////////
        pointerSpeedometer = (PointerSpeedometer) findViewById(R.id.speedom);
        pointerSpeedometer.setMinMaxSpeed(0, 180);
        pointerSpeedometer.setTickNumber(10);
        pointerSpeedometer.setTextSize(50);
        pointerSpeedometer.speedTo(0);
   ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


 //Setting up the spinner for refresh rate selection /////////////////////////////////////////////////////////////
        refrestRate = (Spinner) findViewById(R.id.refreshRates);
        refreshAdapter = ArrayAdapter.createFromResource(this,
                R.array.ratesArr, android.R.layout.simple_spinner_item);

        refreshAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        refrestRate.setAdapter(refreshAdapter);
        refrestRate.setSelection(2);
        refrestRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                refreshTime = Integer.parseInt(adapterView.getSelectedItem().toString());
                //Send to arduino
                write("d");
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//Setting up the spinner for number of iterations before display rate selection ///////////////////////////////////////////
        iterations = (Spinner) findViewById(R.id.iterations);
        iterationAdapter = ArrayAdapter.createFromResource(this,
                R.array.iterationsArr, android.R.layout.simple_spinner_item);

        iterationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        iterations.setAdapter(iterationAdapter);

        iterations.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                iterationValue = Integer.parseInt(adapterView.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        //Set the handler for incoming messages
        handler = new DashboardReceived();
        ((BTManager) this.getApplicationContext()).setHandler(handler);
    }

//On click method for the back button
    public void back(View v) {
        handler = null;
        Intent menu = new Intent(Dashboard.this, MainMenu.class);
        startActivity(menu);
        finish();
    }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Handler to handle incoming messages from the arduino
public static class DashboardReceived extends Handler {

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case RECIEVE_MESSAGE:
                // if receive massage
                byte[] readBuf = (byte[]) msg.obj;
                String strIncom = new String(readBuf, 0, msg.arg1);           // create string from bytes array
                sb.append(strIncom);                                                // append string
                int endOfLineIndex = sb.indexOf("\r\n");                            // determine the end-of-line
                if (endOfLineIndex > 0) {                                            // if end-of-line,
                    String sbprint = sb.substring(0, endOfLineIndex).trim();
                    sb.delete(0, sb.length());
                    String arr[] = sbprint.split(",");
                   if((arr.length >2)) {
                       setValues(arr);
                       Log.d("LOG", "Value: " + sbprint);

                   }
                }
                break;
            case NO_BT:
                ((BTManager) context.getApplicationContext()).arduinoStatus = false;
                AlertDialog noBTDialog = new AlertDialog.Builder(act).create();
                noBTDialog.setTitle("Bluetooth Error");
                noBTDialog.setMessage("Bluetooth stream has encountered an error.");
                noBTDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                Intent menu = new Intent(context, MainMenu.class);
                                act.startActivity(menu);
                                act.finish();

                            }
                        });


                noBTDialog.show();
                break;
        }
    }
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Method to write to arduino
    public static void write(String msg){
    ((BTManager) context).write(msg + getRefreshCode(refreshTime));
}

    public boolean hasStream(){
        return ((BTManager) this.getApplicationContext()).steamReady();
    }

//Method to convert user refresh to single int for arduino
    private static int getRefreshCode(int v){
        switch (v){
            case 100:
                return 0;
            case 250:
                return 1;
            case 500:
                return 2;
            case 750:
                return 3;
            case 1000:
                return 4;
            default:
                return 2;
         }
    }

    //Set the UI values based on the iteration rate given by the user.
    public static void setValues(String v[]){
        int speed = 0, rpm = 0, throttle = 0, fuel = 0, battery = 0;

        //Convert the values to floating point values
        speed = strToInt(v[0]);
        rpm = strToInt(v[1]);
        throttle = strToInt(v[2]);
        fuel = strToInt(v[3]);
        battery = strToInt(v[4]);

        //Add the fuel values to the arraylist so we an get the average.
        fuelValues.add(fuel);
        
        //Check the size of the arraylist and if it has met the threshold display values to user
        if(fuelValues.size() == iterationValue){
            pointerSpeedometer.speedTo(speed, 500);
            rpmTv.setText(rpm+"");
            throttleTv.setText(throttle + "%");
            fuelTv.setText(getAvg(fuelValues)+ "%");
            batteryTv.setText(battery+"V");

        } else if(fuelValues.size() > iterationValue){
            //The user has changed the iteration value to a lower one so we must check the size of the arrays
            cleanArray(fuelValues);
        }


    }

    //function to get the average from the arraylist of values
    private static int getAvg(ArrayList<Integer> values){
        float output = 0;

        for(int i = 0; i < iterationValue;i++){
            output += values.get(i);
        }
        output = output/iterationValue;

        for(int i = 0; i < iterationValue/2;i++) {
            values.remove(i);
        }

        return ((int) output);
    }

    //Function to reset the arraylist to the proper size
    private static void cleanArray(ArrayList<Integer> values){
        int size = iterationValue/2;
        int index = 0;

        while(values.size() > size){
            values.remove(index);
        }
    }

    //Converting a string to a floating point value.
    private static int strToInt(String str){
        float output = 0;
        try {
            output = Float.parseFloat(str);
        } catch(NumberFormatException e){
            Log.d("LOG", e.getMessage());
        }
        return ((int)output);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handler = null;
        Intent menu = new Intent(Dashboard.this, MainMenu.class);
        startActivity(menu);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        write("0");
    }

    @Override
    protected void onResume() {
        super.onResume();
        write("d");
    }

}
