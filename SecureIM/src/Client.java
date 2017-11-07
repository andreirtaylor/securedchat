import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;


public class Client {
	private final String USER_AGENT = "Mozilla/5.0";
	private static KeyPair pair;
	private static PrivateKey priv;
	public static PublicKey pub;
	public static String ptFileName = "plaintextMessage.txt";
	public static String encFileName = "encMessage";
	public static void main(String[] args) throws Exception {
		
		Scanner sc = new Scanner(System.in);
		boolean options[] = new boolean[3]; 
		String securedMessage = "";
		
		
		options = getSecurityOptions(sc);
		String plaintextMessage = getUserMessage(sc);
		sc.close();
		establishSecureConnection();
		
		securedMessage = prepareMessage(plaintextMessage, options);
		
		Client http = new Client();
		
		
		

		http.sendPost(securedMessage);
	}


	private static String getUserMessage(Scanner sc) {
		System.out.println("Enter a message");

		String message = sc.nextLine();
		return message;
	}


	private static void establishSecureConnection() {
		// TODO esablish secure connection
		
	}


	private static String prepareMessage(String message,boolean[] options) {
		if(options[1]){
			//apply integrity
			
			try {
			createKeyPair();
			
			//sign message witha  signature
			Signature dsa = Signature.getInstance("SHA1withDSA", "SUN"); 
			dsa.initSign(priv);
			//InputStream stream = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8.name()));
			
			FileInputStream msgFis = new FileInputStream("message");
			BufferedInputStream bufin = new BufferedInputStream(msgFis);
			byte[] buffer = new byte[1024];
			int len;
			while ((len = bufin.read(buffer)) >= 0) {
			    dsa.update(buffer, 0, len);
			};
			bufin.close();
			//save signature to a file
			byte[] realSig = dsa.sign();
			FileOutputStream sigfos = new FileOutputStream("sig");
			sigfos.write(realSig);
			sigfos.close();
			
			/* save the public key in a file */
			byte[] key = pub.getEncoded();
			FileOutputStream keyfos = new FileOutputStream("clientpk");
			keyfos.write(key);
			keyfos.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
			
			
		}
		if(options[2]){
			//apply authentication
		
		}
		if(options[0]){
			//apply confidentiality
			try{
			KeyGenerator KeyGen = KeyGenerator.getInstance("AES");
	        KeyGen.init(128);

	        SecretKey SecKey = KeyGen.generateKey();

	        Cipher AesCipher = Cipher.getInstance("AES");


	        byte[] byteText = message.getBytes();

	        AesCipher.init(Cipher.ENCRYPT_MODE, SecKey);
	        byte[] byteCipherText = AesCipher.doFinal(byteText);
	        Files.write(Paths.get(encFileName), byteCipherText);

//server side code
	        byte[] cipherText = Files.readAllBytes(Paths.get(encFileName));

	        AesCipher.init(Cipher.DECRYPT_MODE, SecKey);
	        byte[] bytePlainText = AesCipher.doFinal(cipherText);
	        Files.write(Paths.get(ptFileName), bytePlainText);
			}catch(Exception e){
				
			}
			
		}
			return message;
	}


	private static void createKeyPair() throws NoSuchAlgorithmException,
			NoSuchProviderException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
		SecureRandom random;
			random = SecureRandom.getInstance("SHA1PRNG", "SUN");
			keyGen.initialize(1024, random);
			pair = keyGen.generateKeyPair();
			priv = pair.getPrivate();
			pub = pair.getPublic();
	}


	private static boolean[] getSecurityOptions(Scanner sc) {
		boolean options[] = new boolean[3];
		System.out.println("Enter what security Properties (For confidentiality and integrity, input 'ci'. For confidentiality integrity, and authentication, type 'cia')");
		String input = sc.nextLine();

		if(input.contains("c")){
			options[0] = true;
			System.out.println("Confidentiality");
		}
		if(input.contains("i")){
			options[1] = true;
			System.out.println("integrity");
		}
		if(input.contains("a")){
			options[2] = true;
			System.out.println("authentication");
		}
		
		
		return options;
	}


		// HTTP GET request
		private void sendGet() throws Exception {

			String url = "localhost:6969";

			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			// optional default is GET
			con.setRequestMethod("GET");

			//add request header
			con.setRequestProperty("User-Agent", USER_AGENT);

			int responseCode = con.getResponseCode();
			System.out.println("\nSending 'GET' request to URL : " + url);
			System.out.println("Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(
			        new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			//print result
			System.out.println(response.toString());

		}

		// HTTP POST request
		private void sendPost(String securedMessage) throws Exception {

			String url = "http://localhost:6969/connect";
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			con.setDoInput(true);
			con.setDoOutput(true);
			byte[] outputInBytes = securedMessage.getBytes("UTF-8");
			OutputStream os = con.getOutputStream();
			os.write( outputInBytes );    
			os.close();
//			//add reuqest header
//			con.setRequestMethod("POST");
//			con.setRequestProperty("User-Agent", USER_AGENT);
//			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
//
//			String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";
//
//			// Send post request
//			con.setDoOutput(true);
//			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
//			wr.writeBytes(urlParameters);
//			wr.flush();
//			wr.close();
//
//			int responseCode = con.getResponseCode();
//			System.out.println("\nSending 'POST' request to URL : " + url);
//			System.out.println("Post parameters : " + urlParameters);
//			System.out.println("Response Code : " + responseCode);
//
			BufferedReader in = new BufferedReader(
			        new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			//print result
			System.out.println(response.toString());

		}
		
	}

