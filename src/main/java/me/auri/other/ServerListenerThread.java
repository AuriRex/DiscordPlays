package me.auri.other;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import me.auri.other.enc.EncModeAES;

public class ServerListenerThread extends Thread {

    private ServerSocket serverSocket;

    private boolean running = true;

    private static boolean ready = false;

    private int listenPort = 11001;

    private Communicator comsInstance = null;

    private ArrayList<ClientConnectionThread> clients = new ArrayList<>();

    // private static boolean isBusy = false;

    private EncModeAES encAES = new EncModeAES();

    private HashMap<String, String[]> encryptionKeys = new HashMap<>();

    public void setEncKeyData(String identifier, String[] encKeyData) {
        encryptionKeys.put(identifier, encKeyData);
    }

    public String[] getEncKeyData(String identifier) {
        return encryptionKeys.get(identifier);
    }

    public void run() {

        if (comsInstance == null) {
            comsInstance = new Communicator();
            comsInstance.init();
        }

        try {
            serverSocket = new ServerSocket(listenPort);

            while (!ready) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            while(running) {
                Socket soc = serverSocket.accept();

                // while (isBusy) {
                //     try {
                //         Thread.sleep(1);
                //     } catch (InterruptedException e) {
                //         e.printStackTrace();
                //     }
                // }
                // isBusy = true;

                // Check other sockets connection status
                Iterator<ClientConnectionThread> ci = clients.iterator();
                while(ci.hasNext()) {
                    ClientConnectionThread c = ci.next();
                    if(!c.isConnected()) {
                        System.out.println("[SLT] Dropping connection with \""+c.getCom().getName() + ":" + c.getIdentifier() + ":" + c.isRequest() +"\"!");
                        c.close();
                        Communicator.freeSlot(c);
                        ci.remove();
                    }
                }
                try {
                    ClientConnectionThread cl = new ClientConnectionThread(soc, encAES, this);
                    clients.add(cl);
                    cl.start();
                }catch(Exception ex) {
                    // isBusy = false;
                }
                
            }

        } catch (IOException e) {
            if(! (e instanceof SocketException)) {
                e.printStackTrace();
            } else {
                System.out.println("[SLT] Closing...");
            }
            
        }

    }

    public static void setReady() {
        ready = true;
    }

    // public static void setBusy(boolean b) {
    //     isBusy = b;
    // }

    public void stopNow() {
        running = false;

        this.interrupt();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        clients.forEach(c -> {
            c.stopNow();
        });

        clients.clear();
        //serverThread.stopNow();
    }

}