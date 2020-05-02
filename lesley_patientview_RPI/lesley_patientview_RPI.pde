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

import processing.serial.*;
import processing.net.*;
import grafica.*;

// Java Swing Package For prompting message
import java.awt.*;
import javax.swing.*;
import static javax.swing.JOptionPane.*;

// File Packages to record the data into a text file
import javax.swing.JFileChooser;
import java.io.FileWriter;
import java.io.BufferedWriter;

// Date Format
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

// General Java Package
import java.math.*;
import controlP5.*;

ControlP5 cp5;

Textlabel lblHR;
Textlabel lblSPO2;
Textlabel lblRR;
Textlabel lblBP;
Textlabel lblTemp;
Textlabel lblMQTT;
Textlabel lblMQTTStatus;

int pSize = 250;   // 300 normally                                        // Total Size of the buffer
float time = 0;                                              // X axis increment variable

// Buffer for ecg,spo2,respiration,and average of those values
float[] xdata = new float[pSize];
float[] ecgdata = new float[pSize];
float[] reddata = new float[pSize];
float[] bpmArray = new float[pSize];
float[] ecg_avg = new float[pSize];                          
float[] resp_avg = new float[pSize];
float[] irdata = new float[pSize];
float[] spo2Array_IR = new float[pSize];
float[] spo2Array_RED = new float[pSize];
float[] rpmArray = new float[pSize];
float[] ppgArray = new float[pSize];

/************** Graph Related Variables **********************/

double maxe, mine, maxr, minr, maxs, mins;             // To Calculate the Minimum and Maximum of the Buffer
double ecg, red, spo2_ir, spo2_red, ir, redAvg, irAvg, ecgAvg, resAvg;  // To store the current ecg value
boolean startPlot = false;                             // Conditional Variable to start and stop the plot

int step = 0;
int stepsPerCycle = 100;
int lastStepTime = 0;
boolean clockwise = true;
float scale = 5;

/************** file Related Variables **********************/

boolean logging = false;                                // Variable to check whether to record the data or not
FileWriter output;                                      // In-built writer class object to write the data to file
Date datetimeReference;                                 // datetime used as reference to determine time at a data point 
BufferedWriter bufferedWriter;
DateFormat dateFormat;

/************** Logo Related Variables **********************/

PImage logo;
boolean gStatus;                                        // Boolean variable to save the grid visibility status

/************** Testing Related Variables (Lesley) **********************/
boolean testing = false; // true if testing
int index = 0; // index for draw function
IntList ecgTestDataSync; // Synchronized ecg test data with the other data in 125 Hz
IntList ppgTestData;
IntList tempTestDataSync;
IntList hrTestData;
IntList spo2TestData;
int[] dataBufferTestRed;
int[] dataBufferTestIR;
int[] dataBufferTestTemperature;
int[] dataBufferTestHr;
int[] dataBufferTestSpo2;
final int testBufferSize = 100;
final int testDataLength = 8;
int testDataIndex = 0;
java.util.Timer testTimer;
int hrFrequencyIndex = 0;

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
int contentIndex = 0; // Index for data buffer arrays 
int plotIndex = 0; // Index for data buffers at the time of plotting
int prevPlotIndex = 0; // Index for data buffers at the previous time of plotting
LinkedList<Integer> dataBufferRed;
LinkedList<Integer> dataBufferIR;
LinkedList<Integer> dataBufferTemperature;
LinkedList<Integer> dataBufferHr;
LinkedList<Integer> dataBufferSpo2;
int bufferSize = 0;
final int maxBufferSize = 100;
boolean newHrPoint = false;
int tempBufferIndex = 0;
boolean endDrawing = false;
boolean readyForDrawing = true;
final int period = 8; // time between data points

/************** Wifi network Variables (Lesley) **********************/
Server server;
final int wifiPort = 12345;
int wifiPacketCounter = 0;
final int wifiDataLength = 8;
final int wifiPacketLength = 10;

/************** Serial Variables (Lesley) **********************/
final String arduinoPortName = "/dev/ttyUSB0";
int baudrate = 38400;
Serial arduinoPort;
final int serialDataLength = 8;
final int serialPacketLength = 10;
byte[] dataBytes = new byte[100];
final int delimiter = 0x00;

boolean is_raspberrypi=false;

int global_hr;
int global_rr;
float global_temp;
int global_spo2;

int global_test=0;

boolean ECG_leadOff,spo2_leadOff;
boolean ShowWarning = true;
boolean ShowWarningSpo2=true; 

/**
 * Setup everything. Runs once at startup.
 */
