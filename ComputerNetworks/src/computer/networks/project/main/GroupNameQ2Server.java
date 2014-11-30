package computer.networks.project.main;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;

/*
 * Server class
 */
public class GroupNameQ2Server {

	private DatagramSocket socket;
	private DatagramPacket recvPacket;
	private DatagramPacket sendPacket;
	private String ackMsg;
	private int timeoutValueSetByClient;
	private int byteSizeBeingSentByClient;
	private int ackNo = 0;
	private int NBE = 0;
	private int E = 0;
	private boolean ackRecvd; // boolean value that determines if ack has been
	// received
	private boolean flag = true;
	private static int PORT = 12345;
	private File fileTOFetchByServer;
	private FileReader fReader;
	private byte[] buffer; // Represents buffer that holds data
	private int bufferSize; // Represents buffer size

	// Method main
	public static void main(String[] args) {
		GroupNameQ2Server server = new GroupNameQ2Server();
		server.processRequest();
	}

	// Method used to process request
	public void processRequest() {
		try {
			socket = new DatagramSocket(PORT);
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		buffer = new byte[1400]; // Initialize buffer array
		// receive first packet : User input from client
		System.out.println("Waiting for Packets");
		receivePacket(); // receive packets
		System.out.println("Packet received \n");

		intializeParamters();
		// Printing their values
		System.out.println("_______________________________");
		System.out.println("Name of File requested by Client: " + fileTOFetchByServer.getName());
		System.out.println("Number of Bytes of Data Sent: " + byteSizeBeingSentByClient);
		System.out.println("Timeout Value set by Client: " + timeoutValueSetByClient);
		System.out.println("Timeout Value set by Client: " + recvPacket.getAddress().getHostAddress());

		System.out.println("_______________________________");

		// Prepare acknowledgment
		ackMsg = "Ack " + fileTOFetchByServer.length();
		buffer = ackMsg.getBytes();
		// Send ack
		sendPacket();

		try {
			fReader = new FileReader(fileTOFetchByServer);
		} catch (FileNotFoundException ex) {
		}
		try {
			socket.setSoTimeout(2 * timeoutValueSetByClient); // Set server
			// timeout
		} catch (SocketException ex) {
		}

		// Perform the file reading, sending and reciving packets until DONE
		do {
			ackRecvd = false;
			// check if client has read data before reloading buffer
			if (flag) {
				loadBuffer();
				if ((NBE + byteSizeBeingSentByClient) >= fileTOFetchByServer.length()) {
					E = (int) fileTOFetchByServer.length();
				} else {
					E = NBE + byteSizeBeingSentByClient - 1;
				}
			}
			do {
				System.out.println("\nSending sequence number " + NBE + " to " + E);
				sendPacket();
				receivePacket();
				ackNo = (recvPacket.getData()[0] * 127) + (recvPacket.getData()[1]);
				System.out.println("Received ack number " + ackNo);
			} while (!ackRecvd);
			// ack less than 0 indicates done
			if (ackNo < 0) {
				System.out.println("DONE!!!");
				sendDonePacket();
				break;
			}
			if (E + 1 == ackNo) {
				NBE += byteSizeBeingSentByClient;
				flag = true;
			} else {
				flag = false;
			}

		} while (true);

	}

	@SuppressWarnings("unchecked")
	public void intializeParamters() {
		HashMap<String, String> dataReceieved = new HashMap<String, String>();
		// Set file, B and timeout based on user input received from packet
		ByteArrayInputStream byteIntputStream = new ByteArrayInputStream(buffer);
		ObjectInputStream objectIntput = null;
		try {
			objectIntput = new ObjectInputStream(byteIntputStream);
			dataReceieved = (HashMap<String, String>) objectIntput.readObject();
			PORT = Integer.parseInt(dataReceieved.get(GroupNameQ2Client.SYSTEM_PORT_KEY));
			fileTOFetchByServer = new File(dataReceieved.get(GroupNameQ2Client.FILE_KEY));
			byteSizeBeingSentByClient = Integer.parseInt(dataReceieved.get(GroupNameQ2Client.BUFFER_KEY));
			timeoutValueSetByClient = Integer.parseInt(dataReceieved.get(GroupNameQ2Client.TIMEOUT_KEY));
			socket.close();
			socket = new DatagramSocket(PORT);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException ex) {
			System.out.println("Oops something went wrong , the message is =[" + ex.getMessage() + "]");
		}

	}

	// Method used to receive incoming packets
	public void receivePacket() {
		bufferSize = buffer.length;
		recvPacket = new DatagramPacket(buffer, bufferSize);
		try {
			socket.receive(recvPacket);
			ackRecvd = true;
		} catch (IOException ex) {
		}
	}

	// Method used to send packets
	public void sendPacket() {
		bufferSize = buffer.length;
		try {
			sendPacket = new DatagramPacket(buffer, bufferSize, recvPacket.getAddress(), recvPacket.getPort());
			socket.send(sendPacket);
		} catch (Exception ex) {
			// System.out.println("Error sending packet");
		}
	}

	public void sendDonePacket() {
		buffer = GroupNameQ2Client.LAST_PACKET.getBytes();
		bufferSize = buffer.length;
		bufferSize = bufferSize + 2;
		try {
			sendPacket = new DatagramPacket(buffer, bufferSize, recvPacket.getAddress(), recvPacket.getPort());
			socket.send(sendPacket);
		} catch (Exception ex) {
			// System.out.println("Error sending packet");
		}
	}

	// Method used to load buffer from file
	public void loadBuffer() {
		if (fileTOFetchByServer.length() > (byteSizeBeingSentByClient + NBE)) {
			buffer = new byte[byteSizeBeingSentByClient + 2];
		} else {
			buffer = new byte[(int) (fileTOFetchByServer.length() - NBE) + 2];
		}

		int i = 0, pos = 2;
		buffer[0] = (byte) (NBE / 127);
		buffer[1] = (byte) (NBE % 127);
		do {
			try {
				i = fReader.read();
				if (i != 0) {
					buffer[pos] = (byte) i;
					pos++;
				}
			} catch (IOException ex) {
			}
			if (pos == buffer.length) {
				break;
			}
		} while (i != 0);
	}
}
