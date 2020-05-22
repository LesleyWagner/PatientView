//////////////////////////////////////////////////////////////////////////////////////////
//
//   Raspberry Pi/ Desktop GUI for controlling the HealthyPi HAT v3
//
//   Copyright (c) 2016 ProtoCentral
//   
//   This software is licensed under the MIT License(http://opensource.org/licenses/MIT). 
//   
//   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT 
//   NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
//   IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
//   WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
//   SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
/////////////////////////////////////////////////////////////////////////////////////////
package patientviewrpi;

import processing.core.*;
import processing.data.*;
import processing.net.*;
import grafica.*;

// Java Swing Package For prompting message
import java.awt.*;
import javax.swing.*;
import static javax.swing.JOptionPane.*;

// File Packages to record the data into a text file
import javax.swing.JFileChooser;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
// Date Format
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

// General Java Package
import java.math.*;
import controlP5.*;

import patientviewrpi.SensorDataReceiver.UartPacket;

/**
 * @author lesley wagner
 *
 */
public class PatientViewRPI extends PApplet {

	ControlP5 cp5;

	Textlabel lblHR;
	Textlabel lblSPO2;
	Textlabel lblRR;
	Textlabel lblBP;
	Textlabel lblTemp;
	Textlabel lblMQTT;
	Textlabel lblMQTTStatus;
	
	/************** Graph Related Variables **********************/
	boolean startPlot = false;                             // Conditional Variable to start and stop the plot


	/************** Logo Related Variables **********************/

	PImage logo;
	boolean gStatus;                                        // Boolean variable to save the grid visibility status

	/************** Testing Related Variables (Lesley) **********************/
	boolean testing = true; // true if testing
	TestDataStreamer testStreamer;

	/************** Plotting Variables (Lesley) **********************/
	GPlot plotIR;
	GPlot plotRed;
	GPointsArray pointsRed;
	GPointsArray pointsIR;
	int nPoints = 250; // number of plot points
	int nNewPoints = 0;
	int totalPlotsHeight=0;
	int totalPlotsWidth=0;
	int heightHeader=100;

	/************** Data Variables (Lesley) **********************/
	LinkedList<Integer> dataBufferRed;
	LinkedList<Integer> dataBufferIR;
	LinkedList<Integer> dataBufferTemperature;
	LinkedList<Integer> dataBufferHr;
	LinkedList<Integer> dataBufferSpo2;
	int bufferSize = 0;
	final int maxBufferSize = 100;
	boolean endDrawing = false;
	boolean readyForDrawing = true;

	/************** Wifi network Variables (Lesley) **********************/
	Server server;
	final int wifiPort = 12345;
	int wifiPacketCounter = 0;
	final int wifiDataLength = 8;
	final int wifiPacketLength = 10;
	final int delimiter = 0x00;

	/************** Serial Variables (Lesley) **********************/
	SensorDataReceiver receiver = new SensorDataReceiver();

	/************** Alarm Variables (Lesley) **********************/
//	boolean ECG_leadOff,spo2_leadOff;
//	boolean ShowWarning = true;
//	boolean ShowWarningSpo2=true;
	boolean noData;
	boolean noPox;
	boolean noTemp;
	boolean hrHigh;
	boolean hrLow;
	boolean spo2Low;
	boolean tempHigh;
	boolean tempLow;
	final int hrBoundHigh = 200;
	final int hrBoundLow = 50;
	final int spo2BoundLow = 90;
	final int tempBoundHigh = 390;
	final int tempBoundLow = 340;
	

	/**
	 * Set screen settings.
	 */
	public void settings() {
		size(1000, 600);
		// fullScreen(P2D);
	}

