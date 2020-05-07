/**
 * 
 */
package patientviewrpi;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.Timer;

import processing.core.PApplet;
import processing.data.IntList;
import processing.data.Table;
import processing.data.TableRow;

/**
 * @author lesley wagner
 *
 */
public class TestDataStreamer {
	// Store all test data from data files
	IntList ecgTestDataSync; // Synchronized ecg test data with the other data in 125 Hz
	IntList ppgTestData;
	IntList tempTestDataSync;
	IntList hrTestData;
	IntList spo2TestData;
	
	// Buffer test data with new data from the lists every 8 ms.
	int[] dataBufferTestRed;
	int[] dataBufferTestIR;
	int[] dataBufferTestTemperature;
	int[] dataBufferTestHr;
	int[] dataBufferTestSpo2;
	
	final int testBufferSize = 1000;
	final int testDataLength = 8;
	int testDataIndex = 0;
	int hrFrequencyIndex = 0;
	int contentIndex = 0;
	int currentIndex = 0;
	int prevIndex = 0;
	
	Timer testTimer;
	long delay = 1000L;
	long period = 8L;
	
	/**
	 * testDataStreamer constructor
	 */
	public TestDataStreamer() {
		dataBufferTestRed = new int[testBufferSize];
		dataBufferTestIR = new int[testBufferSize];
		dataBufferTestTemperature = new int[testBufferSize];
		dataBufferTestHr = new int[testBufferSize];
		dataBufferTestSpo2 = new int[testBufferSize];
		
		for (int i=0; i<testBufferSize; i++) {
			dataBufferTestRed[i] = 0;
			dataBufferTestIR[i] = 0;
			dataBufferTestTemperature[i] = 0;
			dataBufferTestHr[i] = 0;
			dataBufferTestSpo2[i] = 0;
		}
		
		readTestData();
	}

	/** 
	 * Start streaming test data.
	 */
	public void start() {		
		// update test data every 8 milliseconds
		testTimer = new java.util.Timer("timer");
		testTimer.scheduleAtFixedRate(updateData, delay, period);
	}
	
	/** 
	 * Stop streaming test data.
	 */
	public void stop() {
		testTimer.cancel();
	}
	
