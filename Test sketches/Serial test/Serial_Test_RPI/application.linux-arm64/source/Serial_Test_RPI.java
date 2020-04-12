import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.serial.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class Serial_Test_RPI extends PApplet {

// raspberry pi processing wifi test


String data = "";
String arduinoPortName = "/dev/ttyUSB0";
int baudrate = 9600;
Serial arduinoPort;

public void setup() {
  
  background(50);
  fill(200);
  frameRate(30);
  // connect to arduino
  arduinoPort = new Serial(this, arduinoPortName, baudrate);
}

public void draw() {
  if (arduinoPort.available() > 0) {
    print(arduinoPort.readString());
  }
}
  public void settings() {  size(200, 200); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "Serial_Test_RPI" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
