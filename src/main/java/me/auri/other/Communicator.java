package me.auri.other;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import me.auri.other.events.*;

public class Communicator implements ICommunicator {


    //private static TerrariaServerListenerThread serverThread;

    protected final ArrayList<GenericServerEvent> events = new ArrayList<>();

    private HashMap<String, Boolean> flags = new HashMap<>();

    // The Communicator Name a Gameserver wants to communicate with
    public String getName() {
		return "Communicator";
	}

    static HashMap<String, Communicator> coms = new HashMap<>();

    protected HashMap<String, ClientConnectionThread[]> cctPair = new HashMap<>();

    public void init() {

        initalFlags();

        System.out.println("[Communicator] Adding \""+getName()+"\" as communicator!");
        Communicator.coms.put(getName(), this);

        //serverThread = new TerrariaServerListenerThread();
        //serverThread.start();

        // events.add((TerrariaServerChatEvent) e -> {
        //     System.out.println("ServerChatEvent: "+e);
        // });

        // events.add((TerrariaServerJoinEvent) e -> {
        //     System.out.println("ServerJoinEvent: "+e);
        // });

        // events.add((TerrariaServerLeaveEvent) e -> {
        //     System.out.println("ServerLeaveEvent: "+e);
        // });

    }

    private static HashMap<String, String> formatReplacer = new HashMap<>();

    @Deprecated
    public static void addFormatReplace(String replace, String with) {
        formatReplacer.put(replace, with);
    }

    @Deprecated
    public static void sendFormatedMessage(String author, String message) {
        for(Entry<String,String> e : formatReplacer.entrySet()) {
            message = message.replace(e.getKey(), e.getValue());
        }
        sendMessage("[c/7289DA:<Discord>] " + author + " [c/7289DA:>] " + message);
    }

    @Deprecated
    public static void sendMessage(String message) {
        sendData("say "+message);
    }

    @Deprecated
    public static String sendData(String sendData) {

        int port = 11000;
        try (Socket socket = new Socket("localhost", port)) {

            DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
            outToServer.writeBytes(sendData + "<EOF>");

            InputStream input = socket.getInputStream();
            InputStreamReader reader = new InputStreamReader(input, Charset.forName("UTF-16LE"));

            int character;
            StringBuilder data = new StringBuilder();

            while ((character = reader.read()) != -1) {
                data.append( (char) character);
            }

            return data.toString();


        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
        }

        return "Error";
    }    

    private void initalFlags() {

    }

    public void setFlag(String flag, boolean value) {
        flags.put(flag, value);
    }

    public Boolean getFlag(String flag) {
        return flags.get(flag);
    }

    public void handleIncomingData(String receivedData) {

        String[] rawData = receivedData.split(":", 2);
        if(rawData.length < 2) {
            System.out.println("Received garbage data, canceling event!");
            return;
        }
        String event = rawData[0];
        String content = rawData[1].substring(1);

        if(content.equals("")) {
            System.out.println("Received empty content, canceling event!");
            return;
        }

        handleEvents(event, content);

    }
    
    public void handleEvents(String event, String content) {
        try {
            switch (event) {
                default:
                    events.forEach(e -> {
                        if(e instanceof GenericNotImplementedEvent)
                            e.execute(event + ": " + content);
                    });
            }
        } catch(IndexOutOfBoundsException ex) {
            ex.printStackTrace();
            System.out.println("IOoBException: Communicator -> this shouldn't happen.");
        } catch(Exception ex) {
            System.out.println("Caught exception: " + ex);
            events.forEach(e -> {
                if(e instanceof GenericExceptionEvent)
                    e.execute(ex.toString());
            });
        }
    }

	public void subscribe(GenericServerEvent event) {
        events.add(event);
	}

	public static Communicator getByName(String receivedData) {
        if(Communicator.coms.containsKey(receivedData))
            return Communicator.coms.get(receivedData);

        System.out.println("[Error] Unknown Communicator \""+receivedData+"\", defaulting to standard implementation!");
		return Communicator.coms.get("Communicator");
	}

	public static boolean isSlotAvailable(Communicator com, String identifier, Boolean isRequestSocket) {
        // TODO: THIS
        ClientConnectionThread[] ccta = com.cctPair.get(identifier);

        int index = 0;

        if(isRequestSocket) index++;

        if(ccta == null) {
            ccta = new ClientConnectionThread[2];
        }

        if(ccta[index] == null) {
            return true;
        }


        return false;
    }
    
    public static void setSlot(Communicator com, String identifier, Boolean isRequestSocket, ClientConnectionThread new_cct) {
        ClientConnectionThread[] ccta = com.cctPair.get(identifier);

        int index = 0;

        if(isRequestSocket) index++;

        if(ccta == null) {
            ccta = new ClientConnectionThread[2];
        }

        ccta[index] = new_cct;

        com.cctPair.put(identifier, ccta);
    }

    public static boolean isPartnerThreadAvailable(ClientConnectionThread cct) {
        return getPartnerThread(cct) != null;
    }

    public static ClientConnectionThread getPartnerThread(ClientConnectionThread cct) {
        ClientConnectionThread[] ccta = cct.getCom().cctPair.get(cct.getIdentifier());

        int index = 0;

        if(!cct.isRequest()) {
            index++;
        }

        return ccta[index];
    }

	public static void freeSlot(ClientConnectionThread cct) {

        ClientConnectionThread[] ccta = cct.getCom().cctPair.get(cct.getIdentifier());

        int index = 0;

        if(cct.isRequest()) index++;

        ccta[index] = null;

        cct.getCom().cctPair.put(cct.getIdentifier(), ccta);

	}

	public static void closeConnections(ClientConnectionThread cct) {

        try {
            getPartnerThread(cct).close();
            cct.close();
            freeSlot(getPartnerThread(cct));
            freeSlot(cct);
        } catch(IOException ex) {

        }
        

	}

	

}