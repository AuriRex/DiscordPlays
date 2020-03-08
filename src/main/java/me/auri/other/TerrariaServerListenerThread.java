package me.auri.other;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;

class TerrariaServerListenerThread extends Thread {

    TerrariaServerListenerThread () {}

    ServerSocket welcomeSocket;
    boolean running = true;

    public void run() {
        try {
            String receivedData;
            welcomeSocket = new ServerSocket(11001);

            while (running) {
                Socket connectionSocket = welcomeSocket.accept();
                InputStreamReader reader = new InputStreamReader(connectionSocket.getInputStream(), Charset.forName("UTF-16LE"));
 
                int character;
                StringBuilder data = new StringBuilder();
    
                while ((character = reader.read()) != -1) {
                    data.append( (char) character);
                }
                receivedData = data.toString();
                // System.out.println("TerrariaReceived: " + receivedData);
                TerrariaCommunicator.handleIncomingData(receivedData);
                connectionSocket.close();
            }
        }catch(IOException ex) {
            
        }
    }

	public void stopNow() {
        running = false;
        try {
            welcomeSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("TerrariaServerThread stopped!");
	}

}