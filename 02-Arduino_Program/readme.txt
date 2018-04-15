Sketch_mech_CAN

Authors:
	Kyle Booker
	Braeden Burrows

Date Modified:
	April 15, 2018

Description:
	Arduino sketch to read data from vehicle and transmit to mobile application
	via bluetooth.

Program:
	The setup function initializes both the bluetooth module, serial monitor and the CAN-Bus communication. 
	CAN-bus initialization is done using the function setUp(int).This test attempts to make the connection 
	at different speeds and initializes boolean values to indicate which values (speed, rpm…) are available. 
	If it cannot do this a flag is set. The following if statement is used to log the data to the serial monitor.


	The main loop of the sketch is divided into four sections:

	1. The first part of the loop initializes local variables and monitors for incoming bluetooth messages to 
	indicate which page of the mobile application the user is on.
 
	2. If the page received in the first section is c (indicating the user is on the diagnostics page), a 
	request is made to the vehicle to get the DTC. This data is then sent to the android application via 
	bluetooth and the page indication variable is set to an arbitrary value.

	3. If the page received in the first section is d (indicating the user is on the dashboard page), the 
	refresh rate sent from the user is translated to time in milliseconds using the setRefresh(int) function. 
	The code then requests either certain values from the vehicle or checks if the values are available. This is
	done for speed, rpm, fuel percentage, throttle position and battery voltage. If there are values they sent to
	the android device via bluetooth; if not, 0 is sent.

	4. If the page received in the first section is m (indicating the user is on the main page), a test is done to 
	identify if communication to the vehicle is still available and the result is sent to the android device via
	bluetooth.

