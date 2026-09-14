# Thaumonomicon Research System (1.12 Reference)

Complete deep dive into how the thauminomicon research system works in Thaumcraft 1.12.
This is the reference for porting to 26.2.

## Core Concepts

### Three States of Research

| State | Meaning | Check Method |
|-------|---------|-------------|
| **UNKNOWN** | Player has never discovered this research | `!knowledge.isResearchKnown(key)` |
| **IN_PROGRESS** | Player knows the research but hasn't completed all stages | `knowledge.getResearchStage(key) <= stages.length` |
| **COMPLETE** | Player has finished all stages | `knowledge.isResearchComplete(key)` |

`isResearchComplete(key)` returns `getResearchStatus(key) == COMPLETE`, which is true when:
- The research is known (`research.contains(key)`), AND
- `getResearchStage(key) > entry.getStages().length` (stage counter exceeds total stages)

### Knowledge vs. Research

- **Knowledge** (integers): Trackable progress (e.g., "OBSERVATION;ALCHEMY;1" = 1 point of Alchemy observation). Gained passively or through research rewards. Spent to unlock research stages.
- **Research** (staged entries): The actual book entries. Have multiple stages that must be completed in order.

---

## Data Structure (JSON)

Research entries are defined in JSON files at `assets/thaumcraft/research/<category>.json`.

### Entry Structure

```json
{
  "key": "BASEALCHEMY",              // Unique identifier
  "name": "research.BASEALCHEMY.title",  // Translation key for display name
  "icons": ["thaumcraft:textures/research/cat_alchemy.png"],  // Icon(s) in browser
  "category": "ALCHEMY",            // Which category tab this belongs to
  "location": [0, 0],              // [x, y] position on the research map
  "parents": ["UNLOCKALCHEMY"],    // Required research to see/unlock this
  "meta": ["ROUND", "HIDDEN"],     // Special flags (see below)
  "stages": [ ... ]                // Array of stage objects (see below)
}
```

### Stage Structure

```json
{
  "text": "research.BASEALCHEMY.stage.1",   // Translation key for page text
  "recipes": ["thaumcraft:nitor", ...],     // Recipe bookmarks to show
  "required_knowledge": ["OBSERVATION;BASICS;1"],  // Knowledge points required
  "required_craft": ["thaumcraft:brass_ingot"],     // Must have crafted this
  "required_research": ["OTHERRESEARCH"],           // Must have completed this research
  "obtain": ["item:stack"],                        // Must be carrying this item
  "warp": 2                                        // Warp gained on completion (0-5)
}
```

### Meta Flags

| Flag | Effect |
|------|--------|
| `ROUND` | Round icon shape in browser |
| `SPIKY` | Spiky icon shape, +0.5 theorycrafting bonus |
| `REVERSE` | Inverted icon rendering |
| `HIDDEN` | Not visible until unlockable (+0.1 theorycrafting bonus) |
| `AUTOUNLOCK` | Auto-completes when prerequisites are met |
| `HEX` | Hexagonal icon shape |

### Parent Prefixes

Parents can have special prefixes that modify the unlock requirement:

| Syntax | Meaning |
|--------|---------|
| `"PARENT"` | Must be **complete** |
| `"PARENT@2"` | Must be at **stage 2 or higher** (in progress is OK) |
| `"~PARENT"` | **Soft** requirement (hidden from visibility check but still required for unlock) |
| `"A \|\| B"` | **OR** — either A or B complete |
| `"A && B"` | **AND** — both A and B complete |
| `"!ASPECT"` | Must know the **aspect** (special case) |

---

## Player-Facing Flow

### 1. Research Browser (Map View)

**File**: `GuiResearchBrowser.java`

The player opens the thauminomicon → sees a **map of research icons** arranged by `location: [x, y]`.

**Visibility logic** (`isVisible(res)`):
```
1. If player knows the research → VISIBLE
2. If HIDDEN meta AND player can't unlock → INVISIBLE
3. If HIDDEN meta AND no parents → INVISIBLE
4. If any parent is not visible → INVISIBLE (recursive)
5. Otherwise → VISIBLE (but dimmed if not unlockable)
```

