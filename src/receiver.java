
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.zip.CRC32;
/*
 * server
 * 1.create server and port
 * 2. prepare receiving container
 * 3. seal container into packet
 * 4. receive data 阻塞式接受
 * 5. analyze data
 * 6. release resource
 * 
 * 1. define variables: 
 * 2. assign arguments to corresponding variables
 * 3. create receiver socket and packet to receive data
 * 4. while loop: receive data and get header of this packet
	 * 5. check the checksum, if pkt NOT corrupted:
				 *  write to outputFile
				 *  create ACK packet and send to sender
				 *  check the fin, if fin = 1, break while
				 *  				else fin = 0 , continue
	 *     if pkt corrupted:
	 *     continue: wait another receive,阻塞式
 */
public class receiver {

	public static void main(String[] args) {
		//* 1. define variables: 
		int headerSz = 20;
		int segSz = 576;
		byte[] data;
		CRC32 checksum;
		int ackNum = 0;
		// 2. assign input argument in command line to variables
		if(args.length != 5){
			System.out.println("Receiver program must take 5 arguments!");
			return;
		}
		String filename = args[0]; 
		int listeningPort = Integer.parseInt(args[1]); //41194
		String senderIP = args[2];
		int senderACKPort  = Integer.parseInt(args[3]);//41191
		String logFilename = args[4];
		PrintWriter logWrite = null;
		PrintWriter fileWrite = null;
		try {
            fileWrite = new PrintWriter(filename);// write to file
            if(!logFilename.equals("stdout"))
				logWrite = new PrintWriter(logFilename);
		} catch (FileNotFoundException e) {
			System.out.println("unable to create file!");
			e.printStackTrace();
		}
		
		try{
			// * 3. create receiver socket and packet to receive data
			DatagramSocket receiver = new DatagramSocket(listeningPort);
			byte[] packetRcv = new byte[segSz];
			DatagramPacket packet = new DatagramPacket(packetRcv,packetRcv.length);
/*
 *  * 4. while loop: receive data and get header of this packet
 */
            while(true){
				//System.out.println("waiting for packet "+ ackNum);
            	receiver.receive(packet);
				//System.out.println("packet " + ackNum + " received" );
            	TCPHeader headerRcv = new TCPHeader(Arrays.copyOfRange(packetRcv,0,headerSz));
            	//System.out.println(headerRcv.createHeaderString());
            	data = Arrays.copyOfRange(packetRcv, headerSz,packet.getLength());
            	checksum = new CRC32();
            	checksum.update(data);
            	//print code for debug
//				System.out.println("headerRcv.getChecksum() : "+ (int)(headerRcv.getChecksum()));
//				System.out.println("checksum.getValue() : "+ (int)checksum.getValue());
//				System.out.println("headerRcv.getAckNum() : " + (int)headerRcv.getAckNum());
            	
				if(headerRcv.getChecksum() != (int)checksum.getValue() || ackNum != headerRcv.getSeqNum()){
            		continue;
            	}
				//System.out.println("packet " + headerRcv.getSeqNum() + " received" );
            	ackNum ++;
				// * create DatagramSocket and DatagramPacket for receiver to send packet acknowledgement
				InetSocketAddress senderInetAddr = new InetSocketAddress(senderIP,senderACKPort);
				DatagramPacket ACKpacket = new DatagramPacket(headerRcv.seqNum,headerRcv.seqNum.length ,senderInetAddr );
				//send ack back to sender, seqNumber is in ack packek
				receiver.send(ACKpacket);
				//write output file
				fileWrite.print(new String(data,0,data.length));
				//Write log file
				String logLine = "timestamp: " + getTimeStamp() + ", " + headerRcv.createHeaderString();
				if(logFilename.equals("stdout")){
					System.out.println(logLine);
				}
				else{
					logWrite.println(logLine);
				}
				
				if(headerRcv.getFin() == 1){
					System.out.println("Delivery completed successfully");
					break;
				}
            }
            fileWrite.close();
            if(!logFilename.equals("stdout"))
            	logWrite.close();
            receiver.close();
		}catch (Exception e) {
			e.printStackTrace(fileWrite);
			e.printStackTrace(logWrite);
		}
		
	}
	public static String getTimeStamp(){
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss.SSS a");
		String formattedDate = sdf.format(date);
		return formattedDate ; 
	}

}
