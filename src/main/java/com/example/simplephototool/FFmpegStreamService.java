package com.example.simplephototool;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service responsible for streaming video from a camera using FFmpeg HLS output.
 * Each instance manages one FFmpeg process for one camera and serves HLS via HTTP.
 */
public class FFmpegStreamService {
    
    // Port for HTTP server to serve HLS files
    private static final int BASE_PORT = 49152;
    private static final AtomicInteger portCounter = new AtomicInteger(0);
    
    private final Camera camera;
    private final Settings settings;
    private final CameraStrategy strategy;
    private int port;
    private Path hlsDirectory;
    private HttpServer httpServer;
    
    private Process ffmpegProcess;
    private Thread monitorThread;
    private Thread errorThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Runnable onReadyCallback;
    private Runnable onErrorCallback;
    private final List<String> ffmpegOutput = new ArrayList<>();
    
    /**
     * Creates a new FFmpeg stream service for the specified camera.
     *
     * @param camera The camera to stream from
     * @param settings Application settings
     */
    public FFmpegStreamService(Camera camera, Settings settings) {
        this.camera = camera;
        this.settings = settings;
        this.strategy = CameraStrategyFactory.getStrategy();
        this.port = findAvailablePort();
    }
    
    /**
     * Finds an available port for the HTTP server.
     */
    private static int findAvailablePort() {
        int basePort = BASE_PORT + portCounter.getAndIncrement();
        for (int i = 0; i < 100; i++) {
            int testPort = basePort + i;
            try (ServerSocket socket = new ServerSocket(testPort)) {
                socket.setReuseAddress(true);
                return testPort;
            } catch (Exception e) {
                // Port in use, try next
            }
        }
        return basePort; // Fall back
    }
    
    /**
     * Gets the port number for this stream.
     *
     * @return The HTTP port number
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Gets the HLS stream URL for this camera served over HTTP.
     *
     * @return The HTTP URL to the HLS playlist
     */
    public String getStreamUrl() {
        return "http://127.0.0.1:" + port + "/stream.m3u8";
    }
    
    /**
     * Sets a callback to be invoked when the stream is ready.
     *
     * @param callback The callback
     */
    public void setOnReadyCallback(Runnable callback) {
        this.onReadyCallback = callback;
    }
    
    /**
     * Sets a callback to be invoked when an error occurs.
     *
     * @param callback The callback
     */
    public void setOnErrorCallback(Runnable callback) {
        this.onErrorCallback = callback;
    }
    
    /**
     * Gets the collected FFmpeg output for diagnostics.
     *
     * @return List of output lines
     */
    public List<String> getFFmpegOutput() {
        synchronized (ffmpegOutput) {
            return new ArrayList<>(ffmpegOutput);
        }
    }
    
