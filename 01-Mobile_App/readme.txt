Vehicle Diagnostics Mobile Application

Authors:
	Kyle Booker
	Braeden Burrows

Description:
	Android application that is used with an arduino, mounted with a can bus sheild and obd-II cable, running 
	the Sketch_mech_CAN program to display both real-time data and diagnostic trouble codes.
	
Application:
	The application has four classes.
	
	1. Bluetooth manager class. This class is used to initiate and monitor a bluetooth connection to the arduino. 
	   All functions are comment and explained in the code. 
		
	2. Main Menu class. This class is the main page of the application and is used to navigate the app. It makes a 
	   call to the Bluetooth manager class to create a thread of communication to with the arduino. It also has
	   onclick methods that check if a connection has been established before you can navigate to the next page. The 
	   methods are document in the code.
		
	3. Dashboard class. This class is used to display real-time data from the arduino to the user. It sets the handler for
	   this activity in the bluetooth manager class so that the UI connected to the Dashboard class can be updated 
	   when data is received or the bluetooth connection has been lost. The methods are document in the code.
		
	4.Diagnostic Page. This page is used to display the diagnostic trouble codes sent from the arduino to the user. It 
	  sets the handler for this activity in the bluetooth manager class so that the UI connected to the Diagnostic class 
	  can be updated when data is received or the bluetooth connection has been lost. The methods are document in the 
	  code.
		
	


