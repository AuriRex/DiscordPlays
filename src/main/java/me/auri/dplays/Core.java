package me.auri.dplays;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Snowflake;
import me.auri.other.TerrariaCommunicator;
import me.auri.other.events.*;
import reactor.core.publisher.Mono;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Map.Entry;

public class Core {

    private static final Map<String, Command> commands = new HashMap<>();
    private static final Map<String, Command> adminCommands = new HashMap<>();

    private static ArrayList<String> discordPlaysList = new ArrayList<>();
    private static HashSet<String> allowedKeys = new HashSet<>();

    private static Map<String, String> inputRemap = new HashMap<>();

    public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

    public static Map<String, String> getInputRemap() {
        return inputRemap;
    }

    public static void setInputRemap(Map<String, String> inputRemapNew) {
        inputRemap = inputRemapNew;
    } 

    public static HashSet<String> getAllowedKeys() {
		return allowedKeys;
	}

    public void setAllowedKeys(HashSet<String> allowedKeysNew) {
        allowedKeys = allowedKeysNew;
    }

    
    private static Map<String, Integer> ints = new HashMap<>();

    private static String currentConfig = "default";

    public static int getVar(String var) {
        if(ints.containsKey(var))
            return ints.get(var);
        return 0;
    }

    public static String prefix = "!";

    private static void defaultVars() {
        ints.put("delay", 150);
        ints.put("postDelay", 20);
        ints.put("maxLoops", 15);
    }

    static DiscordClient client;

