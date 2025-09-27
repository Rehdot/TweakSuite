package redot.tweaksuite.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import redot.tweaksuite.client.compile.SuiteDecompiler;
import redot.tweaksuite.client.connect.SuiteClient;
import redot.tweaksuite.client.validate.ClassValidator;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class TweakSuite {

    private final SuiteClient client;
    private final ClassValidator validator;
    private final SuiteDecompiler decompiler;

    public void decompileAndSend(String jarPath) {
        this.validator.validateJar(jarPath);
        List<String> classes = this.decompiler.decompileJar(jarPath);
        this.client.sendClasses(classes);
    }

}
