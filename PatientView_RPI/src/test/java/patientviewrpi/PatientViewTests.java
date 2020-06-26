/**
 * 
 */
package patientviewrpi;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import grafica.GPointsArray;
import patientviewrpi.SensorDataReceiver.UartPacket;

/**
 * @author lesley wagner
 *
 * JUnit test cases for updateData, determineAlarms and encodeData methods.
 */
class PatientViewTests {
	
	PatientViewRPI patientView;
	
	LinkedList<Integer> dataBufferRed;
	LinkedList<Integer> dataBufferIR;
	LinkedList<Integer> dataBufferTemperature;
	LinkedList<Integer> dataBufferHr;
	LinkedList<Integer> dataBufferSpo2;
	GPointsArray pointsRed;
	GPointsArray pointsIR;
	
	final int nPoints = 250; // number of plot points in patientview
	final int bufferSize = 100;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		patientView = new PatientViewRPI();
		
		dataBufferRed = new LinkedList<Integer> ();
		dataBufferIR = new LinkedList<Integer> ();
		dataBufferTemperature = new LinkedList<Integer> ();
		dataBufferHr = new LinkedList<Integer> ();
		dataBufferSpo2 = new LinkedList<Integer> ();
		pointsRed = new GPointsArray(nPoints);
		pointsIR = new GPointsArray(nPoints);
		
		for (int i = 0; i < nPoints; i++) {
			pointsIR.add(i, 0);
			pointsRed.add(i, 0);
		}
		
		for (int i = 0; i < bufferSize; i++) {
			dataBufferRed.push(0);
			dataBufferIR.push(0);
			dataBufferTemperature.push(0);
			dataBufferHr.push(0);
			dataBufferSpo2.push(0);
		}
		
