Access to Vehicle CAN-Bus 

Authors:
	Kyle Booker
	Braeden Burrows
	
01-Mobile_app contains the android studio project for the mobile app.
	You can clone/download the code and run it in android studio or you can use the app-debug.apk 
	(VehicleDiagnostics -> app -> Build -> outputs -> apk -> debug) to install it directly on your mobile device.

02-Arduino_Program contains the arduino program used with the mobile application.
	You can clone/download the code and install on the arduino using Arduino IDE.
	Once this and the mobile application are installed, connect the arduino, mounted with CAN-bus shield and 
	bluetooth module, to your vehicle using the OBD-II cable. The OBD-II cable will power and arduino and the
	program will run automatically.
	
04-Project_Report
	This report contains addition information about the project. 
	
05-Project_Libraries
	Contains the mechanic.h library used in the arduino project. This library allows the arduino to communicate with the 
	CAN bus network on the Vehicle (2008 and newer). Can be installed by downloading then through the arduino 
	IDE go to Sketch -> Include Library -> Add .ZIP Library then pick the mechanic.h library file.
	
06-DemoVideo
	Includes a demo video of the application being used.

07-Pressentation
	This is a copy of the project presentation for the TRU CS Showcase.