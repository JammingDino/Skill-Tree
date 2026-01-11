# Skill Tree Mod

Welcome to the Skill Tree mod, a Minecraft mod that introduces a comprehensive and customizable skill system to the game! This mod allows players to unlock new abilities and perks by progressing through a visually interactive skill tree.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-beige)](https://fabricmc.net/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20-green)](https://minecraft.wiki/w/Java_Edition_1.20)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/dnC6tCcs?color=00AF5C&logo=modrinth&label=Downloads)](https://modrinth.com/mod/skill-tree-(fabric))
[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1397099?logo=curseforge&label=Downloads)](https://www.curseforge.com/minecraft/mc-mods/skill-tree-fabric)

## Table of Contents

- [Features](#-features)
- [How to Use](#-how-to-use)
- [Project Structure](#-project-structure)
- [For Developers](#-for-developers)
  - [Building from Source](#building-from-source)
  - [Key Classes](#key-classes)
- [Contributing](#-contributing)
- [License](#-license)

## Features

*   **Interactive Skill Tree:** A clean, user-friendly UI for viewing and unlocking skills.
*   **Custom Skills:** The system is designed to be easily expandable with new and unique skills.
*   **Prerequisites:** Skills can have dependencies, requiring players to unlock parent skills first to create a true sense of progression.
*   **Skill Altar:** A unique in-game block that serves as the central point for accessing and managing your skills.

## How to Use

1.  **Craft the Skill Altar:** Start by crafting the central block for this mod, the **Skill Altar**. Higher tier skills will require higher tier skill alters.
2.  **Access the Tree:** Place the Skill Altar in the world and right-click it to open the skill tree screen.
3.  **Unlock Skills:** Gather the required resources or experience and click on a skill to unlock it, granting you its abilities!

## Project Structure

The project follows a standard Fabric mod structure. Here is a brief overview of the key directories and files:

```
.
├── .github/workflows                       # CI/CD pipelines (Auto Release and Bump Version)
├── src
│   ├── main
│   │   ├── java/com/jd_skill_tree
│   │   │   ├── api                         # Interfaces for cross-side logic (IUnlockedSkillsData)
│   │   │   ├── blocks                      # Block definitions (SkillAltar, ModBlocks)
│   │   │   │   └── entity                  # Block Entities (SkillAltarBlockEntity)
│   │   │   ├── command                     # Server-side commands (/skill grant, etc.)
│   │   │   ├── mixin                       # Core game modifications (PlayerEntityMixin, LivingEntityMixin)
│   │   │   ├── networking                  # Packet handling (S2C and C2S communication)
│   │   │   ├── skills                      # CORE: Skill data structure and logic
│   │   │   │   ├── actions                 # Active triggers (SkillAction, TriggerType, Handlers)
│   │   │   │   ├── conditions              # Requirement logic (Health, Items, Biomes, etc.)
│   │   │   │   └── effects                 # Passive buffs (Attributes, Enchants, Mining Speed)
│   │   │   ├── utils                       # Helpers (ExperienceUtils, ModRegistries)
│   │   │   └── Jd_skill_tree.java          # Main Mod Initializer
│   │   │
│   │   └── resources                       # Server/Common resources
│   │       ├── assets/jd_skill_tree        # Textures, models, blockstates
│   │       ├── fabric.mod.json             # Mod metadata (License, dependencies)
│   │       └── jd_skill_tree.mixins.json
│   │
│   └── client
│       └── java/com/jd_skill_tree
│           ├── blocks/entity/renderer      # Visual renderers (Floating Book animation)
│           ├── client                      # Client interaction handlers
│           ├── networking                  # Client-side packet receivers
│           ├── screens                     # UI / GUI Logic
│           │   └── widgets                 # Custom UI elements (Skill Nodes)
│           ├── skills                      # Client-side data caching (ClientSkillData)
│           └── Jd_skill_tree_client.java   # Client Mod Initializer
│
├── build.gradle                            # Dependencies and build configuration
└── gradle.properties                       # Mod version and properties
```

## For Developers

This project is built using the **Fabric** modding toolchain for Minecraft 1.20.

### Building from Source

1.  **Prerequisites:**
    *   Java Development Kit (JDK) 17 or newer.
2.  **Clone the repository:**
    ```bash
    git clone https://github.com/JammingDino/jd_skill_tree.git
    cd jd_skill_tree
    ```
3.  **Build the project:**
    *   Use the included Gradle wrapper to build the mod JAR file.
    ```bash
    # On Windows
    gradlew build

    # On macOS/Linux
    ./gradlew build
    ```
    The compiled `.jar` file will be located in the `build/libs` directory.

### Key Classes

*   **`Jd_skill_tree.java`**: The main entry point. It now initializes the **SkillNetworking**, registers the new Action/Effect/Condition types, and handles server lifecycle events for data syncing.
*   **`skills/Skill.java`**: Now a POJO (Plain Old Java Object) loaded entirely from JSON. Instead of hardcoded logic, it contains lists of `SkillEffect`, `SkillAction`, and `SkillCondition` objects.
*   **`skills/SkillLoader.java`**: The resource listener that uses **GSON** to deserialize JSON files from data packs into Java objects. It handles the reloading of skills without restarting the game.
*   **`skills/SkillManager.java`**: The active runtime registry. It caches all loaded skills and tracks which Attributes (e.g., Max Health) need to be monitored by the Mixins.
*   **`blocks/SkillAltar.java`**: Defines the Altar's behavior. It detects player interaction to open the standard Skill Tree UI, or opens the **Developer Editor** if the Altar is Tier 99.
*   **`screens/DeveloperEditorScreen.java`**: The complex client-side GUI that allows developers/admins to create, configure, test, and export new skills directly in-game.
*   **`networking/SkillNetworking.java`**: Handles the critical communication between Server and Client, including syncing the entire Skill Registry (definitions) and the player's unlocked status.
*   **`mixin/PlayerEntityMixin.java`**: The core gameplay logic engine. It injects code into the vanilla player entity to apply passive effects (Attributes, Knockback, XP multipliers) based on unlocked skills.

## Contributing

Contributions are welcome! If you have ideas for new skills, improvements, or bug fixes, please feel free to open an issue or submit a pull request.

## License

This project is licensed under the terms specified in the [LICENSE](LICENSE) file.
