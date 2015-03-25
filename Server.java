import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Server{
	private static int[] owners;
	private static int clientsServed; // counter to track number of clients serviced
	private static String[] servers;
	
	public static void main(String args[]){
		final int serverID, serverInstances, numBooks;
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		String str = null;
		try{
			str = input.readLine();
			String[] inputs = str.split(" ");
			if(inputs.length != 3){
				throw new IllegalArgumentException("Must provide Server ID, number of Server Instances, and Number of Books");
			}
			try{
				serverID = Integer.parseInt(inputs[0]);
				serverInstances = Integer.parseInt(inputs[1]);
				numBooks = Integer.parseInt(inputs[2]);
			}
			catch(NumberFormatException e){
				throw new IllegalArgumentException("Must provide valid integers");
			}
			if((serverID <= 0) || (serverInstances <= 0) || (numBooks <= 0)){
				throw new IllegalArgumentException("Must provide non-negative integers");
			}
			owners = new int[numBooks];
			servers = new String[serverInstances];
			for(int k = 0; k < owners.length; k += 1){
				owners[k] = -1;
			}
			for(int k = 0; k < serverInstances; k += 1){
				servers[k] = input.readLine();
				if(servers[k] == null){
					throw new IllegalArgumentException("Must provide correct number of servers");
				}
			}
			String[] address = servers[serverID - 1].split(":");
			final int TCP_port;
			try{
				TCP_port = Integer.parseInt(address[1]);
			}
			catch(NumberFormatException e){
				throw new IllegalArgumentException("Must provide valid port number");
			}
			TCP_Server(TCP_port);
			//UDP_Server(UDP_port);
		}
		catch(IOException e){
			e.printStackTrace();
			str = "";
		}
	}
	
	/*
	public static void UDP_Server(int UDP_port){
		while(true){
			DatagramPacket dataPacket, returnPacket;
			dataPacket = null;
			byte[] toReturn = null;
			int len = 1024;
			boolean canReturn = false;
			try{
				DatagramSocket dataSocket = new DatagramSocket(UDP_port);
				byte[] buf = new byte[len];
				while(true){
					if(canReturn){
						returnPacket = new DatagramPacket(toReturn, toReturn.length, dataPacket.getAddress(), dataPacket.getPort());
						dataSocket.send(returnPacket);
					}
					canReturn = true;
					dataPacket = new DatagramPacket(buf, buf.length);
					dataSocket.receive(dataPacket);
					String input = new String(dataPacket.getData(), 0, dataPacket.getLength());
					String[] inputs = input.split(" ");
					if(inputs.length != 3){
						toReturn = "Error".getBytes();
						continue;
					}
					if((inputs[1].length() < 2) || (inputs[1].charAt(0) != 'b')){
						toReturn = "Error".getBytes();
						continue;
					}
					int clientID, bookID;
					try{
						clientID = Integer.parseInt(inputs[0]);
						bookID = Integer.parseInt(inputs[1].substring(1));
					}
					catch(NumberFormatException e){
						toReturn = "Error".getBytes();
						continue;
					}
					if(inputs[2] == "reserve"){
						boolean SUCCEEDED = bookReserve(clientID, bookID);
						if(SUCCEEDED){
							toReturn = ("c" + clientID + " b" + bookID).getBytes();
						}
						else{
							toReturn = ("fail c" + clientID + " b" + bookID).getBytes();
						}
					}
					else if(inputs[2] == "return"){
						boolean SUCCEEDED = bookReturn(clientID, bookID);
						if(SUCCEEDED){
							toReturn = ("c" + clientID + " b" + bookID).getBytes();
						}
						else{
							toReturn = ("fail c" + clientID + " b" + bookID).getBytes();
						}
					}
					else{
						toReturn = "Error".getBytes();
						continue;
					}
				}
			}
			catch(SocketException e){
				e.printStackTrace();
			}
			catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	*/
	
	public static void TCP_Server(int TCP_port) throws IOException{
		
	 
		
		ServerSocket myServerSocket = new ServerSocket(TCP_port);
		boolean canReturn = false;
		
		while(true){
			
			
			String toReturn=null;
			//first we want to get the inputs from the client
			Socket mySocket = myServerSocket.accept();
			PrintWriter out = new PrintWriter(mySocket.getOutputStream());
			BufferedReader coming = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
			if(canReturn){
				out.println(toReturn);
				out.flush();
			}
			
			canReturn = true;
			String input = coming.readLine();
			String [] inputs = input.split(" ");
			
			
			if(inputs.length != 3){
				toReturn = "Error";
				continue;
			}
			if((inputs[1].length() < 2) || (inputs[1].charAt(0) != 'b')){
				toReturn = "Error";
				continue;
			}
			int clientID, bookID;
			try{
				clientID = Integer.parseInt(inputs[0]);
				bookID = Integer.parseInt(inputs[1].substring(1));
			}
			catch(NumberFormatException e){
				toReturn = "Error";
				continue;
			}
			if(inputs[2] == "reserve"){
				boolean SUCCEEDED = bookReserve(clientID, bookID);
				if(SUCCEEDED){
					toReturn = ("c" + clientID + " b" + bookID);
				}
				else{
					toReturn = ("fail c" + clientID + " b" + bookID);
				}
			}
			else if(inputs[2] == "return"){
				boolean SUCCEEDED = bookReturn(clientID, bookID);
				if(SUCCEEDED){
					toReturn = ("c" + clientID + " b" + bookID);
				}
				else{
					toReturn = ("fail c" + clientID + " b" + bookID);
				}
			}
			else{
				toReturn = "Error";
				continue;
			}
			
			
			
			
		}
	}
	
	public static synchronized boolean bookReserve(int clientID, int bookID){
		if((bookID < 1) || (bookID > owners.length)){
			return false;
		}
		if(owners[bookID - 1] == -1){
			owners[bookID - 1] = clientID;
			return true;
		}
		return false;
	}
	
	public static synchronized boolean bookReturn(int clientID, int bookID){
		if((bookID < 1) || (bookID > owners.length)){
			return false;
		}
		if(owners[bookID - 1] == clientID){
			owners[bookID - 1] = -1;
			return true;
		}
		return false;
	}
}