package redot.tweaksuite.commons.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SuiteThread extends Thread {

    private boolean permitted = true;

    public SuiteThread(Runnable runnable, String name) {
        super(runnable, name);
    }

}
