import java.util.Scanner;
import java.io.File;

/*
 *  Class to get input from std in and store it in a message file (sending it to the server)
 */
public class MessageWriter extends Thread {
	
	private Scanner scanner;
	private String inputBuffer;
	private String messagePrompt;
	private String messageFilePath;
	private boolean[] options;

	MessageWriter(String messagePrompt, String inputBuffer, String inboxDir, boolean[] options) {
		this.inputBuffer = inputBuffer;
		this.messagePrompt = messagePrompt;
		this.messageFilePath = inboxDir + SecureChat.messageName;
		this.options = options;
		scanner = new Scanner(System.in);
	}

	public void run() {
		while(true) {
			System.out.print(messagePrompt);
			String input = scanner.nextLine();

			Message m = new Message(Message.MESSAGE_TYPE_NORMAL, input);
			m.writeMessageFile(messageFilePath, options, false);

			// send checksum (apply integrity)
			if(options[1]) {
				try {
					String hashMessagePath = messageFilePath + SecureChat.checksumExtension;
					String hashMessage = new String(SecureChat.getHash(input), "UTF-8");

					// always force encrypt the checksum
					m = new Message(Message.MESSAGE_TYPE_NORMAL, hashMessage);
					m.writeMessageFile(hashMessagePath, options, true);
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}