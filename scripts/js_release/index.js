require('dotenv').config();
const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

// --- CONFIGURATION ---
const CONFIG = {
    modName: "Skill Tree",
    version: process.env.RELEASE_VERSION || "v1.1.0",
    modrinthId: process.env.MODRINTH_PROJECT_ID || "dnC6tCcs",
    curseforgeId: process.env.CURSEFORGE_PROJECT_ID || "1397099",
    rawBody: process.env.RELEASE_BODY || `
feat(actions): add Raycast effect and refactor editor for recursion
**New Features:**
- **Raycast Action:** Added \`RaycastActionEffect\`. Casts a ray from the player's eyes with configurable length, fluid collision, and entity hit detection. Passes the precise hit position to child effects.

**Refactoring:**
- **Recursive Editor:** Completely overhauled \`DeveloperEditorScreen\` internals. Replaced flat field mapping with a modular \`EffectConfig\` system. This supports infinite nesting of actions (e.g., Raycast -> Delayed -> Command) in the UI.

**Fixes:**
- Fixed \`TIMER\` actions executing every tick; they now correctly respect the configured interval. #27
- Fixed Developer Editor UI not refreshing action rows immediately when switching Trigger types.
    `
};

// --- HELPER: Convert Image to Base64 ---
function loadBase64Image(filePath) {
    try {
        const fullPath = path.join(__dirname, filePath);
        if (!fs.existsSync(fullPath)) {
            console.warn(`Warning: Image not found at ${fullPath}`);
            return null;
        }
        const fileBuffer = fs.readFileSync(fullPath);
        const ext = path.extname(fullPath).replace('.', '');
        // handle ico specifically or default to png/jpeg
        const mimeType = ext === 'svg' ? 'image/svg+xml' : `image/${ext}`;
        return `data:${mimeType};base64,${fileBuffer.toString('base64')}`;
    } catch (e) {
        console.error(`Error loading image ${filePath}:`, e);
        return null;
    }
}

// --- DATA FETCHING ---
async function fetchStats() {
    console.log("Fetching live stats...");
    let mCount = 84000;
    let cCount = 110000;

    try {
        const mRes = await fetch(`https://api.modrinth.com/v2/project/${CONFIG.modrinthId}`);
        if(mRes.ok) {
            const data = await mRes.json();
            mCount = data.downloads;
        }
    } catch (e) { console.error("Modrinth fetch failed", e); }

    try {
        const cRes = await fetch(`https://api.cfwidget.com/${CONFIG.curseforgeId}`);
        if(cRes.ok) {
            const data = await cRes.json();
            cCount = data.downloads.total;
        }
    } catch (e) { console.error("CurseForge fetch failed", e); }

    return { mCount, cCount };
}

function parseChangelog(rawText) {
    return rawText ? rawText.trim() : "";
}

// --- MAIN ---
(async () => {
    const stats = await fetchStats();
    const changelog = parseChangelog(CONFIG.rawBody); // Keep as full string

    const images = {
        main: loadBase64Image('assets/icon.png'),
        modrinth: loadBase64Image('assets/modrinth.ico'),
        curseforge: loadBase64Image('assets/curseforge.ico')
    };

    const browser = await puppeteer.launch({
        args: ['--no-sandbox', '--disable-setuid-sandbox'],
        // This helps in CI environments
        headless: "new"
    });
    const page = await browser.newPage();
    await page.setViewport({ width: 1080, height: 1080, deviceScaleFactor: 2 });

    const htmlContent = fs.readFileSync(path.join(__dirname, 'assets/template.html'), 'utf-8');
    await page.setContent(htmlContent);

    // Pass the raw changelog string to the browser
    await page.evaluate((config, stats, changelog, images) => {
        window.renderCards(config, stats, changelog, images);
    }, CONFIG, stats, changelog, images);

    await new Promise(r => setTimeout(r, 500));

    const cards = await page.$$('.project-card');
    for (let i = 0; i < cards.length; i++) {
        await cards[i].screenshot({ path: `release_card_${i + 1}.png` });
    }

    await browser.close();
})();