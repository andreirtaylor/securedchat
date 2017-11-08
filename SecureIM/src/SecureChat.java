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



public class SecureChat {

  private static void printUsage(){
    System.out.println("Please use command line arguments to specify if this is a client or a server");
    System.out.println("run     java Client --server   to create a server");
    System.out.println("run     java Client --client   to create a client");
  }

  private static KeyPair pair;
  private static PrivateKey priv;

  public static PublicKey pub;

  // incomming and outgoing folders
  public static String serverIncomming = "serverIncomming/";
  public static String serverOutgoing = "serverOutgoing/";


  // where to save files
  public static String encFileName = "encMessage";
  public static String checkSumFile = "checkSumFile";
  public static String optionsFile = "optionsFile";

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

  // 0 -> Confidentiality
  // 1 -> integrity
  // 2 -> authentication
  private static boolean[] getMessageOptions(){
      boolean[] ret = null;
      try{
        String optionsfn = serverIncomming + optionsFile;
        //server side code
        byte[] cipherText = Files.readAllBytes(Paths.get(optionsfn ));
        Cipher AesCipher = Cipher.getInstance("AES");
        AesCipher.init(Cipher.DECRYPT_MODE, generateOrGetSecretKey());

        byte[] bytePlainText = AesCipher.doFinal(cipherText);
        System.out.println("Options sent");
        String s = new String(bytePlainText, "UTF-8");
        ret = new boolean[s.length()];
        for(int i = 0; i < s.length(); ++i){
          ret[i] = s.charAt(i) == '1' ? true : false;
        }
        if(!ret[0] && !ret[1] && !ret[2]) System.out.print("None");
        if(ret[0]) System.out.print("Confidentiality ");
        if(ret[1]) System.out.print("Integrity ");
        if(ret[2]) System.out.print("Authentication ");
        System.out.println();

        File f = new File(optionsfn);
        f.delete();
      } catch (Exception e){
        e.printStackTrace();
      }
      return ret;
  }

  private static void doServer(Scanner sc) throws InterruptedException {
    int len = new File(serverIncomming).listFiles().length;
    if(len >= 2){
      //TimeUnit.MILLISECONDS.sleep(5000);
      TimeUnit.MILLISECONDS.sleep(500);

      String encryptedMessage = serverIncomming + encFileName;
      String encryptedCheckSum = serverIncomming + checkSumFile;

      try{
        //server side code
        boolean[] options = getMessageOptions();

        String text = "";

        // if confidentiality
        if(options[0]){
          byte[] b = Files.readAllBytes(Paths.get(encryptedMessage));
          Cipher AesCipher = Cipher.getInstance("AES");
          AesCipher.init(Cipher.DECRYPT_MODE, generateOrGetSecretKey());

          byte[] bytePlainText = AesCipher.doFinal(b);
          text = new String(bytePlainText, "UTF-8");
        } else {
          byte[] b = Files.readAllBytes(Paths.get(encryptedMessage));
          text = new String(b, "UTF-8");
        }

        if(options[1]){
          byte[] cs = Files.readAllBytes(Paths.get(encryptedCheckSum));
          Cipher AesCipher = Cipher.getInstance("AES");
          AesCipher.init(Cipher.DECRYPT_MODE, generateOrGetSecretKey());

          byte[] bytePlainText = AesCipher.doFinal(cs);
          String sentCS = new String(bytePlainText, "UTF-8");
          String generatedCS = new String(getMD5(text), "UTF-8");

          if(! sentCS.equals(generatedCS)){
            System.out.println("The checksum does not match, invalid message");
            return;
          }
        }

        if(options[2]){
          String[] msg_pwd = text.split("\n");
          text = msg_pwd[0];
          String pwd = msg_pwd[1];
          System.out.println("Password from client");
          System.out.println(pwd);
          // TODO setup the password testing
        }

        System.out.println("Message from client");
        System.out.println(text);


      } catch (Exception e){
        e.printStackTrace();
      }

      // remove all files
      File dir = new File(serverIncomming);
      for (File file: dir.listFiles()) {
        file.delete();
      }
    }
    TimeUnit.SECONDS.sleep(1);
  }

  // Pass in a String message and return a MD5 checksum
  private static byte[] getMD5(String message){
    byte[] hash = null;
    try{
      MessageDigest digest = MessageDigest.getInstance("MD5");
      hash = digest.digest(message.getBytes("UTF-8"));
    } catch (Exception e){
      e.printStackTrace();
    }
    return hash;
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

    // write the options file
    try{
      String s = "";
      for(int i = 0; i < options.length ;  ++i){
        if(options[i]){
          s += "1";
        } else {
          s += "0";
        }
      }

      // always encrypt the options file
      Cipher AesCipher = Cipher.getInstance("AES");
      AesCipher.init(Cipher.ENCRYPT_MODE, generateOrGetSecretKey());

      Files.write(Paths.get(serverIncomming + optionsFile)
                  , AesCipher.doFinal(s.getBytes()));

    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }


    if(options[1]){
      try{
        //apply integrity
        byte[] hashMessage = getMD5(message);

        // if you are using confidentiality
        Cipher AesCipher = Cipher.getInstance("AES");
        AesCipher.init(Cipher.ENCRYPT_MODE, generateOrGetSecretKey());

        hashMessage = AesCipher.doFinal(hashMessage);

        Files.write(Paths.get(serverIncomming + checkSumFile), hashMessage);
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
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
        //System.out.println("AES key exists, using previous key");
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

    // should never get here
    return null;
  }

  // 0 -> Confidentiality
  // 1 -> integrity
  // 2 -> authentication
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

