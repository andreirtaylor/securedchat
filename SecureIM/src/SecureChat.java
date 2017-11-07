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
import java.util.concurrent.TimeUnit;



public class Client {

  private static void printUsage(){
    System.out.println("Please use command line arguments to specify if this is a client or a server");
    System.out.println("run     java Client --server   to create a server");
    System.out.println("run     java Client --client   to create a client");
  }

  private static KeyPair pair;
  private static PrivateKey priv;

  public static PublicKey pub;
  public static String ptFileName = "plaintextMessage.txt";

  // incomming and outgoing folders
  public static String serverIncomming = "serverIncomming/";
  public static String serverOutgoing = "serverOutgoing/";


  // where to save files
  public static String encFileName = "encMessage";

  public static void main(String[] args) throws Exception {
    if(args.length != 1){
      printUsage();
      return;
    }

    Scanner sc = new Scanner(System.in);
    if(args[0].equals("--server")){ 
      System.out.println("Starting server");

      while(true){
        doServer(sc);
      }
    } else if (args[0].equals("--client")){
      System.out.println("Starting client");

      while(true){

        boolean[] options = getSecurityOptions(sc);
        String plaintextMessage = getUserMessage(sc);
        establishSecureConnection();

        String securedMessage = prepareMessage(plaintextMessage, options, sc);
        //System.out.println(securedMessage);
      }
    } else {
      printUsage();
    }

  }

  private static void doServer(Scanner sc) throws InterruptedException {
    int len = new File(serverIncomming).listFiles().length;
    if(len > 0){
      String encryptedMessage = serverIncomming + encFileName;
      try{
        //server side code

        byte[] cipherText = Files.readAllBytes(Paths.get(encryptedMessage));
        Cipher AesCipher = Cipher.getInstance("AES");
        AesCipher.init(Cipher.DECRYPT_MODE, generateOrGetSecretKey());

        byte[] bytePlainText = AesCipher.doFinal(cipherText);
        System.out.println(new String(bytePlainText, "UTF-8"));

      } catch (Exception e){
        e.printStackTrace();
      }

      File f = new File(encryptedMessage);
      f.delete();
    }
    TimeUnit.SECONDS.sleep(1);
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


  private static String prepareMessage(String message,boolean[] options, Scanner sc) {
    byte[] BytesToWrite = message.getBytes();
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
      System.out.println("Exter your password for authentication");
      String password = sc.nextLine();
      message += "\n" + password;
    }
    if(options[0]){
      //apply confidentiality
      try{
        Cipher AesCipher = Cipher.getInstance("AES");
        AesCipher.init(Cipher.ENCRYPT_MODE, generateOrGetSecretKey());

        BytesToWrite = AesCipher.doFinal(message.getBytes());
      }catch(Exception e){

      }

    }
    // always wrtite the file
    try{
      Files.write(Paths.get(serverIncomming + encFileName), BytesToWrite);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return message;
  }

  private static SecretKey generateOrGetSecretKey(){
    try{
      SecretKey secKey;
      Path path = Paths.get("symKey");
      File f = new File("symKey");


      if(f.exists() && !f.isDirectory()) { 
        System.out.println("AES key exists, using previous key");
        byte[] key = Files.readAllBytes(path);
        secKey = new SecretKeySpec(key, "AES");
      }else{//if key not found, create key
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
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }


  private static void createKeyPair() throws NoSuchAlgorithmException,
          NoSuchProviderException {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
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

