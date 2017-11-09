import java.util.Scanner;
import java.io.File;
import java.io.FileFilter;
import java.security.MessageDigest;

public class SecureChat2 {

	private static final String serverInboxDir = "serverInbox/";
	private static final String clientInboxDir = "clientInbox/";
	protected static final String messageName = "message";
	protected static final String checksumExtension = ".checksum";

	private static Scanner scanner;
	private static boolean start = false;
	private static boolean passwordNeeded = false;
	private static boolean[] options = new boolean[3];

	public static FileFilter hiddenFileFilter = new FileFilter() {
		public boolean accept(File file) {
			if(file.isHidden()) {
				return false;
			}
			return true;
		}
	};
	
	public static void main(String[] args) {

		String messagePrompt = "Enter message: ";
		String inputBuffer = "";
		String readInboxDir = "";
		String writeInboxDir = "";

		scanner = new Scanner(System.in);

		// create client and server inboxes
		File inbox = new File(serverInboxDir);
		if(!inbox.exists()) {
			inbox.mkdir();
		}
		inbox = new File(clientInboxDir);
		if(!inbox.exists()) {
			inbox.mkdir();
		}

		if(args.length != 1){
			printUsage();
			return;
		}

		if(args[0].equals("--server")) {
			System.out.println("Starting server.");
			readInboxDir = serverInboxDir;
			writeInboxDir = clientInboxDir;
			start = true;
			initServer();
		}

		else if(args[0].equals("--client")) {
			System.out.println("Starting client.");
			readInboxDir = clientInboxDir;
			writeInboxDir = serverInboxDir;
			start = true;
			initClient();
		}

		if(start) {
			MessageReader reader = new MessageReader(messagePrompt, inputBuffer, readInboxDir, options);
			reader.setName("reader");
			reader.start();

			MessageWriter writer = new MessageWriter(messagePrompt, inputBuffer, writeInboxDir, options);
			writer.setName("writer");
			writer.start();
		}

	}

	private static void initServer() {
		// set CIA options
		getSecurityOptions();

		System.out.println("Waiting for client.");

		// wait for client initialize
		waitForMessage(serverInboxDir);

		// read CIA options in message
		String messageFilePath = serverInboxDir + messageName;
		File f = new File(messageFilePath);

		if(f.exists()) {
			authenticateClientMessage(messageFilePath, f);
		}
		
		// if authentication is used check password
		while(passwordNeeded) {
			waitForMessage(serverInboxDir);
			f = new File(messageFilePath);
			if(f.exists()) {
				authenticateClientMessage(messageFilePath, f);
			}
		}

	}

	protected static void waitForMessage(String inboxDir) {
		int numFiles = 0;
		while(numFiles == 0) {
			numFiles = new File(inboxDir).listFiles(hiddenFileFilter).length;
		}
	}

	private static void authenticateClientMessage(String messageFilePath, File f) {
		boolean messageAuthenticated = false;

		Message m = new Message();
		m.readPlainTextMessageFile(messageFilePath);

		f.delete();

		if(m.getType() == Message.MESSAGE_TYPE_OPTIONS) {//if the client sent a request (not an IM message)

			boolean[] clientOptions = new boolean[3];
			setOptions(m.getContents(), clientOptions);

			boolean match = true;
			// check if CIA options match
			for(int i = 0; i < options.length; i++) {
				if(options[i] != clientOptions[i]) {
					match = false;
					break;
				}
			}

			// if options match, send a confirmation message back
			if(match) {
				messageAuthenticated = true;
			}
			else {
				System.out.println("Security options don't match. Cannot create connection.");
				start = false;	
			}

		}
		else if(m.getType() == Message.MESSAGE_TYPE_PASSWORD) {//if client sent a password
			//compare to pw table
			if(m.getContents().equals("clientpw")) {
				System.out.println("correct password.");
				messageAuthenticated = true;
				passwordNeeded = false;
			}

		}
		else {
			start = false;
		}

		int messageType;

		if(messageAuthenticated) {
			messageType = Message.MESSAGE_TYPE_CONFIRM;
		}
		else {
			messageType = Message.MESSAGE_TYPE_WRONG_PASSWORD;
		}

		// send the message
		messageFilePath = clientInboxDir + messageName;
		Message confirmMessage = new Message(messageType, "");
		confirmMessage.writePlainTextMessageFile(messageFilePath);
	}

	private static void initClient() {
		String messageSendFilePath = serverInboxDir + messageName;
		String messageReceiveFilePath = clientInboxDir + messageName;

		// select CIA options
		getSecurityOptions();

		// attempt to create establish connection (send CIA options)
		sendOptionsMessage(messageSendFilePath);

		System.out.println("Waiting for confirmation from server.");

		waitForMessage(clientInboxDir);

		// check file exists and that it is the correct message type
		File f = new File(messageReceiveFilePath);

		if(f.exists()) {
			Message m = new Message();
			m.readPlainTextMessageFile(messageReceiveFilePath);
			f.delete();
			if(m.getType() == Message.MESSAGE_TYPE_CONFIRM) {
				System.out.println("Connection established successfully.");
			}
			else {
				start = false;
			}
		}

		// if authentication is used check password
		if(options[2]) {
			passwordNeeded = true;
			boolean success = false;

			while(!success) {
				System.out.println("Enter password:");
				String password = scanner.nextLine();
	

				//send password message
				Message m = new Message(Message.MESSAGE_TYPE_PASSWORD, password);
				m.writePlainTextMessageFile(messageSendFilePath);

				//wait for server to respond
				waitForMessage(clientInboxDir);

				// check file exists and that it is the correct message type
				f = new File(messageReceiveFilePath);

				if(f.exists()) {
					m = new Message();
					m.readPlainTextMessageFile(messageReceiveFilePath);
					f.delete();
					if(m.getType() == Message.MESSAGE_TYPE_CONFIRM) {
						System.out.println("Password correct.");
						success = true;
						passwordNeeded = false;
						start = true;
					}
					else {
						System.out.println("Password incorrect.");
						success = false;
						start = false;
					}
				}
			}

		}
		else {
			//if no password needed, start IM functionality
			start = true;
		}
	}

	private static void getSecurityOptions() {
		System.out.println("Enter desired security options (CIA):");
		String input = scanner.nextLine();
		input = input.toLowerCase();

		if(input.contains("c")) {
			options[0] = true;
		}
		if(input.contains("i")) {
			options[1] = true;
		}
		if(input.contains("a")) {
			options[2] = true;
			passwordNeeded = true;
		}
	}

	private static void setOptions(String optionsString, boolean[] optionsArr) {
		for(int i = 0; i < optionsArr.length; i++) {
			if(optionsString.charAt(i) == '1') {
				optionsArr[i] = true;
			}
		}
	}

	private static void sendOptionsMessage(String messageFilePath) {

		String optionsString = "";
		for(int i = 0; i < options.length; i++){
			if(options[i]){
				optionsString += "1";
			}
			else {
				optionsString += "0";
			}
		}

		Message m = new Message(Message.MESSAGE_TYPE_OPTIONS, optionsString);
		m.writePlainTextMessageFile(messageFilePath);
	}

	// Pass in a String message and return a MD5 checksum
	protected static byte[] getHash(String message){
		byte[] hash = null;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			hash = digest.digest(message.getBytes("UTF-8"));
		}
		catch (Exception e){
			e.printStackTrace();
		}

		return hash;
	}

	private static void printUsage(){
		System.out.println("Please use command line arguments to specify if this is a client or a server");
		System.out.println("run     java SecureChat --server   to create a server");
		System.out.println("run     java SecureChat --client   to create a client");
	}
}