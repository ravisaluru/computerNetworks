

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

/**
 * @author Ravi Chandra Group
 */
public class GroupNameQ2Client {

	private Scanner userInput;
	private String[] givenUserInpt = new String[5];
	private boolean isAckReceived;
	private boolean isTransferDone = false;
	protected static final String FILE_KEY = "FILE";
	protected static final String BUFFER_KEY = "BUFFER";
	protected static final String TIMEOUT_KEY = "TIMEOUT";
	protected static final String SYSTEM_PORT_KEY = "PORT";
	private byte[] dataBuffer;
	private int bufferSize;
	private int requestedFileSize;
	private int userSpecifiedTimeOut;
	private int nextByte = 0;
	private int startOfSequence;
	private int endOfSequence;
	private Integer systemPortToBeUsed = 12345;
	private DatagramSocket socketToBeUsed;
	private DatagramPacket packetsSent;
	private DatagramPacket packetsReceived;
	private InetAddress hostIPAddress;
	private static ArrayList<String> listOfWebAddress;
	private static int currentWebAddressToProcess = 0;
	public static final String LAST_PACKET = "LAST_PACKET";
	private final Lock lock = new ReentrantLock();

	public static void main(String[] args) {
		GroupNameQ2Client client = new GroupNameQ2Client();
		client.processUserInput();
	}

	/**
	 * This method would process the user input for both local and web address
	 */
	public void processUserInput() {
		userInput = new Scanner(System.in);
		int choice = Integer.parseInt((String) getUserInput(Boolean.TRUE));
		if (choice == 1) {
			this.processGetWebAdressRequests();
		} else {
			getUserInput(Boolean.FALSE);
			setBufferAndTimeoutValues();
			processLocalHost();
		}
	}

	public enum InputTypes {
		FILE, BUFFER_SIZE, TIMEOUT, FILE_ERROR, IS_OPTION_CORRECT, ENTER_WELCOME_OPTION, PORT_NUMBER
	}

	/**
	 * This method is specifically designed to process local host requests. This
	 * will also do some validation
	 */
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

	/**
	 * This method handles the user validation
	 */
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
				this.userSpecifiedTimeOut = userInput.nextInt();
				isUserValueCorrect = Boolean.TRUE;
				return this.userSpecifiedTimeOut;
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

	/**
	 * instantiate some variables
	 */
	public void setBufferAndTimeoutValues() {
		bufferSize = Integer.parseInt(givenUserInpt[1]);
		userSpecifiedTimeOut = Integer.parseInt(givenUserInpt[2]);
	}

	public void processGetWebAdressRequests() {
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

	/**
	 * This method asks the user for the list of web address for which a get
	 * request needs to be made
	 */
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

	/**
	 * The main algorithm to process the get request, it will display the result
	 * on the console and also write and save the response of the individual web
	 * address in a file. The file name is choose in accordance to the web
	 * address for understandability.
	 */
	public void runLogic() {

		String name = Thread.currentThread().getName();
		int currentIndex = getCurrentWebAddressRequestToProcessId();
		String response;
		Socket socket = new Socket();
		BufferedWriter bWriter = null;
		BufferedReader bReader = null;
		File file = new File("GET_response_of_" + listOfWebAddress.get(currentIndex) + ".txt");
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

	/*
	 * This method controls synchronized incrementing of the index variable
	 */
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

	/**
	 * This method would process the local host calls
	 */
	public void processLocalHost() {
		try {
			socketToBeUsed = new DatagramSocket();
			hostIPAddress = Inet4Address.getByName("localhost");
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			return;
		}

		try {
			socketToBeUsed.setSoTimeout(1000);
		} catch (SocketException ex) {
		}
		isAckReceived = false;
		do {

			dataBuffer = getBufferForDataToSend();
			sendDataPacket();
			receiveDataPacket();
			if (!isAckReceived) {
				System.out.println("No ack from server, resending packets...");
			}
		} while (!isAckReceived);

		System.out.println("Acknowledgement received");
		requestedFileSize = Integer.parseInt(new String(packetsReceived.getData()).trim().split(" ")[1]);
		System.out.println("File Size: " + requestedFileSize);

		try {
			socketToBeUsed.setSoTimeout(userSpecifiedTimeOut);
		} catch (SocketException ex) {
		}

		do {
			isAckReceived = false;
			receiveDataPacket();
			startOfSequence = (packetsReceived.getData()[0] * 127) + (packetsReceived.getData()[1]);
			if (startOfSequence == nextByte && isAckReceived) {
				if (startOfSequence + bufferSize > requestedFileSize) {
					endOfSequence = requestedFileSize;
					nextByte = -1;
					isTransferDone = true;
				} else {
					endOfSequence = startOfSequence + bufferSize - 1;
					nextByte += bufferSize;
				}
				System.out.println("\nReceived sequence number " + startOfSequence + " to " + endOfSequence);
				System.out.println(new String(Arrays.copyOfRange(dataBuffer, 2, dataBuffer.length)).trim());
				System.out.println();
			} else if (isAckReceived) {
				String dataReceieved = new String(Arrays.copyOfRange(dataBuffer, 0, dataBuffer.length)).trim();
				System.out.println(dataReceieved + " has been received, end of all requests");
				if (LAST_PACKET.equals(dataReceieved)) {
					isTransferDone = true;
				}
			}
			dataBuffer = new byte[2];
			dataBuffer[0] = (byte) (nextByte / 127);
			dataBuffer[1] = (byte) (nextByte % 127);
			sendDataPacket();
		} while (!isTransferDone);
		System.out.println("DONE!!!");
		System.exit(-1);
	}

	/**
	 * This method would create a Map Object and stuff the user inputs. It will
	 * return the byte representation of the object or serialized version
	 */
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

	/**
	 * This method would send the data packets
	 */
	public void sendDataPacket() {
		packetsSent = new DatagramPacket(dataBuffer, dataBuffer.length, hostIPAddress, systemPortToBeUsed);
		try {
			socketToBeUsed.send(packetsSent);
			systemPortToBeUsed = Integer.parseInt(givenUserInpt[4]);
		} catch (IOException ex) {
			System.out.println("Data Packet Not Sent, the error message is =[" + ex.getMessage() + "]");
		} catch (NullPointerException ex) {
			System.out.println("Something went Wrong and error is =[" + ex.getMessage() + "]");
		}

	}

	/**
	 * This method would receive the data packets
	 */
	public void receiveDataPacket() {
		dataBuffer = new byte[bufferSize + 2];
		packetsReceived = new DatagramPacket(dataBuffer, dataBuffer.length);
		try {
			socketToBeUsed.receive(packetsReceived);
			isAckReceived = true;
		} catch (SocketTimeoutException ex) {
		} catch (IOException ex) {
			System.out.println("Error receiving packet");
		}
	}

}
