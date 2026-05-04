## Rise to Ruins ModLoader – Release Notes

### 🚀 Overview

This release is the result of a collaboration between **mattthewaz** (author of the core bytecode‑patching agent) and myself **DDDrag0**, who built the graphical interface, quality‑of‑life improvements, and the complete MVP refactoring.  

The goal was to create a **modern, user‑friendly, and conflict‑free mod loader** for *Rise to Ruins*.

The **Rise to Ruins ModLoader** is a complete rewrite of the previous modding system done by myself. It replaces the old class‑folder approach with a modern **Java Agent** that applies bytecode patches at runtime. This means multiple mods can now modify the same class without conflicts, and mods are distributed as single `.jar` (or `.zip`) files.

The loader comes with a **graphical user interface** (GUI) that makes installing, enabling, disabling, and deleting mods effortless. It also includes a **save‑folder manager** that lets you keep vanilla and modded saves completely separate.

---

## ✨ New Features

### 🧩 Mod Management GUI

- **List of installed mods** – shows name, version, author, and enable/disable status.
- **Drag & drop installation** – just drop a `.zip` or `.jar` file onto the window. The loader will automatically recognise the mod, ask for confirmation if it already exists, and install it.
- **Mod details panel** – displays full description, author, version, and the file path.
- **Context menu** – right‑click a mod to enable, disable, or delete it.
- **Batch actions** – “Enable all” / “Disable all” buttons.
- **Log area** – shows what the loader and the game are doing.

### 💾 Save‑Folder Manager

- Click the **“Manage Saves”** button in the top bar.
- Create a **custom folder name** (e.g. `mod_profiles`, `test_saves`).
- The loader remembers your choice and stores it in `mods/save_folder.properties`.
- You can switch between previously used folders or delete old ones from the history.
- When you launch the game, saves will go into that folder, leaving the original `profiles/` untouched.

### 🧠 Built‑in Mods

Two internal mods are always loaded:

- **`game-version-label`** – displays the modloader version on the main menu.
- **`save-redirect`** – redirects all file operations from `profiles/` to your chosen save folder. Works transparently with all game classes that handle profiles, settings, save games, etc.

### 📦 Mod Distribution Format

Mods are distributed as **`.zip`** files (the loader will convert them to `.jar`) or directly as **`.jar`** files. Each mod **must** contain:

1. Compiled classes implementing `RtRMod` and `ModPatch`.
2. A service file: `META-INF/services/rtrmodloader.api.RtRMod` (one line with the full class name of your entry point).
3. Optionally, a `mod.properties` file (recommended) or `MANIFEST.MF` entries for metadata.

---

## 🎮 Using the ModLoader

### Launching the GUI

Place the `RtRModLoader-0.1.jar` file inside your Rise to Ruins game folder (the one containing `core.jar`).  
If jre 8+ is installed you can run it from explorer or run it with:

```bash
java -jar RtRModLoader-0.1.jar
```

The main window will open. From there you can install, manage, and launch mods.

### Save Folder Management

1. Click **“Manage Saves”**.
2. To create a new folder, click **“New...”** and type a name (only letters, numbers, underscores, hyphens).
3. To switch to a previously used folder, select it and click **“Select”**.
4. To remove a folder from the history (the actual folder on disk is **not** deleted), select it and click **“Delete”** – the default `profiles` folder cannot be removed.
5. The change will take effect **after you restart the game**.

### Installing a Mod

- **Drag & drop** a `.zip` or `.jar` file onto the left panel.
- If the mod already exists, the loader will ask you whether to overwrite it.
- After installation, the mod appears in the list. You can enable/disable it at any time.

### Enabling / Disabling / Deleting

- Click a mod to select it (use Ctrl or Shift for multiple selection).
- Right‑click to open the context menu, or use the “Enable all” / “Disable all” buttons.
- Deleting a mod removes its `.jar` file from the `mods` folder and cleans up its state entry.

### Launching the Game

Press **“Start game”**. The loader will:

- Check if any mods are enabled (if none, it asks for confirmation).
- Launch the game with the Java agent, passing your selected save folder as a system property.
- Show the game’s console output in the **Log** panel.

---

## 📁 File Structure (After First Launch)

```
Rise to Ruins/
├── core.jar
├── lib/jars/
├── mods/                         ← all installed mod .jar files
├── mods/mod_state.properties     ← which mods are enabled/disabled
├── mods/save_folder.properties   ← currently active save folder
├── mods/save_folders_history.properties ← list of all used folders
└── RtRModLoader-0.1.jar          ← the modloader itself
```

The chosen save folder (e.g. `mod_profiles`) will appear alongside the vanilla `profiles` folder.

---

##  👨‍💻 For Modders: How to Create a Mod

### How it works

