import java.io.File;
import java.io.FileFilter;

/*
 *  Reads messages from message files and writes contents to std out
 */
public class MessageReader extends Thread {

	private String inputBuffer;
	private String messagePrompt;
	private String inboxDir;
	private boolean[] options;
	

	public static FileFilter hiddenFileFilter = new FileFilter() {
		public boolean accept(File file) {
			if(file.isHidden()) {
				return false;
			}
			return true;
		}
	};

	public MessageReader(String messagePrompt, String inputBuffer, String inboxDir, boolean[] options) {
		this.inputBuffer = inputBuffer;
		this.messagePrompt = messagePrompt;
		this.inboxDir = inboxDir;
		this.options = options;
	}
	
	public void run() {

		try {
			while(true) {

				// check if new messages exist
				SecureChat2.waitForMessage(inboxDir);
				
				String messageFilePath = inboxDir + SecureChat2.messageName;
				File f = new File(messageFilePath);

				if(f.exists()) {
					boolean validMessage = false;

					// check if regular message file exists
					Message m = new Message();
					m.readMessageFile(messageFilePath, options, false);
					f.delete();

					if(options[1]) {
						// wait for checksum file to arrive
						SecureChat2.waitForMessage(inboxDir);

						String checksumMessageFilePath = messageFilePath + SecureChat2.checksumExtension;
						f = new File(checksumMessageFilePath);

						if(f.exists()) {
							// retrieve the checksum message, always decrypt
							Message checksumMessage = new Message();
							checksumMessage.readMessageFile(checksumMessageFilePath, options, true);
							f.delete();

							String newMessageHash = new String(SecureChat2.getHash(m.getContents()), "UTF-8");

							if(checksumMessage.getContents().equals(newMessageHash)) {
								println("Checksum successful");
								validMessage = true;
							}

						}
						else {
							println("Checksum was not received. Can not validate message.");
						}
					}
					else {
						// always valid if checksum isn't used
						validMessage = true;
					}

					if(validMessage) {
						println("received message: " + m.getContents());
					}
					else {
						println("The checksum does not match. Message is invalid.");
					}
					
					System.out.print(messagePrompt);
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	/* 
	 *  Removes the message prompt before printing the message
	 */
	public void println(String message) {
		erase(messagePrompt.length());
		System.out.println(message);
	}

	public void erase(int length) {
		for(int i = 0; i < length; i++) {
			System.out.print("\b");
		}

		for(int i = 0; i < length; i++) {
			System.out.print(" ");
		}

		for(int i = 0; i < length; i++) {
			System.out.print("\b");
		}
	}
}