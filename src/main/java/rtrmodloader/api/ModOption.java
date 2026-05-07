package rtrmodloader.api;

/**
 * A single user-facing configurable option exposed by a mod.
 * Create instances via the static factory methods.
 */
public final class ModOption {

    public enum Type { BOOLEAN, INTEGER, STRING }

    private final String id;
    private final String label;
    private final Type type;
    private Object value;

    private ModOption(String id, String label, Type type, Object defaultValue) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.value = defaultValue;
    }

    public static ModOption boolOption(String id, String label, boolean defaultValue) {
        return new ModOption(id, label, Type.BOOLEAN, defaultValue);
    }

    public static ModOption intOption(String id, String label, int defaultValue) {
        return new ModOption(id, label, Type.INTEGER, defaultValue);
    }

    public static ModOption stringOption(String id, String label, String defaultValue) {
        return new ModOption(id, label, Type.STRING, defaultValue);
    }

    public String getId()    { return id; }
    public String getLabel() { return label; }
    public Type   getType()  { return type; }

    public boolean getBoolValue()   { require(Type.BOOLEAN); return (Boolean) value; }
    public int     getIntValue()    { require(Type.INTEGER); return (Integer) value; }
    public String  getStringValue() { require(Type.STRING);  return (String)  value; }

    public void setBoolValue(boolean v)  { require(Type.BOOLEAN); this.value = v; }
    public void setIntValue(int v)       { require(Type.INTEGER); this.value = v; }
    public void setStringValue(String v) { require(Type.STRING);  this.value = v; }

    private void require(Type expected) {
        if (type != expected)
            throw new IllegalStateException(
                "Option '" + id + "' is " + type + ", not " + expected);
    }

    /** Serialise to a properties-file string. */
    public String toPropertyString() {
        return value.toString();
    }

    /** Deserialise from a properties-file string; no-op on parse failure. */
    public void fromPropertyString(String s) {
        try {
            switch (type) {
                case BOOLEAN: value = Boolean.parseBoolean(s); break;
                case INTEGER: value = Integer.parseInt(s);      break;
                case STRING:  value = s;                        break;
            }
        } catch (NumberFormatException ignored) {}
    }
}
