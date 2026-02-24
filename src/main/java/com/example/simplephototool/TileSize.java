package com.example.simplephototool;

/**
 * Tile size presets for the responsive grid.
 */
public enum TileSize {
    SMALL(240, 180),
    MEDIUM(320, 240),
    LARGE(480, 360),
    XLARGE(640, 480);

    private final int width;
    private final int height;

    TileSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Calculates optimal tile size based on camera count and available width.
     *
     * @param cameraCount Number of cameras to display
     * @param availableWidth Available width in pixels
     * @return Optimal tile size
     */
    public static TileSize calculateOptimal(int cameraCount, double availableWidth) {
        if (cameraCount == 0) return MEDIUM;

        // Calculate ideal columns based on camera count
        int idealColumns;
        if (cameraCount <= 2) idealColumns = 2;
        else if (cameraCount <= 4) idealColumns = 2;
        else if (cameraCount <= 6) idealColumns = 3;
        else if (cameraCount <= 12) idealColumns = 4;
        else idealColumns = 5;

        // Calculate width per tile (accounting for gaps)
        double gapTotal = (idealColumns - 1) * 10 + 20; // 10px gaps + 20px padding
        double widthPerTile = (availableWidth - gapTotal) / idealColumns;

        // Choose appropriate size
        if (widthPerTile >= XLARGE.width) return XLARGE;
        if (widthPerTile >= LARGE.width) return LARGE;
        if (widthPerTile >= MEDIUM.width) return MEDIUM;
        return SMALL;
    }
}

