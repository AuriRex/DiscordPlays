package me.auri.other;

import me.auri.other.events.*;
import me.auri.other.events.minecraft.*;

public class MinecraftCommunicator extends Communicator {
    
    @Override
    public String getName() {
        return "Minecraft";
    }

    public boolean handleEvents(String identification, String event, String content) {

        try {
            switch (event) {
                case "PlayerChatEvent":
                    events.forEach(e -> {
                        if(e instanceof MinecraftPlayerChatEvent)
                            e.execute(identification, content);
                    });
                    break;
                case "PlayerJoinEvent":
                    events.forEach(e -> {
                        if(e instanceof MinecraftPlayerJoinEvent)
                            e.execute(identification, content);
                    });
                    break;
                case "PlayerQuitEvent":
                    events.forEach(e -> {
                        if(e instanceof MinecraftPlayerQuitEvent)
                            e.execute(identification, content);
                    });
                    break;
                case "PlayerDeathEvent":
                    events.forEach(e -> {
                        if(e instanceof MinecraftPlayerDeathEvent)
                            e.execute(identification, content);
                    });
                    break;
                case "PlayerKickEvent":
                    events.forEach(e -> {
                        if(e instanceof MinecraftPlayerKickEvent)
                            e.execute(identification, content);
                    });
                    break;
                case "PlayerCommandEvent":
                    events.forEach(e -> {
                        if(e instanceof MinecraftPlayerCommandEvent)
                            e.execute(identification, content);
                    });
                    break;
                default:
                    events.forEach(e -> {
                        if(e instanceof GenericNotImplementedEvent)
                            e.execute(identification, event + ": " + content);
                    });
            }
        } catch(IndexOutOfBoundsException ex) {
            ex.printStackTrace();
            System.out.println("IOoBException: Communicator -> this shouldn't happen.");
        } catch(Exception ex) {
            System.out.println("Caught exception: " + ex);
            events.forEach(e -> {
                if(e instanceof GenericExceptionEvent)
                    e.execute(identification, ex.toString());
            });
        }

        return true;
    }

}