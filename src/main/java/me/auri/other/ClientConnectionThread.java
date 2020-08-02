package me.auri.other;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import me.auri.other.enc.*;

public class ClientConnectionThread extends Thread {

    private Socket clientSocket;
    private Communicator com;
    private String identifier = null;
    private Boolean isRequestSocket = null;

    private EncMode enc;
    private Object[] enc_args;

    private ServerListenerThread slt = null;

    public static char TERMINATOR = '\n';

    ClientConnectionThread(Socket socket, EncMode enc, ServerListenerThread slt) {
        this.clientSocket = socket;
        this.com = null;
        this.enc = enc;
        this.slt = slt;
    }

    ClientConnectionThread(Socket socket) {
        this.clientSocket = socket;
        this.com = null;
        enc = new EncMode();
    }

    private boolean running = true;

    private InputStreamReader reader;
    private DataOutputStream outToServer;

    

    public void run() {

        
        try {

            reader = new InputStreamReader(clientSocket.getInputStream()/*, Charset.forName("UTF-16LE")*/);
            outToServer = new DataOutputStream(clientSocket.getOutputStream());

            int character;
            StringBuilder data;
            String receivedData;

            System.out.println("[CCT] Client connecting ...");

            // ## Get Communicator ##

            data = new StringBuilder();
            while ((character = reader.read()) != -1) {
                //System.out.print( (char) character);
                if((char) character == TERMINATOR) break;
                if(!running) {
                    clientSocket.close();
                    return;
                }
                data.append((char) character);
            }
            receivedData = data.toString();
            System.out.println("[CCT] Received from Client: " + receivedData);

            // com:identifier:boolean(isListenSocket)
            String[] meta = receivedData.split(":", 3);

            if(meta.length < 3) {
                System.out.println("[Error] Received garbage data, ClientConnectionThread terminated!");
                return;
            }

            this.com = Communicator.getByName(meta[0]);
            this.identifier = meta[1];
            this.isRequestSocket = Boolean.valueOf(meta[2]);

            enc_args = slt.getEncKeyData(identifier);

            // ####

            if(!Communicator.isSlotAvailable(this.com, this.identifier, this.isRequestSocket)) {
                
                System.out.println("[Error] Slot \""+com.getName() + ":" + identifier +"\" already in use!");
                outToServer.writeBytes( "SocOccupied" + TERMINATOR);
                outToServer.flush();
                return;

            }

            Communicator.setSlot(com, identifier, isRequestSocket, this);

            //System.out.println("added: " + this.identifier + " : " + this.isListenSocket);
           
            outToServer.writeBytes( "connected" + TERMINATOR);
            outToServer.flush();

            // ServerListenerThread.setBusy(false);

            if (isRequestSocket) {
                while (running) {
                    data = new StringBuilder();
                    while ((character = reader.read()) != -1) {
                        if((char) character == TERMINATOR) break;
                        if(!running) {
                            clientSocket.close();
                            return;
                        }
                        data.append((char) character);
                    }


                    // TODO HELP

                    // if(data.length() < 2) {
                    //     System.out.println("test2");
                    //     continue;
                    // }

                    // reader.

                    receivedData = enc.decrypt( data.toString() , enc_args);
                    System.out.println("[CCT (" + com.getName() + ":"+identifier + ":" + isRequestSocket + ")] Received from Client: " + receivedData);
                    if(receivedData.startsWith("ConnectionCloseEvent:")) {
                        String syncname;
                        try {
                            syncname = receivedData.split(":")[1];
                            if(!syncname.equalsIgnoreCase(this.identifier)) {
                                System.out.println("[CCT] Received ConnectionCloseEvent with wrong identifier! Closing connection for \""+this.identifier+"\" anyways.");
                                syncname = this.identifier;
                            }
                        }catch(Exception ex) {
                            syncname = this.identifier;
                        }

                        Communicator.closeConnections(this);
                        return;

                    }
                    if(!com.handleIncomingData(this.identifier, receivedData)) {
                        // Event canceled, close connection
                        System.out.println("[CCT (" + com.getName() + ":"+identifier + ":" + isRequestSocket + ")] Closing connection");
                        Communicator.closeConnections(this);
                        return;
                    }

                    if(clientSocket.isConnected()) {
                        // System.out.println("test");
                        outToServer.writeBytes( enc.encrypt( "rec" , enc_args) + TERMINATOR);
                        outToServer.flush();
                    } 
                    

                    
                }
            } else {

                while (running) {
                    try {
                        Thread.sleep(20);
                        
                        Iterator<String> it = eventList.iterator();

                        while(it.hasNext()) {
                            String send = it.next();

                            System.out.println("Sending: " + send);
                            sendData(send);

                            it.remove();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
                

            
            

        } catch(SocketException e) {
            System.out.println("[CCT (" + com.getName() + ":"+identifier + ":" + isRequestSocket + ")] Closing connection (Connection lost.)");
            Communicator.closeConnections(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void close() throws IOException {

        if(!isRequestSocket) {
            // Send stop to Game Plugin as well
            outToServer.writeBytes( enc.encrypt( "ConnectionCloseEvent" , enc_args) + TERMINATOR);
            outToServer.flush();
        }
        running = false;
        
        clientSocket.close();
    }

    private Queue<String> eventList = new LinkedList<>();

    private String sendData(String data) {
        String ret = "";

        if(isRequestSocket)  {
            System.out.println("Error, trying to send data via Request Socket!");
            return "Error";
        }

        try {
            outToServer.writeBytes( enc.encrypt( data , enc_args) + TERMINATOR);
            outToServer.flush();

            StringBuilder data_read = new StringBuilder();
            int character;
            while ((character = reader.read()) != -1) {
                if((char) character == TERMINATOR) break;
                if(!running) {
                    clientSocket.close();
                    return "Error";
                }
                data_read.append((char) character);
            }
            ret = enc.decrypt( data_read.toString() , enc_args);
            System.out.println("[CCT/R] Received from Client: " + ret);
        } catch (IOException e) {
            e.printStackTrace();
            return "Error";
        }

        return ret;
    }

    public boolean isRequest() {
        return isRequestSocket;
    }

    public String getIdentifier() {
        return identifier;
    }

	public void stopNow() {
        running = false;
        String ip = clientSocket.getInetAddress().toString();
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("[CCT] ClientConnectionThread to \""+ip+"\" stopped!");
	}

	public Communicator getCom() {
        return this.com;
	}

	public boolean isConnected() {
        return clientSocket.isConnected();
        // if(isRequest()) {

        //     return !sendData("ping").equalsIgnoreCase("Error");

        // } else {

        //     // String pp = sendData("partner_ping");
        //     // if(pp.equalsIgnoreCase("Error")) return false;
        //     return true;

        // }
	}

    public void queueEvent(String event, String data) {
        queueData(event + ": " + data);
    }

	public void queueData(String data) {
        if(isRequest()) {
            System.out.println("ERROR: Trying to send on Request Socket!");
            return;
        }
        System.out.println("Adding to queue: " + data);
        eventList.add(data);
	}

}