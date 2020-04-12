// raspberry pi processing wifi test
import processing.net.*;

Server server;
int data = 0;

void setup() {
  size(200, 200);
  background(50);
  fill(200);
  frameRate(5);
  // connect to raspberry pi
  server = new Server (this, 12345);
}

void draw() {
  server.write(data);
  data += 1;
}
