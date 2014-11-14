package computer.networks.project.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/*
 * Server class
 */
public class GroupNameQ2Server {

	private DatagramSocket socket;
	private DatagramPacket recvPacket;
	private DatagramPacket sendPacket;
	private InetAddress host;
	private String ackMsg;
	private int timeout;
	private int B;
	private int ackNo = 0;
	private int NBE = 0;
	private int E = 0;
	private boolean ackRecvd; // boolean value that determines if ack has been
								// received
	private boolean flag = true;
	private final int PORT = 55555;
	private File file;
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
			host = InetAddress.getByName("localhost");
		} catch (Exception ex) {
		}
		buffer = new byte[1400]; // Initialize buffer array
		// receive first packet : User input from client
		System.out.println("Waiting for Packets");
		receivePacket(); // receive packets
		System.out.println("Packet received \n");
		// Set file, B and timeout based on user input received from packet
		file = new File(new String(recvPacket.getData()).split(" ")[0]);
		B = Integer.parseInt(new String(recvPacket.getData()).split(" ")[1]);
		timeout = Integer.parseInt(new String(recvPacket.getData()).trim().split(" ")[2]);

		// Printing their values
		System.out.println("_______________________________");
		System.out.println("File name: " + file.getName());
		System.out.println("B: " + B);
		System.out.println("T: " + timeout);
		System.out.println("_______________________________");

		// Prepare acknowledgment
		ackMsg = "Ack " + file.length();
		buffer = ackMsg.getBytes();
		// Send ack
		sendPacket();

		try {
			fReader = new FileReader(file);
		} catch (FileNotFoundException ex) {
		}
		try {
			socket.setSoTimeout(2 * timeout); // Set server timeout
		} catch (SocketException ex) {
		}

		// Perform the file reading, sending and reciving packets until DONE
		do {
			ackRecvd = false;
			// check if client has read data before reloading buffer
			if (flag) {
				loadBuffer();
				if ((NBE + B) >= file.length()) {
					E = (int) file.length();
				} else {
					E = NBE + B - 1;
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
				break;
			}
			if (E + 1 == ackNo) {
				NBE += B;
				flag = true;
			} else {
				flag = false;
			}

		} while (true);

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

	// Method used to load buffer from file
	public void loadBuffer() {
		if (file.length() > (B + NBE)) {
			buffer = new byte[B + 2];
		} else {
			buffer = new byte[(int) (file.length() - NBE) + 2];
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
