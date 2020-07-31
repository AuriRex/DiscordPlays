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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.auri.other.events.terraria.*;

@Deprecated
public class TerrariaCommunicator {


    private static TerrariaServerListenerThread serverThread;

    private static final ArrayList<TerrariaServerEvent> events = new ArrayList<>();

    private static Pattern pattern_itemtag = null;
    private static Matcher matcher_itemtag = null;

    private static Pattern pattern_itemtag_modifiers = null;
    private static Matcher matcher_itemtag_modifiers = null;

    private static HashMap<String, Boolean> flags = new HashMap<>();

    public static void init() {

        initalFlags();
        
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

    private static HashMap<String, String> formatReplacer = new HashMap<>();

    public static void addFormatReplace(String replace, String with) {
        formatReplacer.put(replace, with);
    }

    public static void sendFormatedMessage(String author, String message) {
        for(Entry<String,String> e : formatReplacer.entrySet()) {
            message = message.replace(e.getKey(), e.getValue());
        }
        sendMessage("[c/7289DA:<Discord>] " + author + " [c/7289DA:>] " + message);
    }

    public static void sendMessage(String message) {
        sendData("say "+message);
    }

    public static String getPrefix(String prefid) {
        return sendData("prefid " + prefid);
    }

    public static String getItemName(String itemid) {
        return sendData("itemid " + itemid);
    }

    private static String replaceModifiers(String mods) {

        // mods -> "/s19,p2"
        if(mods == null) return "";

        if(pattern_itemtag_modifiers == null)
            pattern_itemtag_modifiers = Pattern.compile("([psx])(\\d+)");

        if(matcher_itemtag_modifiers == null)
            matcher_itemtag_modifiers = pattern_itemtag_modifiers.matcher(mods);
        else
            matcher_itemtag_modifiers.reset(mods.substring(1, mods.length()));


        String ret = "";

        while(matcher_itemtag_modifiers.find()) {

            String type = "";
            String value = "";

            try {
                type = matcher_itemtag_modifiers.group(1);
                value = matcher_itemtag_modifiers.group(2);
            } catch(IllegalStateException ex) {
                continue;
            }

            if(type.equals("s") || type.equals("x")) {
                ret += value + "x";
            } else if (type.equals("p")) {
                if(!ret.equals("")) ret += " ";
                ret += getPrefix(value);
            }

        }

        return ret;
    }
    // Regex
    // ([psx])(\d+)

    public static String replaceItemTags(String rawData) {
        return replaceItemTags(rawData, true);
    }

    public static String replaceItemTags(String rawData, boolean inBrackets) {

        // \[i(\/([psx]\d+,?)*)?:(\d+)\]
        if(pattern_itemtag == null)
            pattern_itemtag = Pattern.compile("\\[i(\\/([psx]\\d+,?)*)?:(\\d+)\\]");

        if(matcher_itemtag == null)
            matcher_itemtag = pattern_itemtag.matcher(rawData);
        else
            matcher_itemtag.reset(rawData);
        
        String ret = matcher_itemtag.replaceAll( mr -> {
            
            String mods = "";
            String item_id = "";

            try {
                mods = mr.group(1);
            } catch(IllegalStateException ex) {

            }

            try {
                item_id = mr.group(3);
            } catch(IllegalStateException ex) {
                ex.printStackTrace();
            }

            String item_name = getItemName(item_id);

            String append = replaceModifiers(mods);

            String final_name_combined = "";

            if(append.equals("")) {
                final_name_combined = item_name;
            } else {
                final_name_combined = append + " "  + item_name;
            }

            if(inBrackets) {
                return "[" + final_name_combined + "]";
            } else {
                return final_name_combined;
            }
        });

        return ret;
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

    private static void initalFlags() {
        flags.put("replaceItemTags", true);
    }

    public static void setFlag(String flag, boolean value) {
        if(flags.containsKey(flag)) {
            flags.put(flag, value);
        }
    }

    public static Boolean getFlag(String flag) {
        return flags.get(flag);
    }

    public static void handleIncomingData(String receivedData) {

        String[] rawData = receivedData.split(":", 2);
        String event = rawData[0];
        String content = rawData[1].substring(1);

        if(content.equals("")) {
            System.out.println("Received empty content, canceling event!");
            return;
        }

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
                            if (e instanceof TerrariaServerChatEvent) {
                                if(flags.get("replaceItemTags")) {
                                    // You might ask: Why do this here and not Server side?
                                    // That's a good question tbh - I just wanted to get some experience with regex in java I guess :P
                                    e.execute(replaceItemTags(content));
                                } else {
                                    e.execute(content);
                                }
                            }
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
                            if(! (teamJoinMsg.get(content) <= System.currentTimeMillis() - 5000)) {
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
        } catch(Exception ex) {
            System.out.println("Caught exception: " + ex);
            events.forEach(e -> {
                if(e instanceof TerrariaCommunicatorExceptionEvent)
                    e.execute(ex.toString());
            });
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
            // ret.put("None", "no ip");
            return null;
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