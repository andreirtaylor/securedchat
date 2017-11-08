import java.io.File;
import java.io.FileFilter;

/*
 *  Reads messages from message files and writes contents to std out
 */
public class MessageReader extends Thread {

	private String inputBuffer;
	private String messagePrompt;
	private String inboxDir;
	private static final String messageName = "message";

	public static FileFilter hiddenFileFilter = new FileFilter() {
		public boolean accept(File file) {
			if(file.isHidden()) {
				return false;
			}
			return true;
		}
	};

	public MessageReader(String messagePrompt, String inputBuffer, String inboxDir) {
		this.inputBuffer = inputBuffer;
		this.messagePrompt = messagePrompt;
		this.inboxDir = inboxDir;

		File inbox = new File(inboxDir);
		if(!inbox.exists()) {
			inbox.mkdir();
		}
	}
	
	public void run() {

		try {
			while(true) {

				// check if new messages exist
				int numFiles = new File(inboxDir).listFiles(hiddenFileFilter).length;

				if(numFiles > 0) {

					// assume only 1 message file exists for now

					// check file exists
					String messageFilePath = inboxDir + messageName;
					File f = new File(messageFilePath);

					if(f.exists()) {

						Message m = new Message();
						m.readMessageFile(messageFilePath);
						f.delete();

						erase(messagePrompt.length() + inputBuffer.length());
						System.out.println("received message: " + m.getContents());
						System.out.print(messagePrompt);
					}
				}

				// check for new message every 0.5s
				sleep(500);
			}
		}
		catch(InterruptedException e) {
			System.out.println("Thread " + getName() + " interrupted.");
		}
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