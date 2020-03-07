package me.auri.dplays;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

class InputHandler {

    private Robot r;

    public static InputHandler instance;

    private final Map<String, InputCommand> commands = new HashMap<>();

    private HashMap<String, String> specials = new HashMap<>();

    private HashMap<String, String> pmd_layout = new HashMap<>();

    private String pmd_layout_const = "eos";

    private boolean enabled = true;

    public static boolean setPMDLayout(String layout) {
        if(instance.pmd_layout.containsKey(layout)) {
            instance.pmd_layout_const = layout;
            return true;
        }
        return false; 
    }

    public static String getSpecials() {
        String ret = "";

        for (Entry<String, String> e : instance.specials.entrySet()) {
            ret += e.getKey() + " = " + e.getValue() +"\n";
        }

        return ret;
    }

    public static HashMap<String, String> getPMDLayouts() {
        return (HashMap<String, String>) instance.pmd_layout.clone();
    }

    public InputHandler() {

        if (instance != null) return;
        instance = this;

        try {
            r = new Robot();
            r.setAutoDelay(5);
        } catch (AWTException e) {
            e.printStackTrace();
        }

        commands.put("hold", (cmd, aK, iR) -> {

            String[] parts = cmd.split(" ");

            if(parts.length < 2) return;

            for(String key : parts) {
                if(key.startsWith("!")) continue;

                if(!isAllowed(key, aK)) continue;
                pressKey(remap(key, iR));

            }
            r.delay(Core.getVar("postDelay"));

        });

        commands.put("release", (cmd, aK, iR) -> {

            String[] parts = cmd.split(" ");

            if(parts.length < 2) return;

            for(String key : parts) {
                if(key.startsWith("!")) continue;

                if(!isAllowed(key, aK)) continue;
                releaseKey(remap(key, iR));

            }
            r.delay(Core.getVar("postDelay"));

        });

        commands.put("toggle", (cmd, aK, iR) -> {

            String[] parts = cmd.split(" ");

            if(parts.length < 2) return;

            for(String key : parts) {
                if(key.startsWith("!")) continue;

                if(!isAllowed(key, aK)) continue;
                toggleKey(remap(key, iR));

            }
            r.delay(Core.getVar("postDelay"));

        });

        commands.put("type", (cmd, aK, iR) -> {

            String[] args = cmd.split(" ");
            if(args.length < 2) return;

            cmd = cmd.split(" ",2)[1];

            if(!isAllowed("up", aK)) return;
            if(!isAllowed("down", aK)) return;
            if(!isAllowed("left", aK)) return;
            if(!isAllowed("right", aK)) return;
            if(!isAllowed("A", aK)) return;

            // TODO: rename variables to conform to java naming conventions.
            int keyboard_const = 13;
            String[] keyboard_layout = {
                "ABCDEFGHIJ ,.KLMNOPQRST '-UVWXYZ     ♂♀             0123456789   ",
                "abcdefghij ,.klmnopqrst '-uvwxyz     ♂♀             0123456789   ",
                ",.:;!?   ♂♀      ()         ~@#%+-*/=                            "
            };

            int cursor_x = 0;
            int cursor_y = 0;
            int cursor_k = 0;

            int destination_x = 0;
            int destination_y = 0;

            String final_command = "";

            for (int j = 0; j < cmd.length(); j++) {
                String c = "" + cmd.charAt(j);

                if(! keyboard_layout[cursor_k].contains(c)) {
                    if (cursor_x != 0)
                        final_command += " left+" + (cursor_x);
                    final_command += " up+" + (cursor_y+1);
                    for (int i = 0; i < keyboard_layout.length; i++) {
                        if(keyboard_layout[i].contains(c)) {
                            cursor_k = i;
                            if (cursor_k != 0)
                                final_command += " right+" + cursor_k;
                            final_command += " A";
                            final_command += " left+" + (cursor_k+1);
                            final_command += " right";
                            final_command += " down";
                            cursor_x = 0;
                            cursor_y = 0;

                        }
                    }
                }

                destination_y = (int) (keyboard_layout[cursor_k].indexOf(c) / keyboard_const);
                destination_x = (int) (keyboard_layout[cursor_k].indexOf(c) % keyboard_const);
                if (destination_x - cursor_x < 0)
                    final_command += " left+" + (destination_x - cursor_x) * -1;
                else if (destination_x - cursor_x > 0)
                    final_command += " right+" + (destination_x - cursor_x);

                if (destination_y - cursor_y < 0)
                    final_command += " up+" + (destination_y - cursor_y) * -1;
                else if (destination_y - cursor_y > 0)
                    final_command += " down+" + (destination_y - cursor_y);

                cursor_x = destination_x;
                cursor_y = destination_y;
                final_command += " A";

            }

            System.out.println("type final output: "+final_command);
            handleCommand(final_command.substring(1), aK, iR, null);


        });

        specials.put("(M)", "♂");
        specials.put("(F)", "♀");

        specials.put("\"", "”");
        specials.put("(l\")", "“");
        specials.put("(r\")", "”");

        specials.put("'", "’");
        specials.put("(l')", "‘");
        specials.put("(r')", "’");

        specials.put("(e)", "é");

        specials.put("(...)","…");

        specials.put("(?)", "¿");
        specials.put("(!)", "¡");

        // ¬ == ignore
        pmd_layout.put("eos",      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890:+-,.¡!¿?‘’“”♂♀ ");
        pmd_layout.put("rt",       "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZé1234567890:…+-,.!?‘’“”♂♀ ");
        pmd_layout.put("rt_extra", "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@.&-_#$%:;*+<=> ");

        //abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@.&-_#$%:;*+<=>

        commands.put("pmdtype", (cmd, aK, iR) -> {

            String[] args = cmd.split(" ");
            if(args.length < 2) return;

            for(Entry<String, String> s : specials.entrySet()) {
                cmd = cmd.replace(s.getKey(), s.getValue());
            }

            if(cmd.length() > 10) cmd = cmd.substring(0, 10);

            if(!isAllowed("up", aK)) return;
            if(!isAllowed("down", aK)) return;
            if(!isAllowed("left", aK)) return;
            if(!isAllowed("right", aK)) return;
            if(!isAllowed("A", aK)) return;

            // TODO: rename variables to conform to java naming conventions.
            int keyboard_const = 13;
            String keyboard_layout = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890:+-,.¡!¿?‘’“”♂♀ ";

            if(pmd_layout.containsKey(pmd_layout_const)) {
                keyboard_layout = pmd_layout.get(pmd_layout_const);
            }

            int cursor_x = 0;
            int cursor_y = 0;

            int destination_x = 0;
            int destination_y = 0;

            String final_command = "";

            for (int j = 0; j < cmd.length(); j++) {
                
                String c = "" + cmd.charAt(j);
                if (! keyboard_layout.contains(c)) continue;
                if (c.equals("¬")) continue;

                destination_y = (int) (keyboard_layout.indexOf(c) / keyboard_const);
                destination_x = (int) (keyboard_layout.indexOf(c) % keyboard_const);
                System.out.println("char: "+c+" index: "+keyboard_layout.indexOf(c)+" DestX: "+destination_x+" DestY: "+destination_y);
                if (destination_x - cursor_x < 0)
                    final_command += " left+" + (destination_x - cursor_x) * -1;
                else if (destination_x - cursor_x > 0)
                    final_command += " right+" + (destination_x - cursor_x);

                if (destination_y - cursor_y < 0)
                    final_command += " up+" + (destination_y - cursor_y) * -1;
                else if (destination_y - cursor_y > 0)
                    final_command += " down+" + (destination_y - cursor_y);

                cursor_x = destination_x;
                cursor_y = destination_y;
                final_command += " A";

            }

            System.out.println("type final output: "+final_command);
            handleCommand(final_command.substring(1), aK, iR, null);


        });

    }

    

    private Map<String, Boolean> isPressed = new HashMap<>();

    public static ArrayList<String> getPressedKeys() {
        ArrayList<String> ret = new ArrayList<>();

        for(Entry<String, Boolean> entry : instance.isPressed.entrySet()) {
            if(entry.getValue())
                ret.add(entry.getKey());
        }

        return ret;
    }

    public void pressKey(String key) {
        if(key.equalsIgnoreCase("NULL")) return;
        // System.out.println("Pressing: "+key);
        try {
            r.keyPress((int) KeyEvent.class.getField("VK_" + key.toUpperCase()).getInt(null));
            isPressed.put(key, true);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
        }
    }

    private void releaseKey(String key) {
        if(key.equalsIgnoreCase("NULL")) return;
        try {
            r.keyRelease((int) KeyEvent.class.getField("VK_" + key.toUpperCase()).getInt(null));
            isPressed.put(key, false);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
        }
    }

    private boolean isPressed(String key) {
        if(isPressed.containsKey(key))
            return isPressed.get(key);
        return false;
    }

    private void toggleKey(String key) {
        if(isPressed(key)) releaseKey(key);
        else pressKey(key);
    }        

    private boolean ignoreIfFirstIsNotValid = true;

    public static void setIgnoreIfFirstIsNotValid(boolean b) {
        instance.ignoreIfFirstIsNotValid = b;
    }

    public static boolean getIgnoreIfFirstIsNotValid() {
        return instance.ignoreIfFirstIsNotValid;
    }

	public boolean handleInput(String content) {

        if(!enabled) return false;

        HashSet<String> allowedKeys = Core.getAllowedKeys();
        Map<String, String> inputRemap = Core.getInputRemap();

        if(content.startsWith(Core.prefix)) {

            for (final Map.Entry<String, InputCommand> entry : commands.entrySet()) {
                // We will be using ! as our "prefix" to any command in the system.
                if (content.startsWith(Core.prefix + entry.getKey())) {
                    entry.getValue().execute(content, allowedKeys, inputRemap);
                    return true;
                }
            }

        } else {
            if(!content.startsWith("%"))
                if(!isAllowed(content.split(" ", 2)[0].split(":", 2)[0].split("\\+", 2)[0], allowedKeys)) return false;
            handleCommand(content, allowedKeys, inputRemap, new HashSet<String>());
        }

        return false;
    }
    
    private void handleCommand(String command, HashSet<String> aK, Map<String, String> iR, HashSet<String> macroBlockList) {
        // Right left up+9
        if(macroBlockList == null) macroBlockList = new HashSet<>();


        if(!command.contains(" ") && !command.contains("+") && !command.contains(":") && !command.contains("%")) {
            if(!isAllowed(command, aK)) return;
            executeCommand(remap(command, iR));
            return;
        }

        String[] cmds = command.split(" ");
        
        // System.out.println("Commands: ");
        // Arrays.asList(cmds).forEach(e -> System.out.print(e + " "));
        // System.out.println();

        for (String cmd : cmds) {
            if(cmd.equals("")) continue;

            if(cmd.contains("+") && !cmd.contains(":") && !cmd.startsWith("%")) {

                if(handleLoop(cmd, aK, iR)) continue;

            }else if(!cmd.contains("+") && cmd.contains(":") && !cmd.startsWith("%")) {

                // <key>: h r t
                if(handleModifier(cmd, aK, iR)) continue;


            } else {

                if(cmd.startsWith("%")) {

                    if(macroBlockList.contains(cmd)) continue;

                    if(cmd.contains("+") && !cmd.contains(":")) {

                        // System.out.println("%Macro (Loop): " + cmd);
                        if(handleMacroLoop(cmd, aK, iR, macroBlockList)) continue;
        
                    } else {
                        // System.out.println("%Macro: " + cmd);
                        HashSet<String> newBlockList = copySet(macroBlockList);
                        newBlockList.add(cmd);
                        handleCommand(MacroManager.getMacro(cmd), aK, iR, newBlockList);
                    }

                } else {
                    if(!isAllowed(cmd, aK)) continue;
                    executeCommand(remap(cmd, iR));
                }
                
            }

        }

    }

    private HashSet<String> copySet(HashSet<String> set) {
        return (HashSet<String>) set.clone();
    }

    private boolean handleMacroLoop(String cmd, HashSet<String> aK, Map<String, String> iR, HashSet<String> macroBlockList) {

        String[] parts = cmd.split("\\+");

        if(parts.length == 2) {
            String key = parts[0];
            // if(!isAllowed(key, aK)) return true;
            int repeat = 1;
            try {
                repeat = Integer.parseInt(parts[1]);
            } catch(Exception ex) {
                repeat = 1;
            }
            repeat = Math.min(repeat, Core.getVar("maxLoops")); // Maximum loops is 15
            for(int i = repeat; i > 0; i--) {
                HashSet<String> newBlockList = copySet(macroBlockList);
                newBlockList.add(key);
                handleCommand(MacroManager.getMacro(key), aK, iR, newBlockList);
            }
        }

        return false;
    }

    private boolean handleLoop(String cmd, HashSet<String> aK, Map<String, String> iR) {

        String[] parts = cmd.split("\\+");

        if(parts.length == 2) {
            String key = parts[0];
            if(!isAllowed(key, aK)) return true;
            int repeat = 1;
            try {
                repeat = Integer.parseInt(parts[1]);
            } catch(Exception ex) {
                repeat = 1;
            }
            repeat = Math.min(repeat, Core.getVar("maxLoops")); // Maximum loops is 15
            for(int i = repeat; i > 0; i--) {
                executeCommand(remap(key, iR), Core.getVar("postDelay"));
            }
        }

        return false;

    }

    private boolean handleModifier(String cmd, HashSet<String> aK, Map<String, String> iR) {

        String[] parts = cmd.split(":");

        if(parts.length == 2) {
            String key = parts[0];
            if(!isAllowed(key, aK)) return true;
            
            String modifier = parts[1];

            if (modifier.equalsIgnoreCase("h")) {
                pressKey(remap(key, iR));
            }

            if (modifier.equalsIgnoreCase("r")) {
                releaseKey(remap(key, iR));
            }

            if (modifier.equalsIgnoreCase("t")) {
                toggleKey(remap(key, iR));
            }

            
            if (modifier.equalsIgnoreCase("q")) {
                pressKey(remap(key, iR));
                releaseKey(remap(key, iR));
            }
            
        }

        return false;
    }

    private boolean isAllowed(String key, HashSet<String> aK) {
        return aK.contains(key.toUpperCase());
    }

    private String remap(String key, Map<String, String> iR) {
        if(iR.containsKey(key.toUpperCase()))
            return iR.get(key.toUpperCase());
        return key.toUpperCase();
    }

    private void executeCommand(String key) {
        executeCommand(key, Core.getVar("postDelay"));
    }

    private void executeCommand(String key, int postDelay) {
        executeCommand(key, Core.getVar("delay"), postDelay);
    }

    private void executeCommand(String key, int delay, int postDelay) {

        pressKey(key);
        r.delay(delay);
        releaseKey(key);
        if(postDelay > 0) {
            r.delay(postDelay);
        }
    }

	public static void setEnabled(boolean b) {
        
        instance.enabled = b;

	}

	public static boolean isEnabled() {
		return instance.enabled;
	}

}