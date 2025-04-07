# Kotlin Boomer Shooter Engine

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A work-in-progress 3D game engine inspired by classic "boomer shooters" like Doom and Quake, built entirely in Kotlin using Java Swing for the UI and editor. This engine features an integrated grid-based level editor allowing users to build their own maps directly within the application.

**(Note: This project is currently under development.)**

![Screenshot Placeholder](placeholder.png) <!-- TODO: Add a cool screenshot or GIF! -->

## Features

*   **Integrated Grid-Based Level Editor:**
    *   Build multi-level maps using a simple grid interface.
    *   Place and configure different map elements (Walls, Floors, Water, Ramps, Pillars).
    *   Support for adding/removing floors above and below the current level.
    *   Place Player Spawn points.
    *   Choose between different wall styles (e.g., block walls).
*   **3D Rendering:**
    *   Basic 3D rendering of the created world.
    *   Sky rendering with customizable sky color.
*   **Player Controller:**
    *   Classic FPS movement (Forward, Backward, Strafe Left/Right).
    *   Mouse look for aiming/camera rotation.
    *   Optional Gravity with Jumping.
    *   No-Gravity mode with Fly Up/Down controls.
*   **Editor/Game Mode Toggle:** Seamlessly switch between editing the map and playing within it.
*   **UI:**
    *   Built with Java Swing.
    *   Welcome screen for creating new projects (Open World/Level-Based) or loading existing ones.
    *   Menu bar for file operations (Save/Load World), floor management, and settings.
    *   Docked editor panel alongside the game/editor view.
    *   In-game UI elements (Crosshair, FPS counter, Position/Direction display - toggleable).
*   **Customization:**
    *   Configurable crosshair (Shape, Size, Color, Visibility).
    *   Toggleable FPS counter, position/direction display, and weapon UI elements.
*   **Persistence:**
    *   Save and load created worlds (`.world` files).
    *   Distinguishes between Open World and Level-Based project types during save/load.

## Getting Started

### Prerequisites

*   Java Development Kit (JDK) 8 or later.
*   Kotlin Compiler (usually included with IDEs like IntelliJ IDEA).
*   An IDE like IntelliJ IDEA (Recommended) or a build tool like Gradle/Maven configured for Kotlin.