    static {

        commands.put("help", event -> event.getMessage().getAuthor().get().getPrivateChannel().block()
                .createMessage(createHelp(commands)).block());
        commands.put("ping", event -> event.getMessage().getChannel().block().createMessage("Pong!").block());

        commands.put("pressedkeys", event -> {
            String out = "";
            for(String key : InputHandler.getPressedKeys())
                out += ", " + key;
            event.getMessage().getChannel().block()
                .createMessage("Pressed Keys:\n" + out).block();
        });

        commands.put("listkeys", event -> {
            event.getMessage().getChannel().block()
                .createMessage("Keys:\n" + listKeys()).block();
        });

        commands.put("inputhelp", event -> {
            event.getMessage().getChannel().block()
                .createMessage("Input How-To:\n  0. Type \""+prefix+"listkeys\" to get a List of all pressable Keys.\n  1. type the key into the chat. -> \"RIGHT\"\n  2. chain inputs by seperating them with spaces. -> \"UP RIGHT DOWN LEFT\"\n  3. Loop key inputs with the + operator. -> \"RIGHT+5\" (presses RIGHT 5 times)\n  4. Modify keys with \"!hold <key>\", \""+prefix+"release <key>\" and \""+prefix+"toggle <key>\". -> \""+prefix+"hold RIGHT\"\n  4.5. Modify keys with the : operator (\"<key>:h\", \"<key>:r\" and \"<key>:t\"). -> \"RIGHT:h\"").block();
        });

        commands.put("playing", event -> {
            try {
                HashMap<String, String> online = TerrariaCommunicator.getOnlinePlayers();
                event.getMessage().getChannel().block().createMessage(messageSpec -> {
                    messageSpec.setEmbed(embedSpec -> {
                        embedSpec.setTitle("Online Players:");
                        for(String key : online.keySet())
                            embedSpec.addField("###", key, true);
                        embedSpec.setColor(Color.WHITE);
                    });
                }).block();
            } catch(Exception ex) {
                ex.printStackTrace();
                System.out.println("Exception cought...");
            }
            
        });

        commands.put("online", event -> {
            try {
                boolean isOnline = TerrariaCommunicator.isOnline();
                event.getMessage().getChannel().block().createMessage(messageSpec -> {
                    messageSpec.setEmbed(embedSpec -> {
                        
                        if(isOnline) {
                            embedSpec.setTitle("Terraria Server is online!");
                            embedSpec.setColor(Color.GREEN);
                        } else {
                            embedSpec.setTitle("Terraria Server is offline!");
                            embedSpec.setColor(Color.RED);
                        }
                            
                    });
                }).block();
            } catch(Exception ex) {
                ex.printStackTrace();
                System.out.println("Exception cought...");
            }
            
        });

        // commands.put(">", new InputCommand());

        // #########################
        // ## Admin ##
        // #########################

        adminCommands.put("help", event -> event.getMessage().getAuthor().get().getPrivateChannel().block()
                .createMessage(createHelp(adminCommands)).block());
        adminCommands.put("addDP", event -> discordPlaysList.add(event.getMessage().getChannelId().asString()));
        adminCommands.put("listDP", event -> event.getMessage().getChannel().block()
                .createMessage(listDPL(event.getGuild().block())).block());
        adminCommands.put("removeDP", event -> discordPlaysList.remove(event.getMessage().getChannelId().asString()));
        adminCommands.put("allowkey", event -> allowkey(event));

        adminCommands.put("config", event -> {
            String msg = event.getMessage().getContent().orElse("");

            String[] args = msg.split(" ");

            if(args.length == 2) {
                if (!args[1].equals("")) {
                    switchConfig(args[1]);
                }
            } else if(args.length == 1) {
                event.getMessage().getChannel().block().createMessage("currently selected config: " + currentConfig).block();
            }
        });

        adminCommands.put("var", event -> {
            String msg = event.getMessage().getContent().orElse("");

            String[] args = msg.split(" ");

            if(args.length == 3) {
                String key = args[1];
                String value = args[2];

                if(ints.containsKey(key)) {
                    try {
                        ints.put(key, Integer.parseInt(value));
                    } catch(Exception ex) {
                    }
                    System.out.println("var \"" + key + "\" set to: " + ints.get(key));
                    // event.getMessage().getChannel().block().createMessage("Var \"" + key + "\" set to: " + ints.get(key)).block();
                }

            } else if (args.length == 2) {
                String key = args[1];
                
                if(ints.containsKey(key)) {
                    event.getMessage().getChannel().block().createMessage("var \"" + key + "\": " + ints.get(key)).block();
                }
                    
            }
            
        });

        adminCommands.put("addmacro", event -> {

            String msg = event.getMessage().getContent().orElse("");

            String[] args = msg.split(" ", 3);

            if(args.length == 3) {
                String name = args[1];
                String macro = args[2];

                MacroManager.addMacro(name, macro, event.getMessage().getAuthor().get().getId().asLong());

            }

        });

        // Consumer<EmbedCreateSpec> template = spec -> {
        //     // Edit the spec as you normally would
        // };

        adminCommands.put("macroinfo", event -> {

            String msg = event.getMessage().getContent().orElse("");

            String[] args = msg.split(" ", 2);

            if(args.length == 2) {
                String name = args[1];

                Macro m = MacroManager.getMacroObject(name);

                if(m != null) {

                    String macroName = m.getName();
                    String macroCmds = m.get();
                    
                    
                    // Mono<Message> message = event.getMessage().getChannel().block()
                    // .createMessage(messageSpec -> messageSpec.setEmbed(template.andThen(embedSpec -> {
                        
                    // })));
                    
                    Mono<Message> message = event.getMessage().getChannel().block().createMessage(messageSpec -> {
                        // messageSpec.setContent("Content not in an embed!");
                        // You can see in this example even with simple singular property defining specs the syntax is concise
                        messageSpec.setEmbed(embedSpec -> {
                            if(m.getAuthorID() != 0) {
                                User author = client.getUserById(Snowflake.of(m.getAuthorID())).block();
                                embedSpec.setAuthor(author.getUsername(), null, author.getAvatarUrl());
                            }else {
                                embedSpec.setAuthor(client.getSelf().block().getUsername(), null, client.getSelf().block().getAvatarUrl());
                            }

                            embedSpec.setTitle(macroName);
                            embedSpec.addField("Macro info", macroCmds, false);
                            embedSpec.setColor(Color.MAGENTA);
                            // embedSpec.setDescription("Description is in an embed!");
                        });
                    });
                    // 

                    message.block();

                }

            }

        });

        adminCommands.put("pmdlayout", event -> {

            String msg = event.getMessage().getContent().orElse("");

            String[] args = msg.split(" ", 2);

            if(args.length == 2) {
                String layout = args[1];

                if(! InputHandler.setPMDLayout(layout)){
                    event.getMessage().getChannel().block().createMessage("Error -> Layout \""+layout+"\" doesn't exist!").block();
                }

            } else {

                Mono<Message> message = event.getMessage().getChannel().block().createMessage(messageSpec -> {
                    // messageSpec.setContent("Content not in an embed!");
                    // You can see in this example even with simple singular property defining specs the syntax is concise
                    messageSpec.setEmbed(embedSpec -> {

                        embedSpec.setTitle("PMD Keyboard Layouts");
                        for(Entry<String, String> e : InputHandler.getPMDLayouts().entrySet()) {
                            embedSpec.addField(e.getKey(), e.getValue(), false);
                        }
                        
                        embedSpec.setColor(Color.MAGENTA);
                        // embedSpec.setDescription("Description is in an embed!");
                    });
                });

                message.block();

            }

        });

        adminCommands.put("specials", event -> event.getMessage().getChannel().block().createMessage("Specials:  \n"+InputHandler.getSpecials()).block());

        adminCommands.put("enableDP", event -> InputHandler.setEnabled(true));
        adminCommands.put("disableDP", event -> InputHandler.setEnabled(false));
        adminCommands.put("isDP", event -> event.getMessage().getChannel().block().createMessage("DiscordPlays enabled: "+InputHandler.isEnabled()).block());

        adminCommands.put("quit", event -> quit(event));
        adminCommands.put("exit()", event -> System.exit(2));
    }


