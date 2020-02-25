package me.auri.dplays;

import java.util.HashMap;
import java.util.Map;

public class ScriptManager {

    private static ScriptManager instance;

    private Map<String, String> scriptList;

    public ScriptManager() {

        if(instance != null) return;
        instance = this;

        scriptList = new HashMap<>();

        addScript("$upright", "up right up right up right");
        addScript("$jump", "s:h up+5 s:r");
        addScript("$recursion", "left:h $jump left:r down+5");
        addScript("$error", "right:h $error right:r");

    }

    public static void addScript(String name, String script) {

        instance.scriptList.put(name, script);

    }

    public static String getScript(String name) {
        if(instance.scriptList.containsKey(name))
            return instance.scriptList.get(name);
        return "";
    }

}