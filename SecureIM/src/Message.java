import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


/*
 *  Class used for handling messages, storing message type and contents
 */
public class Message {

	public static final int MESSAGE_TYPE_OPTIONS = 0; // initialization message
	public static final int MESSAGE_TYPE_NORMAL = 1; // regular message
	public static final int MESSAGE_TYPE_CONFIRM = 2; // initializatioin confirmation message
	public static final int MESSAGE_TYPE_PASSWORD = 3; // password
	public static final int MESSAGE_TYPE_WRONG_PASSWORD = 4; // password failed message

	public static boolean[] options = new boolean[3];

	private Integer type;
	private String contents;

	public Message() {}

	public Message(int type, String contents) {
		this.type = type;
		this.contents = contents;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.contents = contents;
	}

	/*
	 *  Write a plain text message to a file
	 *  No encryption done here
	 */
	public void writePlainTextMessageFile(String messageFilePath) {

		if(type == null) {
			System.out.println("Message type not set. Can not send message.");
			return;
		}

		// write message type as the first line
		String messageContent = type + "\n" + contents;

		byte[] fileBytes = messageContent.getBytes();

		try {
			Files.write(Paths.get(messageFilePath), fileBytes);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 *  Write a message to a file with encryption if it is specified in options.
	 *  Encryption can be forced with the forceEncrypt parameter (used for checksum files)
	 */
	public void writeMessageFile(String messageFilePath, boolean[] options, boolean forceEncrypt) {

		if(type == null) {
			System.out.println("Message type not set. Can not send message.");
			return;
		}

		// write message type as the first line
		String messageContent = type + "\n" + contents;

		byte[] fileBytes = messageContent.getBytes();
		
		// encrypt message (apply confidentiality)
		if(options[0] || forceEncrypt) {
			try {
				System.out.println("encrypting");
				Cipher aesCipher = Cipher.getInstance("AES");
				aesCipher.init(Cipher.ENCRYPT_MODE, generateOrGetSecretKey());
				fileBytes = aesCipher.doFinal(fileBytes);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}

		try {
			Files.write(Paths.get(messageFilePath), fileBytes);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 *  Read a plain text message to a file
	 *  No encryption done here
	 */
	public void readPlainTextMessageFile(String messageFilePath) {
		try {
			byte[] fileBytes = Files.readAllBytes(Paths.get(messageFilePath));

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

	/*
	 *  Write a message to a file with encryption if it is specified in options.
	 *  Encryption can be forced with the forceEncrypt parameter (used for checksum 
	 *  files and initialization messages)
	 */
	public void readMessageFile(String messageFilePath, boolean[] options, boolean forceDecrypt) {

		try {
			byte[] fileBytes = Files.readAllBytes(Paths.get(messageFilePath));

			// decrypt
			if(options[0] || forceDecrypt) {
				System.out.println("decrypting");
				Cipher aesCipher = Cipher.getInstance("AES");
				aesCipher.init(Cipher.DECRYPT_MODE, generateOrGetSecretKey());
				fileBytes = aesCipher.doFinal(fileBytes);
			}

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

	/*
	 *  Gets a secret key stored in a file or generates a new
	 *  secret key if the file does not exist
	 */
	private static SecretKey generateOrGetSecretKey(){
		try {
			SecretKey secKey;
			Path path = Paths.get("symKey");
			File f = new File("symKey");

			if(f.exists() && !f.isDirectory()) {
				// key found
				byte[] key = Files.readAllBytes(path);
				secKey = new SecretKeySpec(key, "AES");
			}
			else {
				//key not found, create key
				System.out.println("Generating new AES Key");
				KeyGenerator KeyGen = KeyGenerator.getInstance("AES");

				KeyGen.init(128);

				secKey = KeyGen.generateKey();

				byte[] key = secKey.getEncoded();
				FileOutputStream keyfos = new FileOutputStream("symKey");
				keyfos.write(key);
				keyfos.close();
			}
			return secKey;
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		// should never get here
		return null;
	}

}