    static Guild discord_server = null;
    static MessageChannel terraria_channel = null;

    public static void main(String[] args) {

        String token = "";
        try {
            token = readFile("./config/token.txt").get(0);
            System.out.println("Token retrieved!");
        } catch (Exception e1) {
            e1.printStackTrace();
            System.out.println("No token available! -> token.txt not accessible!");
            System.exit(1);
        }
        final String owner_id = "156647870093590528";

        defaultVars();

        new InputHandler();
        new MacroManager();
        TerrariaCommunicator.init();

        try {
            ArrayList<String> profileAL = readFile("./config/profile.txt");
            String profile = "";
            if (profileAL == null || profileAL.size() == 0)
                profile = "default";
            else
                profile = profileAL.get(0);
            if (profile.equals(""))
                profile = "default";
            loadConfig(profile);
            loadVars(profile);
            loadMacros(profile);
            currentConfig = profile;
            System.out.println("Config \"" + profile + "\" loaded!");
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        try {
            discordPlaysList.addAll(readFile("./config/discord_plays_channel_ids.txt"));
            discordPlaysList.forEach(str -> System.out.println("Added input enabled channel: " + str));
        } catch (IOException e) {
            e.printStackTrace();
        }

        DiscordClientBuilder clientBuilder = new DiscordClientBuilder(token);

        clientBuilder.setInitialPresence(Presence.online(Activity.watching("all of you >:)")));

        client = clientBuilder.build();

        TerrariaCommunicator.addFormatReplace("<:i29:686297958316245002>", "[i:29]");

        try {
            // discord_server = client.getGuildById(Snowflake.of("554100310772154368")).block();
            terraria_channel = (MessageChannel) client.getChannelById(Snowflake.of("676151086700036126")).block();
            // TestServer: 681562227597246471
            // BuCi: 676151086700036126

            TerrariaCommunicator.subscribe((TerrariaServerChatEvent) e -> {
                terraria_channel.createMessage(messageSpec -> {

                    String[] terraria_args = e.split("\n",2);
                    if(terraria_args.length != 2) return;
                    String terraria_msg_author = terraria_args[0];
                    String terraria_msg = terraria_args[1];
                    if(terraria_msg.equals("")) return;
                    // messageSpec.setContent("Content not in an embed!");
                    // You can see in this example even with simple singular property defining specs the syntax is concise
                    messageSpec.setContent("**"+terraria_msg_author + "** > ``" + terraria_msg + "``");
                }).block();
            });

            TerrariaCommunicator.subscribe((TerrariaServerJoinEvent) e -> {
                terraria_channel.createMessage(messageSpec -> {

                    String[] terraria_args = e.split("\n",2);
                    if(terraria_args.length != 2) return;
                    String terraria_user_name = terraria_args[0];
                    String terraria_user_ip = terraria_args[1];
                    if(terraria_user_name.equals("")) return;
                    // messageSpec.setContent("Content not in an embed!");
                    // You can see in this example even with simple singular property defining specs the syntax is concise
                    messageSpec.setEmbed(embedSpec -> {
                        embedSpec.setTitle(terraria_user_name + " is connecting to the Server.");
                        embedSpec.setColor(Color.GREEN);
                    });
                }).block();
            });

            TerrariaCommunicator.subscribe((TerrariaServerLeaveEvent) e -> {
                terraria_channel.createMessage(messageSpec -> {

                    String[] terraria_args = e.split("\n",2);
                    if(terraria_args.length != 2) return;
                    String terraria_user_name = terraria_args[0];
                    String terraria_user_ip = terraria_args[1];
                    if(terraria_user_name.equals("")) return;
                    // messageSpec.setContent("Content not in an embed!");
                    // You can see in this example even with simple singular property defining specs the syntax is concise
                    messageSpec.setEmbed(embedSpec -> {
                        embedSpec.setTitle(terraria_user_name + " left the Server.");
                        embedSpec.setColor(Color.RED);
                    });
                }).block();
            });

            TerrariaCommunicator.subscribe((TerrariaServerBroadcastEvent) e -> {

                terraria_channel.createMessage(messageSpec -> {

                    String[] terraria_args = e.split("\n",2);
                    if(terraria_args.length != 2) return;
                    String terraria_message = terraria_args[0];
                    String terraria_color = terraria_args[1];
                    // System.out.println("Color: "+terraria_color);
                    String[] col = terraria_color.replace("{", "").replace("}", "").split(" ");
                    HashMap<Character, Integer> color_map = new HashMap<>();

                    Color temp_color;

                    try {
                        for(String s : col) {
                            String[] splitted = s.split(":");
                            color_map.put(splitted[0].charAt(0), Integer.parseInt(splitted[1]));
                        }
                        temp_color = new Color(color_map.get('R'),color_map.get('G'),color_map.get('B'));
                    } catch(Exception ex) {
                        temp_color = Color.WHITE;
                    }

                    final Color final_color = temp_color;

                    if(terraria_message.equals("")) return;
                    // messageSpec.setContent("Content not in an embed!");
                    // You can see in this example even with simple singular property defining specs the syntax is concise
                    messageSpec.setEmbed(embedSpec -> {
                        embedSpec.setTitle(terraria_message);
                        if(final_color != null)
                            embedSpec.setColor(final_color);
                        else embedSpec.setColor(Color.WHITE);
                    });
                }).block();

            });

            TerrariaCommunicator.subscribe((TerrariaServerUnknownEvent) e -> {
                System.out.println("Unknown Event: "+e);
            });

        }catch(Exception ex) {
            ex.printStackTrace();
        }
        // LocalDateTime now = LocalDateTime.now();

        client.getEventDispatcher().on(MessageCreateEvent.class)
                // subscribe is like block, in that it will *request* for action
                // to be done, but instead of blocking the thread, waiting for it
                // to finish, it will just execute the results asynchronously.
                .subscribe(event -> {
                    final Message message = event.getMessage();
                    final String content = message.getContent().orElse("");
                    final User author;

                    try {
                        author = message.getAuthor().get();
                    } catch(NoSuchElementException ex) {
                        System.out.println("Error -> Main event: No Author (NoSuchElementException)");
                        return;
                    }
                    final MessageChannel channel = message.getChannel().block();

                    if (author.getId().asString().equals(client.getSelfId().get().asString()))
                        return;

                    

                    LocalDateTime now = LocalDateTime.now();

                    if(content.equals("")) {
                        System.out.println("[" + DTF.format(now) + "] " +event.getMessage().getGuild().block().getName()+":"+ ((TextChannel) event.getMessage().getChannel().block()).getName() + ": " + event.getMember().get().getUsername() + " sent an empty message.");
                        return;
                    }

                    System.out.println("[" + DTF.format(now) + "] " +event.getMessage().getGuild().block().getName()+":"+ ((TextChannel) event.getMessage().getChannel().block()).getName() + ": " + event.getMember().get().getUsername() + " : " + content);
                    if (discordPlaysList.contains(channel.getId().asString())) {
                        // channel.createMessage("\"" + content + "\"").block();
                        
                        if( InputHandler.instance.handleInput(content) )
                            return;
                        
                            
                        
                    }

                    if (author.getId().asString().equals(owner_id) || author.getId().asString().equals("210955280307978240" /* Aeros Discord ID */)) {
                        // Admin Stuff
                        for (final Map.Entry<String, Command> entry : adminCommands.entrySet()) {
                            // We will be using ! as our "prefix" to any command in the system.
                            if (content.toLowerCase().startsWith(prefix + entry.getKey().toLowerCase())) {
                                entry.getValue().execute(event);
                                if(!commands.containsKey(entry.getKey())) return;
                                break;
                            }
                        }
                        // return;
                    }

                    for (final Map.Entry<String, Command> entry : commands.entrySet()) {
                        // We will be using ! as our "prefix" to any command in the system.
                        if (content.toLowerCase().startsWith(prefix + entry.getKey().toLowerCase())) {
                            entry.getValue().execute(event);
                            return;
                        }
                    }

                    if (channel.getId().equals(terraria_channel.getId())) {
                        TerrariaCommunicator.sendFormatedMessage(author.getUsername(), content);
                    }

                });

        client.getEventDispatcher().on(ReadyEvent.class)
            .subscribe(event -> {
                List<Guild> guildList = event.getClient().getGuilds().collectList().block();
                System.out.println("Watching " + guildList.size() + " Guilds:");
                guildList.forEach(guild -> System.out.println("  : " + guild.getId().asString() + " : " + guild.getName()));
            });


        // Login :)
        client.login().block();

    }


    private static void save() {
        try {
            writeFile("./config/discord_plays_channel_ids.txt", discordPlaysList);

            ArrayList<String> tmpAL = new ArrayList<>();
            tmpAL.add(currentConfig);
            writeFile("./config/profile.txt", tmpAL);
        } catch (IOException e) {
            e.printStackTrace();
        }
        saveConfig(currentConfig);
        saveVars(currentConfig);
        saveMacros(currentConfig);
        System.out.println("Config \"" + currentConfig + "\" saved!");
    }

    private static void switchConfig(String newCfg) {

        System.out.println("Switching configs: \"" + currentConfig + "\" -> \"" + newCfg + "\"");
        saveConfig(currentConfig);
        saveVars(currentConfig);
        saveMacros(currentConfig);
        System.out.println("Config \"" + currentConfig + "\" saved!");
        loadConfig(newCfg);
        loadVars(newCfg);
        loadMacros(newCfg);
        currentConfig = newCfg;
        System.out.println("Config \"" + newCfg + "\" loaded!");
        System.out.println("Config switched!");
    }

    static int count = 0; // <-- Trash :)

    private static void loadConfig(String name) {

        String path = "./config/other/"+name+"/";

        if(name.equals("default") ){
            path = "./config/default/";
        }

        allowedKeys = new HashSet<>();
        inputRemap = new HashMap<>();
        count = 0;

        try {
            ArrayList<String> tmpKA = new ArrayList<>();
            readFile(path + "allowed_keys.txt").forEach(str -> {
                str = str.replaceAll("\n", "");
                str = str.toUpperCase();
                // if (str.length() == 1) {
                allowedKeys.add(str);
                tmpKA.add(str);
                System.out.println("Added AllowedKey: " + str);
                // }
            });

            int count = 0;
            for ( String str : readFile(path + "allowed_keys_remap.txt")) {
                str = str.toUpperCase();

                inputRemap.put(tmpKA.get(count), str);
                System.out.println("Added Key Remap: " + tmpKA.get(count) + " -> " + str);
                count++;
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }

    private static void saveConfig(String name) {

        String path = "./config/other/"+name+"/";

        if(name.equals("default") ){
            path = "./config/default/";
        }

        try {
            ArrayList<String> tmpal = new ArrayList<>();
            tmpal.addAll(allowedKeys);
            writeFile(path + "allowed_keys.txt", tmpal);

            tmpal = new ArrayList<>();
            tmpal.addAll(inputRemap.values());
            writeFile(path + "allowed_keys_remap.txt", tmpal);
        } catch(IOException ex) {
            ex.printStackTrace();
        }

    }

    private static void loadVars(String name) {

        String path = "./config/other/"+name+"/vars/";

        if (name.equals("default")) {
            path = "./config/default/vars/";
        }

        ints = new HashMap<>();

        defaultVars();

        try {
            ArrayList<Integer> tmp = new ArrayList<>();
            readFile(path + "v_ints_values.txt").forEach(str -> {
                str = str.replaceAll("\n", "");
                try {
                    tmp.add(Integer.parseInt(str));
                } catch(NumberFormatException ex) {
                    ex.printStackTrace();
                }
            });

            int c = 0;
            for ( String key : readFile(path + "v_ints.txt")) {
                ints.put(key, tmp.get(c));
                c++;
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }

    private static void saveVars(String name) {

        String path = "./config/other/"+name+"/vars/";

        if (name.equals("default")) {
            path = "./config/default/vars/";
        }

        try {
            ArrayList<String> tmpal = new ArrayList<>();
            tmpal.addAll(ints.keySet());
            writeFile(path + "v_ints.txt", tmpal);

            tmpal = new ArrayList<>();
            for (int i : ints.values())
                tmpal.add(""+i);
            writeFile(path + "v_ints_values.txt", tmpal);
        } catch(IOException ex) {
            ex.printStackTrace();
        }

    }

    /*
        Macro File:
            Macro Name
            Author ID
            Macro
    */

    private final static String macroFileSuffix = ".mcr";

    private static void loadMacros(String name) {

        String path = "./config/macros/other/"+name+"/";

        if (name.equals("default")) {
            path = "./config/macros/default/";
        }

        File folder = new File(path);
        folder.mkdirs();

        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isFile()) {
                String macroFileName = fileEntry.getName();
                if(!macroFileName.endsWith(macroFileSuffix)) continue;
                System.out.println("Loading Macro: "+macroFileName);

                ArrayList<String> macro = null;
                try {
                    macro = readFile(path + macroFileName);
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }

                if(macro == null) continue;

                if(macro.size() != 3) continue;

                MacroManager.addMacro(macro.get(0), macro.get(2), Long.parseLong(macro.get(1)));
            }
        }

    }

    private static void saveMacros(String name) {

        String path = "./config/macros/other/"+name+"/";

        if (name.equals("default")) {
            path = "./config/macros/default/";
        }

        ArrayList<Macro> macros = MacroManager.getMacroList();
        
        for(Macro m : macros) {

            try {
                writeFile(path + m.getName() + macroFileSuffix, m.asList());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    private static String listKeys() {
        String ret = "";
        for(String str : allowedKeys) {
            ret += ", " + str;
        }
        return ret;
    }

    private static void allowkey(MessageCreateEvent event) {
        String msg = event.getMessage().getContent().get();
        String key = "";
        String key_remap = "";
        try {
            key = msg.split(" ")[1];
            key_remap = msg.split(" ")[2];
            // if(key.length() != 1) throw new Exception("Key length is not equal to 1 (one)");
        } catch(Exception ex) {
            System.out.println("allowkey(...) -> Error, invalid key / arguments. : " + msg + "\n" + ex.getMessage());
            return;
        }
        
        System.out.println("Chat:Allowkey: "+key.toUpperCase()+" -> "+key_remap.toUpperCase());
        allowedKeys.add(key.toUpperCase());
        inputRemap.put(key.toUpperCase(), key_remap.toUpperCase());
    }

    

    private static String listDPL(Guild guild) {
        String ret = "";

        for (String string : discordPlaysList) {
            ret += guild.getChannelById(Snowflake.of(string)).block().getMention() + "\n";
        }

        if(ret.equals("")) return "None";
        return ret;
    }

    private static void quit(MessageCreateEvent event) {
        save();
        event.getClient().getSelf().block().getClient().logout().block();
        System.exit(0);
    }

    private static String createHelp(Map<String, Command> map) {
        String help = "";
        for(String cmd : map.keySet()) {
            help += cmd + " ; ";
        }
        return help;
    }

    public static ArrayList<String> readFile(String filename) throws FileNotFoundException, IOException {
        System.out.println("Reading file: " + filename);
        File myObj = new File(filename);
        if(!myObj.exists()) {
            myObj.getParentFile().mkdirs();
            myObj.createNewFile();
        }
        Scanner myReader = new Scanner(myObj);
        ArrayList<String> ret = new ArrayList<>();
        while (myReader.hasNextLine()) {
            String str = myReader.nextLine();
            if(!str.equals(""))
                ret.add(str);
        }
        myReader.close();
        
        return ret;
    }

    public static void writeFile(String filename, ArrayList<String> towrite) throws IOException {

        File file = new File(filename);
        
        file.getParentFile().mkdirs();

        FileWriter myWriter = new FileWriter(file);

        for (String string : towrite) {
            myWriter.write(string + "\n");
        }
        myWriter.close();
        System.out.println("Written to file: " + filename);

    }

}
