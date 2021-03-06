package me.auri.dplays;

import java.util.ArrayList;

public class Macro {

    private final String name;

    private String macro;

    private long authorid;

    public String getName() {
        return this.name;
    }

    public String get() {
        return this.macro;
    }

    public void setMacro(String macro, long authorid) {
        this.macro = macro;
        setAuthorID(authorid);
    }

    public long getAuthorID() {
        return this.authorid;
    }

    public void setAuthorID(long authorid) {
        this.authorid = authorid;
    }

    public ArrayList<String> asList() {
        ArrayList<String> ret = new ArrayList<String>();

        ret.add(this.name);
        ret.add("" +  this.authorid);
        ret.add(this.macro);

        return ret;
    }

    public Macro(String name, String macro, long authorid) {

        this.name = name;
        this.macro = macro;
        this.authorid = authorid;

    }

}