**Unlockability** (`canUnlockResearch`):
```
ResearchManager.doesPlayerHaveRequisites(player, key)
  → checks all parents (stripped of ~ prefix)
  → uses knowsResearchStrict() which enforces:
    - Plain key → isResearchComplete()
    - Key@N → isResearchKnown() (stage >= N)
    - A||B → either complete
    - A&&B → both complete
```

**Dimmed icons**: Research that is visible but not yet unlockable is drawn at 10% opacity (`GL11.glColor4f(0.1f, 0.1f, 0.1f, 1.0f)`).

**Notification stars** (rendered as small 16x16 icons on top/bottom of research icons):
- **RESEARCH flag** (top, texture at 176,16): "New research discovered!" → shows a small star/badge
- **PAGE flag** (bottom, texture at 208,16): "New page in existing research" → shows a different badge

These flags are set server-side and synced to client. They are cleared when the player clicks the icon in the browser.

**Tooltip** (when hovering over an icon):
- Shows research name
- If not unlocked: shows "Requires: [parent names]" in yellow
- If RESEARCH flag: shows "§eNew research!" 
- If PAGE flag: shows "§eNew page!"

### 2. Research Page (Detail View)

**File**: `GuiResearchPage.java`

When the player clicks a research icon, the **detail page** opens.

**What's shown:**

#### Page Header
- Research icon + name
- Page navigation arrows (if multiple pages)

#### Main Text (Pre-research)
The `text` field is a **translation key** (e.g., `"research.BASEALCHEMY.stage.1"`).
The actual text is in `assets/thaumcraft/lang/en_us.json`:

```json
"research.BASEALCHEMY.stage.1": "Crucible crafting guide<BR>1) Place crucible..."
```

**Text formatting tokens** (parsed in `parsePages()`):
- `<BR>` — line break
- `<LINE>` — decorative line divider
- `<PAGE>` — page break (splits into multiple pages)
- `~I` — inline image (pulled from image list)
- `~L` — line separator image
- `~D` — divider image
- `~B` — bold line (extra height)
- `§` — formatting codes (color, bold, italic, etc.)

#### Requirements Bar (Bottom of Page, Pre-Research Only)

If `page == 0 && side == 0 && !isComplete`, a **requirements bar** is drawn at the bottom:

- **Research requirements** (`stage.getResearch()`): Icons of required research, with tooltip "Research required"
- **Knowledge requirements** (`stage.getKnow()`): Knowledge type icons, with tooltip "Knowledge required"  
- **Obtain requirements** (`stage.getObtain()`): Item icons, with tooltip "Item required"
- **Craft requirements** (`stage.getCraft()`): Craft reference icons, with tooltip "Must have crafted"

Each requirement is shown as an icon with a **checkmark or X** overlay indicating whether the player currently meets that specific requirement.

#### Recipe Bookmarks (Right Side)
If `stage.getRecipes() != null`, a list of recipe bookmark buttons appears on the right side. Clicking one opens the recipe viewer.

#### Warp Warning (Pre-Research Only)
If `stage.getWarp() > 0 && !isComplete`:
- A **forbidden symbol** is drawn
- Text: "Forbidden Level [1-5]" (color-coded)
- Warning text about warp consequences

#### "Complete Research" Button (Top-Left, Pre-Research Only)

**Position**: Top-left corner of the page (64x12 pixel hitbox)

**Condition**: Only clickable when `hasAllRequisites == true` (all requirements met)

**Click handler** (`mouseClicked`):
```java
if (hasAllRequisites && mx >= 0 && my >= 0 && mx < 64 && my < 12) {
    PacketHandler.INSTANCE.sendToServer(new PacketSyncProgressToServer(research.getKey(), false, true, true));
    Minecraft.getMinecraft().player.playSound(SoundsTC.write, 0.66f, 1.0f);
    hold = true;  // Prevents rapid re-clicks
}
```

