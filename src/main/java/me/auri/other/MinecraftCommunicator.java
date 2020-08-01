package me.auri.other;

import me.auri.other.events.*;

public class MinecraftCommunicator extends Communicator {
    
    @Override
    public String getName() {
        return "Minecraft";
    }

    public void handleEvents(String event, String content) {

        try {
            switch (event) {
                default:
                    events.forEach(e -> {
                        if(e instanceof GenericNotImplementedEvent)
                            e.execute(event + ": " + content);
                    });
            }
        } catch(IndexOutOfBoundsException ex) {
            ex.printStackTrace();
            System.out.println("IOoBException: Communicator -> this shouldn't happen.");
        } catch(Exception ex) {
            System.out.println("Caught exception: " + ex);
            events.forEach(e -> {
                if(e instanceof GenericExceptionEvent)
                    e.execute(ex.toString());
            });
        }

    }

}