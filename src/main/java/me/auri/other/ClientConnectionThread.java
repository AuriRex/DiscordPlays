package me.auri.other;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import me.auri.other.enc.*;

public class ClientConnectionThread extends Thread {

    private Socket clientSocket;
    private Communicator com;
    private String identifier = null;
    private Boolean isRequestSocket = null;

    private EncMode enc;
    private Object[] enc_args;

    public static char TERMINATOR = '\n';
    public static int REQUEST_TIMEOUT = 300000; // = every 5 minutes

    ClientConnectionThread(Socket socket, EncMode enc, Object[] enc_args) {
        this.clientSocket = socket;
        this.com = null;
        this.enc = enc;
        this.enc_args = enc_args;
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
            receivedData = enc.decrypt( data.toString() , enc_args);
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

            // ####

            if(!Communicator.isSlotAvailable(this.com, this.identifier, this.isRequestSocket)) {
                
                System.out.println("[Error] Slot \""+com.getName() + ":" + identifier +"\" already in use!");
                outToServer.writeBytes( enc.encrypt( "SocOccupied" , enc_args) + TERMINATOR);
                outToServer.flush();
                return;

            }

            Communicator.setSlot(com, identifier, isRequestSocket, this);

            //System.out.println("added: " + this.identifier + " : " + this.isListenSocket);
           
            outToServer.writeBytes( enc.encrypt( "connected" , enc_args) + TERMINATOR);
            outToServer.flush();

            ServerListenerThread.setBusy(false);

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
                    receivedData = enc.decrypt( data.toString() , enc_args);
                    System.out.println("[CCT] Received from Client: " + receivedData);
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
                    com.handleIncomingData(receivedData);

                    outToServer.writeBytes( enc.encrypt( "rec" , enc_args) + TERMINATOR);
                    outToServer.flush();
                }
            } else {
                // int c = 0;
                while (running) {
                    try {
                        Thread.sleep(20);
                        // c++;
                        // if(c >= REQUEST_TIMEOUT) {
                        //     c = 0;
                        //     if(sendData("ping").equalsIgnoreCase("Error")) {
                        //         throw new IOException("Client connection Error.");
                        //     }
                        // }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
                

            
            

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

    public String sendData(String data) {
        String ret = "";

        if(!isRequestSocket) return "Error";

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
        if(isRequest()) {

            return !sendData("ping").equalsIgnoreCase("Error");

        } else {

            // String pp = sendData("partner_ping");
            // if(pp.equalsIgnoreCase("Error")) return false;
            return true;

        }
	}

}