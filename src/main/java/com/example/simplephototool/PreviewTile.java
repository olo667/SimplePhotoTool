package com.example.simplephototool;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A tile component that displays a camera preview with click-to-toggle functionality.
 * Each tile manages its own preview thread using FFmpeg for the associated camera.
 */
public class PreviewTile extends VBox {
    
    private static final int THUMBNAIL_FPS = 15;
    
    private final Camera camera;
    private Settings settings;
    private final ImageView imageView;
    private final Label nameLabel;
    private final Label statusLabel;
    private final StackPane previewContainer;
    private final Rectangle overlay;
    
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    
    private Thread previewThread;
    private final AtomicBoolean shouldRun = new AtomicBoolean(false);
    private Process ffmpegProcess;
    private final CameraStrategy strategy;
    
    private double tileWidth;
    private double tileHeight;

    /**
     * Creates a new preview tile for the specified camera with default size.
     *
     * @param camera The camera to preview
     * @param settings Application settings
     */
    public PreviewTile(Camera camera, Settings settings) {
        this(camera, settings, TileSize.MEDIUM);
    }

    /**
     * Creates a new preview tile for the specified camera.
     *
     * @param camera The camera to preview
     * @param settings Application settings
     * @param size The tile size
     */
    public PreviewTile(Camera camera, Settings settings, TileSize size) {
        this.camera = camera;
        this.settings = settings;
        this.strategy = CameraStrategyFactory.getStrategy();
        this.tileWidth = size.getWidth();
        this.tileHeight = size.getHeight();
        
        setSpacing(5);
        setAlignment(Pos.CENTER);
        
        // Preview container
        previewContainer = new StackPane();
        previewContainer.setPrefSize(tileWidth, tileHeight);
        previewContainer.setMaxSize(tileWidth, tileHeight);
        previewContainer.setMinSize(tileWidth, tileHeight);
        previewContainer.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #444; -fx-border-width: 2;");
        
        // ImageView for video frames
        imageView = new ImageView();
        imageView.setFitWidth(tileWidth - 4);
        imageView.setFitHeight(tileHeight - 4);
        imageView.setPreserveRatio(true);
        
        // Semi-transparent overlay for stopped state
        overlay = new Rectangle(tileWidth - 4, tileHeight - 4);
        overlay.setFill(Color.rgb(0, 0, 0, 0.6));
        overlay.setVisible(true);
        
        // Status label
        statusLabel = new Label("▶ Click to Start");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(tileWidth - 20);
        statusLabel.setAlignment(Pos.CENTER);
        
        previewContainer.getChildren().addAll(imageView, overlay, statusLabel);
        StackPane.setAlignment(statusLabel, Pos.CENTER);
        
        // Camera name label
        nameLabel = new Label(camera.getName());
        nameLabel.setStyle(
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: white; " +
            "-fx-background-color: rgba(0, 0, 0, 0.7); " +
            "-fx-padding: 5 10 5 10; " +
            "-fx-background-radius: 3;"
        );
        nameLabel.setMaxWidth(tileWidth);
        nameLabel.setAlignment(Pos.CENTER);
        
        getChildren().addAll(previewContainer, nameLabel);
        
        // Click handler to toggle preview
        previewContainer.setOnMouseClicked(event -> togglePreview());
        previewContainer.setStyle(previewContainer.getStyle() + "-fx-cursor: hand;");
        
        // Update visuals when running state changes
        running.addListener((obs, wasRunning, isRunning) -> updateVisualState());
        selected.addListener((obs, wasSelected, isSelected) -> updateBorder());
    }

    /**
     * Toggles the preview on/off.
     */
    public void togglePreview() {
        if (running.get()) {
            stopPreview();
        } else {
            startPreview();
        }
    }

