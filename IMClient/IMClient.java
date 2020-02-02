package IMClient;
/*
IMClient.java - Instant Message client using UDP and TCP communication.

Text-based communication of commands.
*/

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class IMClient {
	// Protocol and system constants
	public static String serverAddress = "localhost";
	public static int TCPServerPort = 1234;					// connection to server
	public static int UDPServerPort=1235;
	/* 	
	 * This value will need to be unique for each client you are running
	 */
	public static int TCPMessagePort = 1248;				// port for connection between 2 clients
	
	public static String onlineStatus = "100 ONLINE";
	public static String offlineStatus = "101 OFFLINE";

	private BufferedReader reader;							// Used for reading from standard input

	// Client state variables
	private String userId;
	private String buddyId;
	private String status;

	public static void main(String []argv) throws Exception
	{
		IMClient client = new IMClient();
		client.execute();
	}

	public IMClient()
	{
		// Initialize variables
		userId = null;
		status = null;
	}


	public void execute() throws Exception
	{
		initializeThreads();

		String choice;
		reader = new BufferedReader(new InputStreamReader(System.in));

		printMenu();
		choice = getLine().toUpperCase();

		while (!choice.equals("X"))
		{
			if (choice.equals("Y"))
			{	// Must have accepted an incoming connection
				acceptConnection();
			}
			else if (choice.equals("N"))
			{	// Must have rejected an incoming connection
				rejectConnection();
			}
			else if (choice.equals("R"))				// Register
			{	registerUser();
			}
			else if (choice.equals("L"))		// Login as user id
			{	loginUser();
			}
			else if (choice.equals("A"))		// Add buddy
			{	addBuddy();
			}
			else if (choice.equals("D"))		// Delete buddy
			{	deleteBuddy();
			}
			else if (choice.equals("S"))		// Buddy list status
			{	buddyStatus();
			}
			else if (choice.equals("M"))		// Start messaging with a buddy
			{	buddyMessage();
			}
			else
				System.out.println("Invalid input!");

			printMenu();
			choice = getLine().toUpperCase();
		}
		shutdown();
	}

	private void initializeThreads() throws SocketException
	{
		UDPSend send = new UDPSend(this);
	    Thread sendThread = new Thread(send);
	    
		UDPReceive receive=new UDPReceive(this);
	    Thread receiveThread = new Thread(receive);
	    
	    sendThread.start();
	    receiveThread.start();
	    
		
	}

	private void registerUser() throws Exception
	{	// Register user id
		System.out.print("Enter user id: ");
		userId = getLine();
		System.out.println("registering user: "+userId);
		TCPSend("REG "+userId);
	}

	private void loginUser() throws Exception
	{	// Login an existing user (no verification required - just set userId to input)
		System.out.print("Enter user id: ");
		userId = getLine();
		System.out.println("User id set to: "+userId);
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress IPAddress = InetAddress.getByName("localhost");
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		String sentence = "SET "+userId+" "+onlineStatus+" "+1248;
		sendData = sentence.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 1235);
		clientSocket.send(sendPacket);
		clientSocket.close();
		
		
		//SET [userid] [status] [msgport]
	}

	private void addBuddy() throws Exception
	{	// Add buddy if have current user id
		System.out.print("Enter buddy id: ");
		buddyId = getLine();
		TCPSend("ADD "+userId+" "+buddyId);
		
//		ADD [userid] [buddyid]
	}

	private void deleteBuddy() throws Exception
	{	// Delete buddy if have current user id
		System.out.print("Enter buddy id: ");
		buddyId = getLine();
		TCPSend("DEL "+userId  + " "+ buddyId);
		//DEL [userid] [buddyid]
	}

	private void buddyStatus() throws Exception
	{	// Print out buddy status (need to store state in instance variable that received from previous UDP message)
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress IPAddress = InetAddress.getByName("localhost");
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		String sentence = "GET "+userId;
		sendData = sentence.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 1235);
		clientSocket.send(sendPacket);
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		clientSocket.receive(receivePacket);
		String modifiedSentence = new String(receivePacket.getData());
		System.out.println("FROM SERVER:" + modifiedSentence);
		clientSocket.close();
		//GET [userid]
	}

	private void buddyMessage()
	{	// Make connection to a buddy that is online
		// Must verify that they are online and should prompt to see if they accept the connection
	}

	private void shutdown()
	{	// Close down client and all threads
	}

	private void acceptConnection()
	{	// User pressed 'Y' on this side to accept connection from another user
		// Send confirmation to buddy over TCP socket
		// Enter messaging mode
	}

	private void rejectConnection()
	{	// User pressed 'N' on this side to decline connection from another user
		// Send no message over TCP socket then close socket
	}

	private String getLine()
	{	// Read a line from standard input
		String inputLine = null;
		  try{
			  inputLine = reader.readLine();
		  }catch(IOException e){
			 System.out.println(e);
		  }
	 	 return inputLine;
	}
	private void TCPSend(String sentence) throws Exception{
		String modifiedSentence;
		Socket clientSocket = new Socket("localhost", TCPServerPort);
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		outToServer.writeBytes(sentence+'\n');
		modifiedSentence = inFromServer.readLine();
		System.out.println("FROM SERVER: "+modifiedSentence);
		clientSocket.close();
	}
	

	private void printMenu()
	{	System.out.println("\n\nSelect one of these options: ");
		System.out.println("  R - Register user id");
		System.out.println("  L - Login as user id");
		System.out.println("  A - Add buddy");
		System.out.println("  D - Delete buddy");
		System.out.println("  M - Message buddy");
		System.out.println("  S - Buddy status");
		System.out.println("  X - Exit application");
		System.out.print("Your choice: ");
	}

}

