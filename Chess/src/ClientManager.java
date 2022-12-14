/**
 *  Creates the Client handler for the server to input and output data
 *  
 *  @authors Ali Sartaz Khan
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;


public class ClientManager implements Runnable {   
        public static ArrayList<ClientManager> users = new ArrayList<>();
        private Socket socket;
        BufferedWriter out;
        BufferedReader in;
        String username;
        String color;
        String newUserInput;
        int ID;
        String preferredTime;
        
        
    	/**
    	 * Constructor takes in socket and creates new input and output streams
    	 * 
    	 * @param socket: socket
    	 */
        public ClientManager(Socket socket) {
        	try {
            	this.socket = socket; 
				this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				this.newUserInput = in.readLine();
				String[] list = newUserInput.split("[:-]");
				this.username = list[0];
				this.color = list[1];
				this.preferredTime = list[2]; //minutes
				this.preferredTime += (":" + list[3]); // seconds
				System.out.println(newUserInput);
				users.add(this);
				ID = users.size();
				if (ID == 2)
					setValidColor();
	        	broadcastIdToYourself();
	        	broadcastToOthers("PLAYER:" + ID + "-" +color + "-" + username + "-" + preferredTime);
	        	othersBroadcastToYou();
			} 
        	catch (IOException e) {
				e.printStackTrace(); }
        }

    	/**
    	 * Run method
    	 */
        public void run() {
//        	sq.add(out);
            String msgFromUser = "";
            while (socket.isConnected()) {
            	try {
            		msgFromUser = in.readLine(); //AVOID THE FIRST IN
	            	System.out.println(msgFromUser);
	            	broadcastToOthers(msgFromUser);
            	} 
            	catch(Exception e) { close(); } 
            }
        } 
        
        /**
    	 * Sets valid of the the client by comparing colors with both users, where user 1 gets preference
         * on which color to choose.
    	 */
        public void setValidColor() {
        	for (ClientManager user: users) {
				try {
					if (user.username != this.username) { // getting user with ID 1
						if (user.getColor().equals(this.getColor())) {
							System.out.println("Users picked the same color! Updating User 2 color...");
							System.out.println("Before: User1-" + user.getColor() + " User2-"+this.getColor());
							
							if (user.getColor().equals("White"))
								this.setColor("Black");
							else
								this.setColor("White");
							System.out.println("After: User1-" + user.getColor() + " User2-"+this.getColor());
						}
					}
				}
				catch (Exception e) { close(); }
        	}
        }

        /**
    	 * Method to broadcast string from others to yourself 
    	 */
        public void broadcastIdToYourself() {
				try {
					this.out.write("ID:" + this.ID +"-"+this.color + "-" + this.username + "-" + this.preferredTime);
					this.out.newLine();
					this.out.flush();
				}
				catch (Exception e) { close(); }
    	}
        
        
        /**
         * Method to broadcast string from others to yourself 
         */
        public void othersBroadcastToYou() {
        	for (ClientManager user: users) {
				try {
					if (user.username != this.username) {
						this.out.write("PLAYER:" + user.ID + "-" + user.color + "-" 
								+user.username + "-" + user.preferredTime);
						this.out.newLine();
						this.out.flush();}
				}
				catch (Exception e) { close(); }
    		}
        }
        
        /**
         * Method to broadcast string from you to others
         * 
         * @param msgToOthers: string to broadcast 
         */
        public void broadcastToOthers(String msgToOthers) {
    		for (ClientManager user: users) {
				try {
					if (user.username != this.username) {
						System.out.println("Inside broadcast");
						user.out.write(msgToOthers);
						user.out.newLine();
						user.out.flush();}
				}
				catch (Exception e) { close(); }
    		}
    	}
        
        /**
         * Closes server
         */
        public void close() {
         	try {socket.close();
         	in.close(); out.close();} 
            catch (IOException e1) {}
         	System.out.println("Socket is closed!");
         	System.exit(0);
     	}
        
        // -- getters & setters

        /**
         * @return String representation of this object
         */
        public String toString() { return username; }
        
        
        /**
         * @return the color of this client
         */
        public String getColor() { return this.color; }
        
        /**
         * @return client id
         */
        public int getID() { return this.ID; }

        /**
         * Sets color for the client
         * 
         * @param color: color to set client
         */
        public void setColor(String color) { this.color = color; }

}