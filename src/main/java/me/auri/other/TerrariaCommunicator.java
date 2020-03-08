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

import me.auri.other.events.*;

public class TerrariaCommunicator {


    private static TerrariaServerListenerThread serverThread;

    private static final ArrayList<TerrariaServerEvent> events = new ArrayList<>();

    public static void init() {
        
        serverThread = new TerrariaServerListenerThread();
        serverThread.start();

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

    public static void stop() {
        serverThread.stopNow();
    }

    public static void sendFormatedMessage(String author, String message) {
        sendMessage("<Discord> " + author + " > " + message);
    }

    public static void sendMessage(String message) {
        sendData("say "+message);
    }

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

    static HashMap<String, Long> teamJoinMsg = new HashMap<>();

    public static void handleIncomingData(String receivedData) {

        String[] rawData = receivedData.split(":", 2);
        String event = rawData[0];
        String content = rawData[1].substring(1);

        try {
            switch (event) {
                case "ServerJoin":
                    events.forEach(e -> {
                        if (e instanceof TerrariaServerJoinEvent)
                            e.execute(content);
                    });
                    break;
                case "ServerLeave":
                    events.forEach(e -> {
                        if (e instanceof TerrariaServerLeaveEvent)
                            e.execute(content);
                    });
                    break;
                case "ServerChat":
                    events.forEach(e -> {
                        if (content.split("\n", 2)[1].startsWith("/")) {
                            if (e instanceof TerrariaServerCommandEvent)
                                e.execute(content);
                        } else {
                            if (e instanceof TerrariaServerChatEvent)
                                e.execute(content);
                        }
                    });
                    break;
                case "GamePostInitialize":
                    events.forEach(e -> {
                        if (e instanceof TerrariaServerGamePostInitializeEvent)
                            e.execute(content);
                    });
                    break;
                case "ServerInit":
                    events.forEach(e -> {
                        if (e instanceof TerrariaServerInitEvent)
                            e.execute(content);
                    });
                    break;
                case "ServerShutdown":
                    events.forEach(e -> {
                        if (e instanceof TerrariaServerShutdownEvent)
                            e.execute(content);
                    });
                    break;
                case "ServerBroadcast":
                    if (content.split("\n")[0].contains("has joined the ") && content.split("\n")[0].endsWith(" party.")) {

                        if(teamJoinMsg.containsKey(content)) {
                            if(teamJoinMsg.get(content) <= System.currentTimeMillis() - 5000) {
                                teamJoinMsg.remove(content);
                            } else {
                                break;
                            }
                        }

                        events.forEach(e -> {
                            if(e instanceof TerrariaServerJoinPartyEvent)
                                e.execute(content);
                        });

                        teamJoinMsg.put(content, System.currentTimeMillis());
                    } else {
                        events.forEach(e -> {
                            if(e instanceof TerrariaServerBroadcastEvent)
                                e.execute(content);
                        });
                    }
                    break;
                default:
                    events.forEach(e -> {
                        if(e instanceof TerrariaServerUnknownEvent)
                            e.execute(receivedData);
                    });
            }//ServerBroadcast TerrariaServerBroadcastEvent
        } catch(IndexOutOfBoundsException ex) {
            System.out.println("IOoBException: TerrariaCommunicator -> this shouldn't happen.");
        }

	}

	public static void subscribe(TerrariaServerEvent event) {
        events.add(event);
	}

	public static HashMap<String, String> getOnlinePlayers() {

        HashMap<String, String> ret = new HashMap<>();
        String response = sendData("playing");

        String[] rawData = response.split("\n");

        ArrayList<String> rawDataClean = new ArrayList<>();

        for(String s : rawData) {
            if(s.equals("")) continue;

            rawDataClean.add(s);
        }

        if(rawDataClean.size() < 2) {
            ret.put("None", "no ip");
            return ret;
        }

        for(int i = 0; i < rawDataClean.size(); i += 2) {
            ret.put(rawDataClean.get(i), rawDataClean.get(i+1));
        }

        return ret;
	}

	public static boolean isOnline() {
        String ret = sendData("ping");
        if(!ret.equals("Error"))
            return true;
        return false;
	}

}