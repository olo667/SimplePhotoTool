package com.example.simplephototool;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A tile component that displays a camera preview with click-to-toggle functionality.
 * Each tile manages its own preview thread for the associated camera.
 */
public class PreviewTile extends VBox {
    
    private static final double TILE_WIDTH = 320;
    private static final double TILE_HEIGHT = 240;
    private static final int THUMBNAIL_FPS = 15;
    
    private final Camera camera;
    private final ImageView imageView;
    private final Label nameLabel;
    private final Label statusLabel;
    private final StackPane previewContainer;
    private final Rectangle overlay;
    
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    
    private Thread previewThread;
    private final AtomicBoolean shouldRun = new AtomicBoolean(false);
    private FrameGrabber grabber;
    private final CameraStrategy strategy;
    private Settings settings;
    
    /**
     * Creates a new preview tile for the specified camera.
     *
     * @param camera The camera to preview
     * @param settings Application settings for resolution
     */
    public PreviewTile(Camera camera, Settings settings) {
        this.camera = camera;
        this.settings = settings;
        this.strategy = CameraStrategyFactory.getStrategy();
        
        // Set up the container
        setAlignment(Pos.CENTER);
        setSpacing(5);
        setPadding(new Insets(5));
        getStyleClass().add("preview-tile");
        
        // Create preview container with overlay
        previewContainer = new StackPane();
        previewContainer.setPrefSize(TILE_WIDTH, TILE_HEIGHT);
        previewContainer.setMaxSize(TILE_WIDTH, TILE_HEIGHT);
        previewContainer.setMinSize(TILE_WIDTH, TILE_HEIGHT);
        previewContainer.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #444; -fx-border-width: 2;");
        
        // Image view for preview frames
        imageView = new ImageView();
        imageView.setFitWidth(TILE_WIDTH - 4);
        imageView.setFitHeight(TILE_HEIGHT - 4);
        imageView.setPreserveRatio(true);
        
        // Semi-transparent overlay for stopped state
        overlay = new Rectangle(TILE_WIDTH - 4, TILE_HEIGHT - 4);
        overlay.setFill(Color.rgb(0, 0, 0, 0.6));
        overlay.setVisible(true);
        
        // Status label (shows Play icon or Running)
        statusLabel = new Label("▶ Click to Start");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        previewContainer.getChildren().addAll(imageView, overlay, statusLabel);
        StackPane.setAlignment(statusLabel, Pos.CENTER);
        
        // Camera name label
        nameLabel = new Label(camera.getName());
        nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        nameLabel.setMaxWidth(TILE_WIDTH);
        nameLabel.setAlignment(Pos.CENTER);
        
        getChildren().addAll(previewContainer, nameLabel);
        
        // Click handler to toggle preview
        previewContainer.setOnMouseClicked(event -> togglePreview());
        
        // Update visual state when running changes
        running.addListener((obs, wasRunning, isRunning) -> updateVisualState());
        selected.addListener((obs, wasSelected, isSelected) -> updateBorder());
        
        // Set cursor to indicate clickable
        previewContainer.setStyle(previewContainer.getStyle() + "-fx-cursor: hand;");
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
     * Starts the camera preview.
     */
    public void startPreview() {
        if (running.get()) {
            return;
        }
        
        shouldRun.set(true);
        running.set(true);
        
        previewThread = new Thread(() -> {
            Java2DFrameConverter converter = null;
            long frameInterval = 1000 / THUMBNAIL_FPS;
            
            try {
                // Get resolution settings
                String resolution = camera.getEffectiveResolution(settings.getDefaultResolution());
                int[] dimensions = Settings.parseResolution(resolution);
                
                // Use lower resolution for thumbnails
                int width = 640;
                int height = 480;
                if (dimensions != null) {
                    width = Math.min(dimensions[0], 640);
                    height = Math.min(dimensions[1], 480);
                }
                
                grabber = strategy.createGrabber(camera.getDeviceId(), width, height);
                grabber.start();
                
                converter = new Java2DFrameConverter();
                long lastFrameTime = 0;
                
                while (shouldRun.get()) {
                    long currentTime = System.currentTimeMillis();
                    
                    // Frame rate limiting
                    if (currentTime - lastFrameTime < frameInterval) {
                        Thread.sleep(5);
                        continue;
                    }
                    
                    Frame frame = grabber.grab();
                    if (frame == null || frame.image == null) {
                        Thread.sleep(10);
                        continue;
                    }
                    
                    BufferedImage buffered = converter.getBufferedImage(frame);
                    if (buffered != null) {
                        Image fxImage = SwingFXUtils.toFXImage(buffered, null);
                        Platform.runLater(() -> {
                            if (shouldRun.get()) {
                                imageView.setImage(fxImage);
                            }
                        });
                    }
                    
                    lastFrameTime = currentTime;
                }
            } catch (Exception e) {
                System.err.println("Error in preview tile for " + camera.getName() + ": " + e.getMessage());
                Platform.runLater(() -> {
                    statusLabel.setText("⚠ Error");
                    statusLabel.setVisible(true);
                });
            } finally {
                if (converter != null) {
                    try {
                        converter.close();
                    } catch (Exception ignored) {}
                }
                if (grabber != null) {
                    try {
                        grabber.stop();
                        grabber.close();
                    } catch (Exception ignored) {}
                }
                grabber = null;
            }
        });
        
        previewThread.setDaemon(true);
        previewThread.setName("PreviewTile-" + camera.getName());
        previewThread.start();
    }
    
    /**
     * Stops the camera preview.
     */
    public void stopPreview() {
        shouldRun.set(false);
        
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
     * Updates the settings reference.
     *
     * @param settings New settings
     */
    public void setSettings(Settings settings) {
        this.settings = settings;
    }
    
    // Property accessors
    
    public Camera getCamera() {
        return camera;
    }
    
    public boolean isRunning() {
        return running.get();
    }
    
    public BooleanProperty runningProperty() {
        return running;
    }
    
    public boolean isSelected() {
        return selected.get();
    }
    
    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }
    
    public BooleanProperty selectedProperty() {
        return selected;
    }
    
    /**
     * Gets the current preview image.
     *
     * @return The current image or null if not running
     */
    public Image getCurrentImage() {
        return imageView.getImage();
    }
    
    /**
     * Cleans up resources when the tile is removed.
     */
    public void dispose() {
        stopPreview();
    }
}
