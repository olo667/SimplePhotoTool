# GitHub Copilot Instructions for SimplePhotoTool

## Project Overview
SimplePhotoTool is a JavaFX desktop application for managing and previewing camera devices. It uses JavaCV (ByteDeco) for video capture and processing.

## Technology Stack
- **Java 23** with module system (JPMS)
- **JavaFX 17.0.6** for UI (controls, FXML, graphics, swing)
- **JavaCV 1.5.8** (ByteDeco) for camera capture and video processing
- **Maven** for build management
- **ControlsFX, Ikonli, BootstrapFX** for additional UI components

## Architecture & Code Style

### Module System
- This project uses Java modules (module-info.java)
- All JavaFX and external dependencies must be declared in module-info.java
- Use `requires` for dependencies and `opens` packages for FXML reflection

### JavaFX Patterns
- Use FXML for view definitions (stored in `src/main/resources/com/example/simplephototool/`)
- Controllers follow naming convention: `*Controller.java`
- Use `@FXML` annotations for injected UI components
- Separate concerns: Controllers handle UI logic, Services handle business logic

### Camera Management
- `Camera` model class represents camera configuration
- `CameraDevice` represents physical camera devices
- `CameraDeviceDetector` handles device discovery (platform-specific)
- `CameraPreviewService` extends JavaFX Service for async video preview

### Naming Conventions
- Classes: PascalCase (e.g., `MainController`, `CameraListCell`)
- Methods: camelCase (e.g., `updateItem`, `detectCameras`)
- FXML files: kebab-case (e.g., `main-view.fxml`, `add-camera-dialog.fxml`)
- Package: `com.example.simplephototool`

## Important Guidelines

### Threading
- UI updates MUST run on JavaFX Application Thread (use `Platform.runLater()`)
- Camera capture and processing should run on background threads
- Use JavaFX `Service` and `Task` for async operations

### Resource Management
- Always close camera grabbers in try-finally blocks
- Handle IOException and JavaCV exceptions appropriately
- Clean up resources when stopping preview services

### FXML and Controllers
- Load FXML using `FXMLLoader` with proper resource paths
- Set controllers before loading FXML or use `fx:controller` attribute
- Open packages to `javafx.fxml` in module-info.java for reflection

### Dependencies
- When adding JavaFX modules, update both pom.xml AND module-info.java
- JavaCV platform dependency includes native libraries for all platforms
- Keep JavaFX version consistent across all artifacts (currently 17.0.6)

## Build & Run

### Maven Commands
- **Compile**: `mvn compile` (may need sudo if permission issues)
- **Clean**: `mvn clean`
- **Run**: `mvn javafx:run`
- **Package**: `mvn package`

### Common Issues
- Permission errors in target/: Run with sudo or fix directory ownership
- Module not found: Check both pom.xml dependencies and module-info.java requires
- FXML load failures: Verify package is opened in module-info.java

## Code Generation Preferences
- Generate comprehensive Javadoc comments for public APIs
- Include null checks and validation for method parameters
- Use try-with-resources or proper try-finally for resource cleanup
- Prefer composition over inheritance
- Keep methods focused and single-purpose
- Use Optional for potentially null return values

## Testing
- Place tests in standard Maven test structure
- Use JUnit 5 (Jupiter) as configured in pom.xml
- Mock JavaFX components when testing controllers

## Platform Support
- Primary target: Linux
- JavaCV supports Windows, macOS, Linux (platform-specific detection needed)
- Use platform-agnostic JavaFX and Java APIs where possible
