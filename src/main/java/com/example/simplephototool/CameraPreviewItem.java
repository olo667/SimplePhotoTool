package com.example.simplephototool;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * A preview component that displays a camera stream using JavaFX Media.
 * Receives video from an HTTP stream produced by FFmpeg and displays it in a MediaView.
 */
public class CameraPreviewItem extends VBox {
    
    private static final double DEFAULT_WIDTH = 320;
    private static final double DEFAULT_HEIGHT = 240;
    
    private final Camera camera;
    private final Settings settings;
    
    private final MediaView mediaView;
    private final Label nameLabel;
    private final Label statusLabel;
    private final StackPane previewContainer;
    private final Rectangle overlay;
    
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    
    private FFmpegStreamService streamService;
    private MediaPlayer mediaPlayer;
    private Media media;
    
    private double tileWidth = DEFAULT_WIDTH;
    private double tileHeight = DEFAULT_HEIGHT;
    
    /**
     * Creates a new camera preview item for the specified camera.
     *
     * @param camera The camera to preview
     * @param settings Application settings
     */
    public CameraPreviewItem(Camera camera, Settings settings) {
        this(camera, settings, TileSize.MEDIUM);
    }
    
    /**
     * Creates a new camera preview item with specified tile size.
     *
     * @param camera The camera to preview
     * @param settings Application settings
     * @param size The tile size
     */
    public CameraPreviewItem(Camera camera, Settings settings, TileSize size) {
        this.camera = camera;
        this.settings = settings;
        this.tileWidth = size.getWidth();
        this.tileHeight = size.getHeight();
        
        setSpacing(5);
        setAlignment(Pos.CENTER);
        
        // Create preview container
        previewContainer = new StackPane();
        previewContainer.setPrefSize(tileWidth, tileHeight);
        previewContainer.setMaxSize(tileWidth, tileHeight);
        previewContainer.setMinSize(tileWidth, tileHeight);
        previewContainer.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #444; -fx-border-width: 2;");
        
        // MediaView for video playback
        mediaView = new MediaView();
        mediaView.setFitWidth(tileWidth - 4);
        mediaView.setFitHeight(tileHeight - 4);
        mediaView.setPreserveRatio(true);
        
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
        
        previewContainer.getChildren().addAll(mediaView, overlay, statusLabel);
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
        
        Platform.runLater(() -> {
            statusLabel.setText("⏳ Starting...");
            statusLabel.setVisible(true);
        });
        
        // Create and start FFmpeg stream service
        streamService = new FFmpegStreamService(camera, settings);
        
        streamService.setOnReadyCallback(() -> {
            Platform.runLater(() -> {
                try {
                    // Create Media from HTTP stream URL
                    String streamUrl = streamService.getStreamUrl();
                    System.out.println("Connecting to stream: " + streamUrl);
                    
                    media = new Media(streamUrl);
                    mediaPlayer = new MediaPlayer(media);
                    
                    mediaPlayer.setOnReady(() -> {
                        Platform.runLater(() -> {
                            mediaView.setMediaPlayer(mediaPlayer);
                            mediaPlayer.play();
                            running.set(true);
                        });
                    });
                    
                    mediaPlayer.setOnError(() -> {
                        Throwable error = mediaPlayer.getError();
                        System.err.println("Media player error for " + camera.getName() + ": " + 
                                (error != null ? error.getMessage() : "Unknown error"));
                        Platform.runLater(() -> {
                            statusLabel.setText("⚠ Stream Error");
                            statusLabel.setVisible(true);
                        });
                    });
                    
                    media.setOnError(() -> {
                        Throwable error = media.getError();
                        System.err.println("Media error for " + camera.getName() + ": " + 
                                (error != null ? error.getMessage() : "Unknown error"));
                        Platform.runLater(() -> {
                            statusLabel.setText("⚠ Media Error");
                            statusLabel.setVisible(true);
                        });
                    });
                    
                } catch (Exception e) {
                    System.err.println("Failed to create media player for " + camera.getName() + ": " + e.getMessage());
                    statusLabel.setText("⚠ Error");
                    statusLabel.setVisible(true);
                }
            });
        });
        
        streamService.setOnErrorCallback(() -> {
            Platform.runLater(() -> {
                statusLabel.setText("⚠ FFmpeg Error");
                statusLabel.setVisible(true);
                running.set(false);
            });
        });
        
        // Start the FFmpeg stream
        if (!streamService.start()) {
            Platform.runLater(() -> {
                statusLabel.setText("⚠ Failed to Start");
                statusLabel.setVisible(true);
            });
        }
    }
    
    /**
     * Stops the camera preview.
     */
    public void stopPreview() {
        running.set(false);
        
        // Stop media player
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        
        media = null;
        mediaView.setMediaPlayer(null);
        
        // Stop FFmpeg stream
        if (streamService != null) {
            streamService.stop();
            streamService = null;
        }
    }
    
    /**
     * Disposes of all resources.
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
     * Updates the tile size.
     *
     * @param size The new tile size
     */
    public void setTileSize(TileSize size) {
        this.tileWidth = size.getWidth();
        this.tileHeight = size.getHeight();
        
        Platform.runLater(() -> {
            previewContainer.setPrefSize(tileWidth, tileHeight);
            previewContainer.setMaxSize(tileWidth, tileHeight);
            previewContainer.setMinSize(tileWidth, tileHeight);
            
            mediaView.setFitWidth(tileWidth - 4);
            mediaView.setFitHeight(tileHeight - 4);
            
            overlay.setWidth(tileWidth - 4);
            overlay.setHeight(tileHeight - 4);
            
            statusLabel.setMaxWidth(tileWidth - 20);
            nameLabel.setMaxWidth(tileWidth);
        });
    }
    
    /**
     * Updates the settings reference.
     *
     * @param settings The new settings
     */
    public void setSettings(Settings settings) {
        // Settings are passed to stream service on start
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
}
