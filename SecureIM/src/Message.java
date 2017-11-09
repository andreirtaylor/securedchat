import java.nio.file.Paths;
import java.nio.file.Files;

public class Message {

	public static final int MESSAGE_TYPE_OPTIONS = 0; // initialization message
	public static final int MESSAGE_TYPE_NORMAL = 1; // regular message
	public static final int MESSAGE_TYPE_CONFIRM = 2; // initializatioin confirmation message
	public static final int MESSAGE_TYPE_PASSWORD = 3; // initializatioin confirmation message
	public static boolean[] options = new boolean[3];

	private int type;
	private String contents;

	public Message() {}

	public Message(int type, String contents) {
		this.type = type;
		this.contents = contents;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.contents = contents;
	}

	public void writeMessageFile(String messageFilePath, boolean[] options) {

		// write message type as the first line
		String messageContent = type + "\n" + contents;

		byte[] fileBytes = messageContent.getBytes();
		
		// do encryption, etc
		// ...

		try {
			Files.write(Paths.get(messageFilePath), fileBytes);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void readMessageFile(String messageFilePath, boolean[] options) {

		try {
			byte[] fileBytes = Files.readAllBytes(Paths.get(messageFilePath));

			// do options to decrypt

			String messageContent = new String(fileBytes, "UTF-8");

			String t = messageContent.substring(0, 1);
			type = Integer.parseInt(t);

			// remove the 2 first chars that indicated message type
			contents = messageContent.substring(2);

		}
		catch(Exception e) {
			e.printStackTrace();
		}

	}

}