	/**
	 * Setup everything. Runs once at startup.
	 */
	public void setup() {
		pointsIR = new GPointsArray(nPoints);
		pointsRed = new GPointsArray(nPoints);

		heightHeader = 100;

		totalPlotsHeight = height - heightHeader;

		makeGUI();
		surface.setTitle("Fontys PatientView");

		plotRed = new GPlot(this);
		plotRed.setPos(20, 60);
		plotRed.setDim(width - 300, (totalPlotsHeight / 2) - 10);
		plotRed.setBgColor(0);
		plotRed.setBoxBgColor(0);
		plotRed.setLineColor(color(0, 255, 0));
		plotRed.setLineWidth(3);
		plotRed.setMar(0, 0, 0, 0);

		plotIR = new GPlot(this);
		plotIR.setPos(20, (totalPlotsHeight / 2 + 60));
		plotIR.setDim(width - 300, (totalPlotsHeight / 2) - 10);
		plotIR.setBgColor(0);
		plotIR.setBoxBgColor(0);
		plotIR.setLineColor(color(255, 255, 0));
		plotIR.setLineWidth(3);
		plotIR.setMar(0, 0, 0, 0);

		for (int i = 0; i < nPoints; i++) {
			pointsIR.add(i, 0);
			pointsRed.add(i, 0);
		}
		plotRed.setPoints(pointsRed);
		plotIR.setPoints(pointsIR);	
		
		dataBufferRed = new LinkedList<Integer>();
		dataBufferIR = new LinkedList<Integer>();
		dataBufferTemperature = new LinkedList<Integer>();
		dataBufferHr = new LinkedList<Integer>();
		dataBufferSpo2 = new LinkedList<Integer>();
		
		server = new Server(this, wifiPort);

		if (testing) {
			testStreamer = new TestDataStreamer();
			testStreamer.start();
		} 
		else {
			receiver.startSerial(this);
		}	  
		startPlot = true;
	}

	/**
	 * create static GUI elements
	 */
	void makeGUI() {  
		cp5 = new ControlP5(this);
		cp5.addButton("Close")
		.setValue(0)
		.setPosition(width-110,10)
		.setSize(100,40)
		.setFont(createFont("Arial",15))
		.addCallback(new CallbackListener() {
			public void controlEvent(CallbackEvent event) {
				if (event.getAction() == ControlP5.ACTION_RELEASE) {
					CloseApp();
					//cp5.remove(event.getController().getName());
				}
			}
		});

		lblHR = cp5.addTextlabel("lblHR")
				.setText("Heartrate: --- bpm")
				.setPosition(width-250,100)
				.setColorValue(color(255,255,255))
				.setFont(createFont("Impact",24));

		lblSPO2 = cp5.addTextlabel("lblSPO2")
				.setText("SpO2: --- %")
				.setPosition(width-250,150)
				.setColorValue(color(255,255,255))
				.setFont(createFont("Impact",24));

		//lblRR = cp5.addTextlabel("lblRR")
		//.setText("Respiration: --- bpm")
		//.setPosition(width-350,(totalPlotsHeight/3+totalPlotsHeight/3+10))
		//.setColorValue(color(255,255,255))
		//.setFont(createFont("Impact",40));

		/*
	    lblBP = cp5.addTextlabel("lblBP")
	      .setText("BP: --- / ---")
	      .setPosition((width-250),height-25)
	      .setColorValue(color(255,255,255))
	      .setFont(createFont("Verdana",20));
		 */

		lblTemp = cp5.addTextlabel("lblTemp")
				.setText("Temperature: --- \u00B0 C")
				.setPosition(width-250,200)
				.setColorValue(color(255,255,255))
				.setFont(createFont("Impact",24));

		cp5.addButton("logo")
		.setPosition(20,10)
		.setImages(loadImage("protocentral.png"), loadImage("protocentral.png"), loadImage("protocentral.png"))
		.updateSize();         
	}

	/**
	 * Draw GUI on screen. Runs with a frequency of [framerate].
	 */
	public void draw() {	  
		if (startPlot) {
			ArrayList<UartPacket> uartData = new ArrayList<>();
			ArrayList<WifiPacket> wifiData = new ArrayList<>();

			if (testing) {
//				// cancel test timer when we run out of data
//				if (testDataIndex > 2490) {
//					testTimer.cancel();
//					startPlot = false;
//				}
				uartData = testStreamer.getDataPoints();
			} 
			else {
				uartData = receiver.processSensorData();				
			}
			
			// new data packets from receiver?
			if (uartData.size() > 0) {
				noData = false;
				
				updateData(uartData);
				
				for (int i = 0; i < uartData.size(); i++) {
					UartPacket uartPacket = uartData.get(i);
					WifiPacket wifiPacket = new WifiPacket();
					byte alarmsByte = 0;
					
					if (bufferSize-uartData.size()+i > 2) {
						alarmsByte = determineAlarms(bufferSize-uartData.size()+i);
					}
					
					wifiPacket.red = uartPacket.red;
					wifiPacket.ir = uartPacket.ir;
					wifiPacket.hr = uartPacket.hr;
					wifiPacket.spo2 = uartPacket.spo2;
					wifiPacket.temp = uartPacket.temp;
					wifiPacket.alarms = alarmsByte;
				}
				
				if (server.active()) {
					ArrayList<byte[]> encodedData = encodeData(wifiData);
					sendSensorData(encodedData);
				}
			}
			else {
				noData = true;
			}
			
			drawData();
		}
	}