// A record structure to keep track of each individual buddy's status
class BuddyStatusRecord
{	public String IPaddress;
	public String status;
	public String buddyId;
	public String buddyPort;

	public String toString()
	{	return buddyId+"\t"+status+"\t"+IPaddress+"\t"+buddyPort; }

	public boolean isOnline()
	{	return status.indexOf("100") >= 0; }
}

// This class implements the TCP welcome socket for other buddies to connect to.
// I have left it here as an example to show where the prompt to ask for incoming connections could come from.
class UDPSend implements Runnable{
	private IMClient client;
	private ServerSocket welcomeSocket;

	public UDPSend(IMClient c)
	{	client = c;
	}

    public void run()
	{
		// This thread starts an infinite loop looking for TCP requests.
		try
		{
			while (true)
			{
//				BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
//				DatagramSocket clientSocket = new DatagramSocket();
//				InetAddress IPAddress = InetAddress.getByName("localhost");
//				byte[] sendData = new byte[1024];
//				String sentence = inFromUser.readLine();
//				sendData = sentence.getBytes();
//				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 1235);
//				clientSocket.send(sendPacket);
//				clientSocket.close();
			}
	    }
		catch (Exception e)
		{	System.out.println(e); }
	}
}


class UDPReceive implements Runnable{
	private IMClient client;
	private ServerSocket welcomeSocket;

	public UDPReceive(IMClient c)
	{	client = c;
	}

    public void run()
	{
		// This thread starts an infinite loop looking for TCP requests.
		try
		{
			while (true)
			{
//				DatagramSocket clientSocket = new DatagramSocket();
//				byte[] receiveData = new byte[1024];
//				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
//				clientSocket.receive(receivePacket);
//				String modifiedSentence = new String(receivePacket.getData());
//				System.out.println("FROM SERVER:" + modifiedSentence);
//				clientSocket.close();
			}
	    }
		catch (Exception e)
		{	System.out.println(e); }
	}
}

class TCPMessenger implements Runnable
{
	private IMClient client;
	private ServerSocket welcomeSocket;

	public TCPMessenger(IMClient c)
	{	client = c;
	}

    public void run()
	{
		// This thread starts an infinite loop looking for TCP requests.
		try
		{
			while (true)
			{
		    	// Listen for a TCP connection request.
		    	Socket connection = welcomeSocket.accept();


		    	System.out.print("\nDo you want to accept an incoming connection (y/n)? ");
		    	// Read actually occurs with menu readline
			}
	    }
		catch (Exception e)
		{	System.out.println(e); }
	}
}