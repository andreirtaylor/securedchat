import java.util.Arrays;
import java.util.Scanner;
import java.io.Console;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;


/*
 *  Main class for starting SecureChat application
 * 
 *  Can be started with --server or --client command line option to run
 *  the server or the client
 *  
 *  Includes client and server initialization methods and starts the
 *  MessageReader and MessageWriter Threads
 *
 */
public class SecureChat {

	static Console console = System.console();

	private static final String serverInboxDir = "serverInbox/";
	private static final String clientInboxDir = "clientInbox/";
	private static final String passwordDir = "passwords/";
	private static final String serverPass = "serverpw";
	private static final String clientPass = "clientpw";
	protected static final String messageName = "message";
	protected static final String saltFileName = "salt";
	protected static final String checksumExtension = ".checksum";

	private static Scanner scanner;
	private static boolean start = false;
	private static boolean clientPasswordNeeded = false;
	private static boolean serverAuthenticated = false;
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

	/*
	 *  Performs all server initialization steps including comparing selected
	 *	security options to client security options and password authentication
	 */
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
		while(clientPasswordNeeded) {
			authorizeServerPassword();
			waitForMessage(serverInboxDir);
			f = new File(messageFilePath);
			if(f.exists()) {
				authenticateClientMessage(messageFilePath, f);
			}
		}

	}

	/*
	 *  Compare user input to pbkdf2 hashed password
	 */
	private static void authorizeServerPassword() {

		while(!serverAuthenticated) {
			File f = new File(passwordDir + serverPass);
			if(!(f.exists() && !f.isDirectory())) {
				//if no server password file exists, create one 
				
				// create password
				String newPassword;
				try {
					newPassword = generateStrongPasswordHash(new String(console.readPassword("Please enter a password: ")));

					Files.write(Paths.get(passwordDir + serverPass), newPassword.getBytes());
					serverAuthenticated = true;


				}
				catch (Exception e) {
					e.printStackTrace();
				}

			}
			else {

				try {
					byte[] fileBytes = Files.readAllBytes(Paths.get(passwordDir + serverPass));
					String password = generateStrongPasswordHash(new String(console.readPassword("Please enter a password: ")));

					if(Arrays.equals(fileBytes, password.getBytes())) {
						serverAuthenticated = true;
					}
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	private static String generateStrongPasswordHash(String password) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		int iterations = 1000;
		char[] chars = password.toCharArray();
		byte[] salt = getSalt();
		PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 64 * 8);
		SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		byte[] hash = skf.generateSecret(spec).getEncoded();
		return iterations + ":" + toHex(salt) + ":" + toHex(hash);
	}

	private static byte[] getSalt() throws NoSuchAlgorithmException {
		byte[] salt = new byte[16];
		File f = new File(passwordDir + saltFileName);
		if((f.exists() && !f.isDirectory())) {
			try {
				salt = Files.readAllBytes(Paths.get(passwordDir + saltFileName));
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			return salt;

		}
		else {//generate new salt and save
			SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
			sr.nextBytes(salt);
			try {
				FileOutputStream fos = new FileOutputStream(passwordDir + saltFileName);
				fos.write(salt);
				fos.close();
			}
			catch(Exception e){
				e.printStackTrace();
			}
			return salt;
		}
	}

	private static String toHex(byte[] array) throws NoSuchAlgorithmException {
		BigInteger bi = new BigInteger(1, array);
		String hex = bi.toString(16);
		int paddingLength = (array.length * 2) - hex.length();

		if(paddingLength > 0) {
			return String.format("%0"  +paddingLength + "d", 0) + hex;
		}
		else{
			return hex;
		}
	}

	private static void authenticateClientMessage(String messageFilePath, File f) {
		boolean messageAuthenticated = false;

		// force encryption on initialization methods
		Message m = new Message();
		m.readMessageFile(messageFilePath, options, true);

		f.delete();

		if(m.getType() == Message.MESSAGE_TYPE_OPTIONS) {
			//if the client sent a request (not an IM message)

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
		else if(m.getType() == Message.MESSAGE_TYPE_PASSWORD) {
			//if client sent a password

			//compare to pw table
				f = new File(passwordDir + clientPass);
				if(!(f.exists() && !f.isDirectory())) {//if no client password file exists, create one 
					// create password
					try {
						Files.write(Paths.get(passwordDir + clientPass), generateStrongPasswordHash(new String(m.getContents())).getBytes());
						messageAuthenticated = true;

						clientPasswordNeeded = false;

					} catch (Exception e) {
						e.printStackTrace();
					}

				}else{

					try{
						byte[] fileBytes = Files.readAllBytes(Paths.get(passwordDir + clientPass));
					if(Arrays.equals(fileBytes, generateStrongPasswordHash(new String(m.getContents())).getBytes())){
							messageAuthenticated = true;

							clientPasswordNeeded = false;
							
						}
					}catch(Exception e){
						e.printStackTrace();
					}
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
		Message confirmMessage = new Message(messageType, "This is a confirmation message");
		confirmMessage.writePlainTextMessageFile(messageFilePath);
	}

	/*
	 *  Performs all client initialization steps including sending selected
	 *	options to server and password authentication
	 */
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
			clientPasswordNeeded = true;
			boolean success = false;

			while(!success) {
				String password = new String(console.readPassword("Please enter a password: "));

				//send password message
				Message m = new Message(Message.MESSAGE_TYPE_PASSWORD, password);
				m.writeMessageFile(messageSendFilePath, options, false);

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
						clientPasswordNeeded = false;
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

	/*
	 * Sets options array values from stdin input txt
	 */
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
			clientPasswordNeeded = true;
		}
	}

	/*
	 *  Sets optionsArr from a string of options in optionsString
	 */
	private static void setOptions(String optionsString, boolean[] optionsArr) {
		for(int i = 0; i < optionsArr.length; i++) {
			if(optionsString.charAt(i) == '1') {
				optionsArr[i] = true;
			}
		}
	}

	/*
	 * Sends a message with selected options to messageFilePath
	 */
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
		m.writeMessageFile(messageFilePath, options, true);
	}

	/*
	 * Wait for a message to be received in inboxDir
	 * Waits for a short ammount of time to allow file to be successfully 
	 *	written before it is read
	 */
	protected static void waitForMessage(String inboxDir) {
		int numFiles = 0;
		while(numFiles == 0) {
			numFiles = new File(inboxDir).listFiles(hiddenFileFilter).length;
		}

		try {
			// wait to ensure file is completely written
			MessageWriter.sleep(500);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 *  Hash message using SHA-1
	 */
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