	/**
	 * updates data points
	 */
	void updateData(ArrayList<UartPacket> newData) {
		for (UartPacket dataPacket : newData) {
			int red = dataPacket.red;
			int ir = dataPacket.ir;
			int hr = dataPacket.hr;
			int spo2 = dataPacket.spo2;
			int temp = dataPacket.temp;
	
			// add new points to buffers
			dataBufferRed.addLast(red);   
			dataBufferIR.addLast(ir);
			dataBufferHr.addLast(hr);
			dataBufferSpo2.addLast(spo2);
			dataBufferTemperature.addLast(temp);
			bufferSize++;
	
			// add points to plots
			pointsRed.add(nPoints, nPoints, red);
			pointsIR.add(nPoints, nPoints, ir);
	
			// remove first points from the plots
			pointsRed.remove(0);
			pointsIR.remove(0);
	
			// shift plots to the left
			for (int j = 0; j < nPoints; j++) {
				pointsRed.setX(j, j);
				pointsIR.setX(j, j);
			}    
	
			// remove old points from buffers
			if (bufferSize == maxBufferSize) {
				dataBufferRed.removeFirst();
				dataBufferIR.removeFirst();
				dataBufferHr.removeFirst();
				dataBufferSpo2.removeFirst();
				dataBufferTemperature.removeFirst();
				bufferSize--;
			}
		}
	}
	
	/**
	 * Determine alarms.
	 * 
	 * @param packetIndex - index in the databuffers from data points in the data packet.
	 */
	byte determineAlarms(int packetIndex) {
		byte alarms = 0;	
		noPox = false;
		noTemp = false;
		hrHigh = false;
		hrLow = false;
		spo2Low = false;
		tempHigh = false;
		tempLow = false;
		
		// no uart data if no packets received
		if (noData) {
			alarms += 1;
		}
		alarms <<= 1;
		
		// no pulse oximeter data if last three hr and spo2 data points are 0
		boolean noHrData = false;
		boolean noSpo2Data = false;
		if (dataBufferHr.get(packetIndex) == 0 && dataBufferHr.get(packetIndex-1) == 0 && 
				dataBufferHr.get(packetIndex-2) == 0) {
			noHrData = true;
		}
		if (dataBufferSpo2.get(packetIndex) == 0 && dataBufferSpo2.get(packetIndex-1) == 0 && 
				dataBufferSpo2.get(packetIndex-2) == 0) {
			noSpo2Data = true;
		}
		if (noHrData && noSpo2Data) {
			noPox = true;
			alarms += 1;
		}
		alarms <<= 1;
		
		// no temperature data if last three temperature data points are 0
		if (dataBufferTemperature.get(packetIndex) == 0 && dataBufferTemperature.get(packetIndex-1) == 0 && 
				dataBufferTemperature.get(packetIndex-2) == 0) {
			noTemp = true;
			alarms += 1;
		}
		alarms <<= 1;
		
		// heart rate too high if last three heart rate data points > 200 (bpm)
		if (dataBufferHr.get(packetIndex) > hrBoundHigh && dataBufferHr.get(packetIndex-1) > hrBoundHigh &&
				dataBufferHr.get(packetIndex-2) > hrBoundHigh) {
			hrHigh = true;
			alarms += 1;
		}			
		alarms <<= 1;
		
		// heart rate too low if last three heart rate data points < 50 (bpm)
		if (dataBufferHr.get(packetIndex) < hrBoundLow && dataBufferHr.get(packetIndex-1) < hrBoundLow &&
				dataBufferHr.get(packetIndex-2) < hrBoundLow) {
			hrLow = true;
			alarms += 1;
		}			
		alarms <<= 1;
		
		// spo2 too low if last three spo2 data points < 90 (%)
		if (dataBufferSpo2.get(packetIndex) < spo2BoundLow && dataBufferSpo2.get(packetIndex-1) < spo2BoundLow &&
				dataBufferSpo2.get(packetIndex-2) < spo2BoundLow) {
			spo2Low = true;
			alarms += 1;
		}	
		alarms <<= 1;
		
		// temperature too high if last three temperature data points > 390 (decicelcius)
		if (dataBufferTemperature.get(packetIndex) > tempBoundHigh && dataBufferTemperature.get(packetIndex-1) > tempBoundHigh &&
				dataBufferTemperature.get(packetIndex-2) > tempBoundHigh) {
			tempHigh = true;
			alarms += 1;
		}			
		alarms <<= 1;
		
		// temperature too low if last three temperature data points < 340 (decicelcius)
		if (dataBufferTemperature.get(packetIndex) < tempBoundLow && dataBufferTemperature.get(packetIndex-1) < tempBoundLow &&
				dataBufferTemperature.get(packetIndex-2) < tempBoundLow) {
			tempLow = true;
			alarms += 1;
		}	
		
		return alarms;
	}

