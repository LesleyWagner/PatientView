import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.serial.*; 
import grafica.*; 
import java.awt.*; 
import javax.swing.*; 
import static javax.swing.JOptionPane.*; 
import javax.swing.JFileChooser; 
import java.io.FileWriter; 
import java.io.BufferedWriter; 
import java.util.*; 
import java.text.DateFormat; 
import java.text.SimpleDateFormat; 
import java.math.*; 
import controlP5.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class serialUSB_test extends PApplet {

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

                  // Serial Library


// Java Swing Package For prompting message




// File Packages to record the data into a text file




// Date Format




// General Java Package



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

GPlot plotSerial;

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
int index = 0; // index for draw function
FloatList ecgTestDataSync; // Synchronized ecg test data with the other data in 125 Hz
FloatList ppgTestData;
FloatList respTestData;
int testDataIndex = 0;
java.util.Timer testTimer;

/************** Plotting Variables (Lesley) **********************/
GPointsArray pointsSerial;

/************** Data Variables (Lesley) **********************/
int contentIndex = 0; // Index for data buffer arrays 
int plotIndex = 0;
Float[] testBufferSerial;
int bufferSize = 100;

int nPoints1 = pSize;
int totalPlotsHeight=0;
int totalPlotsWidth=0;
int heightHeader=100;
int updateCounter=0;

boolean is_raspberrypi=false;

int global_hr;
int global_rr;
float global_temp;
int global_spo2;

int global_test=0;

boolean ECG_leadOff,spo2_leadOff;
boolean ShowWarning = true;
boolean ShowWarningSpo2=true;

public void setup() {    
  testBufferSerial = new Float[bufferSize];
  pointsSerial = new GPointsArray(nPoints1);
  
  
  //fullScreen();
   
  // ch
  heightHeader=100;
  println("Height:"+height);

  totalPlotsHeight=height-heightHeader;
  
  makeGUI();
  surface.setTitle("Protocentral OpenView");
  
  plotSerial = new GPlot(this);
  plotSerial.setPos(20,60);
  plotSerial.setDim(width-40, (totalPlotsHeight/3)-10);
  plotSerial.setBgColor(0);
  plotSerial.setBoxBgColor(0);
  plotSerial.setLineColor(color(0, 255, 0));
  plotSerial.setLineWidth(3);
  plotSerial.setMar(0,0,0,0);

  for (int i = 0; i < nPoints1; i++) {
    pointsSerial.add(i,0);
  }

  plotSerial.setPoints(pointsSerial);


  /*******  Initializing zero for buffer ****************/

  for (int i=0; i<nPoints1; i++) {
    time = time + 1;
    xdata[i]=time;
    ecgdata[i] = 0;
    reddata[i] = 0;
    ppgArray[i] = 0;
  }
  time = 0;
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
           
      cp5.addScrollableList("Select Serial port")
         .setPosition(250, 5)
         .setSize(250, 100)
         .setFont(createFont("Arial",12))
         .setBarHeight(50)
         .setItemHeight(40)
         .addItems(port.list())
         .setType(ScrollableList.DROPDOWN) // currently supported DROPDOWN and LIST
         .addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent event) {
              if (event.getAction() == ControlP5.ACTION_RELEASED) {
                startSerial(event.getController().getLabel(),115200);
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

// test plots with arbitrary lists of values by Lesley Wagner
public void draw() {
  // background(0);
  
  if (startPlot) {                            // If the condition is true, then the plotting is done  
    plotIndex = contentIndex;
    
    for (int i=plotIndex-1; i>=0; i--) {
      pointsSerial.remove(i);
    }
    
    for (int i=0; i < pSize-plotIndex; i++) {
      pointsSerial.setX(i, i);
    }
    
    for (int i=0; i < plotIndex; i++) {
      pointsSerial.add(pSize-plotIndex+i, pSize-plotIndex+i, testBufferSerial[i]);
    }
    
    background(19,75,102);
  
    plotSerial.setPoints(pointsSerial);
  
    plotSerial.beginDraw();
    plotSerial.drawBackground();
    plotSerial.drawLines();
    plotSerial.endDraw();
      
    contentIndex = 0;
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
public void startSerial(String startPortName, int baud) {
  try {
      port = new Serial(this,startPortName, baud);
      port.clear();
      startPlot = true;
  }
  catch(Exception e) {

    showMessageDialog(null, "Port is busy", "Alert", ERROR_MESSAGE);
    System.exit (0);
  }
}

public void serialEvent (Serial blePort)  {
  inString = blePort.readChar();
  ecsProcessData(inString);
}

public void ecsProcessData(char rxch) {
  testBufferSerial[contentIndex] = (float)rxch;
  contentIndex++;
}
  public void settings() {  size(800, 600, JAVA2D); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "serialUSB_test" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}