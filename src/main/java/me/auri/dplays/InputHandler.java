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

	public boolean handleInput(String content) {

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
            handleCommand(content, allowedKeys, inputRemap, new HashSet<String>());
        }

        return false;
    }
    
    private void handleCommand(String command, HashSet<String> aK, Map<String, String> iR, HashSet<String> macroBlockList) {
        // Right left up+9



        if(!command.contains(" ") && !command.contains("+") && !command.contains(":") && !command.contains("%")) {
            if(!isAllowed(command, aK)) return;
            executeCommand(remap(command, iR));
            return;
        }

        String[] cmds = command.split(" ");
        
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
                        // TODO: Macros
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

            // executeCommand(, Core.getVar("postDelay"));
            
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
        executeCommand(key, 0);
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

}