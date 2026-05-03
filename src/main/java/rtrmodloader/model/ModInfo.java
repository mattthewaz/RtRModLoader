package rtrmodloader.model;

public class ModInfo {
    private final String id;          // Unique mod ID (from RtRMod.getId())
    private final String name;        // Display name
    private final String version;
    private final String path;        // path to the JAR file
    private boolean enabled;
    private final String author;
    private final String description;


    public ModInfo(String id, String name, String version, String path, String author, String description) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.path = path;
        this.enabled = true;
        this.author = author;
        this.description = description;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getPath() { return path; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getAuthor() {
        return author;
    }
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name + " v" + version + (author != null && !author.isEmpty() ? " by " + author : "") + (enabled ? " [✓]" : " [✗]");
    }
}