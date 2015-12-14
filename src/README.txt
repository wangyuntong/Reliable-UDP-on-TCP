This program implements a TCP-like transportation-layer protocol based on UDP. Here are the features of it:

(a) the TCP segment structure used 

The protocol uses segment structure of 576 bytes when sending the segment from sender. Header 20 bytes and data 556 bytes. The structure is like this, with each line represents 32 bits. And the protocol only sends acknowledgement number without using TCP header back to sender since this road is considered reliable. 

 ————————————————————————————————————————-
|    Source port #  |     Dest. Port #    |
 ————————————————————————————————————————-
|     	        Sequence Number 	  |
 ————————————————————————————————————————-
|         Acknowledgement Number	  |
 ————————————————————————————————————————-
| header_L |   fin  |   Window Size       | 
—————————————————————————————————————————-
|            Internet Checksum		  |
——————————————————————————————————————————
|    	   				  | 
|    	   				  | 
|    	   				  | 
|    	          Data		          | 
|    	   				  | 
|    	   				  | 
|    	   				  | 
———————————————————————————————————————————

Note:
1. header_L: 4 bits. header length is fixed to 20
2. fin:  4 bits. We only use fin flag here and 		ignore other flags. Because we can realize required 	function only using fin.
3. Window size is variable.
4. I use CRC32 as internet checksum here. For it’s more robust than simply doing sum. Although it will take up 32 bits space, we don’t need to use urgent data pointer in this protocol.

(b) the states typically visited by a sender and receiver

Sender has 3 states:
1. Sending state 1: When sender’s ACK receiver socket timeout or it’s sending the first window of segments. It sends the whole window packets to receiver. The window size is variable according to your settings in command line.
2. Sending state 2: When sender receives a fine packet without error and is what it’s expecting. Then the receiver will right shift its window by 1 and send a single new packet. This new packet is the last packet in current shifted window.
3. Receiving ACK state: After sending packets, the sender comes to the state that it waits for ACK from receiver. If receive process timeout, it will step to sending state 1. If receive success and packet correct, it will step to sending state 2. After receiving all of the ACKs, the sender will print log information into logger file ad print the process information required.

Receiver:

Receiving packet state: At first, the receiver will expect packet with sequence number 0. And every time it receives the correct and expected packet from sender, it will increment its expecting number by 1 and sends ACK of the packet just received. If it receives wrong packet or unexpected packet, it will continue wait for another packet to come and do nothing.
 
(c) the loss recovery mechanism; and a description of whatever is unusual about your implementation, such as additional features or a list of any known bugs.

There are recovery mechanisms for different disruptions :
1. delay: sender detects delay by throw socket timeout exception to program. Then sender will retransmit all the packets in the current window.
2. bit error: Sender will calculate the checksum for each packet and save checksum in the packet’s header. When receiver receives this packet, it will recalculate the checksum from data domain and compare this checksum with the checksum in the packet’s header. If they are the same, then the data in the packet occurs no bit error. So the receiver sends an ACK for this packet to sender. If they are different, then there is bit error in this packet, the receiver will abandon this packet and wait for another packet to come. After a while, the sender detects a timeout, it will retransmit the packets.
3. out of order and packet loss: when receiver receives packet that is not what it expected, which indicates out-of-order or packet loss, it will abandon this packet and continue waiting for another packet until it receives the correct and expected one. If this happens, the sender will detect a timeout, and it will retransmit the packets.
 