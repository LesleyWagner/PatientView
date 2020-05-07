/**
 * 
 */
package patientviewrpi;

import java.util.ArrayList;

import processing.core.*;
import processing.data.*;
import processing.serial.*;

/**
 * @author lesley wagner
 * 
 * Receives and decodes serial data.
 *
 */
public class SensorDataReceiver {
	final String arduinoPortName = "/dev/ttyUSB0";
	final int baudrate = 9600;
	Serial arduinoPort;
	final int serialDataLength = 8;
	final int serialPacketLength = 10;
	byte[] dataBytes = new byte[100];
	final int delimiter = 0x00;
	
	/**
	 * Start serial connection.
	 * @param applet - processing application object
	 */
	void startSerial(PApplet applet) {
		arduinoPort = new Serial(applet, arduinoPortName);
		// Synchronize reading of data packets with delimiter 0x00
		int nextByte = 0xFF;
		while (nextByte != delimiter) {
			while (arduinoPort.available() == 0) {};
			nextByte = arduinoPort.read();
			// println(nextByte);
			// if (arduinoPort.available() > 0) {
			// println("success");
			// }
		}

		// delay(1000);

		// for (int i = 0; i < 1000; i++) {
		// while (arduinoPort.available() == 0) {};
		// println(arduinoPort.read());
		// }
	}
	
	/**
	 * process sensor data from sensor sent over serial usb
	 */
	ArrayList<int[]> processSensorData() {
	  ArrayList<int[]> newData = new ArrayList<int[]>();
	  byte[] dataPacket = new byte[100];
	  while (arduinoPort.available() >= serialPacketLength && (arduinoPort.readBytesUntil(delimiter, dataPacket) == serialPacketLength)) {
	    int[] dataPoints = decodeData(dataPacket);
	    newData.add(dataPoints);
	  }
	  
	  return newData;
	}

	/**
	 * decode COBS from sensor data packet 
	 */
	int[] decodeData(byte[] dataPacket) {
		int[] dataPoints = new int[5];
		int delimiterIndex = dataPacket[0]; // overhead byte, index of next delimiter
		for (int i = 1; i < serialPacketLength-1; i++) {
			if (i == delimiterIndex) {
				delimiterIndex = i+dataPacket[i];
				dataPacket[i] = 0x00;
			}
		}

		// for debugging
//		System.out.println("data packet");
//		for (int i = 0; i < serialPacketLength; i++) {
//			// println(dataPacket[i]);
//		}

		int red = ((int)dataPacket[1] << 8) | ((int)dataPacket[2] & 0xFF);
		int ir = (((int)dataPacket[3] & 0xFF) << 8) | ((int)dataPacket[4] & 0xFF);
		int hr = dataPacket[5];
		int spo2 = dataPacket[6];
		int temp = (((int)dataPacket[7] & 0xFF) << 8) | ((int)dataPacket[8] & 0xFF);

		dataPoints[0] = red;
		dataPoints[1] = ir;
		dataPoints[2] = hr;
		dataPoints[3] = spo2;
		dataPoints[4] = temp;

		//print("red = ");
		//println(red);
		//print("ir = ");
		//println(ir);
		//println("");

		return dataPoints;
	}
}