public void setup() {
  // create server
  server = new Server(this, wifiPort);
    
  if (testing) {
    testSetup();
  } 
  else {
    dataBufferRed = new LinkedList<Integer>();
    dataBufferIR = new LinkedList<Integer>();
    dataBufferTemperature = new LinkedList<Integer>();
    dataBufferHr = new LinkedList<Integer>();
    dataBufferSpo2 = new LinkedList<Integer>();
  }
  pointsIR = new GPointsArray(nPoints);
  pointsRed = new GPointsArray(nPoints);
  
  //size(1000, 600, JAVA2D);
  fullScreen();
   
  // ch
  heightHeader=100;
  println("Height:"+height);

  totalPlotsHeight=height-heightHeader;
  
  makeGUI();
  surface.setTitle("Fontys PatientView");
  
  plotRed = new GPlot(this);
  plotRed.setPos(20,60);
  plotRed.setDim(width-300, (totalPlotsHeight/2)-10);
  plotRed.setBgColor(0);
  plotRed.setBoxBgColor(0);
  plotRed.setLineColor(color(0, 255, 0));
  plotRed.setLineWidth(3);
  plotRed.setMar(0,0,0,0);
  
  plotIR = new GPlot(this);
  plotIR.setPos(20,(totalPlotsHeight/2+60));
  plotIR.setDim(width-300, (totalPlotsHeight/2)-10);
  plotIR.setBgColor(0);
  plotIR.setBoxBgColor(0);
  plotIR.setLineColor(color(255, 255, 0));
  plotIR.setLineWidth(3);
  plotIR.setMar(0,0,0,0);

  for (int i = 0; i < nPoints; i++) {
    pointsIR.add(i,0);
    pointsRed.add(i,0);
  }
  plotRed.setPoints(pointsRed);
  plotIR.setPoints(pointsIR);
    
  // start serial connection
  arduinoPort = new Serial(this, arduinoPortName);
  int nextByte = 0xFF;
  while (nextByte != delimiter) {
    while (arduinoPort.available() == 0) {};
    nextByte = arduinoPort.read();
    //println(nextByte);
    //if (arduinoPort.available() > 0) {
    //  println("success");
    //}
  }
  
  //delay(1000);
  
  //for (int i = 0; i < 1000; i++) {
  //  while (arduinoPort.available() == 0) {};
  //  println(arduinoPort.read());
  //}
  
  startPlot = true;
}

/**
 * create static GUI elements
 */
public void makeGUI() {  
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
     } 
     );

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
  ArrayList<int[]> newData = new ArrayList<int[]>();
  ArrayList<byte[]> encodedData = new ArrayList<byte[]>();
  
  if (startPlot) {
    if (testing) {
      // cancel test timer when we run out of data
      if (testDataIndex>2490) {
        testTimer.cancel();
        startPlot = false;
      }
      newData = updateTestPoints();
    }
    else {
      newData = processArduinoData();
    }
    
    if (server.active()) {
      encodedData = encodeData(newData);
      sendSensorData(encodedData);
    }
  }
    
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
 * process sensor data from Arduino sent over serial usb
 */
public ArrayList<int[]> processArduinoData() {
  ArrayList<int[]> newData = new ArrayList<int[]>();
  byte[] dataPacket = new byte[100]; // data packet with 10 bytes
  while (arduinoPort.available() >= serialPacketLength && (arduinoPort.readBytesUntil(delimiter, dataPacket) == serialPacketLength)) {
    int[] dataPoints = decodeData(dataPacket);
    updatePoints(dataPoints);
    newData.add(dataPoints);
    println("new data");
  }
  
  return newData;
}

/**
 * decode COBS from arduino data packet 
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
  println("data packet");
  for (int i = 0; i < serialPacketLength; i++) {
    // println(dataPacket[i]);
  }
  
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

/**
 * updates data points
 */
void updatePoints(int[] dataPoints) {
  int red = dataPoints[0];
  int ir = dataPoints[1];
  int hr = dataPoints[2];
  int spo2 = dataPoints[3];
  int temp = dataPoints[4];
  
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
  
  // add new parameter readings to GUI
  lblHR.setText("Heartrate: " + String.valueOf(dataBufferHr.getLast()) + " bpm");
  lblSPO2.setText("SpO2: " + String.valueOf(dataBufferSpo2.getLast()) + " %");
  lblTemp.setText("Temperature: " + String.format("%.1f", dataBufferTemperature.getLast()/(float)10) + " \u00B0 C");         
  
  // remove old points from buffers
  if (bufferSize == maxBufferSize) {
    dataBufferRed.removeFirst();
    dataBufferIR.removeFirst();
    dataBufferHr.removeFirst();
    dataBufferSpo2.removeFirst();
    dataBufferTemperature.removeFirst();
  }    
}

/**
 * encode data with COBS
 */
