package rtrmodloader.core;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GameLauncher {

    /**
     * Launches the game with the Java Agent that manages mods.
     * It is no longer necessary to pass the list of mods; the agent reads
     * the state from mods/mod_state.properties.
     *
     * @param agentJarPath path to the modloader JAR (containing ModLoaderAgent)
     * @param callback     callback to notify events
     * @return Game process
     */
    public static Process launch(String agentJarPath, GameLauncherCallback callback) throws IOException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        File currentDir = new File(".").getCanonicalFile();

        // Classpath: only core.jar + libraries (mod JARs will be added by the agent)
        List<String> classpathEntries = new ArrayList<>();
        File coreJar = new File(currentDir, "core.jar");
        if (!coreJar.exists()) {
            throw new FileNotFoundException("core.jar not found in " + currentDir);
        }
        classpathEntries.add(coreJar.getAbsolutePath());

        File libFolder = new File(currentDir, "lib/jars");
        if (libFolder.exists()) {
            File[] jars = libFolder.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jars != null) {
                for (File jar : jars) {
                    classpathEntries.add(jar.getAbsolutePath());
                }
            }
        }
        String classpath = String.join(File.pathSeparator, classpathEntries);

        // VM options: natives, agent, and directory mods (optional)
        String nativePath = getNativePath(currentDir);
        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-Djava.library.path=" + nativePath);
        command.add("-javaagent:" + agentJarPath);
        // Allows the GUI to display the mods folder (optional)
        command.add("-Drtr.mods.dir=" + new File(currentDir, "mods").getAbsolutePath());
        command.add("-cp");
        command.add(classpath);
        command.add("rtr.system.Launcher");

        String commandStr = String.join(" ", command);
        callback.onGameOutput("[DEBUG] Command: " + commandStr);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(currentDir);
        pb.redirectErrorStream(false);
        Process process = pb.start();

        Thread outReader = createOutputReaderThread(callback, process);
        outReader.start();

        Thread errReader = new Thread(() -> {
            try (BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = err.readLine()) != null) {
                    callback.onGameOutput("[ERR] " + line);
                }
            } catch (IOException e) {}
        });
        errReader.setDaemon(true);
        errReader.start();
        callback.onGameOutput("[DEBUG] Process started, PID=" + process);

        // Thread to read stdout (already exists, but let's add a log)
        Thread outputReader = createOutputReaderThread(callback, process);
        outputReader.start();

        // Separate thread to check if the process crashes immediately
        Thread monitorThread = new Thread(() -> {
            try {
                Thread.sleep(1000);
                if (!process.isAlive()) {
                    callback.onGameOutput("[DEBUG] Process died within 1 second, exit code: " + process.exitValue());
                } else {
                    callback.onGameOutput("[DEBUG] Process still alive after 1 second");
                }
            } catch (InterruptedException ignored) {}
        });
        monitorThread.start();

        Thread waitForThread = new Thread(() -> {
            try {
                int exitCode = process.waitFor();
                callback.onGameFinished(exitCode);
            } catch (InterruptedException e) {
                callback.onGameError(e);
            }
        });
        waitForThread.start();

        return process;
    }

    private static Thread createOutputReaderThread(GameLauncherCallback callback, Process process) {
        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    callback.onGameOutput(line);
                }
            } catch (IOException e) {
                if (process.isAlive()) callback.onGameError(e);
            }
        });
        outputReader.setDaemon(true);
        return outputReader;
    }

    private static String getNativePath(File currentDir) {
        File natives = new File(currentDir, "natives");
        if (!natives.exists()) {
            natives = new File(currentDir, "lib/natives");
        }
        return natives.exists() ? natives.getAbsolutePath() : "";
    }

    public interface GameLauncherCallback {
        void onGameStarting();
        void onGameOutput(String line);
        void onGameFinished(int exitCode);
        void onGameError(Exception e);
    }
}