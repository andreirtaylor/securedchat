import java.util.Scanner;
import java.io.File;
import java.io.FileFilter;

public class SecureChat2 {

	private static final String serverInboxDir = "serverInbox/";
	private static final String clientInboxDir = "clientInbox/";
	private static final String messageName = "message";

	private static Scanner scanner;
	private static boolean start = false;
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
			MessageReader reader = new MessageReader(messagePrompt, inputBuffer, readInboxDir);
			reader.setName("reader");
			reader.start();

			MessageWriter writer = new MessageWriter(messagePrompt, inputBuffer, writeInboxDir);
			writer.setName("writer");
			writer.start();
		}

	}

	private static void initServer() {
		// set CIA options
		getSecurityOptions();

		System.out.println("Waiting for client.");

		// wait for client initialize
		int numFiles = 0;
		while(numFiles == 0) {
			numFiles = new File(serverInboxDir).listFiles(hiddenFileFilter).length;
		}

		// read CIA options in message
		String messageFilePath = serverInboxDir + messageName;
		File f = new File(messageFilePath);

		if(f.exists()) {
			Message m = new Message();
			m.readMessageFile(messageFilePath);
			if(m.getType() == Message.MESSAGE_TYPE_INIT) {
				f.delete();

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
					messageFilePath = clientInboxDir + messageName;
					Message confirmMessage = new Message(Message.MESSAGE_TYPE_CONFIRM_INIT, "");
					confirmMessage.writeMessageFile(messageFilePath);
				}
				else {
					System.out.println("Options don't match. Cannot create connection.");
					start = false;	
				}

			}
			else {
				start = false;
			}
		}

	}

	private static void initClient() {
		// check password
		// ...

		// select CIA options
		getSecurityOptions();

		// attempt to create establish connection (send CIA options)
		sendOptionsMessage(serverInboxDir + messageName);

		System.out.println("Waiting for confirmation from server.");
		// wait for confirmation message
		int numFiles = 0;
		while(numFiles == 0) {
			numFiles = new File(clientInboxDir).listFiles(hiddenFileFilter).length;
		}

		// check file exists and that it is the correct message type
		String messageFilePath = clientInboxDir + messageName;
		File f = new File(messageFilePath);

		if(f.exists()) {
			Message m = new Message();
			m.readMessageFile(messageFilePath);
			f.delete();
			if(m.getType() == Message.MESSAGE_TYPE_CONFIRM_INIT) {
				System.out.println("Session established successfully.");
			}
			else {
				start = false;
			}
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

		Message m = new Message(Message.MESSAGE_TYPE_INIT, optionsString);
		m.writeMessageFile(messageFilePath);
	}

	private static void printUsage(){
		System.out.println("Please use command line arguments to specify if this is a client or a server");
		System.out.println("run     java SecureChat --server   to create a server");
		System.out.println("run     java SecureChat --client   to create a client");
	}
}