# 🛠️ TweakSuite
_A real-time framework for tweaking Minecraft runtimes._

---

## 📦 Requirements
- Code editor (IntelliJ recommended)
- Fabric Loader or Fabric-compatible client
- JVM arguments (seriously, **don't skip these**):
```text
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
```

---

## ⚙️ The Process
1. You write de-obfuscated Yarn or Mojang code.
2. You run the `execute` Gradle task.
3. TweakSuite compiles, remaps, decompiles, and _yeets_ your code into the game.
4. Minecraft executes it. During runtime.

---

## 🚀 Getting Started
1. Clone this repository into your editor.
2. Either build the client-side mod, or download it from the latest release.
3. Launch Minecraft with Fabric _(Don't forget the JVM args above - it will break)_
4. In your editor, go to `redot.tweaksuite.suite.sandbox`
5. Write code, hit `execute` and watch the sorcery unfold.

---

## ✨ Features
- Write & run code _while_ the game is running.
- Re-write and run the same class without renaming it every time.
- Remapping support... because nobody likes `method_30918`

---

## 🧱 Limitations
- Only compiles classes inside the sandbox directory.
- Compiled classes are garbage collected when the JVM feels like it.
- There's a short wait while Gradle does its startup dance.
- Classes with identical names in separate directories cannot compile.

---

## 🧪 Writing Code

To get your code to actually **run**, you’ll need to annotate 
a static, no-parameter method with `@Entrypoint`

### Example:
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

## 🛑 The Kill Switch

This will try to stop TweakSuite code execution. 
You have two. One is a keybind in-game,
and the other is the `killProcesses` Gradle task.  

If the kill switch does not work, best of luck to you.

---

## ⚠️ Disclaimer
TweakSuite is the successor to [ConcurrentExecutor](https://github.com/Rehdot/ConcurrentExecutor)
... but now with remapping, IntelliSense, and far more potential for disaster.

If you crash, die, get banned... that's on you.

---
Sincerely,  
**Redot ❤️**