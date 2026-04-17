# RtRModLoader

A mod loader for Rise to Ruins. Mods are dropped into a folder and loaded automatically when the game starts — no game files are modified.

## Installation

### 1. Download

Download the latest `RtRModLoader-0.1.jar` from the releases page and place it in your **Rise to Ruins game folder** (the same folder as `Core.jar`).

To find the game folder in Steam: right-click Rise to Ruins → **Manage** → **Browse local files**.

### 2. Create a mods folder

Create a `mods/` folder inside the game folder and place your mod JARs inside it:

```
Rise to Ruins/
    Core.jar
    RtRModLoader-0.1.jar
    mods/
        some-mod.jar
        some-other-mod.jar
```

### 3. Set Steam launch options

Right-click Rise to Ruins in Steam → **Properties** → **Launch Options**, and paste the following:

**Linux/Mac:**
```
JAVA_TOOL_OPTIONS="-javaagent:RtRModLoader-0.1.jar -Drtr.mods.dir=mods" %command%
```

**Windows:**
```
JAVA_TOOL_OPTIONS="-javaagent:RtRModLoader-0.1.jar -Drtr.mods.dir=mods" %command%
```

Steam launches the game from the game folder, so the paths are the same on all platforms — no absolute paths needed. `JAVA_TOOL_OPTIONS` is automatically read by the JVM before the game starts, so no game files need to be touched. The `%command%` at the end is required by Steam.

### 4. Launch the game

Start the game through Steam as normal. If this worked correctly you should see the RtRModLoader text in the lower left of the main menu, above the game's own version.

### Installing mods

Drop mod JARs into the `mods/` folder and restart the game. That's it.

### Disabling a mod without removing it

Create a file called `disabled.txt` in the `mods/` folder. Add one mod ID per line — the loader will skip those mods on startup. Lines starting with `#` are treated as comments.

```
# disabled.txt
archipelago
```

The mod ID is printed in the log when a mod loads: `[RtRModLoader] Loaded mod: archipelago`.

---

## Writing a mod

This section is for mod developers.

### How it works

The loader attaches to the JVM as a `-javaagent` before the game starts. It scans the mods directory for JARs, discovers mods via `ServiceLoader`, then registers a single `ClassFileTransformer` that intercepts game classes as they load and applies patches from all mods. Patching is done with [Javassist](https://www.javassist.org/), which allows inserting or replacing Java source snippets directly into game methods without needing the game's source.

### Building

Requires `Core.jar` from the game directory. The pom defaults to `../RtR` relative to the project root, or override with:

```sh
mvn package -Drtr.home=/path/to/RtR
```

The output fat JAR is `target/RtRModLoader-0.1.jar`.

### Implementing a mod

**1. Implement `RtRMod`**

```java
public class MyMod implements RtRMod {
    @Override
    public String getId() { return "my-mod"; }

    @Override
    public Map<String, List<ModPatch>> getPatches() {
        Map<String, List<ModPatch>> patches = new HashMap<>();
        patches.put("rtr/some/GameClass", Collections.singletonList(new MyPatch()));
        return patches;
    }
}
```

**2. Implement `ModPatch`**

```java
public class MyPatch implements ModPatch {
    @Override
    public void apply(CtClass cc, ClassLoader loader) throws Exception {
        CtMethod method = cc.getDeclaredMethod("someMethod");
        method.insertBefore("{ System.out.println(\"hello from patch\"); }");
    }
}
```

Class names use slash-separated internal form (e.g. `rtr/goal/Goal`, not `rtr.goal.Goal`).

**3. Register via ServiceLoader**

Create `src/main/resources/META-INF/services/rtrmodloader.api.RtRMod` containing your fully-qualified class name:

```
com.example.mymod.MyMod
```

**4. Accessing your mod instance from patch code**

Javassist-inserted code runs under the game's classloader, which can't see mod classes directly. Use `ModRegistry` to share your mod instance across classloader boundaries:

```java
// In your mod constructor:
ModRegistry.register("my-mod", this);

// In a static helper called from patch code:
MyMod mod = (MyMod) ModRegistry.get("my-mod");
```

`ModRegistry` stores instances in `System.getProperties()`, which is visible to all classloaders.

### API

| Type | Purpose |
|------|---------|
| `RtRMod` | Entry point for a mod — returns the map of class names to patches |
| `ModPatch` | A single Javassist patch targeting one class |
| `ModRegistry` | Cross-classloader singleton registry for sharing mod instances |