### Running the Engine

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-username/your-repo-name.git
    cd your-repo-name
    ```
2.  **Open the project** in your preferred IDE (e.g., IntelliJ IDEA).
3.  **Locate the main entry point** of the application (the file containing the `main` function that creates and runs the `Game3D` instance).
4.  **Run** the `main` function.

## How to Use

1.  **Welcome Screen:** On launch, you'll see a welcome screen. Choose:
    *   `Create New Open World`: Start a new project intended as a single, large map.
    *   `Create New Level-Based`: Start a new project intended to have distinct levels (though currently saved/loaded as single world files).
    *   `Load Existing World`: Load a previously saved `.world` file.
2.  **The Interface:**
    *   **Left Panel (Editor Panel):** Contains tools and options for editing the map, changing settings, and toggling modes.
    *   **Right Panel (Viewport):** Shows either the `GridEditor` view or the `RenderPanel` (game view).
    *   **Top Menu Bar:** Access file operations, floor management, and various settings (Display, Controls, etc.).
3.  **Editor Mode:**
    *   The default mode after creating/loading a world.
    *   Use the tools in the Editor Panel to draw walls, floors, place objects, etc., on the grid.
    *   Use the Menu Bar (`Floors` menu) to switch between different floor levels or add new ones.
    *   The Viewport shows a top-down or isometric view of the grid level you are editing.
    *   Save your progress using `File -> Save World`.
4.  **Game Mode:**
    *   Click the "Editor Mode" button (it will change to "Game Mode") or press the **`E`** key (default) to switch.
    *   The Viewport switches to the 3D rendered view from the player's perspective.
    *   The mouse cursor will be hidden, and mouse movement will control the camera.
    *   Use the configured keys to move around the world you built.
    *   Press **`E`** again to return to Editor Mode.

## Controls

### Editor Mode

*   **Mouse:** Interact with the grid editor (place walls, select objects, etc.
*   **Menu Bar:** Access various editor functions.
*   **`E` Key:** Toggle between Editor and Game Mode.
*   **Number Keys for faster Object Selection, 1, 2, 3, 4, 5, 6

### Game Mode

*   **Mouse Movement:** Look/Rotate Camera.
*   **`W` Key:** Move Forward
*   **`S` Key:** Move Backward
*   **`A` Key:** Strafe Left
*   **`D` Key:** Strafe Right
*   **`Fly Up` Key:** Fly Up (if gravity is disabled)
*   **`Shift` Key:** Fly Down(if gravity is disabled)
*   **`E` Key:** Toggle between Game and Editor Mode.
*   **`U` Key (Example):** Toggle Weapon/Game UI Visibility

**(Note: Check the `KeyBindings.kt` file for the definitive key codes.)**

## Core Concepts

*   **Grid:** The fundamental structure used in the editor. Maps are defined by placing elements within cells on different floor levels.
*   **Game3D:** The main class orchestrating the editor, renderer, player, UI, and mode switching.
*   **Renderer:** Handles drawing the 3D scene based on the geometry generated from the grid.
*   **Player:** Represents the user in Game Mode, handling movement, camera control, and collision (basic).
*   **Walls, Floors, etc.:** Geometric primitives generated from the grid data for rendering and collision.
*   **Swing UI:** The application uses Java's Swing framework for all visual components.

## Future Plans / Roadmap

This engine is actively under development. Here's a breakdown of planned features, improvements, and areas for future work:

**Core Engine & Rendering:**

*   [ ] **Rendering Enhancements:**
    *   [ ] Implement basic shader support for more advanced visual effects.
    *   [ ] Add a basic lighting system (e.g., simple shading based on wall orientation, potentially light sources).
    *   [ ] Support for ceiling textures.
    *   [ ] Support for transparent textures (e.g., for windows, grates).
    *   [ ] Implement sprite rendering for 2D objects (items, decorations, potentially simple enemies).
    *   [ ] Add texture mipmapping for improved texture quality at distance and reduced aliasing.
    *   [ ] Implement a simple particle system (e.g., for sparks, smoke, blood).
*   [ ] **Performance Optimization:**
    *   [ ] Implement frustum culling (don't render objects outside the camera's view).
    *   [ ] Implement distance-based culling (don't render objects beyond a certain distance).
    *   [ ] Investigate spatial partitioning (e.g., Quadtree/Octree or potentially BSP-like structures) for faster rendering and collision checks.
    *   [ ] Cache expensive calculations where possible.
*   [ ] **Physics & Collision:**
    *   [ ] Develop a simple physics system beyond basic AABB checks.
    *   [ ] Fix issues with floor transition collisions ("teleportation").
    *   [ ] Refine player movement in water (currently slightly jittery).
*   [ ] **Bug Fixes & Refinements:**
    *   [ ] Fix visible borders between floor tiles.
    *   [ ] Address texture "sliding" issues on surfaces.

**Gameplay Mechanics:**

*   [ ] **Combat System:**
    *   [ ] Implement a full weapon system (handling different weapon types, ammo).
    *   [ ] Allow creation/definition of custom weapons.
    *   [ ] Implement a damage system (player health, enemy health, damage application).
    *   [ ] Add projectile logic.
*   [ ] **Interactive Objects:**
    *   [ ] Implement breakable walls and floors.
    *   [ ] Add support for doors (sliding, swinging) and potentially animated walls/lifts.
    *   [ ] Introduce `PropObject` for static decorations.
    *   [ ] Add Trigger volumes/objects for scripting events.
    *   [ ] Implement functional Elevators.
    *   [ ] Add placeable Light Sources (linked to the lighting system).
    *   [ ] Add collectable Items (health, ammo, keys).
*   [ ] **Player & Camera:**
    *   [ ] Add alternate render views (e.g., zoomed/sniper scope effect).
*   [ ] **Enemy AI:**
    *   [ ] (Not explicitly listed, but implied by "boomer shooter") Basic enemy implementation (pathfinding, states, attacks).

**Level Editor & Content:**

*   [ ] **Editor Features:**
    *   [ ] Add support for placing generic 3D Objects (`.obj`, `.gltf`?).
    *   [ ] Add dedicated editor panels/tools for specific object types (e.g., Water Panel).
    *   [ ] Allow optional border rendering for individual objects, not just globally.
*   [ ] **Editor UI/UX:**
    *   [ ] Restructure editor panels for better organization (e.g., move Border panel to a 'World' tab).
*   [ ] **Project Management & Workflow:**
    *   [ ] Restructure the save system, potentially organizing levels within a single project structure (especially for Level-Based type).
    *   [ ] Add a "Check Existing Worlds" feature in the loader, perhaps with double-click-to-load functionality.

**UI/UX:**

*   [ ] **General UI:**
    *   [ ] Improve the overall visual design and layout of the editor and game UI.
*   [ ] **In-Game UI:**
    *   [ ] Add visual feedback options (e.g., configurable blood effects on screen).
*   [ ] **Menus & Screens:**
    *   [ ] Create a dedicated Start Menu screen.
    *   [ ] Implement a comprehensive Settings Menu (graphics, audio, controls).
    *   [ ] Add a Loading Screen (for larger levels).
    *   [ ] Add a Credits screen.

**Build & Distribution:**

*   [ ] **Game Building:** Implement a system or process to package games created *with* the engine into standalone executables.

*(This list is subject to change and prioritization)*

## Contributing

Contributions are welcome! If you'd like to contribute, please:

1.  Fork the repository.
2.  Create a new branch (`git checkout -b feature/your-feature-name`).
3.  Make your changes.
4.  Commit your changes (`git commit -m 'Add some feature'`).
5.  Push to the branch (`git push origin feature/your-feature-name`).
6.  Open a Pull Request.

Please try to follow the existing coding style.

## License

This project is licensed under the MIT License.
