package computer.networks.project.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Scanner;

/*
 * Client class
 */
public class GroupNameQ2Client implements Runnable {

	// Instance variables
	private Scanner userInput;
	private String givenUserInpt;
	private boolean isAckReceived; // boolean value that determines if ack has
	// been
	// received
	private boolean transferDone = false; // boolean flag to indicate when it is
	// done
	private byte[] dataBuffer; // Represents buffer that holds data to be sent
	private int bufferSize; // Represents buffer size
	private int fileSize; // Represents file size
	private int timeout; // Holds timeout value
	private int NBE = 0; // Next expected byte
	private int S; // Start sequence
	private int E; // End sequence
	private final int systemPortToBeUsed = 55555; // Port used for communication
	private DatagramSocket socketEntry; // Reference for socket used to send and
	// receive packets
	private DatagramPacket packetsSent; // Reference for packets sent
	private DatagramPacket packetsReceived; // Reference for packets received
	private InetAddress hostIPAddress; // Reference to the Receiver's IP details
	private long t1, t2;

	// Method main
	public static void main(String[] args) {
		// TODO code application logic here
		GroupNameQ2Client client = new GroupNameQ2Client();
		client.getUserInput();

		// checks the input
		if (client.givenUserInpt.equalsIgnoreCase("local")) {
			client.getUDPInput();
			client.processLocalHost();
		} else {
			client.initThreads();
		}
	}

	// Method getServerInput: used to read input from user
	public void processUserInput() {
		userInput = new Scanner(System.in);
		System.out.print("Please enter a choice, \n 1. Web Address. \n 2.Localhost. \n Please enter either 1 or 2");
		int choice = userInput.nextInt();

		if (choice == 1) {
			this.initThreads();
		}
	}

	// Method getUDPInput: used to read input from user
	public void getUDPInput() {
		System.out.println("Enter file path, buffer size and timeout separated by space");
		givenUserInpt = userInput.nextLine();
		bufferSize = Integer.parseInt(givenUserInpt.split(" ")[1]); // set
		// bufferSize
		timeout = Integer.parseInt(givenUserInpt.split(" ")[2]); // set timeout
		// value
	}

	// The thread handler method
	public void initThreads() {
		Thread th = new Thread(this, "Thread1");
		Thread th2 = new Thread(this, "Thread2");
		th.start();
		th2.start();
		try {
			th.join();
			th2.join();
		} catch (InterruptedException ex) {
		}
		System.out.println("Request time (Milliseconds)");
		System.out.println("1\t" + t1);
		System.out.println("2\t" + t2);
	}

	@Override
	public void run() {
		long startTime, endTime;

		String name = Thread.currentThread().getName();
		String response;
		startTime = System.currentTimeMillis();
		File output;
		Socket socket = new Socket();
		BufferedWriter bWriter = null;
		BufferedReader bReader = null;

		int number = name.endsWith("Thread1") ? 1 : 2;
		output = new File("WebResponse_" + number + ".txt");

		try {
			socket.connect(new InetSocketAddress(givenUserInpt, 80));
			bWriter = new BufferedWriter(new PrintWriter(socket.getOutputStream(), true));
			bReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			bWriter.write("GET / HTTP/1.0\r\n\r\n");
			bWriter.flush();
			System.out.println(name + ": Request Sent");
			bWriter = new BufferedWriter(new FileWriter(output));
			bWriter.write(givenUserInpt + "\r\n");
			do {
				response = bReader.readLine();
				if (response != null) {
					bWriter.write(response + "\r\n");
				}
			} while (response != null);
			bWriter.flush();

		} catch (Exception ex) {
			System.out.println(name + ": Error occured: " + ex.getMessage());
			System.exit(-1);
		} finally {
			try {
				bWriter.close();
				bReader.close();
				socket.close();
			} catch (IOException ex) {

			}
		}

		endTime = System.currentTimeMillis();
		if (name.equals("Thread1")) {
			t1 = endTime - startTime;
		} else {
			t2 = endTime - startTime;
		}
	}

	// Method to process Local Host UDP communication
	public void processLocalHost() {
		// Initializing socket and host computer
		try {
			socketEntry = new DatagramSocket();
			// host = Inet4Address.getByName("169.254.112.231");
			hostIPAddress = Inet4Address.getByName("localhost");
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			return;
		}

		try {
			socketEntry.setSoTimeout(1000); // Default time to wait before
			// resending
			// packet
		} catch (SocketException ex) {
		}

		isAckReceived = false; // Set boolean flag to false

		// keeps sending the input entered by user in one packet until ack is
		// received
		do {
			dataBuffer = givenUserInpt.getBytes(); // loading buffer to be sent
			sendPacket();
			receivePacket();
			if (!isAckReceived) {
				System.out.println("No ack from server, resending packets...");
			}
		} while (!isAckReceived);

		// Ack received
		System.out.println("Acknowledgement received");
		fileSize = Integer.parseInt(new String(packetsReceived.getData()).trim().split(" ")[1]);
		System.out.println("File Size: " + fileSize);

		try {
			// set timeout size according to user input
			socketEntry.setSoTimeout(timeout);
		} catch (SocketException ex) {
		}

		// Wait for the next B bytes or remaining bytes in file
		do {
			isAckReceived = false;
			receivePacket();
			S = (packetsReceived.getData()[0] * 127) + (packetsReceived.getData()[1]);
			if (S == NBE && isAckReceived) {
				if (S + bufferSize > fileSize) {
					E = fileSize;
					NBE = -1;
					transferDone = true;
				} else {
					E = S + bufferSize - 1;
					NBE += bufferSize;
				}
				System.out.println("\nReceived sequence number " + S + " to " + E);
				System.out.println(new String(Arrays.copyOfRange(dataBuffer, 2, dataBuffer.length)).trim());
				System.out.println();
			}
			dataBuffer = new byte[2];
			dataBuffer[0] = (byte) (NBE / 127);
			dataBuffer[1] = (byte) (NBE % 127);
			sendPacket();
		} while (!transferDone);
		System.out.println("DONE!!!");
	}

	// Method to send packets
	public void sendPacket() {
		packetsSent = new DatagramPacket(dataBuffer, dataBuffer.length, hostIPAddress, systemPortToBeUsed);
		try {
			socketEntry.send(packetsSent);
		} catch (IOException ex) {
			System.out.println("Error sending packet");
		}
	}

	// Method to receive packets
	public void receivePacket() {
		dataBuffer = new byte[bufferSize + 2]; // set buffer size according to
		// user
		// input
		packetsReceived = new DatagramPacket(dataBuffer, dataBuffer.length);
		try {
			socketEntry.receive(packetsReceived);
			isAckReceived = true;
		} catch (SocketTimeoutException ex) {
			// System.out.println("timeout"); Uncomment this line to see the
			// timeout instances
		} catch (IOException ex) {
			System.out.println("Error receiving packet");
		}
	}
}
