import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
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
import java.nio.file.Path;
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
import javax.xml.bind.DatatypeConverter;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.security.MessageDigest;



public class Client {
  private final String USER_AGENT = "Mozilla/5.0";
  private static void printUsage(){
      System.out.println("Please use command line arguments to specify if this is a client or a server");
      System.out.println("run     java Client --server   to create a server");
      System.out.println("run     java Client --client   to create a client");
  }
  private static KeyPair pair;
  private static PrivateKey priv;
  public static PublicKey pub;
  public static String ptFileName = "plaintextMessage.txt";
  public static String encFileName = "encMessage";
  public static void main(String[] args) throws Exception {
    if(args.length != 1){

      printUsage();
      return;
    }

    if(args[0].equals("--server")){ 
      System.out.println("Starting server");
    } else if (args[0].equals("--client")){
      System.out.println("Starting client");
      Scanner sc = new Scanner(System.in);
      boolean options[] = new boolean[3];
      String securedMessage = "";


      options = getSecurityOptions(sc);
      String plaintextMessage = getUserMessage(sc);
      sc.close();
      establishSecureConnection();

      securedMessage = prepareMessage(plaintextMessage, options);

    } else {
      printUsage();
    }

  }

  // Pass in a String message and return a MD5 checksum
  private static String getMD5(String message){
    String result = null;
    try{
      MessageDigest digest = MessageDigest.getInstance("MD5");
      byte[] hash = digest.digest(message.getBytes("UTF-8"));
    } catch (Exception e){
      e.printStackTrace();
    }
    return result;
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
    	  
    	  Cipher AesCipher = Cipher.getInstance("AES");
    	  Path path = Paths.get("symKey");
    	  File f = new File("symKey");
    	  SecretKey secKey; 
    	  if(f.exists() && !f.isDirectory()) { 
    		
    	  byte[] key = Files.readAllBytes(path);
    	  secKey = new SecretKeySpec(key, "AES");
    	  }else{//if key not found, create key
    	  KeyGenerator KeyGen = KeyGenerator.getInstance("AES");
        
        KeyGen.init(128);

        secKey = KeyGen.generateKey();

        byte[] key = secKey.getEncoded();
        FileOutputStream keyfos = new FileOutputStream("symKey");
        keyfos.write(key);
        keyfos.close();
    	 }

        byte[] byteText = message.getBytes();

        AesCipher.init(Cipher.ENCRYPT_MODE, secKey);
        byte[] byteCipherText = AesCipher.doFinal(byteText);
        Files.write(Paths.get(encFileName), byteCipherText);

        //server side code
        byte[] cipherText = Files.readAllBytes(Paths.get(encFileName));

        AesCipher.init(Cipher.DECRYPT_MODE, secKey);
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
}

