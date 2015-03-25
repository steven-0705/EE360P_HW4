import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Client{
	private static int clientID, serverInstances;
	private static String IPAddress;
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
					//Cycle through servers until one is available
				}
				/*
				else if(inputs.length == 4){
					int port;
					try{
						port = Integer.parseInt(inputs[2]);
					}
					catch(NumberFormatException e){
						continue;
					}
					String toSend = clientID + " " + inputs[0] + " " + inputs[1];
					if(inputs[3] == "U"){
						UDP_Client(port, toSend);
					}
					else if(inputs[3] == "T"){
						TCP_Client(port,toSend);
					}
				}
				*/
			}
		}
		catch(IOException e){
			e.printStackTrace();
			str = "";
		}
	}
	
	private static void TCP_Client(int port, String toSend) throws IOException {
		InetAddress ib = InetAddress.getByName(IPAddress);
		Socket toServer = new Socket(ib,port);
		BufferedReader bReader = new BufferedReader(new InputStreamReader(toServer.getInputStream()));
		PrintStream pStream = new PrintStream(toServer.getOutputStream());
		pStream.println(toSend);
		pStream.flush();
		String res = bReader.readLine();
		System.out.println(res) ;
	}

	public static void UDP_Client(int UDP_port, String toSend){
		String returned = null;
		int len = 1024;
		byte[] rbuffer = new byte[len];
		DatagramPacket sendPacket, receivePacket;
		try{
			InetAddress ia = InetAddress.getByName(IPAddress);
			DatagramSocket dataSocket = new DatagramSocket();
			byte[] buffer = new byte[toSend.length()];
			buffer = toSend.getBytes();
			sendPacket = new DatagramPacket(buffer, buffer.length, ia, UDP_port);
			dataSocket.send(sendPacket);
			receivePacket = new DatagramPacket(rbuffer, rbuffer.length);
			dataSocket.receive(receivePacket);
			returned = new String(receivePacket.getData(), 0, receivePacket.getLength());
		}
		catch (UnknownHostException e) {
            System.err.println(e);
        }
		catch (SocketException e) {
            System.err.println(e);
        }
		catch (IOException e) {
            System.err.println(e);
        }
		System.out.println(returned);
	}
}