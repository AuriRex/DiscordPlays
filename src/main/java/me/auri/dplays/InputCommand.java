package me.auri.dplays;

import java.util.HashSet;
import java.util.Map;

interface InputCommand {
    public void execute(String cmd, HashSet<String> aK, Map<String, String> iR);
}