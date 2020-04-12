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

/************** Packet Validation  **********************/
private static final int CESState_Init = 0;
private static final int CESState_SOF1_Found = 1;
private static final int CESState_SOF2_Found = 2;
private static final int CESState_PktLen_Found = 3;

/*CES CMD IF Packet Format*/
private static final int CES_CMDIF_PKT_START_1 = 0x0A;
private static final int CES_CMDIF_PKT_START_2 = 0xFA;
private static final int CES_CMDIF_PKT_STOP = 0x0B;

/*CES CMD IF Packet Indices*/
private static final int CES_CMDIF_IND_LEN = 2;
private static final int CES_CMDIF_IND_LEN_MSB = 3;
private static final int CES_CMDIF_IND_PKTTYPE = 4;
private static int CES_CMDIF_PKT_OVERHEAD = 5;

/************** Packet Related Variables **********************/

int ecs_rx_state = 0;                                        // To check the state of the packet
int CES_Pkt_Len;                                             // To store the Packet Length Deatils
int CES_Pkt_Pos_Counter, CES_Data_Counter;                   // Packet and data counter

int CES_Pkt_PktType;         // To store the Packet Type
char CES_Pkt_Data_Counter[] = new char[1000];                // Buffer to store the data from the packet
char CES_Pkt_ECG_Counter[] = new char[4];                    // Buffer to hold ECG data
char ces_pkt_red_counter[] = new char[4];                   // Respiration Buffer
char CES_Pkt_SpO2_Counter_RED[] = new char[4];               // Buffer for SpO2 RED
char ces_pkt_ir_counter[] = new char[4];                // Buffer for SpO2 IR

int pSize = 250;   // 300 normally                                        // Total Size of the buffer
int arrayIndex = 0;                                          // Increment Variable for the buffer
float time = 0;                                              // X axis increment variable

// Buffer for ecg,spo2,respiration,and average of thos values
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
double respirationVoltage=20;                          // To store the current respiration value
boolean startPlot = false;                             // Conditional Variable to start and stop the plot

int step = 0;
int stepsPerCycle = 100;
int lastStepTime = 0;
boolean clockwise = true;
float scale = 5;

/************** File Related Variables **********************/

boolean logging = false;                                // Variable to check whether to record the data or not
FileWriter output;                                      // In-built writer class object to write the data to file
JFileChooser jFileChooser;                              // Helps to choose particular folder to save the file
Date date;                                              // Variables to record the date related values                              
BufferedWriter bufferedWriter;
DateFormat dateFormat;

/************** Port Related Variables **********************/

Serial port = null;                                     // Oject for communicating via serial port
String[] comList;                                       // Buffer that holds the serial ports that are paired to the laptop
char inString = '\0';                                   // To receive the bytes from the packet
String selectedPort;                                    // Holds the selected port number

/************** Logo Related Variables **********************/

PImage logo;
boolean gStatus;                                        // Boolean variable to save the grid visibility status

/************** Testing Related Variables (Lesley) **********************/
boolean testing = false;
int index = 0; // index for draw function
FloatList ecgTestDataSync; // Synchronized ecg test data with the other data in 125 Hz
FloatList ppgTestData;
int testDataIndex = 0;
java.util.Timer testTimer;

/************** Plotting Variables (Lesley) **********************/
GPlot plotIR;
GPlot plotRed;
GPointsArray pointsRed;
GPointsArray pointsIR;
GPointsArray newPointsRed;
GPointsArray newPointsIR;
int nPoints = 250; // number of plot points
int nNewPoints = 0;
int totalPlotsHeight=0;
int totalPlotsWidth=0;
int heightHeader=100;

/************** Data Variables (Lesley) **********************/
int contentIndex = 0; // Index for data buffer arrays 
int plotIndex = 0; // Index for data buffers at the time of plotting
int prevPlotIndex = 0; // Index for data buffers at the previous time of plotting
Float[] dataBufferRed;
Float[] dataBufferIR;
Float[] tempBufferRed;
Float[] tempBufferIR;
int tempBufferIndex = 0;
int bufferSize = 100;
boolean endDrawing = false;
boolean readyForDrawing = true;

/*********** Serial variables (Lesley) ***********/
String arduinoPortName = "/dev/ttyUSB0";
int baudrate = 9600;
Serial arduinoPort;
int dataLength = 10;
byte[] dataBytes = new byte[100];
byte delimiter = 0x00;

boolean is_raspberrypi=true;

int global_hr;
int global_rr;
float global_temp;
int global_spo2;

int global_test=0;

boolean ECG_leadOff,spo2_leadOff;
boolean ShowWarning = true;
boolean ShowWarningSpo2=true;

TimerTask updateData = new TimerTask() {
    public void run() {
      // clear buffers and move recently added data to the beginning of the buffers
      dataBufferRed[contentIndex] = ecgTestDataSync.get(testDataIndex);
      dataBufferIR[contentIndex] = ppgTestData.get(testDataIndex);
      testDataIndex++;
      contentIndex++;
      if (contentIndex == bufferSize) {
        contentIndex = 0;
      }
    }
  };