		patientView.dataBufferRed = dataBufferRed;
		patientView.dataBufferIR = dataBufferIR;
		patientView.dataBufferTemperature = dataBufferTemperature;
		patientView.dataBufferHr = dataBufferHr;
		patientView.dataBufferSpo2 = dataBufferSpo2;
		patientView.pointsRed = pointsRed;
		patientView.pointsIR = pointsIR;
	}
	
	/**
	 * TUD1 Empty list, no input data; 
	 * data buffers and plot points stay the same
	 */
	@Test
	void testUpdateDataEmpty() {						
		patientView.updateData(new ArrayList<UartPacket>());
		assertEquals(dataBufferRed, patientView.dataBufferRed);
		assertEquals(dataBufferIR, patientView.dataBufferIR);
		assertEquals(dataBufferTemperature, patientView.dataBufferTemperature);
		assertEquals(dataBufferHr, patientView.dataBufferHr);
		assertEquals(dataBufferSpo2, patientView.dataBufferSpo2);
		assertEquals(pointsRed, patientView.pointsRed);
		assertEquals(pointsIR, patientView.pointsIR);
	}
	
	/**
	 * TUD2 list with one data packet; 
	 * data points get added to data buffers and plot points
	 */
	@Test
	void testUpdateDataOne() {
		ArrayList<UartPacket> newData = new ArrayList<>();
		UartPacket packet = new UartPacket();
		packet.packetID = 1;
		packet.red = 1;
		packet.ir = 1;
		packet.hr = 1;
		packet.spo2 = 1;
		packet.temp = 1;
		newData.add(packet);
		
		patientView.updateData(newData);
		
		assertEquals(1, patientView.dataBufferRed.peekLast());
		assertEquals(1, patientView.dataBufferIR.peekLast());
		assertEquals(1, patientView.dataBufferHr.peekLast());
		assertEquals(1, patientView.dataBufferSpo2.peekLast());
		assertEquals(1, patientView.dataBufferTemperature.peekLast());
		assertEquals(1, patientView.pointsRed.getLastPoint().getY());
		assertEquals(1, patientView.pointsRed.getLastPoint().getY());
	}
	
	/**
	 * TUD3 list with three data packets; 
	 * data points get added to data buffers and plot points
	 */
	@Test
	void testUpdateDataThree() {
		ArrayList<UartPacket> newData = new ArrayList<>();		
		for (int i = 1; i <= 3; i++) {
			UartPacket packet = new UartPacket();
			packet.packetID = i;
			packet.red = i;
			packet.ir = i;
			packet.hr = i;
			packet.spo2 = i;
			packet.temp = i;
			newData.add(packet);
		}
		patientView.updateData(newData);
		
		for (int i = 1; i <= 3; i++) {
			assertEquals(i, patientView.dataBufferRed.get(bufferSize-4+i));
			assertEquals(i, patientView.dataBufferIR.get(bufferSize-4+i));
			assertEquals(i, patientView.dataBufferHr.get(bufferSize-4+i));
			assertEquals(i, patientView.dataBufferSpo2.get(bufferSize-4+i));
			assertEquals(i, patientView.dataBufferTemperature.get(bufferSize-4+i));
			assertEquals(i, patientView.pointsRed.getY(nPoints-4+i));
			assertEquals(i, patientView.pointsRed.getY(nPoints-4+i));
		}
	}
	
	/**
	 * TDA1 every data point is 0
	 */
	@Test
	void testDetermineAlarmsZero() {						
		assertEquals(0b01101101, patientView.determineAlarms());
	}
	
	/**
	 * TDA2 every data point is maximum
	 */
	@Test
	void testDetermineAlarmsMax() {
		for (int i = 0; i < bufferSize; i++) {
			dataBufferTemperature.set(i, 65535);
			dataBufferHr.set(i, 255);
			dataBufferSpo2.set(i, 255);
		}
		
		patientView.dataBufferTemperature = dataBufferTemperature;
		patientView.dataBufferHr = dataBufferHr;
		patientView.dataBufferSpo2 = dataBufferSpo2;
		
		assertEquals(0b00010010, patientView.determineAlarms());
	}
	
	/**
	 * TDA3 last 2 points in every buffer are 0, 
	 * the rest of the data points are within normal range
	 */
	@Test
	void testDetermineAlarmsTDA3() {	
		for (int i = 0; i < bufferSize-2; i++) {
			dataBufferTemperature.set(i, 370);
			dataBufferHr.set(i, 120);
			dataBufferSpo2.set(i, 95);
		}
		
		for (int i = bufferSize-2; i < bufferSize; i++) {
			dataBufferTemperature.set(i, 370);
			dataBufferHr.set(i, 120);
			dataBufferSpo2.set(i, 95);
		}

		patientView.dataBufferTemperature = dataBufferTemperature;
		patientView.dataBufferHr = dataBufferHr;
		patientView.dataBufferSpo2 = dataBufferSpo2;
		
		assertEquals(0b00000000, patientView.determineAlarms());
	}
	
	/**
	 * TDA4 last 3 points in hr and spo2 buffers are 0, 
	 * the rest of the data points are within normal range
	 */
	@Test
	void testDetermineAlarmsTDA4() {	
		for (int i = 0; i < bufferSize-3; i++) {
			dataBufferTemperature.set(i, 370);
			dataBufferHr.set(i, 120);
			dataBufferSpo2.set(i, 95);
		}
		
		for (int i = bufferSize-3; i < bufferSize; i++) {
			dataBufferTemperature.set(i, 370);
			dataBufferHr.set(i, 0);
			dataBufferSpo2.set(i, 0);
		}

		patientView.dataBufferTemperature = dataBufferTemperature;
		patientView.dataBufferHr = dataBufferHr;
		patientView.dataBufferSpo2 = dataBufferSpo2;
		
		assertEquals(0b01001100, patientView.determineAlarms());
	}
	
	/**
	 * TDA5 last 3 points in hr buffer are 0, 
	 * last 3 points in temperature buffer are low, 
	 * the rest of the data points are within normal range
	 */
	@Test
	void testDetermineAlarmsTDA5() {	
		for (int i = 0; i < bufferSize-3; i++) {
			dataBufferTemperature.set(i, 370);
			dataBufferHr.set(i, 120);
			dataBufferSpo2.set(i, 95);
		}
		
		for (int i = bufferSize-3; i < bufferSize; i++) {
			dataBufferTemperature.set(i, 330);
			dataBufferHr.set(i, 0);
			dataBufferSpo2.set(i, 99);
		}

		patientView.dataBufferTemperature = dataBufferTemperature;
		patientView.dataBufferHr = dataBufferHr;
		patientView.dataBufferSpo2 = dataBufferSpo2;
		
		assertEquals(0b00001001, patientView.determineAlarms());
	}
	
	/**
	 * TDA6 last 3 points in temperature buffer are high, 
	 * last 3 points in spo2 buffer are low,
	 * last 3 points in hr buffer are high, 
	 * the rest of the data points are within normal range
	 */
	@Test
	void testDetermineAlarmsTDA6() {	
		for (int i = 0; i < bufferSize-3; i++) {
			dataBufferTemperature.set(i, 370);
			dataBufferHr.set(i, 120);
			dataBufferSpo2.set(i, 95);
		}
		
		for (int i = bufferSize-3; i < bufferSize; i++) {
			dataBufferTemperature.set(i, 410);
			dataBufferHr.set(i, 220);
			dataBufferSpo2.set(i, 80);
		}

		patientView.dataBufferTemperature = dataBufferTemperature;
		patientView.dataBufferHr = dataBufferHr;
		patientView.dataBufferSpo2 = dataBufferSpo2;
		
		assertEquals(0b00010110, patientView.determineAlarms());
	}
	
	/**
	 * TDA7 last 3 points in temperature buffer are 0, 
	 * last 3 points in spo2 buffer are 0,
	 * last 2 points in hr buffer are 0, 
	 * the rest of the data points are within normal range
	 */
	@Test
	void testDetermineAlarmsTDA7() {	
		for (int i = 0; i < bufferSize-3; i++) {
			dataBufferTemperature.set(i, 370);
			dataBufferHr.set(i, 120);
			dataBufferSpo2.set(i, 95);
		}
		
		dataBufferTemperature.set(bufferSize-3, 0);
		dataBufferHr.set(bufferSize-3, 120);
		dataBufferSpo2.set(bufferSize-3, 0);
		
		for (int i = bufferSize-2; i < bufferSize; i++) {
			dataBufferTemperature.set(i, 0);
			dataBufferHr.set(i, 0);
			dataBufferSpo2.set(i, 0);
		}

		patientView.dataBufferTemperature = dataBufferTemperature;
		patientView.dataBufferHr = dataBufferHr;
		patientView.dataBufferSpo2 = dataBufferSpo2;
		
		assertEquals(0b00100101, patientView.determineAlarms());
	}
	
	/**
	 * TED1 all data points are 0
	 */
	@Test
	void testEncodeDataTED1() {	
		ArrayList<UartPacket> newData = new ArrayList<>();		
		UartPacket packet = new UartPacket();
		packet.packetID = 1;
		packet.red = 0;
		packet.ir = 0;
		packet.hr = 0;
		packet.spo2 = 0;
		packet.temp = 0;
		newData.add(packet);
		byte alarms = 0b01101101;
		
		ArrayList<byte[]> encodedData = patientView.encodeData(newData, alarms);		
		byte[] wifiPacket = {1,1,1,1,1,1,1,2,1,1,1,1,1,1,1,1,2,109,0};

		assertArrayEquals(wifiPacket, encodedData.get(0));
	}
	
	/**
	 * TED2 First packet has data points all 0, 
	 * second packet has data points all maximum,
	 * third packet has data points all maximum except temperature = 0
	 */
	@Test
	void testEncodeDataTED2() {	
		ArrayList<UartPacket> newData = new ArrayList<>();		
		UartPacket packet1 = new UartPacket();
		packet1.packetID = 1;
		packet1.red = 0;
		packet1.ir = 0;
		packet1.hr = 0;
		packet1.spo2 = 0;
		packet1.temp = 0;
		newData.add(packet1);
		UartPacket packet2 = new UartPacket();
		packet2.packetID = 2;
		packet2.red = 65535;
		packet2.ir = 65535;
		packet2.hr = 255;
		packet2.spo2 = 255;
		packet2.temp = 65535;
		newData.add(packet2);
		UartPacket packet3 = new UartPacket();
		packet3.packetID = 3;
		packet3.red = 65535;
		packet3.ir = 65535;
		packet3.hr = 255;
		packet3.spo2 = 255;
		packet3.temp = 0;
		newData.add(packet3);
		byte alarms = 0b00000000;
		
		ArrayList<byte[]> encodedData = patientView.encodeData(newData, alarms);		
		byte[] wifiPacket1 = {1,1,1,1,1,1,1,2,1,1,1,1,1,1,1,1,1,1,0};
		byte[] wifiPacket2 = {1,1,1,1,1,1,1,10,2,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,1,0};
		byte[] wifiPacket3 = {1,1,1,1,1,1,1,8,3,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,1,1,1,0};

		assertArrayEquals(wifiPacket1, encodedData.get(0));
		assertArrayEquals(wifiPacket2, encodedData.get(1));
		assertArrayEquals(wifiPacket3, encodedData.get(2));
	}
}