    /**
     * Starts the camera preview using FFmpeg.
     */
    public void startPreview() {
        if (running.get()) {
            return;
        }
        
        shouldRun.set(true);
        running.set(true);
        
        previewThread = new Thread(() -> {
            long frameInterval = 1000 / THUMBNAIL_FPS;
            
            try {
                ProcessBuilder pb = strategy.buildFFmpegCommand(camera, settings);
                pb.redirectErrorStream(false);
                ffmpegProcess = pb.start();
                
                InputStream inputStream = new BufferedInputStream(ffmpegProcess.getInputStream());
                
                // Get resolution
                int[] dimensions = strategy.getResolution(camera, settings);
                int width = (dimensions != null) ? dimensions[0] : 640;
                int height = (dimensions != null) ? dimensions[1] : 480;
                int bytesPerPixel = strategy.getBytesPerPixel();
                int frameSize = width * height * bytesPerPixel;
                
                byte[] frameBuffer = new byte[frameSize];
                WritableImage writableImage = new WritableImage(width, height);
                PixelFormat<java.nio.ByteBuffer> pixelFormat = PixelFormat.getByteRgbInstance();
                
                long lastFrameTime = 0;
                
                while (shouldRun.get()) {
                    long currentTime = System.currentTimeMillis();
                    
                    // Read frame
                    int bytesRead = readFully(inputStream, frameBuffer);
                    if (bytesRead < frameSize) {
                        break; // End of stream
                    }

                    // Frame rate limiting
                    if (currentTime - lastFrameTime < frameInterval) {
                        continue;
                    }
                    
                    // Convert byte array to JavaFX image and update UI
                    byte[] frameCopy = frameBuffer.clone();
                    Platform.runLater(() -> {
                        if (shouldRun.get()) {
                            try {
                                writableImage.getPixelWriter().setPixels(
                                    0, 0, width, height,
                                    pixelFormat,
                                    frameCopy, 0, width * bytesPerPixel
                                );
                                imageView.setImage(writableImage);
                            } catch (Exception ignored) {}
                        }
                    });

                    lastFrameTime = currentTime;
                }
            } catch (Exception e) {
                System.err.println("Error in preview tile for " + camera.getName() + ": " + e.getMessage());
                Platform.runLater(() -> {
                    statusLabel.setText("⚠ Error");
                    statusLabel.setVisible(true);
                });
            } finally {
                if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
                    ffmpegProcess.destroy();
                }
                ffmpegProcess = null;
            }
        });
        
        previewThread.setDaemon(true);
        previewThread.setName("PreviewTile-" + camera.getName());
        previewThread.start();
    }
    
    /**
     * Reads exactly the requested number of bytes from the input stream.
     */
    private int readFully(InputStream in, byte[] buffer) throws java.io.IOException {
        int totalRead = 0;
        while (totalRead < buffer.length) {
            int bytesRead = in.read(buffer, totalRead, buffer.length - totalRead);
            if (bytesRead == -1) {
                return totalRead;
            }
            totalRead += bytesRead;
        }
        return totalRead;
    }

    /**
     * Stops the camera preview.
     */
    public void stopPreview() {
        shouldRun.set(false);
        
        // Terminate FFmpeg process
        if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
            ffmpegProcess.destroy();
        }

        if (previewThread != null) {
            try {
                previewThread.join(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            previewThread = null;
        }
        
        running.set(false);
        imageView.setImage(null);
    }
    
    /**
     * Disposes of all resources used by this tile.
     */
    public void dispose() {
        stopPreview();
    }

    /**
     * Updates the visual state based on running status.
     */
    private void updateVisualState() {
        Platform.runLater(() -> {
            if (running.get()) {
                overlay.setVisible(false);
                statusLabel.setVisible(false);
            } else {
                overlay.setVisible(true);
                statusLabel.setText("▶ Click to Start");
                statusLabel.setVisible(true);
            }
        });
    }
    
    /**
     * Updates the border based on selected state.
     */
    private void updateBorder() {
        Platform.runLater(() -> {
            if (selected.get()) {
                previewContainer.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #4a9eff; -fx-border-width: 3; -fx-cursor: hand;");
            } else {
                previewContainer.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #444; -fx-border-width: 2; -fx-cursor: hand;");
            }
        });
    }
    
    /**
     * Updates the tile size dynamically.
     *
     * @param size New tile size
     */
    public void setTileSize(TileSize size) {
        this.tileWidth = size.getWidth();
        this.tileHeight = size.getHeight();

        Platform.runLater(() -> {
            previewContainer.setPrefSize(tileWidth, tileHeight);
            previewContainer.setMaxSize(tileWidth, tileHeight);
            previewContainer.setMinSize(tileWidth, tileHeight);

            imageView.setFitWidth(tileWidth - 4);
            imageView.setFitHeight(tileHeight - 4);

            overlay.setWidth(tileWidth - 4);
            overlay.setHeight(tileHeight - 4);

            statusLabel.setMaxWidth(tileWidth - 20);
            nameLabel.setMaxWidth(tileWidth);
        });
    }
    
    /**
     * Updates the settings reference.
     *
     * @param settings New settings
     */
    public void setSettings(Settings settings) {
        this.settings = settings;
    }
    
    public Camera getCamera() {
        return camera;
    }
    
    public BooleanProperty runningProperty() {
        return running;
    }
    
    public boolean isRunning() {
        return running.get();
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public boolean isSelected() {
        return selected.get();
    }
    
    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }
}
