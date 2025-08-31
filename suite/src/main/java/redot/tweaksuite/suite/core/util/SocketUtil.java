package redot.tweaksuite.suite.core.util;

import redot.tweaksuite.commons.Constants;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SocketUtil {

    public static void sendClassesOverSocket(List<String> classes) throws IOException {
        sendOverSocket(writer -> {
            for (String classDef : classes) {
                classDef = classDef.replaceFirst("(?m)^package\\s+[\\w.]+;\\s*", "// <3\n")
                        .replace("import redot.tweaksuite.suite.sandbox", "// import redot.tweaksuite.suite.sandbox");
                writer.write(classDef);
                writer.newLine();

                String className = Constants.extractClassName(classDef);

                if (ClassValidator.PERM_CLASS_NAMES.contains(className)) {
                    writer.write(Constants.PERM_CLASS_END_STRING);
                } else {
                    writer.write(Constants.CLASS_END_STRING);
                }

                writer.newLine();
            }
        });
    }

    public static void sendOverSocket(BufferedWriterConsumer writerConsumer) {
        try (Socket socket = new Socket(Constants.SOCKET_IP, Constants.SOCKET_PORT);
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            writerConsumer.accept(writer);
            writer.flush();
        } catch (Exception e) {
            System.err.println("Failed sending material over socket: " + e.getMessage());
        }
    }

    @FunctionalInterface
    public interface BufferedWriterConsumer {
        void accept(BufferedWriter writer) throws IOException;
    }

}