	/**
	 * get new data points
	 * 
	 * @returns list of integer arrays with new data points
	 */
	public ArrayList<int[]> getDataPoints() {
		ArrayList<int[]> newData = new ArrayList<int[]>();
		int nNewPoints = 0;

		currentIndex = contentIndex;
		if (currentIndex > prevIndex) {
			nNewPoints = currentIndex - prevIndex;;
		}
		else {
			nNewPoints = currentIndex + testBufferSize - prevIndex;
		}

//		// remove n points at the beginning of the plot
//		for (int i=nNewPoints-1; i >= 0; i--) {
//			pointsRed.remove(i);
//			pointsIR.remove(i);
//		}
//
//		// move plot to the left
//		for (int i=0; i < nPoints-nNewPoints; i++) {
//			pointsRed.setX(i, i);
//			pointsIR.setX(i, i);
//		}

		// add n new points
		int bufferIndex = prevIndex;
		for (int i=0; i < nNewPoints; i++) {      
			if (bufferIndex == testBufferSize) {
				bufferIndex = 0;
			}
			int red = dataBufferTestRed[bufferIndex];
			int ir = dataBufferTestIR[bufferIndex];
			int hr = dataBufferTestHr[bufferIndex];
			int spo2 = dataBufferTestSpo2[bufferIndex];
			int temp = dataBufferTestTemperature[bufferIndex];   

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
		prevIndex = currentIndex;
		return newData;
	}
	
	/**
	 * Update data with new point from test data. Runs every 8 ms.
	 */
	private TimerTask updateData = new TimerTask() {
		public void run() {
			// clear buffers and move recently added data to the beginning of the buffers
			dataBufferTestRed[contentIndex] = ecgTestDataSync.get(testDataIndex);
			dataBufferTestIR[contentIndex] = ppgTestData.get(testDataIndex);   
			dataBufferTestTemperature[contentIndex] = tempTestDataSync.get(hrFrequencyIndex);
			dataBufferTestHr[contentIndex] = hrTestData.get(hrFrequencyIndex);
			dataBufferTestSpo2[contentIndex] = spo2TestData.get(hrFrequencyIndex);
			contentIndex++;      
			if (contentIndex == testBufferSize) {
				contentIndex = 0;
			}
			if ((testDataIndex % 125) == 0) {
				hrFrequencyIndex++;
			}
			testDataIndex++;
		}
	};
	
	/**
	 * read test data from data files.
	 */
	private void readTestData() {
		// ecg test values
		try (InputStream ecgInput = new FileInputStream(new File(getClass().getResource("/ecg_testdata.csv").toURI()))) {
			String[] ecgFileContents = PApplet.loadStrings(ecgInput); // contents of ecg data file
			String[] ecgTestDataStrings = ecgFileContents[0].split(",");
			IntList ecgTestData = new IntList();
			for (int i = 0; i < ecgTestDataStrings.length; i++) {
				String formattedString = ecgTestDataStrings[i];
				while (!formattedString.matches("\\-?0\\.\\d{3}")) {
					formattedString += "0";
				}
				ecgTestData.append(PApplet.parseInt(formattedString.replaceAll("0\\.0*", "")));
			}
			ecgTestDataSync = new IntList();
			// take every fourth entry of ecg test data at 500 Hzto synchronize it with the other data at 125 Hz
			for (int i = 0; i < ecgTestData.size(); i++) {
				if ((i % 4) == 0) {
					ecgTestDataSync.append(ecgTestData.get(i));
				}
			}
		}
		catch (Exception e){
			e.printStackTrace();
			System.out.println("ECG file not found.");
		}

		// ppg test values
		try (InputStream ppgInput = new FileInputStream(new File(getClass().getResource("/bidmc_01_Signals.csv").toURI()))) {
			Table ppgTestTable = new Table (ppgInput, "csv, header");
			ppgTestData = new IntList();  
			for (TableRow row : ppgTestTable.rows()) {    
				String formattedString = row.getString(2).replaceAll("0\\.0*", "");
				int parsedInt = PApplet.parseInt(formattedString);
				while (parsedInt < 10000) {
					parsedInt *= 10;
				}
				parsedInt >>= 2; // make the value fit in two bytes
				ppgTestData.append(parsedInt);
			}
		}
		catch (Exception e){
			e.printStackTrace();
			System.out.println("PPG file not found.");
		}

		// temp test values 
		try (InputStream tempInput = new FileInputStream(new File(getClass().getResource("/temperature_testdata.csv").toURI()))) {
			String[] tempFileContents = PApplet.loadStrings(tempInput); 
			String[] tempTestDataStrings = tempFileContents[0].split(",");
			IntList tempTestData = new IntList();
			for (int i = 0; i < tempTestDataStrings.length; i++) {
				String formattedString = tempTestDataStrings[i].replaceAll("(\\d{2})(\\.)(\\d{1})(\\d*)", "$1$3");
				tempTestData.append(PApplet.parseInt(formattedString));
			}
			tempTestDataSync = new IntList();
			// take every eight entry of temp test data at 8 Hz to synchronize with other data at 1 Hz
			for (int i = 0; i < tempTestData.size(); i++) {
				if ((i % 8) == 0) {
					tempTestDataSync.append(tempTestData.get(i));
				}
			}
		}
		catch (Exception e){
			e.printStackTrace();
			System.out.println("ECG file not found.");
		}

		// hr and spo2 test values
		try (InputStream hrInput = new FileInputStream(new File(getClass().getResource("/HR_SPO2_testdata.csv").toURI()))) {
			Table hrspo2TestTable = new Table(hrInput, "csv, header");
			hrTestData = new IntList();
			spo2TestData = new IntList();

			for (TableRow row : hrspo2TestTable.rows()) {
				spo2TestData.append(row.getInt(0));
				hrTestData.append(row.getInt(1));
			}
		}
		catch (Exception e){
			e.printStackTrace();
			System.out.println("PPG file not found.");
		}
	}
}