// setup test data and timer
public void testSetup() {
  frameRate(30);
  
  // ecg test values
  String[] ecgFileContents = loadStrings("ecg_testdata.csv"); // contents of ecg data file
  float[] ecgTestData = float(ecgFileContents[0].split(","));
  ecgTestDataSync = new FloatList();
  // take every fourth entry of ecg test data at 500 Hzto synchronize it with the other data at 125 Hz
  for (int i = 0; i < ecgTestData.length; i++) {
    if ((i % 4) == 0) {
      ecgTestDataSync.append(ecgTestData[i]);
    }
  }
  
  // ppg and respiration test values
  Table testTable = loadTable("bidmc_01_Signals.csv", "header");
  ppgTestData = new FloatList();
  
  for (TableRow row : testTable.rows()) {
    ppgTestData.append(row.getFloat(2));
  }
  
    
  // startPlot = true; // for testing
  
  // update test data every 8 milliseconds
  testTimer = new java.util.Timer("timer");
  long delay = 1000L;
  long period = 16L;
  testTimer.scheduleAtFixedRate(updateData, delay, period);
}
  

public void setup() {
  // uncomment when testing the GUI
  //testing = true;
  if (testing) {
    testSetup();
  }
  
  // start serial connection
  arduinoPort = new Serial(this, arduinoPortName, baudrate);
  
  dataBufferRed = new Float[bufferSize];
  dataBufferIR = new Float[bufferSize];
  tempBufferRed = new Float[bufferSize];
  tempBufferIR = new Float[bufferSize];
  pointsIR = new GPointsArray(nPoints);
  pointsRed = new GPointsArray(nPoints);
  newPointsRed = new GPointsArray(bufferSize);
  newPointsIR = new GPointsArray(bufferSize);
  
  for (int i=0; i<bufferSize; i++) {
    newPointsRed.add(i, 0);  
    newPointsIR.add(i, 0);
    dataBufferRed[i] = 0F;
    dataBufferIR[i] = 0F;
  }
  
  //size(800, 600, JAVA2D);
  fullScreen();
   
  // ch
  heightHeader=100;
  println("Height:"+height);

  totalPlotsHeight=height-heightHeader;
  
  makeGUI();
  surface.setTitle("Protocentral OpenView");
  
  plotRed = new GPlot(this);
  plotRed.setPos(20,60);
  plotRed.setDim(width-40, (totalPlotsHeight/2)-10);
  plotRed.setBgColor(0);
  plotRed.setBoxBgColor(0);
  plotRed.setLineColor(color(0, 255, 0));
  plotRed.setLineWidth(3);
  plotRed.setMar(0,0,0,0);
  
  plotIR = new GPlot(this);
  plotIR.setPos(20,(totalPlotsHeight/2+60));
  plotIR.setDim(width-40, (totalPlotsHeight/2)-10);
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
  startPlot = true;
  
  int delim = 0xFF;
  while (delim != delimiter) {
    delim = arduinoPort.read();
    println(delim);
  };
}


public void makeGUI() {  
   cp5 = new ControlP5(this);
   cp5.addButton("Close")
     .setValue(0)
     .setPosition(width-110,10)
     .setSize(100,40)
     .setFont(createFont("Arial",15))
     .addCallback(new CallbackListener() {
      public void controlEvent(CallbackEvent event) {
        if (event.getAction() == ControlP5.ACTION_RELEASED) {
          testTimer.cancel();
          CloseApp();
          //cp5.remove(event.getController().getName());
        }
      }
     } 
     );
  
   cp5.addButton("Record")
     .setValue(0)
     .setPosition(width-225,10)
     .setSize(100,40)
     .setFont(createFont("Arial",15))
     .addCallback(new CallbackListener() {
      public void controlEvent(CallbackEvent event) {
        if (event.getAction() == ControlP5.ACTION_RELEASED) {
          RecordData();
          //cp5.remove(event.getController().getName());
        }
      }
     } 
     );   

     lblHR = cp5.addTextlabel("lblHR")
      .setText("Heartrate: --- bpm")
      .setPosition(width-350,5)
      .setColorValue(color(255,255,255))
      .setFont(createFont("Impact",40));
      
    lblSPO2 = cp5.addTextlabel("lblSPO2")
    .setText("SpO2: --- %")
    .setPosition(width-350,(totalPlotsHeight/3+10))
    .setColorValue(color(255,255,255))
    .setFont(createFont("Impact",40));

    lblRR = cp5.addTextlabel("lblRR")
    .setText("Respiration: --- bpm")
    .setPosition(width-350,(totalPlotsHeight/3+totalPlotsHeight/3+10))
    .setColorValue(color(255,255,255))
    .setFont(createFont("Impact",40));
   
    /*
    lblBP = cp5.addTextlabel("lblBP")
      .setText("BP: --- / ---")
      .setPosition((width-250),height-25)
      .setColorValue(color(255,255,255))
      .setFont(createFont("Verdana",20));
    */
    
    lblTemp = cp5.addTextlabel("lblTemp")
      .setText("Temperature: --- \u00B0 C")
      .setPosition((width/3)*2,height-70)
      .setColorValue(color(255,255,255))
      .setFont(createFont("Verdana",40));
      
     cp5.addButton("logo")
     .setPosition(20,10)
     .setImages(loadImage("protocentral.png"), loadImage("protocentral.png"), loadImage("protocentral.png"))
     .updateSize();         
}

