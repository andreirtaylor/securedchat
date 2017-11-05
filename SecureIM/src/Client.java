import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.net.HttpURLConnection;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;


public class Client {
	private final String USER_AGENT = "Mozilla/5.0";

	public static void main(String[] args) throws Exception {
		
		
		boolean options[] = new boolean[3]; 
		String securedMessage = "";
		String plaintextMessage = "";
		
		options = getSecurityOptions();
		
		establishSecureConnection();
		securedMessage = prepareMessage(plaintextMessage, options);
		
		Client http = new Client();
		
		
		

		http.sendPost(securedMessage);
	}


	private static void establishSecureConnection() {
		// TODO Auto-generated method stub
		
	}


	private static String prepareMessage(String message,boolean[] options) {
		if(options[1]){
			//apply integrity
		}
		if(options[2]){
			//apply authentication
		
		}
		if(options[0]){
			//apply confidentiality
		}
			return message;
	}


	private static boolean[] getSecurityOptions() {
		boolean options[] = new boolean[3];
		System.out.println("Enter what security Properties (For confidentiality and integrity, input 'ci'. For confidentiality integrity, and authentication, type 'cia')");
		Scanner sc = new Scanner(System.in);
		String input = sc.nextLine();
		sc.close();
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

			//add reuqest header
			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent", USER_AGENT);
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

			String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";

			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();
			System.out.println("\nSending 'POST' request to URL : " + url);
			System.out.println("Post parameters : " + urlParameters);
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
		
	}