This sends a packet to the server with `checks=true` (server re-validates requisites).

### 3. Server-Side Research Progression

**File**: `ResearchManager.progressResearch(player, key, sync)`

When the server receives the progress packet:

```
1. Check: isResearchComplete(key)? → if yes, return false
2. Check: doesPlayerHaveRequisites(player, key)? → if no, return false
3. Fire: ResearchEvent.Research (cancellable by other mods)
4. If not known: knowledge.addResearch(key)  → marks as "known"
5. Get current stage from player's saved stage counter
6. Advance stage: knowledge.setResearchStage(key, cs + 1)
7. If this was the last stage (popups=true):
   a. Set POPUP flag (triggers client toast notification)
   b. Set RESEARCH flag (triggers browser star) — unless noFlags=true
   c. Grant rewardItem (if any)
   d. Grant rewardKnow (if any)
   e. Auto-unlock siblings:
      For each sibling: if !isResearchComplete && doesPlayerHaveRequisites → progressResearch(sibling)
   f. Check all research for addenda unlocks
8. Sync knowledge to client (PacketSyncKnowledge)
```

**`startResearchWithPopup`** (used for AUTOUNLOCK and passive discoveries):
```java
boolean b = progressResearch(player, key, true);
if (b) {
    knowledge.setResearchFlag(key, POPUP);     // → client toast
    knowledge.setResearchFlag(key, RESEARCH);  // → browser star
}
```

### 4. Client-Side Sync & Notifications

**File**: `PacketSyncKnowledge.java`

When the server syncs knowledge to the client:

```
For each research entry:
  1. Update local PlayerKnowledge stage/known state
  2. If POPUP flag is set:
     - Clear the flag
     - Add a ResearchToast to the toast GUI:
       Minecraft.getMinecraft().getToastGui().add(new ResearchToast(researchEntry))
  3. If RESEARCH flag is set:
     - Keep it (browser star stays until player clicks the icon)
  4. If PAGE flag is set:
     - Keep it (browser star stays until player clicks the icon)
```

**ResearchToast**: A Minecraft toast notification showing:
- The research icon
- The research name
- "New Research Discovered!" text

### 5. Post-Research Text

Once `isComplete == true`:
- The **requirements bar is hidden** (no longer drawn)
- The **warp warning is hidden**
- The **"Complete Research" button is hidden**
- The **text changes to the next stage** (or the final stage if multi-stage)
- **Recipe bookmarks remain visible** (this is the main post-research content)
- The page shows the **full text** of the completed stage

For multi-stage research, the player clicks "Complete Research" on each stage in order. Each stage has its own `text`, `recipes`, `required_knowledge`, etc.

---

## State Machine Summary

```
                    ┌─────────────────────────────────────────────────┐
                    │           RESEARCH ENTRY LIFECYCLE             │
                    └─────────────────────────────────────────────────┘

  UNKNOWN ──→ IN_PROGRESS ──→ COMPLETE
     │            │                │
     │            │                │
  Not in      Player has      All stages
  research    "seen" it       finished.
  list        (stage > 0)    Stage > total.
                             Rewards given.
                             Siblings unlocked.
```

**UNKNOWN → IN_PROGRESS**: 
- Trigger: Player clicks "Complete Research" button (all requisites met), OR
- Trigger: `startResearchWithPopup()` called (AUTOUNLOCK, passive discovery)
- Effect: `knowledge.addResearch(key)`, stage advances to 1

**IN_PROGRESS → COMPLETE**:
- Trigger: Player completes final stage
- Effect: POPUP + RESEARCH flags set, rewards granted, siblings auto-unlocked

**Visibility in Browser**:
- UNKNOWN + not HIDDEN → shown dimmed (10% opacity)
- UNKNOWN + HIDDEN → not shown at all
- IN_PROGRESS → shown normally
- COMPLETE → shown normally (bright)

**Flags**:
- `RESEARCH` flag → star on icon in browser (cleared when player clicks icon)
- `PAGE` flag → different star (new page in existing research)
- `POPUP` flag → toast notification (cleared immediately on sync)

