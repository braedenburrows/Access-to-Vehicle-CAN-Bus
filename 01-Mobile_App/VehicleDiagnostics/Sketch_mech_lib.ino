/********************************** Works with vehicles 2008 and newer
 *  This sketch test the mechanic library and Bluetooth communication.
 *  A page variable will be sent from the android application. This will determine the output 
 *  of the arduino sketch. The pages are as follows:
 *    m - is the main page. No data is sent.
 *    c - is the DTC page. DTC will be sent to the android. When finish the page is set to m.
 *    d - is the dashboard page. Speed, throttle position and rpm's will be sent until the user
 *        changes the page. When the user goes to the dashboard page a variable will be sent 
 *        appended to the page character. This variable will indicate the read rate for reading 
 *        data from the vehicle. The values of this variable can be from 1-10 and they will be 
 *        translated into a usable integer refresh rate by the method setRefresh(char[]). The 
 *        data sent to the android is appende with four characters of padding that indicate the 
 *        what data is being sent. This is done to prevent errors from the Bluetooth communucation
 *        dropping/altering characters in transmission. 
 */
#include <Mechanic.h>
#include <SoftwareSerial.h>
#include <stdlib.h>

//Function declarations
int setRefresh(char d[]);
void setUp(int a);

/* to communicate with the Bluetooth module's TXD pin */
#define BT_SERIAL_TX 5
/* to communicate with the Bluetooth module's RXD pin */
#define BT_SERIAL_RX 4
//Setting up the bluetooth module

SoftwareSerial Bt(BT_SERIAL_TX, BT_SERIAL_RX);

ObdInterface obd;

//Variable to count the number of requests for hasSpeed...hasBattery
boolean hasSpeed = false, hasRpm = false, hasFuel = false,
hasThrottle = false, hasBattery = false, canCommunicate = false;; 
//Holds the current page and a vulue to determine refresh rate if page is dashboard
char page[] = {'m','0'};
//default refresh rate
int refresh = 500, setUpNumber = 0, dataNotFound = 0;



void setup() {
      Serial.begin(9600); 
    
    //Start the bluetooth module
      Bt.begin(57600);
      delay(300);
    
      obd.setDebug(false); //Variable in library to print debug info
    
      while(setUpNumber < 4 && !(hasSpeed || hasRpm || hasFuel || hasThrottle)){
        Serial.println("Setting up CAN-Bus");
        setUp(setUpNumber);
        setUpNumber++;
      }
    
    //Setup failed to communicate with car.
      if(setUpNumber == 4 && !(hasSpeed || hasRpm || hasFuel || hasThrottle)){
        Serial.println("Error! CAN-Bus could not be Initialized");
      } else {
        Serial.println("CAN-Bus ready");
        //Send message to application saying arduino is ready.
         canCommunicate = true;    
      }
  } //End of setUp

