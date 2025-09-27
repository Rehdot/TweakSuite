package redot.tweaksuite.server.connect;

import redot.tweaksuite.server.TweakSuite;

import java.io.BufferedReader;
import java.io.IOException;

/// An outline for how TweakSuite *could* be used with server architecture
public interface SuiteServer {

    void start();

    void stop();

    void listen();

    void readClasses(BufferedReader reader) throws IOException;

}