    /**
     * Starts the FFmpeg streaming process and HTTP server.
     *
     * @return true if started successfully
     */
    public boolean start() {
        if (running.get()) {
            return true;
        }
        
        try {
            // Create temporary directory for HLS segments
            String sanitizedName = camera.getName().replaceAll("[^a-zA-Z0-9-_]", "_");
            hlsDirectory = Files.createTempDirectory("spt_hls_" + sanitizedName + "_");
            hlsDirectory.toFile().deleteOnExit();
            
            System.out.println("HLS directory: " + hlsDirectory);
            
            // Start HTTP server to serve HLS files
            startHttpServer();
            
            ProcessBuilder pb = strategy.buildFFmpegHttpStreamCommand(camera, settings, port, hlsDirectory.toString());
            // Don't merge streams - we want to capture stderr separately
            pb.redirectErrorStream(false);
            
            // Log the command being executed
            System.out.println("=== FFmpeg Stream Start ===");
            System.out.println("Camera: " + camera.getName());
            System.out.println("HLS Path: " + hlsDirectory);
            System.out.println("HTTP Server: http://127.0.0.1:" + port);
            System.out.println("Command: " + String.join(" ", pb.command()));
            System.out.println("===========================");
            
            ffmpegProcess = pb.start();
            running.set(true);
            
            // Thread to read stdout
            monitorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(ffmpegProcess.getInputStream()))) {
                    String line;
                    while (running.get() && (line = reader.readLine()) != null) {
                        synchronized (ffmpegOutput) {
                            ffmpegOutput.add("[stdout] " + line);
                        }
                        System.out.println("[FFmpeg-" + camera.getName() + "-stdout] " + line);
                    }
                } catch (Exception e) {
                    System.err.println("Error reading FFmpeg stdout: " + e.getMessage());
                }
            });
            monitorThread.setDaemon(true);
            monitorThread.setName("FFmpeg-stdout-" + camera.getName());
            monitorThread.start();
            
            // Thread to read stderr (where FFmpeg sends most of its output)
            errorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(ffmpegProcess.getErrorStream()))) {
                    String line;
                    boolean notifiedReady = false;
                    
                    while (running.get() && (line = reader.readLine()) != null) {
                        synchronized (ffmpegOutput) {
                            ffmpegOutput.add("[stderr] " + line);
                        }
                        System.out.println("[FFmpeg-" + camera.getName() + "-stderr] " + line);
                        
                        // Detect when FFmpeg starts writing HLS segments
                        // Look for "Opening" which indicates HLS segment file creation
                        if (!notifiedReady && 
                                ((line.contains("Opening") && line.contains(".ts")) || 
                                 line.contains("Output #0"))) {
                            notifiedReady = true;
                            if (onReadyCallback != null) {
                                // Wait for first segment to be written
                                waitForPlaylist();
                                onReadyCallback.run();
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error reading FFmpeg stderr: " + e.getMessage());
                }
                
                // Check if process ended unexpectedly
                if (running.get() && ffmpegProcess != null && !ffmpegProcess.isAlive()) {
                    int exitCode = ffmpegProcess.exitValue();
                    System.err.println("=== FFmpeg Process Ended ===");
                    System.err.println("Camera: " + camera.getName());
                    System.err.println("Exit code: " + exitCode);
                    System.err.println("Collected output lines: " + ffmpegOutput.size());
                    System.err.println("============================");
                    
                    if (exitCode != 0 && onErrorCallback != null) {
                        onErrorCallback.run();
                    }
                }
            });
            errorThread.setDaemon(true);
            errorThread.setName("FFmpeg-stderr-" + camera.getName());
            errorThread.start();
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to start FFmpeg stream for " + camera.getName() + ": " + e.getMessage());
            e.printStackTrace();
            running.set(false);
            return false;
        }
    }
    
    /**
     * Waits for the HLS playlist file to be created and have content.
     */
    private void waitForPlaylist() {
        if (hlsDirectory == null) return;
        
        Path playlist = hlsDirectory.resolve("stream.m3u8");
        int maxWait = 10000; // 10 seconds max
        int waited = 0;
        int interval = 200;
        
        while (waited < maxWait && running.get()) {
            if (Files.exists(playlist)) {
                try {
                    // Wait for at least one segment to be listed
                    String content = Files.readString(playlist);
                    if (content.contains(".ts")) {
                        System.out.println("HLS playlist ready: " + playlist);
                        return;
                    }
                } catch (Exception e) {
                    // File might still be being written
                }
            }
            try {
                Thread.sleep(interval);
                waited += interval;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        System.out.println("HLS playlist wait timeout, proceeding anyway");
    }
    
    /**
     * Starts a simple HTTP server to serve HLS files.
     */
    private void startHttpServer() throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        httpServer.createContext("/", exchange -> {
            String requestPath = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            
            if (requestPath.equals("/")) {
                requestPath = "/stream.m3u8";
            }
            
            // Remove leading slash
            String filename = requestPath.substring(1);
            Path filePath = hlsDirectory.resolve(filename);
            File file = filePath.toFile();
            
            if (file.exists() && file.isFile()) {
                // Determine content type
                String contentType = "application/octet-stream";
                if (filename.endsWith(".m3u8")) {
                    contentType = "application/vnd.apple.mpegurl";
                } else if (filename.endsWith(".ts")) {
                    contentType = "video/mp2t";
                }
                
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                
                // Handle HEAD requests without body
                if ("HEAD".equalsIgnoreCase(method)) {
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    exchange.sendResponseHeaders(200, file.length());
                    try (FileInputStream fis = new FileInputStream(file);
                         OutputStream os = exchange.getResponseBody()) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }
                }
            } else {
                String response = "File not found: " + filename;
                if ("HEAD".equalsIgnoreCase(method)) {
                    exchange.sendResponseHeaders(404, -1);
                } else {
                    exchange.sendResponseHeaders(404, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                }
            }
            exchange.close();
        });
        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.start();
        System.out.println("HTTP server started on port " + port);
    }
    
    /**
     * Stops the FFmpeg streaming process and HTTP server.
     */
    public void stop() {
        running.set(false);
        
        // Stop HTTP server
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
            System.out.println("HTTP server stopped");
        }
        
        if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
            ffmpegProcess.destroy();
            try {
                // Wait for process to terminate
                ffmpegProcess.waitFor(java.util.concurrent.TimeUnit.SECONDS.toMillis(2), 
                        java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Force kill if still alive
            if (ffmpegProcess.isAlive()) {
                ffmpegProcess.destroyForcibly();
            }
        }
        
        if (monitorThread != null && monitorThread.isAlive()) {
            monitorThread.interrupt();
            try {
                monitorThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        if (errorThread != null && errorThread.isAlive()) {
            errorThread.interrupt();
            try {
                errorThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Clean up HLS directory
        cleanupHlsDirectory();
        
        ffmpegProcess = null;
        monitorThread = null;
        errorThread = null;
        
        System.out.println("Stopped FFmpeg stream for " + camera.getName());
    }
    
    /**
     * Cleans up HLS segment files.
     */
    private void cleanupHlsDirectory() {
        if (hlsDirectory == null) return;
        
        try {
            File[] files = hlsDirectory.toFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            hlsDirectory.toFile().delete();
        } catch (Exception e) {
            System.err.println("Error cleaning up HLS directory: " + e.getMessage());
        }
        hlsDirectory = null;
    }
    
    /**
     * Checks if the stream is currently running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return running.get() && ffmpegProcess != null && ffmpegProcess.isAlive();
    }
    
    /**
     * Gets the camera associated with this stream.
     *
     * @return The camera
     */
    public Camera getCamera() {
        return camera;
    }
    
    /**
     * Resets the port counter. Call when all streams are stopped.
     */
    public static void resetPortCounter() {
        portCounter.set(0);
    }
}
