package rtrmodloader.core;

public class ModLogger {
    private static LogCallback callback = null;
    private static boolean consoleOutput = true;

    public interface LogCallback {
        void onLog(String message);
    }

    public static void setCallback(LogCallback cb) {
        callback = cb;
    }

    public static void setConsoleOutput(boolean enabled) {
        consoleOutput = enabled;
    }

    public static void info(String msg) {
        if (callback != null) callback.onLog(msg);
        if (consoleOutput) System.out.println(msg);
    }

    public static void error(String msg) {
        String errMsg = "❌ " + msg;
        if (callback != null) callback.onLog(errMsg);
        if (consoleOutput) System.err.println(errMsg);
    }

    public static void error(String msg, Throwable t) {
        String errMsg = "❌ " + msg;
        if (callback != null) callback.onLog(errMsg);
        if (consoleOutput) {
            System.err.println(errMsg);
            t.printStackTrace(System.err);
        }
    }
    public static void warn(String msg) {
        String warnMsg = "⚠️ " + msg;
        if (callback != null) callback.onLog(warnMsg);
        if (consoleOutput) System.out.println(warnMsg);
    }

    public static void debug(String msg) {
        if (callback != null) callback.onLog("[DEBUG] " + msg);
        if (consoleOutput) System.out.println("[DEBUG] " + msg);
    }
}