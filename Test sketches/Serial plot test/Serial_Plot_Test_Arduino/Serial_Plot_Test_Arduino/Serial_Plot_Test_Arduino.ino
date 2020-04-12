/*
  MAX30105 Breakout: Output all the raw Red/IR/Green readings
  By: Nathan Seidle @ SparkFun Electronics
  Date: October 2nd, 2016
  https://github.com/sparkfun/MAX30105_Breakout

  Outputs all Red/IR/Green values.

  Hardware Connections (Breakoutboard to Arduino):
  -5V = 5V (3.3V is allowed)
  -GND = GND
  -SDA = A4 (or SDA)
  -SCL = A5 (or SCL)
  -INT = Not connected

  The MAX30105 Breakout can handle 5V or 3.3V I2C logic. We recommend powering the board with 5V
  but it will also run at 3.3V.

  This code is released under the [MIT License](http://opensource.org/licenses/MIT).
*/

#include <Wire.h>
#include "MAX30105.h"

MAX30105 particleSensor;
int index = 0;
uint8_t delimiter = 0x00;
uint8_t dataLength = 10;
uint8_t overhead = 9;
uint8_t dataPacket[10];

#define debug Serial //Uncomment this line if you're using an Uno or ESP
//#define debug SerialUSB //Uncomment this line if you're using a SAMD21

void setup() {
  debug.begin(9600);

  // Initialize sensor
  if (particleSensor.begin() == false) {
    debug.println("MAX30105 was not found. Please check wiring/power. ");
    while (1);
  }

  particleSensor.setup(); //Configure sensor. Use 6.4mA for LED drive
  particleSensor.setPulseAmplitudeGreen(0); //Turn off Green LED
}

void loop() { 
    uint32_t red = particleSensor.getRed();
    uint32_t ir = particleSensor.getIR();

//    debug.print("red = ");
//    debug.println(red);
//    debug.print("ir = ");
//    debug.println(ir);
  
    // transform data with COBS protocol
    dataPacket[9] = delimiter;
    dataPacket[0] = overhead;
    
    // split data into individual bytes
    int shift = 0;
    for (int i = 1; i < 5; i++) {
      dataPacket[i] = red >> shift;
      shift += 8;
    }
    shift = 0;
    for (int i = 5; i < 9; i++) {
      dataPacket[i] = ir >> shift;
      shift += 8;
    }
    
    // loop over data bytes and find delimiter bytes
    uint8_t *delimiterByte = &dataPacket[0];
    *delimiterByte = 1;
    for (int i = 1; i < dataLength-1; i++) {
      if (dataPacket[i] == delimiter) {
        delimiterByte = &dataPacket[i];
        *delimiterByte = 0;
      }
      (*delimiterByte)++;
    }

//    for (int i = 0; i < dataLength; i++) {
//      debug.print("dataPacket[");
//      debug.print(i);
//      debug.print("] = ");
//      debug.println(dataPacket[i]);
//    }
//    debug.println("");
    
    // transmit data
    debug.write(dataPacket, dataLength);
    index++;
}
