package redot.tweaksuite.client.util;

import redot.tweaksuite.commons.Constants;
import redot.tweaksuite.commons.model.ThreadRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

/// Utility class to handle Strings sent from suite to client
public class ConnectionUtil {

    public static void listenToSocket() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(Constants.SOCKET_PORT)) {
                while (true) {
                    try (Socket socket = serverSocket.accept();
                         BufferedReader reader = new BufferedReader(
                                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                        handleConnection(reader);
                    } catch (IOException e) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            } catch (IOException ignored) {}
        }, "TweakSuiteSocketListener").start();
    }

    private static void handleConnection(BufferedReader reader) throws IOException {
        List<String> tempClasses = new LinkedList<>();
        List<String> permClasses = new LinkedList<>();
        StringBuilder currentClass = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.equals(Constants.CLASS_END_STRING)) {
                if (!currentClass.isEmpty()) {
                    tempClasses.add(currentClass.toString());
                    currentClass = new StringBuilder();
                }
            } else if (line.equals(Constants.PERM_CLASS_END_STRING)) {
                if (!currentClass.isEmpty()) {
                    permClasses.add(currentClass.toString());
                    currentClass = new StringBuilder();
                }
            } else if (line.equals(Constants.KILL_STRING)) {
                ThreadRegistry.killProcesses();
                return;
            } else {
                if (!currentClass.isEmpty()) {
                    currentClass.append("\n");
                }
                currentClass.append(line);
            }
        }

        if (!currentClass.isEmpty()) {
            tempClasses.add(currentClass.toString());
        }
        if (!tempClasses.isEmpty()) {
            CompileUtil.compileClasses(tempClasses, permClasses);
        }
    }


}