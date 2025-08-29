package redot.tweaksuite.commons.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/// A data structure representing a singular class sent from suite to client
@Getter @Setter
@NoArgsConstructor
public class SuiteClass {

    private String className, classDef;
    private Class<?> literalClass;

}