The loader attaches to the JVM as a `-javaagent` before the game starts. It scans the mods directory for JARs, discovers mods via `ServiceLoader`, then registers a single `ClassFileTransformer` that intercepts game classes as they load and applies patches from all mods. Patching is done with [Javassist](https://www.javassist.org/), which allows inserting or replacing Java source snippets directly into game methods without needing the game's source.

### Building

Requires `Core.jar` from the game directory. The pom defaults to `../RtR` relative to the project root, or override with:

```sh
mvn package -Drtr.home=/path/to/RtR
```

The output fat JAR is `target/RtRModLoader-0.1.jar`.


### 1. Implement the API

Create a class that implements `rtrmodloader.api.RtRMod`.  
Example:

```java
public class MyMod implements RtRMod {
    @Override 
    public String getId() { return "my-mod-id"; }
  
    @Override 
    public Map<String, List<ModPatch>> getPatches() {
        Map<String, List<ModPatch>> patches = new HashMap<>();
        patches.put("rtr/SomeClass", Collections.singletonList((cc, loader) -> {
            // Javassist patch code here
        }));
        return patches;
    }
}
```
Class names use slash-separated internal form (e.g. `rtr/goal/Goal`, not `rtr.goal.Goal`).

### 2. Implement `ModPatch`

```java
public class MyPatch implements ModPatch {
    @Override
    public void apply(CtClass cc, ClassLoader loader) throws Exception {
        CtMethod method = cc.getDeclaredMethod("someMethod");
        method.insertBefore("{ System.out.println(\"hello from patch\"); }");
    }
}
```
### 3. Register the Entry Point

Create the file `META-INF/services/rtrmodloader.api.RtRMod` containing the **fully qualified class name** of your `RtRMod` implementation:

```
my.mod.MyMod
```

### 4. Accessing your mod instance from patch code

Javassist-inserted code runs under the game's classloader, which can't see mod classes directly. Use `ModRegistry` to share your mod instance across classloader boundaries:

```java
// In your mod constructor:
ModRegistry.register("my-mod", this);

// In a static helper called from patch code:
MyMod mod = (MyMod) ModRegistry.get("my-mod");
```

`ModRegistry` stores instances in `System.getProperties()`, which is visible to all classloaders.

### 5. Package Your Mod (more details below)

- **Option A (easiest):** Zip the whole structure into a `.zip` file. The loader will convert it to a `.jar` upon installation.
- **Option B:** Directly create a `.jar` file (with correct manifest and service file).

### 6. Test

Drag & drop your mod into the GUI. It should appear with the correct name, version, and author. You can then enable it and launch the game.

---
### API

| Type | Purpose |
|------|---------|
| `RtRMod` | Entry point for a mod — returns the map of class names to patches |
| `ModPatch` | A single Javassist patch targeting one class |
| `ModRegistry` | Cross-classloader singleton registry for sharing mod instances |

---

## 📝 How to Package Your Mod

### Option 1 – Recommended: `mod.properties`
Place a file named **`mod.properties`** in the root of your `.zip` or `.jar`:

```properties
# Required: unique ID (no spaces, only letters/numbers/_-)
id=my-awesome-mod

# Required: display name
name=My Awesome Mod

# Optional (default = "1.0")
version=2.1.0

# Optional (default = empty)
author=YourName

# Optional – use \n for line breaks
description=Adds new buildings and monsters.\nCompatible with game version 1.5+
```

### Option 2 – `MANIFEST.MF` (fallback)
If no `mod.properties` is found, the loader reads from `META-INF/MANIFEST.MF`:

```plaintext
Manifest-Version: 1.0
Implementation-Title: my-awesome-mod
Implementation-Version: 2.1.0
Implementation-Vendor: YourName
Description: Adds new buildings...
```

If both are missing, the loader uses the filename (without extension) as ID and name.

### Required structure for a working mod
Your mod must be a **valid JAR** (or a `.zip` that will be converted to a JAR) containing:

- Compiled classes that implement `RtRMod` and `ModPatch`.
- The service file:  
  `META-INF/services/rtrmodloader.api.RtRMod`  
  containing the **fully qualified name** of your `RtRMod` implementation (e.g. `com.example.MyMod`).

Example minimal mod:

```
my-mod.zip
├── mod.properties
├── com/
│   └── example/
│       ├── MyMod.class
│       └── MyPatch.class
└── META-INF/
    └── services/
        └── rtrmodloader.api.RtRMod   (contains: com.example.MyMod)
```

---

## ❓ Troubleshooting

- **Game doesn’t launch** – Make sure `core.jar` is present and that you are using Java 8 or newer.
- **Mod doesn’t appear** – Check that your mod contains the service file and that the class name in it is correct.
- **Mod appears but has no description** – Add a `mod.properties` file with the `description` field.
- **Save redirection not working** – Verify that you have restarted the game after changing the save folder, and that the `-Drtr.save.folder` property is being passed (check the log).

---

## 🙏 Credits

- **Bytecode patching engine & agent** – [mattthewaz](https://github.com/mattthewaz) (original `ModLoaderAgent`, `ModDispatchTransformer`, Javassist integration).
- **GUI, MVP refactoring, save‑folder manager, metadata system, QoL improvements** – [DDDrag0](https://github.com/DDDrag0).

---

## 📄 License
This project is open source. Please refer to the included [LICENSE](LICENSE) file.