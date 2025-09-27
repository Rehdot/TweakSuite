package redot.tweaksuite.client.connect;

import lombok.RequiredArgsConstructor;
import redot.tweaksuite.client.validate.DefaultValidator;
import redot.tweaksuite.commons.Constants;
import redot.tweaksuite.commons.util.StringUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.List;
import java.util.Set;

/// A base client implementation which, by default, communicates with a BaseServer
@RequiredArgsConstructor
public class DefaultClient implements SuiteClient {

    protected final int serverPort = 49277;
    protected final String serverIP = "127.0.0.1";
    protected final DefaultValidator validator;

    @Override
    public void sendClasses(List<String> classes) {
        final String combined = this.getCombinedClasses(classes);
        this.sendOverSocket(writer -> writer.write(combined));
    }

    public String getCombinedClasses(List<String> classes) {
        StringBuilder builder = new StringBuilder();
        Set<String> permClassNames = this.validator.getPermClassNames();

        for (String classDef : classes) {
            builder.append(classDef).append("\n");

            String className = StringUtil.extractClassName(classDef);

            if (permClassNames.contains(className)) {
                builder.append(Constants.PERM_CLASS_END_STRING);
            } else {
                builder.append(Constants.CLASS_END_STRING);
            }

            builder.append("\n");
        }

        return builder.toString();
    }

    public void sendOverSocket(WriterConsumer writerConsumer) {
        try (var socket = new Socket(this.serverIP, this.serverPort);
             var os = new OutputStreamWriter(socket.getOutputStream());
             var writer = new BufferedWriter(os)) {
            writerConsumer.accept(writer);
            writer.flush();
            System.out.println("\nSent sources over socket.");
        } catch (Exception e) {
            System.err.println("\nFailed sending source over socket: " + e.getMessage());
        }
    }

    @FunctionalInterface
    public interface WriterConsumer {
        void accept(BufferedWriter writer) throws IOException;
    }

}