	/**
	 * encode data with COBS
	 */
	ArrayList<byte[]> encodeData(ArrayList<WifiPacket> newData) {
		ArrayList<byte[]> encodedData = new ArrayList<byte[]>();

		for (WifiPacket dataPacket : newData) { 
			byte[] encodedPacket = new byte[10];
			int overhead = wifiPacketLength-1;

			// split data into individual bytes
			int red = dataPacket.red;
			encodedPacket[1] = (byte) (red >> 8);
			encodedPacket[2] = (byte) red;
			int ir = dataPacket.ir;
			encodedPacket[3] = (byte) (ir >> 8);
			encodedPacket[4] = (byte) ir;
			int hr = dataPacket.hr;
			encodedPacket[5] = (byte) hr;
			int spo2 = dataPacket.spo2;
			encodedPacket[6] = (byte) spo2;
			int temp = dataPacket.temp;
			encodedPacket[7] = (byte) (temp >> 8);
			encodedPacket[8] = (byte) temp;

			// transform data with COBS protocol 
			encodedPacket[0] = (byte) overhead;
			encodedPacket[9] = delimiter;

			// for debugging 
			//println("untransformed datapacket");
			//for (int i = 0; i < wifiPacketLength; i++) {
			//  println(dataPacket[i]);
			//}

			int delimiterIndex = 0;     
			for (int i = 1; i < wifiPacketLength-1; i++) {
				if (encodedPacket[i] == delimiter) {
					encodedPacket[delimiterIndex] = (byte) (i-delimiterIndex);
					delimiterIndex = i;
				}     
			}      
			encodedPacket[delimiterIndex] = (byte) (wifiPacketLength-1-delimiterIndex);

			// for debugging
			//println("transformed datapacket:");
			//for (int i = 0; i < wifiPacketLength; i++) {
			//  println(dataPacket[i]);
			//}

			encodedData.add(encodedPacket);
		}
		return encodedData;
	}

	/**
	 * send sensor data packets to laptop over wifi
	 */
	void sendSensorData(ArrayList<byte[]> newData) {
		for (byte[] dataPacket : newData) {
			server.write(dataPacket);
		}
	}

	/**
	 * draw new data on the screen.
	 */
	void drawData() {	
		// add new parameter readings to GUI
		lblHR.setText("Heartrate: " + String.valueOf(dataBufferHr.getLast()) + " bpm");
		lblSPO2.setText("SpO2: " + String.valueOf(dataBufferSpo2.getLast()) + " %");
		lblTemp.setText("Temperature: " + String.format("%.1f", dataBufferTemperature.getLast()/(float)10) + " \u00B0 C");

		plotRed.setPoints(pointsRed);
		plotIR.setPoints(pointsIR);

		background(19,75,102);

		plotRed.beginDraw();
		plotRed.drawBackground();
		plotRed.drawLines();
		plotRed.endDraw();

		plotIR.beginDraw();
		plotIR.drawBackground();
		plotIR.drawLines();
		plotIR.endDraw();

		endDrawing = true;
	}

	/**
	 * close the app
	 */
	void CloseApp() {
		EventQueue.invokeLater(new Runnable() {
	        @Override
	        public void run() {
	        	int dialogResult = JOptionPane.showConfirmDialog (null, "Would You Like to Close The Application?");
	    		if (dialogResult == JOptionPane.YES_OPTION) {
	    			//try {
	    			//  //Runtime runtime = Runtime.getRuntime();
	    			//  //Process proc = runtime.exec("sudo shutdown -h now");
	    			//  System.exit(0);
	    			//}
	    			//catch(Exception e) {
	    			//  exit();
	    			//}
	    			exit();
	    		} 
	    		else {
	    		}
	        }
	    });
	}

	/**
	 * close log file on exit
	 */
	void prepareExitHandler() {
		Thread exitHook = new Thread(new Runnable() {
			public void run() {
				if (testing) {
					testStreamer.stop();
				}
			}}
				);
		Runtime.getRuntime().addShutdownHook(exitHook);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PApplet.main("patientviewrpi.PatientViewRPI");
	}
}
