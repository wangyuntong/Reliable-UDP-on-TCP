/*
 * sender
 * 1. create client and port
 * 2. prepare data: reads data from a file
 * 3. seal data as packet and define data receive IP Address and port number
 * 4. send
 * 5. release resource
 * 
 * 1. define variables: size, retransmissionTimes, data
 * 2. assign input argument in command line to variables
 * 3. read input file and open log file outputStream and cal how many pkts needed
 * 4. for loop with pkt number: 
	 * 5. fill the pkt header: source, dest, seq, ack, fin, window, cksum, dataPointer
	 * 6. fill the data body: last one and the rest different
	 * 7. combine header and body into packet
	 * 8. create DatagramSocket and DatagramPacket for sender to send packet
	 * 9. create DatagramSocket and DatagramPacket for sender to receive ACK packet
	 * 10. if receive ack, which indicates successfully transmission, 
	 *      	print log file line corresponding to this packet & if it's the last packet, system.out.println
	 *     else if timeout for this ACK socket
	 *     		retransmit and return to step 9
 */

//when window size is bigger than the file size , will run incorrectly????

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.zip.CRC32;

public class sender {
	public static void main(String[] args) throws FileNotFoundException, SocketException {
		// 1. define variables: size, retransmissionTimes, data
		int headerSz = 20;
		int segSz = 576;
		int dataSz = segSz - headerSz;
		int segRetransmitted = 0;
		int totalByteSent = 0;
		String remoteReceiverIP = "localhost";
		int fin = 0;
        long timeReceived;// to evaluate RTT
        //int timeout = 1000;
        int ackNum = 0;
        boolean isTimeout = false;
        int winLeft = 0; //the left edge of window
        int EstimatedRTT = 50; // Initialize estimated RTT with 50
        int DevRTT = 10; // Initialize deviation of RTT with 10
        int TimeoutInterval = EstimatedRTT + 4 * DevRTT; // Initialize timeout interval with 320
        int SampleRTT = 0;
        double alpha = 0.125;
        double beta = 0.25;
		// 2. assign input argument in command line to variables
		if(args.length < 5 ){
			System.out.println("Sender program must take at least 5 arguments!");
			return;
		}
		String filename = args[0]; 
		remoteReceiverIP = args[1];
		int remoteReceiverPort = Integer.parseInt(args[2]);//41194
		int senderACKPort  = Integer.parseInt(args[3]);//41191
		int senderPort = 20002;
		String logFilename = args[4];
		PrintWriter logWrite = null;
        if(!logFilename.equals("stdout"))
        	logWrite = new PrintWriter(logFilename);
		int winSz = Integer.parseInt(args[5]);
		try{
			// * 3. read input file and open log file outputStream and cal how many pkts needed
			File inFile = new File(filename);
			FileInputStream input = new FileInputStream(inFile);
			int totalByteInFile = (int) inFile.length();
			byte[] wholeFile = new byte[totalByteInFile];
			input.read(wholeFile);
			int totalPktNum = (totalByteInFile/dataSz) + 1 ;
			//DatagramSocket senderSocket = new DatagramSocket(senderACKPort);
			DatagramSocket senderSocket = new DatagramSocket(senderPort);
			DatagramSocket senderACKSocket = new DatagramSocket(senderACKPort);
			InetSocketAddress receiverInetAddr = new InetSocketAddress(remoteReceiverIP,remoteReceiverPort);
			TCPHeader[] headerGroup = new TCPHeader[totalPktNum];
	        long[] timeSent = new long[totalPktNum];// to evaluate RTT

			// * 4. for loop with pkt number: 
			int i = 0;
			byte[] data;
				while(winLeft < totalPktNum )
				{
					//send the whole window, if timeout or it's the first window
					if(isTimeout || winLeft == 0){
						isTimeout = false; //reset isTimeout
						for( i = winLeft ; i < winLeft + winSz && i < totalPktNum ; i ++ ) {
							// * 6. fill the data body: last one and the rest different
							if (i == totalPktNum - 1){
								data = new byte[totalByteInFile - i*dataSz];
								data = Arrays.copyOfRange(wholeFile,i*dataSz,totalByteInFile);
								fin = 1;
							}
							else{
								data = new byte[dataSz];
								data = Arrays.copyOfRange(wholeFile,i*dataSz,i*dataSz + dataSz);
								fin = 0;
							}
				            CRC32 checksum = new CRC32();
				            checksum.update(data);
				            int checksumValue = (int) checksum.getValue();
				            //print code for debugging
				            //System.out.println("checksumValue : " + checksumValue);
				            
							// * 5. fill the pkt header: source, dest, seq, ack, fin, window, cksum, dataPointer
				            headerGroup[i]  = new TCPHeader(senderPort, remoteReceiverPort, i, winLeft, 
														headerSz, fin, winSz, checksumValue  );
			            	//System.out.println( headerGroup[i].createHeaderString());
							// * 7. combine header and body into packet
							byte[] bytePacket = new byte[headerSz + data.length];
							byte[] headerByte = new byte[headerSz];
							headerByte = headerGroup[i] .createByteArray();
							bytePacket = combine(bytePacket, headerByte,data);
							
							// * 8. create DatagramSocket and DatagramPacket for sender to send packet
							DatagramPacket packet = new DatagramPacket(bytePacket, bytePacket.length,receiverInetAddr );
							// * send the packet
							senderSocket.send(packet);
							//System.out.println("packet: "+ i + " sent");
							totalByteSent+=  packet.getLength(); // add length of current packet onto total byte sent length
							timeSent[i] = System.currentTimeMillis();
						}
						senderACKSocket.setSoTimeout((int) TimeoutInterval);
						//System.out.println("packet: "+ winLeft + " to " + i + " sent");

					}
					//send a single packet. this packet is the first pkt in current window
					else if(winLeft + winSz - 1 < totalPktNum)
					{
						i = winLeft + winSz - 1;
						if (i == totalPktNum - 1){
							data = new byte[totalByteInFile - i*dataSz];
							data = Arrays.copyOfRange(wholeFile,i*dataSz,totalByteInFile);
							fin = 1;
						}
						else{
							data = new byte[dataSz];
							data = Arrays.copyOfRange(wholeFile,i*dataSz,i*dataSz + dataSz);
							fin = 0;
						}
			            CRC32 checksum = new CRC32();
			            checksum.update(data);
			            int checksumValue = (int) checksum.getValue();
			            //print code for debugging
			            //System.out.println("checksumValue : " + checksumValue);
			            
						// * 5. fill the pkt header: source, dest, seq, ack, fin, window, cksum, dataPointer
						headerGroup[i]  = new TCPHeader(senderPort, remoteReceiverPort, i, winLeft, 
													headerSz, fin, winSz, checksumValue  );
		            	//System.out.println( headerGroup[i].createHeaderString());
						// * 7. combine header and body into packet
						byte[] bytePacket = new byte[headerSz + data.length];
						byte[] headerByte = new byte[headerSz];
						headerByte = headerGroup[i].createByteArray();
						bytePacket = combine(bytePacket, headerByte,data);
						
						// * 8. create DatagramSocket and DatagramPacket for sender to send packet
						DatagramPacket packet = new DatagramPacket(bytePacket, bytePacket.length,receiverInetAddr );
						// * send the packet
						senderSocket.send(packet);
						//System.out.println("packet: "+ i + " sent");
						totalByteSent+=  packet.getLength(); // add length of current packet onto total byte sent length
						//senderSocket.setSoTimeout(timeout);
						timeSent[i] = System.currentTimeMillis();
					}

					
					try{
							// * 9. create DatagramSocket and DatagramPacket for sender to receive ACK packet
							byte[] ACKcontainer = new byte[segSz];
							DatagramPacket ACKpacket = new DatagramPacket(ACKcontainer,ACKcontainer.length);
							// wait to receive ack using the same socket as before send
						
							//System.out.println("waiting for ACK of Packet : " + winLeft);
							do{
							senderACKSocket.receive(ACKpacket);
							//senderSocket.receive(ACKpacket);
							ackNum = byteArrayToInt(ACKcontainer);
							}
							while(ackNum != winLeft);
							//System.out.println("ack " + ackNum +" received");
							timeReceived = System.currentTimeMillis();
							// Estimate RTT using sampleRTT , estimate timeout interval using RTT and DevRTT
							SampleRTT = (int) (timeReceived - timeSent[ackNum]);							
							EstimatedRTT = (int) (EstimatedRTT * ( 1.0 - alpha ) +  SampleRTT * alpha) ;
							DevRTT = (int) (DevRTT * ( 1 - beta ) + Math.abs(SampleRTT - EstimatedRTT) * beta) ;
							TimeoutInterval = EstimatedRTT + 4 * DevRTT; 
							// system print for debug
//							System.out.println("SampleRTT : " + SampleRTT);
//							System.out.println("EstimatedRTT : " + EstimatedRTT);
//							System.out.println("DevRTT : " + DevRTT);
//							System.out.println("Timeout Interval : " + TimeoutInterval);

							//create log line
							String logLine = "timestamp: " + getTimeStamp() + ", " + headerGroup[ackNum].createHeaderString() + 
									" Estimated RTT: " + EstimatedRTT + " ms";
							// * 10. if receive ack, which indicates successfully transmission, 
							// *      	print log file line corresponding to this packet & if it's the last packet, system.out.println
							// *     else if timeout for this ACK socket
							// *     		retransmit and return to step 9
							if(logFilename.equals("stdout")){
								System.out.println(logLine);
							}
							else{
								logWrite.println(logLine);
							}
							if( winLeft == totalPktNum -1 ){
								System.out.println("Delivery completed successfully");
								System.out.println("Total bytes sent = " + totalByteSent);
								System.out.println("Segments sent = " + totalPktNum);
								System.out.println("Segments retransmitted = " + segRetransmitted);
								break;
							}
							winLeft ++ ;
							isTimeout = false; 
							continue;
							// when transmission done

						}catch (SocketTimeoutException e){
							segRetransmitted += winSz;
							isTimeout = true; 	}
				}// end while
	
			//end for
			input.close();
			if(!logFilename.equals("stdout"))
				logWrite.close();
			senderSocket.close();	
		}
	catch (FileNotFoundException  e){
		System.out.println("file not found!");
		e.printStackTrace();}
	catch (IOException  e1){
			e1.printStackTrace();}
	}//end main

	//function combine takes 3 arguments: b, b1, b2. return b which combine b1 and b2
	public static byte[] combine(byte[]b ,byte[] b1, byte[] b2){
		int i = 0 ;
		for (i = 0 ; i < b1.length; i++){
			b[i] = b1[i];
		}
		for (i = b1.length ; i < b1.length + b2.length ; i++){
			b[i] = b2[i-b1.length];
		}
		return b;
	}
	public static String getTimeStamp(){
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss.SSS a");
		String formattedDate = sdf.format(date);
		return formattedDate ; 
	}
    // to print RTT only to certain # of decimal places
    public static double truncateDouble(double n, int numDigits) {
        double result = n;
        String aswr = "" + n;
        int i = aswr.indexOf('.');
        if (i != -1) {
            if (aswr.length() > i + numDigits) {
                aswr = aswr.substring(0, i + numDigits + 1);
                result = Double.parseDouble(aswr);
            }
        }
        return result;
    }
    public static int byteArrayToInt(byte[] b) {
        return b[3] & 0xFF
                | (b[2] & 0xFF) << 8
                | (b[1] & 0xFF) << 16
                | (b[0] & 0xFF) << 24;
    }
}

