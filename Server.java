import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.PriorityQueue;

public class Server{
	private static int[] owners;
	private static int clientsServed;
	private static int crashAmount;
	private static int crashTime;
	private static int serverID;
	private static int serverInstances;
	private static int numBooks;
	private static String[] servers;
	private static final int TIMEOUT = 100;
	private static final boolean DEBUG = false;
	
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
			if(DEBUG){
				System.out.println("Server ID: " + serverID);
				System.out.println("Number of Servers " + serverInstances);
				System.out.println("Number of Books " + numBooks);
				System.out.println("------------------------------------------------------------------");
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
			if(DEBUG){
				System.out.println("TCP Port: " + TCP_port);
				System.out.println("------------------------------------------------------------------");
			}
			Thread thread = new Thread(){
				public void run(){
					try {
						TCP_Server(TCP_port);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};
			thread.start();
			crashAmount = 0;
			while(true){
				str = input.readLine();
				if(str == null){
					continue;
				}
				inputs = str.split(" ");
				if(inputs.length != 3){
					continue;
				}
				if(!inputs[0].equals("crash")){
					continue;
				}
				try{
					int temp = Integer.parseInt(inputs[1]);
					if((temp < crashAmount) || (crashAmount == 0)){
						crashAmount = Integer.parseInt(inputs[1]);
						crashTime = Integer.parseInt(inputs[2]);
					}
				}
				catch(NumberFormatException e){
					continue;
				}
			}
		}
		catch(IOException e){
			e.printStackTrace();
			str = "";
		}
	}
	
	public static void TCP_Server(int TCP_port) throws IOException{
		ServerSocket myServerSocket;
		PriorityQueue<LamportRequest> queue = new PriorityQueue<LamportRequest>();
		LamportClock clock = new LamportClock(serverID);
		try{
			myServerSocket = new ServerSocket(TCP_port);
			while(true){
				//first we want to get the inputs from the client
				Socket mySocket = myServerSocket.accept();
				PrintWriter out = new PrintWriter(mySocket.getOutputStream());
				BufferedReader coming = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
				String input = coming.readLine();
				if(input.length() == 0){
					mySocket.close();
					continue;
				}
				if(input.charAt(0) == 'c'){
					if(DEBUG){
						System.out.println("Message Recieved: " + input);
						System.out.println("------------------------------------------------------------------");
					}
					clock.incrementTime();
					LamportRequest request = new LamportRequest(serverID, input, clock.getTime(), out, mySocket);
					queue.add(request);
					for(int k = 0; k < servers.length; k += 1){
						if((k+1) != serverID){
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
							Socket socket = new Socket(ia, port);
							socket.setSoTimeout(TIMEOUT);
							PrintWriter writer = new PrintWriter(socket.getOutputStream());
							writer.println(serverID + " request " + request.pid + " " + request.timestamp.pid + " " + request.timestamp.time);
							writer.flush();
							socket.close();
						}
					}
				}
				else{
					if(DEBUG){
						System.out.println("Message Recieved: " + input);
						System.out.println("------------------------------------------------------------------");
					}
					String[] inputs = input.split(" ");
					if(inputs[1].equals("request")){
						LamportRequest request= new LamportRequest(Integer.parseInt(inputs[2]), Integer.parseInt(inputs[3]), Integer.parseInt(inputs[4]));
						queue.add(request);
						clock.update(request.getTime());
						InetAddress ia;
						int port;
						String[] address = servers[request.getTime().pid - 1].split(":");
						if(address[0].equals("localhost")){
							ia = InetAddress.getLocalHost();
						}
						else{
							ia = InetAddress.getByName(address[0]);
						}
						port = Integer.parseInt(address[1]);
						Socket socket = new Socket(ia, port);
						socket.setSoTimeout(TIMEOUT);
						PrintWriter writer = new PrintWriter(socket.getOutputStream());
						writer.println(serverID + " ack " + request.getTime().pid + " " + request.getTime().time + " " + clock.getTime().pid + " " + clock.getTime().time);
						writer.flush();
						mySocket.close();
						socket.close();
					}
					else if(inputs[1].equals("ack")){
						LamportClock myClock = new LamportClock(Integer.parseInt(inputs[2]), Integer.parseInt(inputs[3]));
						for(LamportRequest request: queue){
							if((request.pid == serverID) && (request.getTime().time == myClock.time)){
								request.incrementAcks();
								break;
							}
						}
						clock.update(new LamportClock(Integer.parseInt(inputs[4]), Integer.parseInt(inputs[5])));
						mySocket.close();
					}
					else if(inputs[1].equals("release")){
						int myID = Integer.parseInt(inputs[0]);
						for(LamportRequest request: queue){
							if(request.pid == myID){
								queue.remove(request);
								break;
							}
						}
						mySocket.close();
					}
					if((queue.size() > 0) && (queue.peek().okayCS())){
						if(DEBUG){
							System.out.println("Thread in Critical Section");
							System.out.println("------------------------------------------------------------------");
						}
						LamportRequest request = queue.remove();
						String toReturn = handleRequest(request.message);
						PrintWriter writer = request.writer;
						writer.println(toReturn);
						writer.flush();
						request.socket.close();
						clientsServed += 1;
						for(int k = 0; k < servers.length; k += 1){
							if((k+1) != serverID){
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
								Socket socket = new Socket(ia, port);
								socket.setSoTimeout(TIMEOUT);
								writer = new PrintWriter(socket.getOutputStream());
								writer.println(serverID + " release " + request.message);
								writer.flush();
								socket.close();
							}
						}
					}
					if(clientsServed >= crashAmount && (crashAmount != 0)){
						try{
							Thread.sleep(crashTime);
						}
						catch(InterruptedException e ){
							continue;
						}
						crashAmount = 0;
					}
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
		if(inputs[2].equals("reserve")){
			boolean SUCCEEDED = bookReserve(clientID, bookID);
			if(SUCCEEDED){
				toReturn = ("c" + clientID + " b" + bookID);
			}
			else{
				toReturn = ("fail c" + clientID + " b" + bookID);
			}
		}
		else if(inputs[2].equals("return")){
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
	
	private static class LamportRequest{
		private int pid;
		private int acks;
		private String message;
		private LamportClock timestamp;
		private PrintWriter writer;
		private Socket socket;
		
		public LamportRequest(int pid, String message, LamportClock timestamp, PrintWriter writer, Socket socket){
			this.pid = pid;
			this.message = message;
			this.timestamp = timestamp;
			this.writer = writer;
			this.socket = socket;
			this.acks = 0;
		}
		
		public LamportRequest(int pid, int lcpid, int time){
			this.pid = pid;
			this.timestamp = new LamportClock(lcpid, time);
			message = null;
			acks = 0;
			writer = null;
			socket = null;
		}
		
		public LamportClock getTime(){
			return timestamp.getTime();
		}
		
		public void incrementAcks(){
			acks += 1;
		}
		
		public boolean okayCS(){
			return ((pid == serverID) && (acks == (servers.length - 1)));
		}
	}
	
	private static class LamportClock{
		private int pid;
		private int time;
		
		public LamportClock(int pid){
			this.pid = pid;
			this.time = 0;
		}
		
		public LamportClock(int pid, int time){
			this.pid = pid;
			this.time = time;
		}
		
		public LamportClock getTime(){
			LamportClock clock = new LamportClock(this.pid);
			clock.time = this.time;
			return clock;
		}
		
		public void incrementTime(){
			time += 1;
		}
		
		public void update(LamportClock clock){
			if(this.isGreater(clock) < 0){
				this.time = clock.time + 1;
			}
			else{
				this.time += 1;
			}
		}
		
		public int isGreater(LamportClock clock){
			 int value = ((Integer) this.time).compareTo((Integer) clock.time);
			 if(value == 0){
				 return ((Integer) this.pid).compareTo((Integer) clock.pid);
			 }
			 return value;
		}
	}
}