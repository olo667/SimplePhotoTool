# Simple Photo Tool

A small desktop utility to take snapshots from multiple USB cameras.

- Add cameras with USB ports
- Choose output folder and filename format in Settings
- Mark cameras as **active/inactive**
- Take snapshots via **button** or **keyboard shortcuts**
- Built with **JavaFX**, using **FFmpeg

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
- **Live preview** of cameras in the main window

---

## Technology Stack

- **Language:** Java (modular project – see `module-info.java`)
- **UI:** JavaFX
- **Camera / USB access:**
  - FFmpeg for device detection and use
- **Build tool:** Maven (`pom.xml`)

---

## Getting Started

### SystemRequirements
- **Java:** JDK 17+ 
- **FFmpeg** installed (on windows run "winget install ffmpeg" in powershell)

