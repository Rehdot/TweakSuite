🛠️ TweakSuite
=
_A real-time framework for tweaking Minecraft runtimes._

---

### 📦 Requirements

- Code editor (IntelliJ recommended)
- Fabric Loader or Fabric-compatible client
- JVM arguments (seriously, **don't skip these**):
```text
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
```

---

### ⚙️ The Process
1. You write de-obfuscated Yarn or Mojang code.
2. You run the `execute` Gradle task.
3. TweakSuite compiles, remaps, decompiles, and injects your code into the game.
4. Minecraft executes it. During runtime.

---

### 🚀 Getting Started

1. Clone this repository into your editor.
2. Either build the client-side mod, or download it from the latest release.
3. Launch Minecraft with Fabric _(Don't forget the JVM args above - it will break)_
4. In your editor, go to `redot.tweaksuite.suite.sandbox`
5. Write code, hit `execute` and watch the sorcery unfold.

---

### 🧱 Limitations
- Only compiles classes inside the sandbox directory.
- Classes with identical names in separate directories cannot compile.
- There's a short wait while Gradle does its startup dance.

---

### 🔀 Mappings
TweakSuite supports Mojang's **Official**, and Fabric's **Intermediary** & **Yarn** mappings.
To switch between them, just change one line in `suite/build.gradle`:
```groovy
// <!> CHANGE THIS VARIABLE TO SWAP MAPPINGS <!>
def mapping = MappingType.OFFICIAL
// Alternatives:
// MappingType.INTERMEDIARY
// MappingType.YARN
```
Just like any other variable. You're welcome.

---

### 🧪 Writing Code

To get your code to actually **run**, you’ll need to annotate 
a static, no-parameter method with `@Entrypoint`

<details>
<summary>Entrypoint example</summary>

```java
package redot.tweaksuite.suite.sandbox;

import redot.tweaksuite.commons.Entrypoint;

public class TestClass {
    
    @Entrypoint
    public static void run() {
        System.out.println("Hello, world from TweakSuite!");
    }
    
}
```
</details>

---

### 🖊️ Class Permanence

This is a fun feature if used correctly. In TweakSuite, classes can be
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
which makes no sense. So we don't allow it.
</details>

<details>
   <summary>Permanent class example</summary>

Permanent classes simply require to be annotated as `Permanent`. 
TweakSuite will do the rest.

```java
package redot.tweaksuite.suite.sandbox;

import redot.tweaksuite.commons.Permanent;

@Permanent
public class PermanentClass {

   public static String PERM_STRING = "abc";

}
```
</details>

---

### 🛑 The Kill Switch
This will attempt to stop TweakSuite code execution.

You have two options:
1. An in-game keybind (`K` by default)
2. The `killProcesses` Gradle task

Realistically, you’ll only need this if you get stuck in a `while (true)` loop.  
And while I *could* tell you not to write infinite loops... let’s be real; they’re fun, and useful for testing. 
So, the kill switch exists. 

<details>
   <summary>Safe infinite loop examples</summary>

Just try to write **safe** infinite loops, like the following:
```java
while (ThreadManager.permits()) {
    // this is safe
}

while (true) { // this is also safe
    ThreadManager.beg();
}

while (true) { // still safe
    ThreadManager.sleepMS(100);
}
```
</details>

<details>
<summary>How it works</summary>

1. **First, it asks nicely.**  
   If your code is merciful enough to use `ThreadManager` in the loop, the thread _should_ honor the kill request. Terms and conditions apply for concurrency.

2. **Then, it chooses violence.**  
   If permit checking isn’t implemented (or was ignored), it escalates to `Thread#stop()`. This is unsafe, but it might work.

3. **If that still doesn’t work:**  
   You’re on your own. I suggest you reflect on your decisions, and then start using `ThreadManager`.
</details>

---

### ⚠️ Disclaimer
TweakSuite is the successor to [ConcurrentExecutor](https://github.com/Rehdot/ConcurrentExecutor)... 
but now with remapping, IntelliSense, class permanence, and far more potential for disaster.

If you crash, die, get banned... that's on you. ✌️

---
Sincerely,  
**Redot ❤️**