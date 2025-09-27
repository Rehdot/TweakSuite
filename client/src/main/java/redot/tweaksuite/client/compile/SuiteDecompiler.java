package redot.tweaksuite.client.compile;

import java.util.List;

public interface SuiteDecompiler {

    List<String> decompileJar(String jarPath);

}
