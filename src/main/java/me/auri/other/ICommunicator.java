package me.auri.other;

public interface ICommunicator {
    
    public boolean handleEvents(String identification, String event, String content);

}