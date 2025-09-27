package redot.tweaksuite.server.data;

import lombok.Data;
import lombok.NoArgsConstructor;

/// A data structure representing a singular class sent from suite to client
@Data
@NoArgsConstructor
public class SuiteClass {

    private String className, classDef;
    private Class<?> literalClass;

}
