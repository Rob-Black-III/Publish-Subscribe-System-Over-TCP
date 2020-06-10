package edu.rit.cs.pubsub;


import java.io.DataInputStream;
import java.util.Scanner;

/**
 * Program the client runs. Sends commands to the EventManager
 */
public class PubSubAgent{
	public static final String PROGRAM_USAGE  = "Usage: PubSubAgent.java <SERVER_ADDRESS> <SERVER_PORT>";
	public static final String PROMPT = "> ";

	private String serverAddress;
	private int serverPort;
	private Client clientConnection;

	/**
	 * Constructor
	 * @param serverAddress address of the EventManager
	 * @param serverPort port of the EventManager
	 */
	public PubSubAgent(String serverAddress, int serverPort){
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;

		//Connect to Server
		this.clientConnection = new Client(serverAddress,serverPort);

		//Parse Command-line input
		this.handleInput();
	}

	/**
	 * Handles the reading and writing via multithreading
	 */
	private void handleInput(){
		Thread readFromServerThread = new Thread(new ReadInputThread(this.clientConnection));
		Thread writeToServerThread = new Thread(new WriteOutputThread(this.clientConnection));
		writeToServerThread.start();
		readFromServerThread.start();

		printToScrean(PROMPT);
	}

	/**
	 * Program usage and exit
	 */
	private static void printUsageAndExit(){
		System.out.println(PROGRAM_USAGE);
		System.exit(0);
	}

	/**
	 * Driver program
	 * @param args
	 */
	public static void main(String args[]){
		if(args.length != 2){
			printUsageAndExit();
		}

		new PubSubAgent(args[0], Integer.parseInt(args[1]));
	}

	/**
	 * Protected print for threading
	 * @param text
	 */
	synchronized void printToScreanNewline(String text){
		System.out.println(text);
	}

	/**
	 * Protected print for threading
	 * @param text
	 */
	synchronized void printToScrean(String text){
		System.out.print(text);
	}

	/**
	 * Thread to read data
	 */
	class ReadInputThread implements Runnable{

		private Client clientConnection;

		public ReadInputThread(Client c){
			this.clientConnection = c;
		}

		@Override
		public void run() {
			while(true){
				//Read and print response (blocking operation, ok)
				Object response = this.clientConnection.receiveObject();

				printToScreanNewline((String)response);

				if(((String)response).equals("exit good")){
					this.clientConnection.kill();
					System.exit(0);
				}

				printToScrean(PROMPT);

				//Wait a bit
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}
	}

	/**
	 * Thread to write data.
	 */
	class WriteOutputThread implements Runnable{

		private Client clientConnection;

		public WriteOutputThread(Client c){
			this.clientConnection = c;
		}

		synchronized String getInputFromScanner(Scanner myScanner){
			String input = myScanner.nextLine();
			return input;
		}

		@Override
		public void run() {
			Scanner myScanner = new Scanner(System.in);
			while(true){
				String input = getInputFromScanner(myScanner);

				//Send the input directly to EventManager for Parsing
				this.clientConnection.sendObject(input);

			}
		}
	}
}
