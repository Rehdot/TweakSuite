package redot.tweaksuite.server.connect;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import redot.tweaksuite.commons.Constants;
import redot.tweaksuite.server.TweakSuite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.List;

/// A default server implementation for usages anywhere locally.
/// This is completely optional, as TweakSuite
/// doesn't even necessarily need to operate as a server.
@Getter
@RequiredArgsConstructor
public class DefaultServer implements SuiteServer {

    private boolean running = true;
    private final int port = 49277;
    private final TweakSuite tweakSuite;

    @Override
    public void start() {
        this.running = true;
        new Thread(this::listen, "TweakSuiteSocketListener").start();
    }

    @Override
    public void stop() {
        this.running = false;
    }

    @Override
    public void listen() {
        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
            while (this.running) {
                try (var socket = serverSocket.accept();
                     var isr = new InputStreamReader(socket.getInputStream());
                     var reader = new BufferedReader(isr)) {
                    this.readClasses(reader);
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
    }

    @Override
    public void readClasses(BufferedReader reader) throws IOException {
        List<String> tempClasses = new LinkedList<>();
        List<String> permClasses = new LinkedList<>();
        StringBuilder currentClass = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            switch (line) {
                case Constants.CLASS_END_STRING -> {
                    if (!currentClass.isEmpty()) {
                        tempClasses.add(currentClass.toString());
                        currentClass = new StringBuilder();
                    }
                }
                case Constants.PERM_CLASS_END_STRING -> {
                    if (!currentClass.isEmpty()) {
                        permClasses.add(currentClass.toString());
                        currentClass = new StringBuilder();
                    }
                }
                case Constants.KILL_STRING -> {
                    this.tweakSuite.getThreadRegistry().killProcesses();
                    return;
                }
                default -> {
                    if (!currentClass.isEmpty()) {
                        currentClass.append("\n");
                    }
                    currentClass.append(line);
                }
            }
        }

        if (!currentClass.isEmpty()) {
            tempClasses.add(currentClass.toString());
        }
        if (!tempClasses.isEmpty()) {
            this.tweakSuite.getCompiler().compileClasses(tempClasses, permClasses);
        }
    }

}
