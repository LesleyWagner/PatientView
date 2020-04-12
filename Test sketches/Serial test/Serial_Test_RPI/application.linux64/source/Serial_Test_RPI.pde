// raspberry pi processing wifi test
import processing.serial.*;

String data = "";
String arduinoPortName = "/dev/ttyUSB0";
int baudrate = 9600;
Serial arduinoPort;

void setup() {
  size(200, 200);
  background(50);
  fill(200);
  frameRate(30);
  // connect to arduino
  arduinoPort = new Serial(this, arduinoPortName, baudrate);
}

void draw() {
  if (arduinoPort.available() > 0) {
    print(arduinoPort.readString());
  }
}
