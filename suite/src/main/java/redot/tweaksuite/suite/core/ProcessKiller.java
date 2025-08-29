package redot.tweaksuite.suite.core;

import redot.tweaksuite.commons.Constants;
import redot.tweaksuite.suite.core.util.SocketUtil;

public class ProcessKiller {

    public static void main(String[] args) {
        SocketUtil.sendOverSocket(writer -> {
            writer.write(Constants.KILL_STRING);
            System.out.println("Sent kill instructions to client.");
        });
    }

}
