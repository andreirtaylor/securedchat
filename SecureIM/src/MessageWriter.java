import java.util.Scanner;
import java.io.File;

/*
 *  Gets input from std in and stores it in a message file
 */
public class MessageWriter extends Thread {
	
	private Scanner scanner;
	private String inputBuffer;
	private String messagePrompt;
	private static final String messageFileName = "message";
	private String messageFilePath;

	MessageWriter(String messagePrompt, String inputBuffer, String inboxDir) {
		this.inputBuffer = inputBuffer;
		this.messagePrompt = messagePrompt;
		this.messageFilePath = inboxDir + messageFileName;
		scanner = new Scanner(System.in);

		File inbox = new File(inboxDir);
		if(!inbox.exists()) {
			inbox.mkdir();
		}
	}

	public void run() {
		while(true) {
			System.out.print(messagePrompt);
			String input = scanner.nextLine();

			Message m = new Message(Message.MESSAGE_TYPE_NORMAL, input);
			m.writeMessageFile(messageFilePath);
		}
	}
}