---

## Key File Map

| File | Purpose |
|------|---------|
| `api/research/ResearchEntry.java` | Data model: key, name, parents, siblings, icons, meta, stages, addenda, rewards |
| `api/research/ResearchStage.java` | Data model: text, recipes, obtain, craft, know, research, warp |
| `api/research/ResearchCategory.java` | Container for a category's research entries + display position |
| `api/research/ResearchCategories.java` | Static registry of all categories and entries |
| `api/research/ResearchEvent.java` | Forge events: `Research` (cancellable), `Knowledge` (cancellable) |
| `api/capabilities/IPlayerKnowledge.java` | Interface: knowledge tracking, research flags, stage management |
| `api/capabilities/IPlayerKnowledge.EnumResearchFlag` | `PAGE`, `RESEARCH`, `POPUP` |
| `api/capabilities/ThaumcraftCapabilities.java` | Static helpers: `knowsResearch()`, `knowsResearchStrict()` |
| `common/lib/research/ResearchManager.java` | Core logic: `progressResearch()`, `doesPlayerHaveRequisites()`, `parseAllResearch()`, `addKnowledge()` |
| `common/lib/capabilities/PlayerKnowledge.java` | Implementation: NBT storage, stage tracking, flag management |
| `client/gui/GuiResearchBrowser.java` | Map view: icon rendering, visibility, stars, tooltips, click handling |
| `client/gui/GuiResearchPage.java` | Detail view: text rendering, requirements bar, recipe bookmarks, "Complete" button |
| `client/gui/ResearchToast.java` | Toast notification for new research |
| `common/lib/network/PacketSyncProgressToServer.java` | Client→Server: "I want to complete this research" |
| `common/lib/network/PacketSyncKnowledge.java` | Server→Client: sync knowledge state + flags |
| `common/lib/network/PacketSyncResearchFlagsToServer.java` | Client→Server: clear RESEARCH/PAGE flags |
| `assets/thaumcraft/research/*.json` | Research entry definitions |
| `assets/thaumcraft/lang/en_us.json` | Localized text for all research entries |

---

## 26.2 Port Status (as of this doc)

### ✅ Working
- `ResearchEntry`, `ResearchStage`, `ResearchCategory` data models
- `ResearchManager` with `progressResearch()`, `doesPlayerHaveRequisites()`, `parseAllResearch()`
- `PlayerKnowledge` NBT storage and stage tracking
- `ThaumcraftCapabilities.knowsResearch()` / `knowsResearchStrict()`
- `IPlayerKnowledge` with `EnumResearchFlag` (PAGE, RESEARCH, POPUP)
- Research JSON parsing from `data/thaumcraft/research/`
- ResearchEvent system (Knowledge + Research events)

### ⚠️ Needs Verification
- **GUI**: `GuiResearchBrowser` → `ThaumonomiconScreen` (map view with icons, stars, tooltips)
- **GUI**: `GuiResearchPage` → detail page (text, requirements bar, recipe bookmarks, Complete button)
- **Toast**: `ResearchToast` equivalent in 26.2
- **Packet sync**: `PacketSyncProgressToServer` → modern NeoForge packet
- **Packet sync**: `PacketSyncKnowledge` → modern NeoForge packet with flag handling
- **Packet sync**: `PacketSyncResearchFlagsToServer` → modern NeoForge packet
- **Text formatting**: `<BR>`, `<PAGE>`, `<LINE>`, `~I`, `~L`, `~D` tokens in 26.2 rendering
- **Star rendering**: RESEARCH/PAGE flag icons on browser map

### ❌ Missing
- **Recipe bookmark system**: `stage.getRecipes()` → recipe viewer integration
- **Warp warning UI**: Forbidden symbol + level display
- **Addenda system**: `ResearchAddendum` for conditional extra content
- **Theorycrafting bonus**: SPIKY (+0.5) and HIDDEN (+0.1) meta bonuses
