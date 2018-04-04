package com.bdev.burrows.vehiclediagnostics;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class Diagnostics extends AppCompatActivity {
    public static TextView dtcCount;
    public static ImageView engineLight;
    public static final int RECIEVE_MESSAGE = 1;
    private static StringBuilder sb = new StringBuilder();
    private static ArrayList dtcs;
    private static ArrayAdapter dtcAdapter;
    private static Context context;
    private static ListView troubleCodes;
    private static MessageReceived handler;
    public static final int NO_BT = 2;
    public static AppCompatActivity act;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_diagnostics);
        context = getApplicationContext();
        act = Diagnostics.this;


        dtcCount = (TextView) findViewById(R.id.dtcCount);
        engineLight = (ImageView) findViewById(R.id.engineLight);
        troubleCodes = (ListView) findViewById(R.id.troubleCodes);
        engineLight.setImageResource(R.drawable.engine);

        dtcs = new ArrayList();

        write("c");

        if(!hasStream()){
            //No stream return to the home page.
            Log.d("LOG", "No stream");
            Intent backToHome = new Intent(this, MainMenu.class);
            startActivity(backToHome);
            finish();
        }

        //Set the handler for incoming messages
        handler = new MessageReceived();
        ((BTManager) this.getApplicationContext()).setHandler(handler);

    }

    //Method for the back button - r
    public void back(View v){
        handler = null;
        Intent menu = new Intent(Diagnostics.this, MainMenu.class);
        startActivity(menu);
        finish();
    }

    //Methods to access BTManager variables /////////////////////////////////////////////////////////////////////////////////

    //Method to check if there is a communication thread open.
    public boolean hasStream(){
        return ((BTManager) this.getApplicationContext()).steamReady();
    }

    public static void write(String msg){
        ((BTManager) context).write(msg+1);
    }

    public static void setUI(String dtc[]){
        int number = 0;
        try{
            number = Integer.parseInt(dtc[0]);
        } catch (NumberFormatException e){
            write("c");
        }
        if(dtc.length > 0){
            if(number >= 1) {
                engineLight.setImageResource(R.drawable.engine_orange);
                Log.d("LOG", "Value: " + (number >= 1) + " " + number);
            }
                 dtcCount.setText("Number of DTC's: " + number);
                //Add rest of the items to an array and set an array adapter for the display
                dtcs.clear();
                for(int i = 1; i < dtc.length;i++) {
                    Log.d("LOG", "data: " + dtc[i]);
                    if(!dtcs.contains(dtc[i]))
                         dtcs.add(dtc[i]);
                }
                dtcAdapter = new ArrayAdapter(context, android.R.layout.simple_list_item_1, dtcs);
                troubleCodes.setAdapter(dtcAdapter);

            } else {
            write("c");
        }
    }

    //Handler to handle incoming messages from the arduino
    public static class MessageReceived extends Handler{
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case RECIEVE_MESSAGE:
                    // if receive massage
                    byte[] readBuf = (byte[]) msg.obj;
                    String strIncom = new String(readBuf, 0, msg.arg1);           // create string from bytes array
                    sb.append(strIncom);                                                // append string
                    Log.d("LOG", "diag " + sb);
                    int endOfLineIndex = sb.indexOf("\r\n");                            // determine the end-of-line
                    if (endOfLineIndex > 0) {                                            // if end-of-line,
                        String sbprint = sb.substring(0, endOfLineIndex).trim();               // extract string
                        sb.delete(0, sb.length());
                        String codes[] = sbprint.split(",");
                        setUI(codes);
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



    @Override
    public void onBackPressed() {
        handler = null;
        Intent menu = new Intent(Diagnostics.this, MainMenu.class);
        startActivity(menu);
        finish();
    }
}
