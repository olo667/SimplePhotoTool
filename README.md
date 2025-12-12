# Simple Photo Tool

A small desktop utility to take snapshots from multiple USB cameras.

- Add cameras with USB ports
- Choose output folder and filename format in Settings
- Mark cameras as **active/inactive**
- Take snapshots via **button** or **keyboard shortcuts**
- Built with **JavaFX**, **usb4java**, and **JavaCV**

> Next version (planned): Live preview of camera feeds.

---

## Features

- **USB camera management**
  - Detect and add cameras connected via USB
  - Per-camera active/inactive toggle (use only the cameras you need)

- **Configurable output**
  - Select an **output folder** for snapshots
  - Define a **filename pattern** (e.g. `camera-{id}_{timestamp}.jpg`)

- **Snapshot capture**
  - Capture snapshots from all active cameras
  - Trigger via:
    - UI button in the main window
    - Keyboard shortcut(s)

- **Planned / TODO**
  - **Live preview** of cameras in the main window

---

## Technology Stack

- **Language:** Java (modular project – see `module-info.java`)
- **UI:** JavaFX
- **Camera / USB access:**
  - usb4java – low-level USB access
  - JavaCV – camera capture and image handling
- **Build tool:** Maven (`pom.xml`)

---

## Project Structure

Key files and directories:

- `pom.xml` – Maven configuration and dependencies
- `src/main/java/com/example/simplephototool/`
  - `Application.java` – JavaFX entry point
  - `MainController.java` – main UI controller (camera list, snapshot button, etc.)
- `src/main/resources/com/example/simplephototool/`
  - `main-view.fxml` – JavaFX layout for the main window
- `module-info.java` – module declarations for JavaFX and libraries

---

## Getting Started

### Prerequisites

- **Java:** JDK 17+ (or the version your `pom.xml` targets)
- **Maven:** 3.6+ (or use the included `mvnw` / `mvnw.cmd` wrapper)
- **Native requirements:**
  - Drivers for your USB cameras installed on the OS

### Build

From the project root (`D:\SimplePhotoTool`):

```cmd
mvnw.cmd clean package
```

### Run

If the Maven JavaFX plugin is configured:

```cmd
mvnw.cmd javafx:run
```

Or run the built JAR (update the filename to match your build):

```cmd
cd target
java -jar simplephototool-0.0.1-SNAPSHOT.jar
```

---

## Usage

1. **Connect USB cameras**
   - Plug in the cameras you want to use.

2. **Configure settings**
   - Open **Settings**:
     - Choose the **output folder** where photos will be saved.
     - Set the **filename format**, e.g. `photo-{timestamp}.jpg`.

3. **Select active cameras**
   - Use the list to mark cameras as **active** or **inactive**.

4. **Take snapshots**
   - Click the **snapshot button**, or
   - Use the configured **keyboard shortcut(s)**.

5. **Check your photos**
   - Open the output folder you set in Settings.

---

## Roadmap

- [ ] Live preview of photos / camera feed in the main window

---

## License

Copyright 2025, Olaf Imiolek.

Licensed under the Apache License, Version 2.0 (the "License");
You may not use this file except in compliance with the License.
You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
