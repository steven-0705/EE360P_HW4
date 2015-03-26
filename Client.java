import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class Client{
	private static int clientID;
	private static int serverInstances;
	private static String[] servers;
	
	public static void main(String args[]) throws IOException{
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		String str = null;
		try{
			str = input.readLine();
			String inputs[] = str.split(" ");
			if(inputs.length != 2){
				throw new IllegalArgumentException("Must provide Client ID and Number of Server Instances");
			}
			try{
				clientID = Integer.parseInt(inputs[0]);
				serverInstances = Integer.parseInt(inputs[1]);
			}
			catch(NumberFormatException e){
				throw new IllegalArgumentException("Must provide valid integers");
			}
			if(clientID <= 0){
				throw new IllegalArgumentException("Must provide non-negative integers");
			}
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
				if((inputs.length == 2) && (inputs[0] == "sleep")){
					int sleep;
					try{
						sleep = Integer.parseInt(inputs[1]);
					}
					catch(NumberFormatException e){
						continue;
					}
					try{
						Thread.sleep(sleep);
					}
					catch(InterruptedException e){
						continue;
					}
				}
				else if((inputs.length == 2) && (inputs[0].charAt(0) == 'b')){
					TCP_Client(str);
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
					pStream.println(toSend);
					pStream.flush();
					String res = bReader.readLine();
					System.out.println(res) ;
					toServer.close();
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