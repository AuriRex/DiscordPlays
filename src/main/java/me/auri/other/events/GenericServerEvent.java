package me.auri.other.events;

public interface GenericServerEvent {
    void execute(String identifier, String content);
}