// windows processing wifi test
import processing.net.*;

Client client;
int data;

void setup() {
  size(200, 200);
  background(50);
  fill(200);
  frameRate(5);
  // connect to raspberry pi
  client = new Client (this, "192.168.4.1", 12345);
}

void draw() {
}

void clientEvent(Client someClient) {
  data = client.read();
  println(data);
}
  