void loop() {
  //Values to calculate run time of CAN-Bus functions 
  long startTime;
  long canTime = millis() - startTime;
  
  //Local variables to hold run time values
  char buffer[256];
  float speed = 0, rpm = 0, load = 0, temp = 0, fuel = 0, throttle = 0, battery = 0;
  int count = 0;

  //Check to see what page the user is on if value is sent.
  if(Bt.available()){  
      page[0] = '\0';
      page[1] = '\0';
      Bt.readBytes(page, 2);
      Serial.println(page[0]);
      Serial.println(page[1]);
      Serial.println();
  }

//If the user is on the Diagnostics page of the application/////////////////////////////////////////////////
  if(page[0] == 'c'){
        dataNotFound = 0;
  //Send a request using mode 3 to get the DTC
      if (obd.getMultiframePid(0x03, -1, buffer, count)) {
          Serial.print("# of DTCs: "); Serial.println(count / 2, DEC);
          Serial.println();
        
          //Send the number of codes to android
          Bt.print(count / 2, DEC); Bt.print(","); 
          /*
           * Loop through the buffer to translate the DTCs
           * based on Wikipedia: http://en.wikipedia.org/wiki/OBD-II_PIDs#Bitwise_encoded_PIDs
           */  
          for (int i = 0; i < count; i += 2) {
            byte first = (byte)buffer[i];
            byte second = (byte)buffer[i + 1];
            
            /*
             * The two highest bits encode the subsystem.
             * shift the first byte by 6 so the it is 00, 01, 10, or 11 
             */
            switch(first >> 6) {
              case 0: Serial.print("P"); Bt.print("P"); break;
              case 1: Serial.print("C"); Bt.print("C"); break;
              case 2: Serial.print("B"); Bt.print("B"); break;
              case 3: Serial.print("U"); Bt.print("U");break;
            }
           
             //Next two bits are the first digit (0-3).
             
            Serial.print((first >> 4) & 0x03, DEC);
            Bt.print((first >> 4) & 0x03, DEC);
            
            //Remaining 12 bits are three hex digits.
            
            Serial.print(first & 0x0f, HEX);
            Bt.print(first & 0x0f, HEX);
            
            Serial.print(second >> 4, HEX);
            Bt.print(second >> 4, HEX);
            
            Serial.print(second & 0x0f, HEX);
            Bt.print(second & 0x0f, HEX);
            
            Bt.print(",");
        }
        Bt.print("\r\n");
        Bt.flush();
        //Change the page.
        page[0] = '0';
        page[1] = '1';
    }

 //If the user is on the dashboard page of the application/////////////////////////////////////////////
  } else if(page[0] == 'd') {
      //Get the refresh rate
      refresh = setRefresh(page);
  
    //PID, MIN value, Max value, variable to store value.
      if (hasSpeed) {
           startTime = millis();
           obd.getPidAsFloat(0x0d, 0.0f, 255.0f, speed);
           canTime = millis() - startTime;
           Serial.print(speed); Serial.println(" km");
      } else {
          dataNotFound++;
          obd.isPidSupported(0x0d, hasSpeed);
      }
  
      if (hasRpm) {
          obd.getPidAsFloat(0x0c, 0.0f, 16383.75f, rpm);
          Serial.print(rpm); Serial.println(" rpm");
       } else {
          dataNotFound++;
          obd.isPidSupported(0x0c, hasRpm);
      }
    
      if (hasFuel) {
          canTime = millis() - startTime;
          obd.getPidAsFloat(0x2F, 0.0f, 100.0f, fuel);
          Serial.print(fuel); Serial.println(" fuel");
      } else {
          dataNotFound++;
          obd.isPidSupported(0x2f, hasFuel);   
      }
    
      if (hasThrottle) {
          obd.getPidAsFloat(0x11, 0.0f, 100.0f, throttle);
          Serial.print(throttle); Serial.println(" throttle");
      } else {
          dataNotFound++;
          obd.isPidSupported(0x11, hasThrottle);
          delay(50);
      } 
    
      if(hasBattery){
          obd.getPidAsFloat(0x42, 0.0f, 65.535f, battery);
          Serial.print(battery); Serial.println(" V");
      } else {
          dataNotFound++;
          obd.isPidSupported(0x42, hasBattery);
      }

 //Sending data to the android device/////////////////////////////////////////
      Bt.print(speed); Bt.print(",");
      Bt.print(rpm); Bt.print(",");
      Bt.print(throttle); Bt.print(",");
      Bt.print(fuel);Bt.print(",");
      Bt.print(battery);Bt.print("\r\n");
      Bt.flush();

      Serial.print("Number of requests missed: ");
      Serial.println(dataNotFound);
      Serial.print("Time to get speed (ms): ");
      Serial.println(canTime);
     
      delay(refresh);
   
    //End of dashboard page if statement
 } else if(page[0] == 'm'){
    dataNotFound = 0;
///////////////////////////////////////////////////////////////////
    //Check battery because it should always have a value if car is on
      if(!hasBattery){
          obd.isPidSupported(0x42, hasBattery);
       }
      Serial.println("Connection Test");
    
    if(hasBattery){
        obd.getPidAsFloat(0x42, 0.0f, 65.535f, battery);
        if(battery == 0){
            canCommunicate = false;
        } else if(battery > 0){
            canCommunicate = true;
        }
     } 
///////////////////////////////////////////////////////////////////

 //Send the value of can communicate to ensure that the application knows /////////////
    if(canCommunicate){
        Bt.print("ready");
        Bt.print("\r\n");
        Bt.flush();
        page[0] = '0'; 
    } else {
        Serial.println("Failed");
        Bt.print("nocom");
        Bt.print("\r\n");
        Bt.flush();
        page[0] = '0';
    }
  }
}

//Function to set the return rate based on user input.
int setRefresh(char d[]){
  int rate = 500;
  switch(d[1]){
          case '0':
              rate = 100;
              break;
          case '1':
              rate = 250;
              break;
          case '2':
              rate = 500;
              break;
          case '3':
              rate = 750;
              break;
          case '4':
              rate = 1000;
              break;
          default:
              rate = 500;
              break;
        }

  return (rate-100);
  
}

void setUp(int a){
  switch (a) {
    case 0:
      obd.setSlow(false);
      obd.setExtended(false);
    
      break;
    case 1:
      obd.setSlow(true);
      obd.setExtended(false);
      
      break;
    case 2:
      obd.setSlow(false);
      obd.setExtended(true);
      break;
    case 3:
      obd.setSlow(true);
      obd.setExtended(true);  
      break;  
  }
    obd.begin();
      
    obd.isPidSupported(0x0d, hasSpeed);
    obd.isPidSupported(0x0c, hasRpm);
    obd.isPidSupported(0x42, hasBattery);
    obd.isPidSupported(0x2f, hasFuel);
    obd.isPidSupported(0x11, hasThrottle);
}
 


    
 





