package computer.networks.project.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
 * Client class
 */
public class GroupNameQ2Client {

	// Instance variables
	private Scanner userInput;
	private String[] givenUserInpt = new String[5];
	private boolean isAckReceived; // boolean value that determines if ack has
	// been
	// received
	private boolean transferDone = false; // boolean flag to indicate when it is
	protected static final String FILE_KEY = "FILE";
	protected static final String BUFFER_KEY = "BUFFER";
	protected static final String TIMEOUT_KEY = "TIMEOUT";
	protected static final String SYSTEM_PORT_KEY = "PORT";
	private byte[] dataBuffer; // Represents buffer that holds data to be sent
	private int bufferSize; // Represents buffer size
	private int fileSize; // Represents file size
	private int timeout; // Holds timeout value
	private int NBE = 0; // Next expected byte
	private int S; // Start sequence
	private int E; // End sequence
	private Integer systemPortToBeUsed = 12345; // Port used for communication
	private DatagramSocket socketEntry; // Reference for socket used to send and
	// receive packets
	private DatagramPacket packetsSent; // Reference for packets sent
	private DatagramPacket packetsReceived; // Reference for packets received
	private InetAddress hostIPAddress; // Reference to the Receiver's IP details
	private static ArrayList<String> listOfWebAddress;
	private static int currentWebAddressToProcess = 0;
	public static final String LAST_PACKET = "LAST_PACKET";
	private final Lock lock = new ReentrantLock();

	// Method main
	public static void main(String[] args) {
		GroupNameQ2Client client = new GroupNameQ2Client();
		client.processUserInput();

		// checks the input
		if ("localhost".equalsIgnoreCase(client.givenUserInpt[3])) {
			client.setBufferAndTimeoutValues();
			client.processLocalHost();
		}
	}

	// Method getServerInput: used to read input from user
	public void processUserInput() {
		userInput = new Scanner(System.in);
		int choice = Integer.parseInt((String) getUserInput(Boolean.TRUE));
		if (choice == 1) {
			this.initThreads();
		} else {
			getUserInput(Boolean.FALSE);
		}
	}

	public enum InputTypes {
		FILE, BUFFER_SIZE, TIMEOUT, FILE_ERROR, IS_OPTION_CORRECT, ENTER_WELCOME_OPTION, PORT_NUMBER
	}

	private Object getUserInput(boolean isWelcomeMessageTobeDisplayed) {
		if (isWelcomeMessageTobeDisplayed) {
			System.out.print("Please enter a choice, \n 1. Web Address. \n 2.Localhost. \n Please enter either 1 or 2");
			return validateUserInput(userInput.nextInt(), InputTypes.IS_OPTION_CORRECT);
		} else {
			System.out.println("Please enter the file path of the file you want to send");
			String filePath = (String) validateUserInput(userInput.next(), InputTypes.FILE);
			givenUserInpt[0] = filePath;
			String bufferSize = validateUserInput(null, InputTypes.BUFFER_SIZE).toString();
			givenUserInpt[1] = bufferSize;
			givenUserInpt[3] = new String("localhost");
			givenUserInpt[2] = validateUserInput(null, InputTypes.TIMEOUT).toString();
			givenUserInpt[4] = validateUserInput(null, InputTypes.PORT_NUMBER).toString();
			return null;
		}
	}

	protected Object validateUserInput(Object givenInput, InputTypes currentOptionToCheck) {
		boolean isUserValueCorrect = Boolean.FALSE;

		while (!isUserValueCorrect) {
			switch (currentOptionToCheck) {
			case FILE:
				File userFile = new File((String) givenInput);
				if (userFile.isFile()) {
					isUserValueCorrect = Boolean.TRUE;
					return userFile.getAbsolutePath();
				} else {
					currentOptionToCheck = InputTypes.FILE_ERROR;
					continue;
				}
			case FILE_ERROR:
				System.out.println("Please enter the file path again, the entered file path is wrong!");
				currentOptionToCheck = InputTypes.FILE;
				givenInput = userInput.next();
				continue;
			case BUFFER_SIZE:
				System.out.println("Please enter the Buffer Size");
				int input = userInput.nextInt();
				return (new Integer(input));
			case IS_OPTION_CORRECT:
				Integer userChoiceInput = (int) givenInput;
				if (userChoiceInput == 1 || userChoiceInput == 2) {
					isUserValueCorrect = Boolean.TRUE;
					return userChoiceInput.toString();
				} else {
					currentOptionToCheck = InputTypes.ENTER_WELCOME_OPTION;
					continue;
				}
			case ENTER_WELCOME_OPTION:
				System.out.print("Please enter a choice, \n 1. Web Address. \n 2.Localhost. \n Please enter either 1 or 2");
				if (userInput.hasNextInt()) {
					givenInput = userInput.nextInt();
					currentOptionToCheck = InputTypes.IS_OPTION_CORRECT;
				} else {
					System.out.println("Please enter only Number 1 or 2");
					currentOptionToCheck = InputTypes.ENTER_WELCOME_OPTION;
				}
				continue;
			case TIMEOUT:
				System.out.println("Please enter an integer value to set timeout in Milliseconds");
				this.timeout = userInput.nextInt();
				isUserValueCorrect = Boolean.TRUE;
				return this.timeout;
			case PORT_NUMBER:
				System.out.println("Please enter the port number you want to send as a Number, example 12345");
				if (userInput.hasNextInt()) {
					return userInput.nextInt();
				} else {
					System.out.println("Please enter only Number like 12345 or 21234");
					currentOptionToCheck = InputTypes.PORT_NUMBER;
				}
				continue;
			}
		}
		return null;
	}

