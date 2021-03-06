import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class Client{
	private static String clientID;
	private static int serverInstances;
	private static String[] servers;
	private static final boolean DEBUG = false;
	
	public static void main(String args[]) throws IOException{
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		String str = null;
		try{
			str = input.readLine();
			String inputs[] = str.split(" ");
			if(inputs.length != 2){
				throw new IllegalArgumentException("Must provide Client ID and Number of Server Instances");
			}
			clientID = inputs[0];
			try{
				serverInstances = Integer.parseInt(inputs[1]);
			}
			catch(NumberFormatException e){
				throw new IllegalArgumentException("Must provide valid integers");
			}
			if(clientID.charAt(0) != 'c'){
				throw new IllegalArgumentException("Must provide valid client ID");
			}
			if(DEBUG){
				System.out.println("Client ID: " + clientID);
				System.out.println("Number of Servers: " + serverInstances);
				System.out.println("------------------------------------------------------------------");
			}
			servers = new String[serverInstances];
			for(int k = 0; k < serverInstances; k += 1){
				servers[k] = input.readLine();
				if(servers[k] == null){
					throw new IllegalArgumentException("Must provide correct number of servers");
				}
			}
			while(true){
				try{
					str = input.readLine();
				}
				catch(IOException e){
					break;
				}
				if(str == null){
					break;
				}	
				inputs = str.split(" ");
				if(inputs[0].equals("sleep")){
					int sleep;
					try{
						sleep = Integer.parseInt(inputs[1]);
					}
					catch(NumberFormatException e){
						continue;
					}
					try{
						Thread.sleep(sleep);
						if(DEBUG){
							System.out.println("Thread sleeping for " + sleep + " milliseconds");
							System.out.println("------------------------------------------------------------------");
						}
					}
					catch(InterruptedException e){
						continue;
					}
				}
				else if(inputs[0].charAt(0) == 'b'){
					String toSend = clientID + " " + inputs[0] + " " + inputs[1];
					TCP_Client(toSend);
				}
			}
		}
		catch(IOException e){
			e.printStackTrace();
			str = "";
		}
		input.close();
	}
	
	private static void TCP_Client(String toSend) throws IOException {	
		Socket toServer;
		int k = 0;
		try{
			while(true){
				try{
					InetAddress ia;
					int port;
					String[] address = servers[k].split(":");
					if(address[0].equals("localhost")){
						ia = InetAddress.getLocalHost();
					}
					else{
						ia = InetAddress.getByName(address[0]);
					}
					port = Integer.parseInt(address[1]);
					toServer = new Socket(ia, port);
					k = (k+1) % serverInstances;
					BufferedReader bReader = new BufferedReader(new InputStreamReader(toServer.getInputStream()));
					PrintStream pStream = new PrintStream(toServer.getOutputStream());
					if(DEBUG){
						System.out.println("Sending Message: " + toSend);
						System.out.println("------------------------------------------------------------------");
					}
					pStream.println(toSend);
					pStream.flush();
					String res = bReader.readLine();
					System.out.println(res) ;
					toServer.close();
					break;
				}
				catch(SocketTimeoutException e){
					
				}
			}
		}
		catch(UnknownHostException e){
			e.printStackTrace();
		}
		
		
		
	}
}