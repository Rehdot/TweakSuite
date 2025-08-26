package redot.tweaksuite.commons;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/// A data structure representing a singular class sent from suite to client
@Getter @Setter
@NoArgsConstructor
public class SuiteClass {

    private String className, classDef;
    private Class<?> literalClass;
    private boolean permanent = false, newPermanent = false, oldPermanent = false;

    public SuiteClass(String className, String classDef, Class<?> literalClass) {
        this.className = className;
        this.classDef = classDef;
        this.literalClass = literalClass;
    }

    public boolean hasLiteral() {
        return this.literalClass != null;
    }

}
