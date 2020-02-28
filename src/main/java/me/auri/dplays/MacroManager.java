package me.auri.dplays;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MacroManager {

    private static MacroManager instance;

    private Map<String, Macro> macroList;

    public MacroManager() {

        if(instance != null) return;
        instance = this;

        macroList = new HashMap<>();

    }

    public static void addMacro(String name, String macro, long authorid) {

        instance.macroList.put(name, new Macro(name, macro, authorid));

    }

    public static String getMacro(String name) {
        if(instance.macroList.containsKey(name))
            return instance.macroList.get(name).get();
        return "";
    }

    public static Macro getMacroObject(String name) {
        if(instance.macroList.containsKey(name))
            return instance.macroList.get(name);
        return null;
    }

    public static long getAuthorID(String name) {
        if(instance.macroList.containsKey(name))
            return instance.macroList.get(name).getAuthorID();
        return 0;
    }

    public static ArrayList<Macro> getMacroList() {
        ArrayList<Macro> ret = new ArrayList<Macro>();
        for(Macro macro : instance.macroList.values()) {
            ret.add(macro);
        }
        return ret;
    }

}