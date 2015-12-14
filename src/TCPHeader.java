
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class TCPHeader {
	// The source port for this TCP this.
	byte[] sourcePort = new byte[2];
	// The destination port for this TCP this.
	byte[] destPort = new byte[2];
	// The sequence number for this TCP this.
	byte[] seqNum = new byte[4];
	// The acknowledgement number for this TCP this.
	byte[] ackNum = new byte[4];
	// The length for this TCP header, default as 20
	byte headerLen = (byte) (20 & 0xFF);
	// The fin flag for this TCP this.
	byte fin;
	// The window size for this header
	byte[] winSz = new byte[2];
	// The checksum for this TCP this.
	byte[] checksum = new byte[4];
	
    // Method to convert integer to byte array length=2
    public byte[] intTo2ByteArray(int a) {
        byte[] arr = new byte[2];
        arr[1] = (byte) (a & 0xFF);
        arr[0] = (byte) ((a >> 8) & 0xFF);
        return arr;
    }

    // Method to convert integer to byte array length=4
    public byte[] intTo4ByteArray(int a) {
        byte[] arr = new byte[4];
        arr[3] = (byte) (a & 0xFF);
        arr[2] = (byte) ((a >> 8) & 0xFF);
        arr[1] = (byte) ((a >> 16) & 0xFF);
        arr[0] = (byte) ((a >> 24) & 0xFF);
        return arr;
    }

    // method to convert byte array length 2 to integer
    public int twoByteArrayToInt(byte[] b) {
        return b[1] & 0xFF
                | (b[0] & 0xFF) << 8;
    }

    // method to convert byte array length 4 to integer
    public int fourByteArrayToInt(byte[] b) {
        return b[3] & 0xFF
                | (b[2] & 0xFF) << 8
                | (b[1] & 0xFF) << 16
                | (b[0] & 0xFF) << 24;
    }
    
	public TCPHeader(	int sourcePort, int destPort, 	int seqNum, 
					int ackNum,		int headerLen, 	int fin, 
					int winSz, 		int checksum 	){
		this.sourcePort = intTo2ByteArray(sourcePort);
		this.destPort = intTo2ByteArray(destPort);
		this.seqNum = intTo4ByteArray(seqNum);
		this.ackNum = intTo4ByteArray(ackNum);
		this.headerLen = (byte) (headerLen & 0xFF);
		this.fin = (byte) (fin & 0xFF);
		this.winSz  = intTo2ByteArray(winSz);
		this.checksum = intTo4ByteArray(checksum);
	}
	public TCPHeader(	byte[] headerByte	){
		this.sourcePort = Arrays.copyOfRange(headerByte,0,2);
		this.destPort = Arrays.copyOfRange(headerByte,2,4);
		this.seqNum = Arrays.copyOfRange(headerByte,4,8);
		this.ackNum = Arrays.copyOfRange(headerByte,8,12);
		this.headerLen = Arrays.copyOfRange(headerByte,12,13)[0];
		this.fin = Arrays.copyOfRange(headerByte,13,14)[0];
		this.winSz  = Arrays.copyOfRange(headerByte,14,16);
		this.checksum = Arrays.copyOfRange(headerByte,16,20);
	}
	
	public byte[] createByteArray () {
		byte[] byteArray = new byte[headerLen];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(sourcePort);
            outputStream.write(destPort);
            outputStream.write(seqNum);
            outputStream.write(ackNum);
            outputStream.write(headerLen);
            outputStream.write(fin);
            outputStream.write(winSz);
            outputStream.write(checksum);
            byteArray = outputStream.toByteArray();
        } catch (IOException e) {
            e.getMessage();
        }
		return byteArray;
	}
	
	public String createHeaderString(){
        String sourcePort = (Integer.toString(twoByteArrayToInt(this.sourcePort)));
        String destPort = (Integer.toString(twoByteArrayToInt(this.destPort)));
        String seqNum = (Integer.toString(fourByteArrayToInt(this.seqNum)));
        String ackNum = (Integer.toString(fourByteArrayToInt(this.ackNum)));
        String fin = (Integer.toString((int)this.fin & 0xFF));
//        String headerLen = (Integer.toString((int)this.headerLen & 0xFF));
//        String winSz = (Integer.toString(twoByteArrayToInt(this.winSz)));
//        String checksum = (Integer.toString(twoByteArrayToInt(this.checksum)));
        
		String headerString = new String ("source: " +sourcePort +", destination: " +  destPort +", Sequence # : " + seqNum +", ACK # : " + ackNum + ", fin: " +fin) ;
		return headerString;
	}
	
	public int getSeqNum (){
		return fourByteArrayToInt(this.seqNum);
	}
	
	public int getSourcePort (){
		return twoByteArrayToInt(this.sourcePort);
	}
	
	public int getDestPort (){
		return twoByteArrayToInt(this.destPort);
	}
	public int getAckNum (){
		return fourByteArrayToInt(this.ackNum);
	}
	public int getFin (){
		return (int)this.fin & 0xFF;
	}
	public int getChecksum (){
		return fourByteArrayToInt(this.checksum);
	}	

}
