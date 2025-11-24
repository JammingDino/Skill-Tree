# JD Skill Tree

Welcome to **JD Skill Tree**, a fully data-driven progression mod for Minecraft 1.20! This mod introduces a tiered skill altar system that allows players to trade experience levels for permanent passive buffs, attribute increases, and mining speed bonuses.

What sets this mod apart is its focus on **customization**. Server owners and modpack creators can build entirely new skill trees directly inside the game without writing a single line of code.

## Table of Contents

- [Features](#features)
- [gameplay-guide](#gameplay-guide)
- [Creating Addons (No Coding Required)](#creating-addons-no-coding-required)
- [Installation & Dependencies](#installation--dependencies)
- [For Developers](#for-developers)
- [License](#license)

### ***You can also [Look through the wiki for more information](https://github.com/JammingDino/Skill-Tree/wiki)***

## Features

*   **Tiered Progression:** Unlock skills through 5 distinct tiers of Altars (Wood, Iron, Diamond, Emerald, and Obsidian).
*   **XP-Based Economy:** Skills cost Experience Levels, turning your XP bar into a valuable resource for permanent power.
*   **Passive Buffs:** Unlock attributes like Max Health, Movement Speed, Attack Damage, and specialized Mining Speed bonuses.
*   **Visual Interface:** A clean, interactive GUI connecting skills via parent-child prerequisite nodes.
*   **In-Game Editor:** A built-in developer environment to design custom skills visually and export them as JSON.

## Gameplay Guide

1.  **Craft a Skill Altar:** Begin by crafting the **Tier 1 Skill Altar**.
2.  **Place & Interact:** Place the altar in the world and right-click it.
3.  **Unlock Skills:** If you have enough XP levels, click a skill to unlock it.
4.  **Upgrade:** To access more powerful skills, you must craft higher-tier altars (Iron, Diamond, etc.). A Tier 2 Altar allows you to view and unlock Tier 2 skills, provided you have the prerequisites from the previous tier.

## Creating Addons (No Coding Required)

JD Skill Tree is designed for "Creative Users." You can create your own skill trees for modpacks or servers using the built-in **Developer Console**.

### How to use the In-Game Editor:

1.  **Enter Creative Mode.**
2.  **Get the Developer Altar:** Search for the **"Developer Skill Altar"** in the creative menu (or use `/give @s jd_skill_tree:developer_skill_altar`).
3.  **Open the Console:** Place the block and right-click it. This opens the **Developer Editor Screen**.
4.  **Design Your Skill:**
    *   **Name & Description:** Set the display info.
    *   **Icon:** Use autocomplete to find any item in the game to use as the icon.
    *   **Cost & Tier:** Set the XP level cost and the Altar Tier required.
    *   **Effects:** Add multiple effects (e.g., `generic.max_health` + `ADDITION` + `2.0`).
    *   **Prerequisites:** Link it to other skill IDs.
5.  **Export:** Click the **Export button** to automatically create a datapack within your world save with the given names that you set in the GUI, or press the **"Copy JSON"** button which will copy the complete code for that skill to your clipboard!
6.  **Create the Datapack:** Paste that JSON into a file (e.g., `my_skill.json`) inside a datapack folder structure: `data/jd_skill_tree/skills/`.

**Note:** You do not need to know Java or JSON formatting. The editor handles the syntax for you.

## Installation & Dependencies

This mod requires **Fabric Loader** and the following dependencies:
*   [Fabric API](https://modrinth.com/mod/fabric-api)
*   [owo-lib](https://modrinth.com/mod/owo-lib) (Required for the UI)

## For Developers

This project is built using the **Fabric** modding toolchain for Minecraft 1.20.

### Building from Source

1.  **Prerequisites:**
    *   Java Development Kit (JDK) 17.
2.  **Clone the repository:**
    ```bash
    git clone https://github.com/JammingDino/jd_skill_tree.git
    cd jd_skill_tree
    ```
3.  **Build the project:**
    ```bash
    # On Windows
    gradlew build

    # On macOS/Linux
    ./gradlew build
    ```
    The compiled `.jar` file will be located in the `build/libs` directory.

## License

This project is licensed under the terms specified in the [LICENSE](LICENSE) file.