	public void setBufferAndTimeoutValues() {
		bufferSize = Integer.parseInt(givenUserInpt[1]);
		timeout = Integer.parseInt(givenUserInpt[2]);
	}

	// The thread handler method
	public void initThreads() {

		listOfWebAddress = new ArrayList<String>();
		getUserInputForWebAddress(InputTypes.ENTER_WELCOME_OPTION);
		int numberOfThreadsToCreate = listOfWebAddress.size();
		ExecutorService totalThreadsToCreate = Executors.newFixedThreadPool(numberOfThreadsToCreate);

		for (int i = 0; i < numberOfThreadsToCreate; i++) {
			totalThreadsToCreate.submit(new Runnable() {
				@Override
				public void run() {
					runLogic();
				}
			});
		}
		totalThreadsToCreate.shutdown();
	}

	public void getUserInputForWebAddress(InputTypes key) {
		switch (key) {
		case ENTER_WELCOME_OPTION:
			System.out.println("please enter the web addresses you want to send the GET message");
			if (userInput.hasNext()) {
				listOfWebAddress.add(userInput.next());
			}
			System.out.println("Do you want to enter another address ? Please answer in true or false only!");
			if (userInput.hasNextBoolean()) {
				boolean userRespnse = userInput.nextBoolean();
				if (userRespnse) {
					key = InputTypes.ENTER_WELCOME_OPTION;
					getUserInputForWebAddress(key);
				} else {
					break;
				}
			}
			break;
		default:
			break;
		}
	}

	public void runLogic() {

		String name = Thread.currentThread().getName();
		int currentIndex = getCurrentWebAddressRequestToProcessId();
		String response;
		Socket socket = new Socket();
		BufferedWriter bWriter = null;
		BufferedReader bReader = null;
		File file = new File("GET_RESPONSE_OF_" + listOfWebAddress.get(currentIndex) + ".txt");
		System.out.println("Printing output for thread =[" + name + "For the url =[" + listOfWebAddress.get(currentIndex) + "]");

		try {
			socket.connect(new InetSocketAddress(listOfWebAddress.get(currentIndex), 80));
			bWriter = new BufferedWriter(new PrintWriter(socket.getOutputStream(), true));
			bReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			bWriter.write("GET / HTTP/1.0\r\n\r\n");
			bWriter.flush();
			System.out.println(name + ": Request Sent");
			bWriter = new BufferedWriter(new FileWriter(file));
			bWriter.write(listOfWebAddress.get(currentIndex) + "\r\n");
			do {
				response = bReader.readLine();
				if (response != null) {
					System.out.println(response);
					bWriter.write(response + "\r\n");
				}
			} while (response != null);
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
	}

	public int getCurrentWebAddressRequestToProcessId() {
		try {
			lock.lock();
			int index = currentWebAddressToProcess;
			currentWebAddressToProcess++;
			return index;
		} finally {
			lock.unlock();
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

			dataBuffer = getBufferForDataToSend();// loading
			// buffer
			// to
			// be
			// sent
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
			} else if (isAckReceived) {
				String dataReceieved = new String(Arrays.copyOfRange(dataBuffer, 2, dataBuffer.length)).trim();
				if (LAST_PACKET.equals(dataReceieved)) {
					transferDone = true;
				}
			}
			dataBuffer = new byte[2];
			dataBuffer[0] = (byte) (NBE / 127);
			dataBuffer[1] = (byte) (NBE % 127);
			sendPacket();
			if (dataBuffer[0] < 0) {
				transferDone = true;
			}
		} while (!transferDone);
		System.out.println("DONE!!!");
		System.exit(-1);
	}

	protected byte[] getBufferForDataToSend() {
		HashMap<String, String> dataToSend = new HashMap<String, String>();
		dataToSend.put(FILE_KEY, givenUserInpt[0]);
		dataToSend.put(BUFFER_KEY, givenUserInpt[1]);
		dataToSend.put(TIMEOUT_KEY, givenUserInpt[2]);
		dataToSend.put(SYSTEM_PORT_KEY, givenUserInpt[4]);
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		ObjectOutput objectOutput = null;
		try {
			objectOutput = new ObjectOutputStream(byteOutputStream);
			objectOutput.writeObject(dataToSend);
		} catch (IOException e) {
			System.out.println("Something went wrong here =[" + e.getMessage() + "]");
		}
		return byteOutputStream.toByteArray();
	}

	// Method to send packets
	public void sendPacket() {
		packetsSent = new DatagramPacket(dataBuffer, dataBuffer.length, hostIPAddress, systemPortToBeUsed);
		try {
			socketEntry.send(packetsSent);
			systemPortToBeUsed = Integer.parseInt(givenUserInpt[4]);
		} catch (IOException ex) {
			System.out.println("Data Packet Not Sent, the error message is =[" + ex.getMessage() + "]");
		} catch (NullPointerException ex) {
			System.out.println("Something went Wrong and error is =[" + ex.getMessage() + "]");
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
