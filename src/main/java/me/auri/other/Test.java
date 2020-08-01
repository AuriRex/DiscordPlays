package me.auri.other;

public class Test {

    public static void main(String[] args) {

        ServerListenerThread slt = new ServerListenerThread();
        slt.start();

        // Add other Communicators here
        new MinecraftCommunicator().init();

        ServerListenerThread.setReady();

        // int c = 5;

        while (true) {
            try {
                // System.out.println(c);
                Thread.sleep(1000);
                // c--;
                // if(c <= 0) {
                //     System.out.println("Goodbye");
                //     slt.stopNow();
                //     return;
                // }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

}