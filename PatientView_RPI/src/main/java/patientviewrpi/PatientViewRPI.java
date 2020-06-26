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
import processing.net.*;
import grafica.*;
import controlP5.*;

// Java Swing Package For prompting message
import java.awt.*;
import javax.swing.*;
import static javax.swing.JOptionPane.*;

// File Packages to record the data into a text file
import javax.swing.JFileChooser;

// Date Format
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import patientviewrpi.SensorDataReceiver.UartPacket;

/**
 * @author lesley wagner
 *
 */
public class PatientViewRPI extends PApplet {

	ControlP5 cp5;
	Textlabel lblHR;
	Textlabel lblSPO2;
	Textlabel lblTemp;
	Textlabel lblAlarm;

	/************** Testing Related Variables (Lesley) **********************/
	boolean testing = false; // true if testing
	TestDataStreamer testStreamer;

	/************** Plotting Variables (Lesley) **********************/
	boolean startPlot = false;    
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
	final int bufferSize = 100;

	/************** Wifi network Variables (Lesley) **********************/
	Server server;
	final int wifiPort = 12345;
	int wifiPacketCounter = 0;
	final int wifiDataLength = 17;
	final int wifiPacketLength = 19;
	final int delimiter = 0x00;

	/************** Serial Variables (Lesley) **********************/
	SensorDataReceiver receiver = new SensorDataReceiver();

	/************** Alarm Variables (Lesley) **********************/
	LinkedList<Boolean> noDataBuffer;
	final int noDataBufferSize = 30;
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
		//size(1000, 600);
		fullScreen();
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
		noDataBuffer = new LinkedList<Boolean>();
		
		for (int i = 0; i < bufferSize; i++) {
			dataBufferRed.push(0);
			dataBufferIR.push(0);
			dataBufferTemperature.push(0);
			dataBufferHr.push(0);
			dataBufferSpo2.push(0);
		}
		
		for (int i = 0; i < noDataBufferSize; i++) {
			noDataBuffer.push(false);
		}
		
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
				.setFont(createFont("Impact",22));

		lblSPO2 = cp5.addTextlabel("lblSPO2")
				.setText("SpO2: --- %")
				.setPosition(width-250,150)
				.setColorValue(color(255,255,255))
				.setFont(createFont("Impact",22));

		lblTemp = cp5.addTextlabel("lblTemp")
				.setText("Temperature: --- \u00B0 C")
				.setPosition(width-250,200)
				.setColorValue(color(255,255,255))
				.setFont(createFont("Impact",22));
		
