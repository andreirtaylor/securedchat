import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Message {

	public static final int MESSAGE_TYPE_OPTIONS = 0; // initialization message
	public static final int MESSAGE_TYPE_NORMAL = 1; // regular message
	public static final int MESSAGE_TYPE_CONFIRM = 2; // initializatioin confirmation message
	public static final int MESSAGE_TYPE_PASSWORD = 3; // password
	public static final int MESSAGE_TYPE_WRONG_PASSWORD = 4; // password failed message

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
		
		// encrypt message (apply confidentiality)
		if(options[0] && type == MESSAGE_TYPE_NORMAL) {
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

		// // use checksum (apply integrity)
		// if(options[1]) {
		// 	try {
		// 		byte[] hashMessage = getMD5(messageContent);

		// 		// if you are using confidentiality
		// 		Cipher aesCipher = Cipher.getInstance("AES");
		// 		aesCipher.init(Cipher.ENCRYPT_MODE, generateOrGetSecretKey());

		// 		hashMessage = aesCipher.doFinal(hashMessage);

		// 		Files.write(Paths.get(serverIncomming + checkSumFile), hashMessage);
		// 	}
		// 	catch(Exception e) {
		// 		e.printStackTrace();
		// 	}
		// }
		

		try {
			Files.write(Paths.get(messageFilePath), fileBytes);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

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
	 * Does encryption and checks checksum
	 * Should only be used for MESSAGE_TYPE_NORMAL
	 */
	public void readMessageFile(String messageFilePath, boolean[] options) {

		try {
			byte[] fileBytes = Files.readAllBytes(Paths.get(messageFilePath));

			// decrypt
			if(options[0]) {
				System.out.println("decrypting");
				Cipher aesCipher = Cipher.getInstance("AES");
				aesCipher.init(Cipher.DECRYPT_MODE, generateOrGetSecretKey());
				fileBytes = aesCipher.doFinal(fileBytes);
			}

			// checksum
			if(options[1]) {

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