ArrayList<byte[]> encodeData(ArrayList<int[]> newData) {
  ArrayList<byte[]> encodedData = new ArrayList<byte[]>();
  
  for (int[] dataPoint : newData) { 
    byte[] dataPacket = new byte[10];
    int overhead = testDataLength-1;
    
    // split data into individual bytes
    int red = dataPoint[0];
    dataPacket[1] = (byte) (red >> 8);
    dataPacket[2] = (byte) red;
    int ir = dataPoint[1];
    dataPacket[3] = (byte) (ir >> 8);
    dataPacket[4] = (byte) ir;
    int hr = dataPoint[2];
    dataPacket[5] = (byte) hr;
    int spo2 = dataPoint[3];
    dataPacket[6] = (byte) spo2;
    int temp = dataPoint[4];
    dataPacket[7] = (byte) (temp >> 8);
    dataPacket[8] = (byte) temp;
    
    // transform data with COBS protocol 
    dataPacket[0] = (byte) overhead;
    dataPacket[9] = delimiter;
    
    // for debugging 
    //println("untransformed datapacket");
    //for (int i = 0; i < wifiPacketLength; i++) {
    //  println(dataPacket[i]);
    //}
    
    int delimiterIndex = 0;     
    for (int i = 1; i < wifiPacketLength-1; i++) {
      if (dataPacket[i] == delimiter) {
        dataPacket[delimiterIndex] = (byte) (i-delimiterIndex);
        delimiterIndex = i;
      }     
    }      
    dataPacket[delimiterIndex] = (byte) (wifiPacketLength-1-delimiterIndex);
    
    // for debugging
    //println("transformed datapacket:");
    //for (int i = 0; i < wifiPacketLength; i++) {
    //  println(dataPacket[i]);
    //}
    
    encodedData.add(dataPacket);
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
 * close the app
 */
public void CloseApp() {
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

/*
 * write new data record in log file
 */
void logData(float red, float ir, float hr, float spo2, float temperature) {
  try {
    dateFormat = new SimpleDateFormat("HH:mm:ss:SSS");
    long millisReference = datetimeReference.getTime();
    long currentMillis = millisReference + period;
    datetimeReference.setTime(currentMillis);
    bufferedWriter.write(dateFormat.format(datetimeReference) + "," + red + "," + ir +
    "," + String.format("%.0f", hr) + "," + String.format("%.0f", spo2) + "," + String.format("%.2f", temperature));
    bufferedWriter.newLine();
  }
  catch(IOException e) {
    println("It broke!!!");
    e.printStackTrace();
  }
}

/**
 * close log file on exit
 */
void prepareExitHandler() {
  Thread logfileHook = new Thread(new Runnable() {
    public void run() {
      if (testing) {
        testTimer.cancel();
      }
      try {
        bufferedWriter.close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }}
  );
  Runtime.getRuntime().addShutdownHook(logfileHook);
}

/**
 * Update data with new point from test data. Runs every 8 ms.
 */
TimerTask updateData = new TimerTask() {
  public void run() {
    // clear buffers and move recently added data to the beginning of the buffers
    dataBufferTestRed[contentIndex] = ecgTestDataSync.get(testDataIndex);
    dataBufferTestIR[contentIndex] = ppgTestData.get(testDataIndex);      
    contentIndex++;      
    if (contentIndex == testBufferSize) {
      contentIndex = 0;
    }
    if ((testDataIndex % 125) == 0) {
      dataBufferTestTemperature[hrFrequencyIndex] = tempTestDataSync.get(hrFrequencyIndex);
      dataBufferTestHr[hrFrequencyIndex] = hrTestData.get(hrFrequencyIndex);
      dataBufferTestSpo2[hrFrequencyIndex] = spo2TestData.get(hrFrequencyIndex);
      hrFrequencyIndex++;
      newHrPoint = true;
    }
    testDataIndex++;
  }
};

/** 
 * setup test data and timer
 */
public void testSetup() {
  frameRate(30);
  
  dataBufferTestRed = new int[testBufferSize];
  dataBufferTestIR = new int[testBufferSize];
  dataBufferTestTemperature = new int[testBufferSize];
  dataBufferTestHr = new int[testBufferSize];
  dataBufferTestSpo2 = new int[testBufferSize];
  
  for (int i=0; i<testBufferSize; i++) {
    dataBufferTestRed[i] = 0;
    dataBufferTestIR[i] = 0;
  }
  
  // ecg test values
  String[] ecgFileContents = loadStrings("../ecg_testdata.csv"); // contents of ecg data file
  String[] ecgTestDataStrings = ecgFileContents[0].split(",");
  IntList ecgTestData = new IntList();
  for (int i = 0; i < ecgTestDataStrings.length; i++) {
    String formattedString = ecgTestDataStrings[i];
    while (!formattedString.matches("\\-?0\\.\\d{3}")) {
      formattedString += "0";
    }
    ecgTestData.append(parseInt(formattedString.replaceAll("0\\.0*", "")));
  }
  ecgTestDataSync = new IntList();
  // take every fourth entry of ecg test data at 500 Hzto synchronize it with the other data at 125 Hz
  for (int i = 0; i < ecgTestData.size(); i++) {
    if ((i % 4) == 0) {
      ecgTestDataSync.append(ecgTestData.get(i));
    }
  }
  
  // ppg test values
  Table ppgTestTable = loadTable("../bidmc_01_Signals.csv", "header");
  ppgTestData = new IntList();  
  for (TableRow row : ppgTestTable.rows()) {    
    String formattedString = row.getString(2).replaceAll("0\\.0*", "");
    int parsedInt = parseInt(formattedString);
    while (parsedInt < 10000) {
      parsedInt *= 10;
    }
    parsedInt >>= 2; // make the value fit in two bytes
    ppgTestData.append(parsedInt);
  }
  
  // temp test values 
  String[] tempFileContents = loadStrings("../temperature_testdata.csv"); 
  String[] tempTestDataStrings = tempFileContents[0].split(",");
  IntList tempTestData = new IntList();
  for (int i = 0; i < tempTestDataStrings.length; i++) {
    String formattedString = tempTestDataStrings[i].replaceAll("(\\d{2})(\\.)(\\d{1})(\\d*)", "$1$3");
    tempTestData.append(parseInt(formattedString));
  }
  tempTestDataSync = new IntList();
  // take every eight entry of temp test data at 8 Hz to synchronize with other data at 1 Hz
  for (int i = 0; i < tempTestData.size(); i++) {
    if ((i % 8) == 0) {
      tempTestDataSync.append(tempTestData.get(i));
    }
  }
  
  // hr and spo2 test values
  Table hrspo2TestTable = loadTable("../HR_SPO2_testdata.csv");
  hrTestData = new IntList();
  spo2TestData = new IntList();
  
  for (TableRow row : hrspo2TestTable.rows()) {
    spo2TestData.append(row.getInt(0));
    hrTestData.append(row.getInt(1));
  }
  
  // update test data every 8 milliseconds
  testTimer = new java.util.Timer("timer");
  long delay = 1000L;
  long period = 8L;
  testTimer.scheduleAtFixedRate(updateData, delay, period);
}

/**
 * updates points when testing
 * returns list of integer arrays with new data points
 */
ArrayList<int[]> updateTestPoints() {
  ArrayList<int[]> newData = new ArrayList<int[]>();
  
  if (startPlot) { 
    prevPlotIndex = plotIndex;
    plotIndex = contentIndex;
    if (plotIndex > prevPlotIndex) {
      nNewPoints = plotIndex - prevPlotIndex;
    }
    else {
      nNewPoints = plotIndex + testBufferSize - prevPlotIndex;
    }
    
    // new data
    int hrIndex = (hrFrequencyIndex == 0) ? 0 : hrFrequencyIndex-1;
    int hr = dataBufferTestHr[hrIndex];
    int spo2 = dataBufferTestSpo2[hrIndex];
    int temp = dataBufferTestTemperature[hrIndex];
    
    if (newHrPoint) {
      newHrPoint = false;
      lblHR.setText("Heartrate: " + String.valueOf(hr) + " bpm");
      lblSPO2.setText("SpO2: " + String.valueOf(spo2) + " %");
      lblTemp.setText("Temperature: " + String.format("%.1f", temp/(float)10) + " \u00B0 C");
    }           
    
    // remove n points at the beginning of the plot
    for (int i=nNewPoints-1; i >= 0; i--) {
      pointsRed.remove(i);
      pointsIR.remove(i);
    }
    
    // move plot to the left
    for (int i=0; i < nPoints-nNewPoints; i++) {
      pointsRed.setX(i, i);
      pointsIR.setX(i, i);
    }
    
    // add n new points
    int bufferIndex = prevPlotIndex;
    for (int i=0; i < nNewPoints; i++) {      
      if (bufferIndex == testBufferSize) {
        bufferIndex = 0;
      }
      int red = dataBufferTestRed[bufferIndex];
      int ir = dataBufferTestIR[bufferIndex];
      pointsRed.add(nPoints-nNewPoints+i, nPoints-nNewPoints+i, red);
      pointsIR.add(nPoints-nNewPoints+i, nPoints-nNewPoints+i, ir);
      if (logging) {
        logData(red, ir, hr, spo2, temp);
      }
      
      // add new point for transmitting
      int[] newDataPoint = new int[5];
      newDataPoint[0] = red;
      newDataPoint[1] = ir;
      newDataPoint[2] = hr;
      newDataPoint[3] = spo2;
      newDataPoint[4] = temp;
      newData.add(newDataPoint);
      
      bufferIndex = bufferIndex + 1;            
    }        
  }
  return newData;
}