		lblAlarm = cp5.addTextlabel("lblAlarm")
				.setText("Test for alarm message")
				.setPosition(width-250,250)
				.setColorValue(color(255,0,0))
				.setFont(createFont("Impact",16));
	}

	/**
	 * Draw GUI on screen. Runs with a frequency of [framerate].
	 */
	public void draw() {	  
		if (startPlot) {
			ArrayList<UartPacket> uartData = new ArrayList<>();

			if (testing) {
				uartData = testStreamer.getDataPoints();
			} 
			else {
				uartData = receiver.processSensorData();				
			}
			
			// new data packets from receiver?
			if (uartData.size() > 0) {
				noData = false;	
				
				for (int i = 0; i < noDataBufferSize; i++) {
					noDataBuffer.set(i, false);
				}
				updateData(uartData);	
				
				byte alarmsByte = determineAlarms();
				
				if (server.active()) {
					ArrayList<byte[]> encodedData = encodeData(uartData, alarmsByte);
					sendSensorData(encodedData);
				}
			}
			else {
				noDataBuffer.addLast(true);
				noDataBuffer.removeFirst();
				
				// if no we have no data for the last 30 frames (1 second), then set no data alarm
				if (!noDataBuffer.contains(false)) {
					noData = true;
				}
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
			dataBufferRed.removeFirst();
			dataBufferIR.removeFirst();
			dataBufferHr.removeFirst();
			dataBufferSpo2.removeFirst();
			dataBufferTemperature.removeFirst();
		}
	}
	
	/**
	 * Determine alarms.
	 * 
	 * @param packetIndex - index in the databuffers from data points in the data packet.
	 */
	byte determineAlarms() {
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
		if (dataBufferHr.get(bufferSize-3) == 0 && dataBufferHr.get(bufferSize-1) == 0 && 
				dataBufferHr.get(bufferSize-2) == 0) {
			noHrData = true;
		}
		if (dataBufferSpo2.get(bufferSize-3) == 0 && dataBufferSpo2.get(bufferSize-1) == 0 && 
				dataBufferSpo2.get(bufferSize-2) == 0) {
			noSpo2Data = true;
		}
		if (noHrData && noSpo2Data) {
			noPox = true;
			alarms += 1;
		}
		alarms <<= 1;
		
		// no temperature data if last three temperature data points are 0
		if (dataBufferTemperature.get(bufferSize-3) == 0 && dataBufferTemperature.get(bufferSize-1) == 0 && 
				dataBufferTemperature.get(bufferSize-2) == 0) {
			noTemp = true;
			alarms += 1;
		}
		alarms <<= 1;
		
		// heart rate too high if last three heart rate data points > 200 (bpm)
		if (dataBufferHr.get(bufferSize-3) > hrBoundHigh && dataBufferHr.get(bufferSize-1) > hrBoundHigh &&
				dataBufferHr.get(bufferSize-2) > hrBoundHigh) {
			hrHigh = true;
			alarms += 1;
		}			
		alarms <<= 1;
		
		// heart rate too low if last three heart rate data points < 50 (bpm)
		if (dataBufferHr.get(bufferSize-3) < hrBoundLow && dataBufferHr.get(bufferSize-1) < hrBoundLow &&
				dataBufferHr.get(bufferSize-2) < hrBoundLow) {
			hrLow = true;
			alarms += 1;
		}			
		alarms <<= 1;
		
		// spo2 too low if last three spo2 data points < 90 (%)
		if (dataBufferSpo2.get(bufferSize-3) < spo2BoundLow && dataBufferSpo2.get(bufferSize-1) < spo2BoundLow &&
				dataBufferSpo2.get(bufferSize-2) < spo2BoundLow) {
			spo2Low = true;
			alarms += 1;
		}	
		alarms <<= 1;
		
		// temperature too high if last three temperature data points > 390 (decicelcius)
		if (dataBufferTemperature.get(bufferSize-3) > tempBoundHigh && dataBufferTemperature.get(bufferSize-1) > tempBoundHigh &&
				dataBufferTemperature.get(bufferSize-2) > tempBoundHigh) {
			tempHigh = true;
			alarms += 1;
		}			
		alarms <<= 1;
		
		// temperature too low if last three temperature data points < 340 (decicelcius)
		if (dataBufferTemperature.get(bufferSize-3) < tempBoundLow && dataBufferTemperature.get(bufferSize-1) < tempBoundLow &&
				dataBufferTemperature.get(bufferSize-2) < tempBoundLow) {
			tempLow = true;
			alarms += 1;
		}	
		
		return alarms;
	}

	/**
	 * encode data with COBS
	 */
	ArrayList<byte[]> encodeData(ArrayList<UartPacket> newData, byte alarms) {
		ArrayList<byte[]> encodedData = new ArrayList<byte[]>();

		for (UartPacket dataPacket : newData) { 
			byte[] encodedPacket = new byte[wifiPacketLength];
			int overhead = wifiPacketLength-1;

			// split data into individual bytes
			// packet ID not used for now
			long packetID = dataPacket.packetID;
			encodedPacket[1] = (byte) (packetID >> 56);
			encodedPacket[2] = (byte) (packetID >> 48);
			encodedPacket[3] = (byte) (packetID >> 40);
			encodedPacket[4] = (byte) (packetID >> 32);
			encodedPacket[5] = (byte) (packetID >> 24);
			encodedPacket[6] = (byte) (packetID >> 16);
			encodedPacket[7] = (byte) (packetID >> 8);
			encodedPacket[8] = (byte) packetID;
			int red = dataPacket.red;
			encodedPacket[9] = (byte) (red >> 8);
			encodedPacket[10] = (byte) red;
			int ir = dataPacket.ir;
			encodedPacket[11] = (byte) (ir >> 8);
			encodedPacket[12] = (byte) ir;
			int hr = dataPacket.hr;
			encodedPacket[13] = (byte) hr;
			int spo2 = dataPacket.spo2;
			encodedPacket[14] = (byte) spo2;
			int temp = dataPacket.temp;
			encodedPacket[15] = (byte) (temp >> 8);
			encodedPacket[16] = (byte) temp;
			encodedPacket[17] = alarms;

			// transform data with COBS protocol 
			encodedPacket[0] = (byte) overhead;
			encodedPacket[18] = delimiter;

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
	 * send sensor data packets to laptop over wifis
	 */
	void sendSensorData(ArrayList<byte[]> newData) {
		for (byte[] dataPacket : newData) {
			server.write(dataPacket);
		}
	}

	/**	 * draw new data on the screen.
	 */
	void drawData() {	
		background(19,75,102);
		
		// add new parameter readings to GUI
		lblHR.setColorValue(color(255,255,255));
		lblSPO2.setColorValue(color(255,255,255));
		lblTemp.setColorValue(color(255,255,255));	
		
		if (noData) {
			lblAlarm.setText("No connection with wrist band");
			lblTemp.setText("Temperature: ---");
			lblHR.setText("Heartrate: ---");
			lblSPO2.setText("SpO2: ---");
		}
		else if (noTemp || noPox || hrHigh || hrLow || spo2Low || tempHigh || tempLow) {
			if (noTemp) {				
				if (hrHigh) {
					lblHR.setColorValue(color(255,0,0));
				}
				else if (hrLow) {
					lblHR.setColorValue(color(255,0,0));
				}
				if (spo2Low) {
					lblSPO2.setColorValue(color(255,0,0));
				}
				
				lblTemp.setText("Temperature: ---");
				lblAlarm.setText("No readings from temp sensor");
				lblHR.setText("Heartrate: " + String.valueOf(dataBufferHr.getLast()) + " bpm");
				lblSPO2.setText("SpO2: " + String.valueOf(dataBufferSpo2.getLast()) + " %");
			}	
			else if (noPox) {			
				if (tempHigh) {
					lblTemp.setColorValue(color(255,0,0));
					lblAlarm.setText("Temperature is too high");
				}
				else if (tempLow) {
					lblTemp.setColorValue(color(255,0,0));
					lblAlarm.setText("Temperature is too low");
				}
				
				lblHR.setText("Heartrate: ---");
				lblSPO2.setText("SpO2: ---");
				lblAlarm.setText("No readings from pox sensor");
				lblTemp.setText("Temperature: " + String.format("%.1f", dataBufferTemperature.getLast()/(float)10) + " \u00B0C");
			}
			else {
				if (hrHigh) {
					lblHR.setColorValue(color(255,0,0));
					lblAlarm.setText("Heart rate is too high");
				}
				else if (hrLow) {
					lblHR.setColorValue(color(255,0,0));
					lblAlarm.setText("Heart rate is too low");
				}
				if (spo2Low) {
					lblSPO2.setColorValue(color(255,0,0));
					lblAlarm.setText("SPO2 level is too low");
				}
				if (tempHigh) {
					lblTemp.setColorValue(color(255,0,0));
					lblAlarm.setText("Temperature is too high");
				}
				else if (tempLow) {
					lblTemp.setColorValue(color(255,0,0));
					lblAlarm.setText("Temperature is too low");
				}
				
				lblHR.setText("Heartrate: " + String.valueOf(dataBufferHr.getLast()) + " bpm");
				lblSPO2.setText("SpO2: " + String.valueOf(dataBufferSpo2.getLast()) + " %");
				lblTemp.setText("Temperature: " + String.format("%.1f", dataBufferTemperature.getLast()/(float)10) + " \u00B0 C");	
			}
		}		
		else {
			lblAlarm.setText("");
			lblHR.setText("Heartrate: " + String.valueOf(dataBufferHr.getLast()) + " bpm");
			lblSPO2.setText("SpO2: " + String.valueOf(dataBufferSpo2.getLast()) + " %");
			lblTemp.setText("Temperature: " + String.format("%.1f", dataBufferTemperature.getLast()/(float)10) + " \u00B0 C");	
		}
		
		System.out.println(dataBufferHr.getLast());
		System.out.println(dataBufferSpo2.getLast());
		System.out.println(dataBufferTemperature.getLast());
		System.out.println();
		
		plotRed.setPoints(pointsRed);
		plotIR.setPoints(pointsIR);
		plotRed.beginDraw();
		plotRed.drawBackground();
		plotRed.drawLines();
		plotRed.endDraw();

		plotIR.beginDraw();
		plotIR.drawBackground();
		plotIR.drawLines();
		plotIR.endDraw();
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
