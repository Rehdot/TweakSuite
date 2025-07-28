package redot.tweaksuite.suite.core;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ProcessKiller {

    public static void main(String[] args) {
        try {
            sendKillInstruction();
            System.out.println("Sent kill instructions to client.");
        } catch (Exception ignored) {}
    }

    public static void sendKillInstruction() throws IOException {
        try (Socket socket = new Socket("127.0.0.1", 49277);
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write("---TWEAKSUITE-KILL-SWITCH---");
            writer.flush();
        }
    }

}
