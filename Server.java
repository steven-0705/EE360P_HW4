import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Server{
	private static int[] owners;
	private static int clientsServed;
	private static int serverID;
	private static int serverInstances;
	private static int numBooks;
	private static String[] servers;
	private static DirectClock clock;
	private static LamportMutex mutex;
	private static ServerSocket clientSocket;
	private static final int TIMEOUT = 100;
	
	public static void main(String args[]){
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
			input.close();
			clock = new DirectClock(serverID - 1);
			mutex = new LamportMutex(serverID - 1, clock);
			Thread thread = new Thread(){
				public void run(){
					try {
						TCP_Server(TCP_port);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			thread.start();
		}
		catch(IOException e){
			e.printStackTrace();
			str = "";
		}
	}
	
	public static void TCP_Server(int TCP_port) throws IOException{
		try{
			clientSocket = new ServerSocket(TCP_port);
			Socket socket = clientSocket.accept();
			PrintWriter out = new PrintWriter(socket.getOutputStream());
			BufferedReader coming = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String input;
			while((input = coming.readLine()) != null){
				String[] inputs = input.split(" ");
				if(inputs[0].equals("mutex")){
					mutex.handleMsg(input);
				}
				else if(inputs[0].equals("server")){
					input = input.replace("server", "");
					handleRequest(input);
				}
				else{
					mutex.requestCS();
					String toReturn = handleRequest(input);
					out.println(toReturn);
					out.flush();
					toReturn = "server " + toReturn;
					mutex.broadcastMsg(toReturn);
					mutex.releaseCS();
				}
			}
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public static String handleRequest(String input){
		String toReturn=null;
		String [] inputs = input.split(" ");
		if(inputs.length != 3){
			toReturn = "Error";
			return toReturn;
		}
		if((inputs[1].length() < 2) || (inputs[1].charAt(0) != 'b')){
			toReturn = "Error";
			return toReturn;
		}
		int clientID, bookID;
		try{
			clientID = Integer.parseInt(inputs[0].substring(1));
			bookID = Integer.parseInt(inputs[1].substring(1));
		}
		catch(NumberFormatException e){
			toReturn = "Error";
			return toReturn;
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
			return toReturn;
		}
		return toReturn;
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
	
	private static class LamportMutex{
		private int[] queue;
		private int myId;
		private DirectClock clock;
		
		public LamportMutex(int myId, DirectClock clock){
			queue = new int[serverInstances];
			this.myId = myId;
			for(int k = 0; k < serverInstances; k += 1){
				queue[k] = Integer.MAX_VALUE;
			}
		}
		
		public synchronized void requestCS(){
			try{
				clock.sendAction();
				queue[myId] = clock.getValue(myId);
				broadcastMsg("mutex request " + myId + " " + clock.getValue(myId) + " " + queue[myId]);
				while(!okayCS()){
					wait();
				}
			}
			catch(InterruptedException e){
				e.printStackTrace();
			}
		}
		
		public synchronized void releaseCS(){
			queue[myId] = Integer.MAX_VALUE;
			broadcastMsg("mutex release " + myId + " " + clock.getValue(myId) + " " + queue[myId]);
		}
		
		public boolean okayCS(){
			for(int k = 0; k < serverInstances; k += 1){
				if(isGreater(queue[myId], myId, queue[k], k)){
					return false;
				}
				if(isGreater(queue[myId], myId, clock.getValue(k), k)){
					return false;
				}
			}
			return true;
		}
		
		public boolean isGreater(int entry1, int pid1, int entry2, int pid2){
			if(entry2 == Integer.MAX_VALUE){
				return false;
			}
			return ((entry1 > entry2) || ((entry1 == entry2) && (pid1 > pid2)));
		}
		
		public void handleMsg(String msg){
			String[] inputs = msg.split(" ");
			if(!inputs[0].equals("mutex")){
				throw new IllegalArgumentException("Non-mutex message passed to mutex handler");
			}
			if(inputs.length != 5){
				throw new IllegalArgumentException("Invalid number of tokens in mutex message");
			}
			String str = inputs[1];
			int msgId = Integer.parseInt(inputs[2]);
			int msgClock = Integer.parseInt(inputs[3]);
			int msgTimeStamp = Integer.parseInt(inputs[4]);
			clock.recieveAction(msgId, msgClock);
			if(str.equals("request")){
				queue[msgId] = msgClock;
				sendMsg("mutex ack " + myId + " " + clock.getValue(myId) + " " + queue[myId], servers[msgId]);
			}
			else if(str.equals("release")){
				queue[msgId] = Integer.MAX_VALUE;
			}
			else if(str.equals("ack")){
				queue[msgId] = msgTimeStamp;
			}
			else{
				throw new IllegalArgumentException("Invalid mutex message");
			}
			notifyAll();
		}
		
		public void broadcastMsg(String msg){
			int serverIndex = 0;
			for(String server: servers){
				if(serverIndex != myId){
					sendMsg(msg, server);
				}
				serverIndex += 1;
			}
		}
		
		public void sendMsg(String msg, String server){
			try{
				clock.sendAction();
				InetAddress ia = getHost(myId);
				int port = getPort(myId);
				Socket socket;
				socket = new Socket(ia, port);
				socket.setSoTimeout(TIMEOUT);
				PrintWriter out = new PrintWriter(socket.getOutputStream());
				out.println(msg);
				out.flush();
				socket.close();
			}
			catch(IOException e){
				System.err.println(e);
			}
		}
		
		public InetAddress getHost(int k) throws UnknownHostException{
			String[] address = servers[k].split(":");
			if(address[0].equals("localhost")){
				return InetAddress.getLocalHost();
			}
			else{
				return InetAddress.getByName(address[0]);
			}
		}
		
		public int getPort(int k) throws NumberFormatException{
			String[] port = servers[k].split(":");
			return Integer.parseInt(port[1]);
		}
	}
	
	private static class DirectClock{
		private int[] clock;
		private int myId;
		
		public DirectClock(int myId){
			this.myId = myId;
			clock = new int[serverInstances];
			for(int k = 0; k < serverInstances; k += 1){
				clock[k] = 0;
			}
			clock[myId] = 1;
		}
		
		public int getValue(int k){
			return clock[k];
		}
		
		public void tick(){
			clock[myId] += 1;
		}
		
		public void sendAction(){
			tick();
		}
		
		public void recieveAction(int sender, int sentValue){
			clock[sender] = Math.max(clock[sender], sentValue);
			clock[myId] = Math.max(clock[myId], sentValue) + 1;
		}
	}
}