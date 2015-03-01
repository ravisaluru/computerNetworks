

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

/**
 * @author Ravi Chandra Group
 */
public class GroupNameQ2Server {

	private DatagramSocket dataSocket;
	private DatagramPacket receivedPacketFromClient;
	private DatagramPacket sendPacketToClient;
	private String messagePacket;
	private int timeoutValueSetByClient;
	private int byteSizeBeingSentByClient;
	private int acknowledmentNumber = 0;
	private int nextByte = 0;
	private int endOfSequence = 0;
	private boolean isMessagePacketAcknowledgemtRecieved;
	private boolean flag = true;
	private static int serverDefaultPortNumber = 12345;
	private File fileTOFetchByServer;
	private FileReader fileReaderObject;
	private byte[] defaultBuffer;
	private int bufferSizeAsPerClient;

	public static void main(String[] args) {
		GroupNameQ2Server server = new GroupNameQ2Server();
		server.processRequest();
	}

	public void processRequest() {
		try {
			dataSocket = new DatagramSocket(serverDefaultPortNumber);
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		defaultBuffer = new byte[1400];
		System.out.println("Waiting for Packets");
		receiveDataPacket();
		System.out.println("Packet received \n");

		intializeParamters();
		System.out.println("_______________________________");
		System.out.println("Name of File requested by Client: " + fileTOFetchByServer.getName());
		System.out.println("Number of Bytes of Data Sent: " + byteSizeBeingSentByClient);
		System.out.println("Timeout Value set by Client: " + timeoutValueSetByClient);
		System.out.println("Timeout Value set by Client: " + receivedPacketFromClient.getAddress().getHostAddress());

		System.out.println("_______________________________");

		messagePacket = "Acknowledgement " + fileTOFetchByServer.length();
		defaultBuffer = messagePacket.getBytes();
		sendDataPacket();

		try {
			fileReaderObject = new FileReader(fileTOFetchByServer);
		} catch (FileNotFoundException ex) {
		}
		try {
			dataSocket.setSoTimeout(2 * timeoutValueSetByClient);
		} catch (SocketException ex) {
		}

		do {
			isMessagePacketAcknowledgemtRecieved = false;
			if (flag) {
				loadBufferMethod();
				if ((nextByte + byteSizeBeingSentByClient) >= fileTOFetchByServer.length()) {
					endOfSequence = (int) fileTOFetchByServer.length();
				} else {
					endOfSequence = nextByte + byteSizeBeingSentByClient - 1;
				}
			}
			do {
				System.out.println("\nSending sequence number " + nextByte + " to " + endOfSequence);
				sendDataPacket();
				receiveDataPacket();
				acknowledmentNumber = (receivedPacketFromClient.getData()[0] * 127) + (receivedPacketFromClient.getData()[1]);
				System.out.println("Received ack number " + acknowledmentNumber);
			} while (!isMessagePacketAcknowledgemtRecieved);
			if (acknowledmentNumber < 0) {
				System.out.println("DONE!!!");
				sendDonePacket();
				break;
			}
			if (endOfSequence + 1 == acknowledmentNumber) {
				nextByte += byteSizeBeingSentByClient;
				flag = true;
			} else {
				flag = false;
			}

		} while (true);

	}

	@SuppressWarnings("unchecked")
	public void intializeParamters() {
		HashMap<String, String> dataReceieved = new HashMap<String, String>();
		ByteArrayInputStream byteIntputStream = new ByteArrayInputStream(defaultBuffer);
		ObjectInputStream objectIntput = null;
		try {
			objectIntput = new ObjectInputStream(byteIntputStream);
			dataReceieved = (HashMap<String, String>) objectIntput.readObject();
			serverDefaultPortNumber = Integer.parseInt(dataReceieved.get(GroupNameQ2Client.SYSTEM_PORT_KEY));
			fileTOFetchByServer = new File(dataReceieved.get(GroupNameQ2Client.FILE_KEY));
			byteSizeBeingSentByClient = Integer.parseInt(dataReceieved.get(GroupNameQ2Client.BUFFER_KEY));
			timeoutValueSetByClient = Integer.parseInt(dataReceieved.get(GroupNameQ2Client.TIMEOUT_KEY));
			dataSocket.close();
			dataSocket = new DatagramSocket(serverDefaultPortNumber);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NullPointerException ex) {
			System.out.println("Oops something went wrong , the message is =[" + ex.getMessage() + "]");
		}

	}

	public void receiveDataPacket() {
		bufferSizeAsPerClient = defaultBuffer.length;
		receivedPacketFromClient = new DatagramPacket(defaultBuffer, bufferSizeAsPerClient);
		try {
			dataSocket.receive(receivedPacketFromClient);
			isMessagePacketAcknowledgemtRecieved = true;
		} catch (IOException ex) {
		}
	}

	public void sendDataPacket() {
		bufferSizeAsPerClient = defaultBuffer.length;
		try {
			sendPacketToClient = new DatagramPacket(defaultBuffer, bufferSizeAsPerClient, receivedPacketFromClient.getAddress(),
					receivedPacketFromClient.getPort());
			dataSocket.send(sendPacketToClient);
		} catch (Exception ex) {
			System.out.println("Error sending packet" + ex.getMessage());
		}
	}

	public void sendDonePacket() {
		defaultBuffer = GroupNameQ2Client.LAST_PACKET.getBytes();
		bufferSizeAsPerClient = defaultBuffer.length;
		try {
			sendPacketToClient = new DatagramPacket(defaultBuffer, bufferSizeAsPerClient, receivedPacketFromClient.getAddress(),
					receivedPacketFromClient.getPort());
			dataSocket.send(sendPacketToClient);
		} catch (Exception ex) {
			System.out.println("Error sending packet" + ex.getMessage());
		}
	}

	public void loadBufferMethod() {
		if (fileTOFetchByServer.length() > (byteSizeBeingSentByClient + nextByte)) {
			defaultBuffer = new byte[byteSizeBeingSentByClient + 2];
		} else {
			defaultBuffer = new byte[(int) (fileTOFetchByServer.length() - nextByte) + 2];
		}

		int index = 0, positionOfBuffer = 2;
		defaultBuffer[0] = (byte) (nextByte / 127);
		defaultBuffer[1] = (byte) (nextByte % 127);
		do {
			try {
				index = fileReaderObject.read();
				if (index != 0) {
					defaultBuffer[positionOfBuffer] = (byte) index;
					positionOfBuffer++;
				}
			} catch (IOException ex) {
			}
			if (positionOfBuffer == defaultBuffer.length) {
				break;
			}
		} while (index != 0);
	}
}
