# Skill Tree Mod

Welcome to the Skill Tree mod, a Minecraft mod that introduces a comprehensive and customizable skill system to the game! This mod allows players to unlock new abilities and perks by progressing through a visually interactive skill tree.

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
├── src
│   ├── main
│   │   ├── java/com/jd_skill_tree
│   │   │   ├── blocks                 # Contains custom block definitions (e.g., SkillAltar)
│   │   │   ├── skills                 # Core logic for the skill system (Skill class, ModSkills registry)
│   │   │   ├── utils                  # Utility classes like ModRegistries for event handling
│   │   │   └── Jd_skill_tree.java     # Main mod initializer class
│   │   └── resources
│   │       ├── assets/jd_skill_tree   # Textures, models, and language files
│   │       └── data/jd_skill_tree     # Recipes, loot tables, and tags
│   └── client                         # Client-side specific code (e.g., UI screens, renderers)
└── build.gradle                       # The main Gradle build script
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

*   **`Jd_skill_tree.java`**: The main entry point for the mod. It handles the initialization of all blocks, items, and registries.
*   **`skills/Skill.java`**: The data class that defines a single skill. It holds information like its ID, title, description, icon, cost, and any prerequisites.
*   **`skills/ModSkills.java`**: Acts as a registry for all the skills in the mod. This is where all skills are defined and instantiated.
*   **`blocks/SkillAltar.java`**: Defines the behavior of the Skill Altar block, such as what happens when a player right-clicks it.
*   **`blocks/ModBlocks.java`**: A registry for all custom blocks added by the mod.
*   **`utils/ModRegistries.java`**: A utility class to centralize the registration process for various game elements like items, blocks, and event handlers.

## Contributing

Contributions are welcome! If you have ideas for new skills, improvements, or bug fixes, please feel free to open an issue or submit a pull request.

## License

This project is licensed under the terms specified in the [LICENSE](LICENSE) file.
