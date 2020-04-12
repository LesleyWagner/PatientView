import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.net.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class WIFI_Test_RPI extends PApplet {

// raspberry pi processing wifi test


Server server;
int data = 0;

public void setup() {
  
  background(50);
  fill(200);
  frameRate(5);
  // connect to raspberry pi
  server = new Server (this, 12345);
}

public void draw() {
  server.write(data);
  data += 1;
}


  public void settings() {  size(200, 200); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "WIFI_Test_RPI" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
