🛠️ TweakSuite
=
_A real-time framework for tweaking Java runtimes._

---

### ❗ Important Notice

TweakSuite is now a general-purpose API. Before, it
was only an implementation for Fabric. TweakFabric is
now the Fabric implementation of TweakSuite.

---

### 💪 Capabilities

Within a running JVM, TweakSuite is capable of:

- Compiling user-written Java source code
- Running code in contexts delegated by the user
- Injecting existing methods with extra functionality

The core idea is to allow writing code in some capacity,
and get it to compile and work in whatever context you want.
This is so we can more efficiently test and debug anything
and everything inside a running Java application.

---

### 📐 Structure

<details>
<summary>Client</summary>

The client assumes the position of **writing, compiling, decompiling & sending**
user-written code to the TweakSuite server.

TweakSuite could support any client
architecture. For example, if the user wanted to send
their sources over a socket, or wanted to save
them to a file, either could be implemented.

Client dependency:
```xml
<dependency>
    <groupId>redot.tweaksuite</groupId>
    <artifactId>client</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>compile</scope>
</dependency>
```

</details>

<details>
<summary>Commons</summary>

Commons contains the code that the TweakSuite 
client and server both have access to,
i.e. all annotations, injection helpers, constants,
and ThreadManager.

</details>

<details>
<summary>Server</summary>

The server assumes the position of **compiling, injecting
& running** code, as sent from the client.

The server architecture is the most robust implementation
in all of TweakSuite. Its functionality can be overridden,
though I'd only advise those who are familiar with Java
internals do so. TweakSuite could realistically support 
any server architecture.

Server dependency:
```xml
<dependency>
    <groupId>redot.tweaksuite</groupId>
    <artifactId>server</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>compile</scope>
</dependency>
```

</details>

---

### 📦 Requirements

Any runtime which uses TweakSuite **must** use the following JVM arguments:
```text
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
```

To code with TweakSuite, you need the following repository:
```xml
<repository>
    <id>zenith-artifactory</id>
    <name>TweakSuite</name>
    <url>https://artifactory.zenithstudios.dev/artifactory/tweaksuite</url>
</repository>
```

---

### 🧪 Writing Code

To get user-written code to run, you’ll need to annotate 
a static, no-parameter method with `@Entrypoint`.

<details>
<summary>Entrypoint example</summary>

```java
package your.project.structure.sandbox;

import redot.tweaksuite.commons.annotation.Entrypoint;

public class TestClass {
    
    @Entrypoint
    public static void run() {
        System.out.println("Hello, world!");
    }
    
}
```
</details>

---

### 🖊️ Class Permanence

This is a fun feature if used correctly. With TweakSuite, classes can be
either **runtime-temporary**, or **runtime-permanent**.

Permanent classes compile **once**, and then are immutable for the rest
of runtime, meaning even if their sources are changed, they'll
still be the same.

<details>
<summary>Usages</summary>

This opens up the possibility of saving variables across
multiple temporary compilations. Static variables will then hold their values, unlike
in temporary classes. They can act as caches for data
you need to read or manipulate. They can act
as bridges between multiple runtime compilations.

- Put variables you'd like **never** to change in perm classes.
- Write anything else into temp classes.
</details>

<details>
<summary>The Rules of Referencing</summary>

Your classes should use one another as follows,
if you'd like them to compile:

- Temporary → Temporary ✅
- Permanent → Permanent ✅
- Temporary → Permanent ✅
- Permanent → Temporary ❌

**Why?**
- Perms are compiled to disk and loaded once by the base ClassLoader.
- Temps are compiled in-memory and hot-swapped through a sandboxed ClassLoader.
- If perms could reference temps, they'd be tied to classes that keep disappearing,
which makes no sense. So we can't allow it.
</details>

<details>
   <summary>Permanent class example</summary>

Permanent classes simply require to be annotated as `@Permanent`. 
TweakSuite will do the rest.

```java
package your.project.structure.sandbox;

import redot.tweaksuite.commons.annotation.Permanent;

@Permanent
public class PermanentClass {

   public static String PERM_STRING = "abc";

}
```
</details>

---

### 💉 Method Injection

This feature is for altering the behavior of methods
during runtime. It finds the method, then it changes the
bytecode of the method and re-compiles the class.

The Inject annotation automatically uses the MethodFinder 
and MethodInjector classes from commons to do this. These classes
are available to client users during runtime.

I would be hesitant to use method injection on any
sources that have been heavily modified by another agent,
as that is unlikely to work.

<details>
<summary>Injection example</summary>

Here's an example usage of the Inject annotation:
```java
package your.project.client.sandbox;

import your.project.server.model.Dog;
import redot.tweaksuite.commons.annotation.*;
import redot.tweaksuite.commons.inject.method.InjectionPoint;

public class Test {

    @Entrypoint
    public static void run() {
        Dog dog = new Dog(); // assume Dog is a class within runtime
        dog.setSize(4.0); // this would print the "Set size" message
    }

    // assume setSize is a void method taking a double parameter
    @Inject(value = Dog.class, name = "setSize", point = InjectionPoint.RETURN)
    public static void injectSetSize(@This Dog dog, double size) {
        System.out.println("Set size " + size + " for Dog: " + dog.toString());
    }

}

```

</details>

---

### ⚠️ Disclaimer
TweakSuite has become a capable API. 
With great power comes great responsibility.
Handle with care.

---
Sincerely,  
**Redot ❤️**