// process red and ir data points and decode CRCB
void processDataPoints() {
  // decode CRCB
  // red point
  int red = 0;
  int dataByte = 0;
  int shift = 0;
  int distance = dataBytes[0]; // overhead byte
  for (int i = 1; i < 5; i++) {
    distance--;
    if (distance == 0) {
      dataByte = delimiter;
      distance = dataBytes[i];
    }
    else {
      dataByte = (int)dataBytes[i] & 0xFF;
    }
    red = red | (dataByte << shift);
    shift += 8;
  }
  
  // ir point
  int ir = 0;
  shift = 0;
  for (int i = 5; i < 9; i++) {
    distance--;
    if (distance == 0) {
      dataByte = delimiter;
      distance = dataBytes[i];
    }
    else {
      dataByte = (int)dataBytes[i] & 0xFF;
    }
    ir = ir | (dataByte << shift);
    shift += 8;
  }
  print("red = ");
  println(red);
  print("ir = ");
  println(ir);
  println("");
  
  pointsRed.add(nPoints+nNewPoints, red);
  pointsIR.add(nPoints+nNewPoints, ir);
}

// test plots with arbitrary lists of values by Lesley Wagner
public void draw() {
  
  if (testing) {
    // cancel test timer when we run out of data
    if (testDataIndex>2490) {
      testTimer.cancel();
      startPlot = false;
    }
  }
  
  if (startPlot) {
    // process new data
    
    //prevPlotIndex = plotIndex;
    //plotIndex = contentIndex;
    //if (plotIndex > prevPlotIndex) {
    //  nNewPoints = plotIndex - prevPlotIndex;
    //}
    //else {
    //  nNewPoints = plotIndex + bufferSize - prevPlotIndex;
    //}
    
    nNewPoints = 0;
    while (arduinoPort.readBytesUntil(delimiter, dataBytes) == 10) {    
      processDataPoints();
      nNewPoints++;
    }
    
    // remove n points at the beginning of the plot
    for (int i=nNewPoints-1; i >= 0; i--) {
      pointsRed.remove(i);
      pointsIR.remove(i);
    }
    
    // move plot to the left
    for (int i=0; i < nPoints; i++) {
      pointsRed.setX(i, i);
      pointsIR.setX(i, i);
    }
    
    // add n new points to the end of the plot
    //int bufferIndex = prevPlotIndex;
    //for (int i=0; i < nNewPoints; i++) {      
    //  if (bufferIndex == bufferSize) {
    //    bufferIndex = 0;
    //  }
    //  pointsRed.add(nPoints-nNewPoints+i, nPoints-nNewPoints+i, dataBufferRed[bufferIndex]);
    //  pointsIR.add(nPoints-nNewPoints+i, nPoints-nNewPoints+i, dataBufferIR[bufferIndex]);
    //  bufferIndex = bufferIndex + 1;
    //}
    
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
  // Default value is set
  else {                                  
  }
}

public void CloseApp() {
  int dialogResult = JOptionPane.showConfirmDialog (null, "Would You Like to Close The Application?");
  if (dialogResult == JOptionPane.YES_OPTION) {
    try {
      //Runtime runtime = Runtime.getRuntime();
      //Process proc = runtime.exec("sudo shutdown -h now");
      System.exit(0);
    }
    catch(Exception e) {
      exit();
    }
  } 
  else {
  }
}

public void RecordData() {
    try {
    jFileChooser = new JFileChooser();
    jFileChooser.setSelectedFile(new File("log.csv"));
    jFileChooser.showSaveDialog(null);
    String filePath = jFileChooser.getSelectedFile()+"";

    if ((filePath.equals("log.txt"))||(filePath.equals("null"))) {
    } 
    else {    
      logging = true;
      date = new Date();
      output = new FileWriter(jFileChooser.getSelectedFile(), true);
      bufferedWriter = new BufferedWriter(output);
      bufferedWriter.write(date.toString()+"");
      bufferedWriter.newLine();
      bufferedWriter.write("TimeStamp,ECG,PPG");
      bufferedWriter.newLine();
    }
  }
  catch(Exception e) {
    println("File Not Found");
  }
}

// write new data record in log file
void logData(double ecg, double red, double ir) {
  try {
    date = new Date();
    dateFormat = new SimpleDateFormat("HH:mm:ss");
    bufferedWriter.write(dateFormat.format(date)+","+ecg+","+red+","+ir);
    bufferedWriter.newLine();
  }
  catch(IOException e) {
    println("It broke!!!");
    e.printStackTrace();
  }
}
