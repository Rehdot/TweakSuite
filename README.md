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
3. TweakSuite compiles, remaps, decompiles, and _yeets_ your code into the game.
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
- Compiled classes are garbage collected when the JVM feels like it.
- There's a short wait while Gradle does its startup dance.
- Classes with identical names in separate directories cannot compile.

---

### 🔀 Mappings
TweakSuite supports both Mojang's Official and Fabric's Intermediary mappings.
To switch between them, just change one line in `suite/build.gradle`:
```groovy
// <!> CHANGE THIS VARIABLE TO SWAP MAPPINGS <!>
def mapping = MappingType.OFFICIAL // or MappingType.INTERMEDIARY
```
Just like any other variable. You're welcome.

---

### 🧪 Writing Code

To get your code to actually **run**, you’ll need to annotate 
a static, no-parameter method with `@Entrypoint`

#### Example:
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
---

### 🛑 The Kill Switch
This will attempt to stop TweakSuite code execution.

You have two options:
1. An in-game keybind
2. The `killProcesses` Gradle task

Realistically, you’ll only need this if you get stuck in a `while (true)` loop.  
And while I *could* tell you not to write infinite loops... let’s be real; they’re fun, and useful for testing. So, the kill switch exists.

Here’s how it works:

1. **First, it asks nicely.**  
   If your code is merciful enough to call `ThreadManager.beg();` somewhere inside that loop, the thread will honor the kill request.

2. **Then, it chooses violence.**  
   If begging isn’t implemented (or ignored), it escalates to `Thread#stop()`. This is unsafe, but it might work.

3. **If that still doesn’t work:**  
   You’re on your own. I suggest you reflect on your decisions, and then start using `ThreadManager.beg();`. It literally begs the thread registry to spare your thread's life.

---

### ⚠️ Disclaimer
TweakSuite is the successor to [ConcurrentExecutor](https://github.com/Rehdot/ConcurrentExecutor)
... but now with remapping, IntelliSense, and far more potential for disaster.

If you crash, die, get banned... that's on you. ✌️

---
Sincerely,  
**Redot ❤️**