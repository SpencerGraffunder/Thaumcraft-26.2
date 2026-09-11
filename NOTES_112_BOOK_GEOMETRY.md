# 1.12 Thaumonomicon recipe-popup geometry — VERBATIM from 1.12 source

Source: `src/main/java_old/client/gui/GuiResearchPage.java` (1.12.2 BETA26, authoritative).
All popup methods receive the PANEL CENTER as (x, y) = (panelX+128, panelY+128).
Panel = 255×255 paper.png at panelX=(width-256)/2, panelY=(height-256)/2.

## KEY RENDERING FACT (verified)
- 1.12 overlay `gui_researchbook_overlay.png` is **512×512** but 1.12 addresses it with
  **256-space UVs** (drawTexturedModalRect divides by 256). So 1.12 UV (u,v,w,h) samples
  texture pixels (2u, 2v, 2w, 2h).
- Station art is drawn under `glTranslate(x,y) + glScale(2,2)`: on-screen size = 2×UV size,
  on-screen pos = (x + 2·lx, y + 2·ly).
- ⇒ **26.2 blit for 1.12 `drawTexturedModalRect(lx,ly,u,v,w,h)` under that transform:**
  `blit(OVERLAY, cx+2lx, cy+2ly, 2u, 2v, 2w, 2h, 512, 512)`   [cx,cy = panel center]
- The 26.2 repo overlay is **byte-identical** to the 1.12 one (md5 a668d831…). Book + paper
  also identical. So NO texture changes needed — only the blit UV/pos/size.

## 1.12 art UVs (256-space) → 26.2 (512-space)
| piece       | 1.12 uv (u,v,w,h) | 26.2 uv (2u,2v,2w,2h) | 26.2 pos (cx,cy)     | 26.2 size |
|-------------|-------------------|------------------------|----------------------|-----------|
| arcane grid | 112,15,52,52      | 224,30,104,104         | (cx-52, cy-52)       | 104×104   |
| arcane arrow| 20,3,16,16        | 40,6,32,32             | (cx-16, cy-92)       | 32×32     |
| arcane wand | 68,76,12,12       | 136,152,24,24          | (cx-12, cy+80)       | 24×24 (α.4)|
| craft grid  | 60,15,51,52       | 120,30,102,104         | (cx-52, cy-52)       | 102×104   |
| craft arrow | 20,3,16,16        | 40,6,32,32             | (cx-16, cy-92)       | 32×32     |
| cruc flame  | 0,3,56,17         | 0,6,112,34             | (cx-56, cy-58)       | 112×34    |
| cruc pot    | 0,20,56,48        | 0,40,112,96            | (cx-56, cy-24)       | 112×96    |
| cruc redstrn| 100,84,11,13      | 200,168,22,26          | (cx-50, cy-52)       | 22×26     |
| inf band    | 0,3,56,17         | 0,6,112,34             | (cx-56, cy-92)       | 112×34    |
| inf altar   | 200,77,60,44      | 400,154,120,88         | (cx-56, cy-52)       | 120×88 (overflows 512 by 8 — same as 1.12) |

(Infusion art base translate is (x, y+20), not (x,y) — already baked into the cy offsets above.)

## ARCANE (drawArcaneCraftingPage L1174)
- title: recipe.type.arcane / .shapeless, centered (cx, cy-104), 0x505050
- grid art + arrow art (above)
- wand art α0.4 (above)
- output item 16×16: (cx-8, cy-84)
- crystals: for each aspect a (sz=size): item `ThaumcraftApiHelper.makeCrystal(aspect, amount)`
  16×16 with count at (cx + 4 - sz*10 + a*20, cy+59)
- vis number: centered (cx, cy+90), 0x505050  (drawn on top of the faded wand)
- grid items (shaped: index=col+row*rw; shapeless: index=k): 16×16 with count at
  (cx - 40 + (i%3)*32, cy - 40 + (i/3)*32), i=0..8

## CRAFTING (drawCraftingPage L1241)
- title: recipe.type.workbench / .workbenchshapeless, (cx, cy-104), 0x505050
- grid art + arrow art (above)
- output 16×16: (cx-8, cy-84)
- grid items: (cx - 40 + (i%3)*32, cy - 40 + (i/3)*32), 16×16 with count

## CRUCIBLE (drawCruciblePage L1288)
- title: recipe.type.crucible, (cx, cy-104), 0x505050
- flame art, pot art, redstone/arrow art (above)
- output 16×16: (cx-8, cy-50)
- catalyst 16×16 with count: (cx-64, cy-56)   [the redstone, on the LEFT]
- aspects: perRow=3, sx=cx-28, sy=cy+8-10*rows, rows=(size-1)/3, shift=(3-size%3)*10;
  aspect at (sx + total%3*20 + shift*m, sy + total/3*20), 16×16 with count (drawTag)

## INFUSION (drawInfusionPage L1345)
- title: recipe.type.infusion, (cx, cy-104), 0x505050
- band art + altar art (above)
- output 16×16: (cx-8, cy-85)
- central item 16×16 with count: (cx-8, cy-16)
- components (le of them): rot starts -90°, +360/le each; at
  (cx + cos(rot)*40 - 8, cy - 8 + sin(rot)*40 - 8), 16×16 with count
- aspects: perRow=5, sx=cx-48, sy=cy+50-10*rows, rows=(size-1)/5, shift=(5-size%5)*10;
  aspect at (sx + total%5*20 + shift*m, sy + total/5*20), 16×16 with count
- instability: "tc.inst tc.inst.<n>" (n=min(5, inst/2)), centered (cx, cy+94), 0x505050

## ASPECT POPUP (drawAspectsInsert L1059 + drawAspectPage L1072)
- 255×255 paper at panel origin; content (x,y)=(panelX+60, panelY+24)
- 5 rows, row r at y+r*40:
  - hover bg: (x-2, y+r*40-2) 32×32 (16×16 @2×) α0.5, tex3=_back.png
  - aspect icon: (x+2, y+r*40+2) 16×16 @1.5× = 24×24 (drawTag, no count)
  - name 0.5×: centered at (x+16, y+r*40+29), 0x505050
  - if components: comp0 icon 16×16 @1.25×=20×20 at (x+60, y+r*40+4); comp1 at (x+102, y+r*40+4)
    comp0 name 0.5× centered (x+72, y+r*40+29); comp1 name centered (x+114, y+r*40+29)
    "=" at (x+41, y+r*40+12) 0x999999 ; "+" at (x+89, y+r*40+12) 0x999999
  - else (primal): "tc.aspect.primal" at (x+54, y+r*40+12) 0x777777
- paging arrows (tex1=book): prev (x-20, y+208)=(panelX+40,panelY+232) uv(0,184,12,8);
  next (x+144,y+208)=(panelX+204,panelY+232) uv(12,184,12,8)

## RECIPE POPUP paging arrows (drawRecipe L908)
- prev: (panelX+40, panelY+232) uv(0,184,12,8); next: (panelX+204, panelY+232) uv(12,184,12,8)
- (bob-animated in 1.12; 26.2 can draw static)

## 26.2 API notes
- graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, 512, 512[, ARGB])
- graphics.item(stack, x, y) ; graphics.itemDecorations(font, stack, x, y)  ← draw item with NO bg
- graphics.text(font, str, x, y, color, shadow) ; graphics.centeredText(font, str, x, y, color)
- AspectRenderer.drawAspect(g, x, y, aspect, amount) = 16×16 icon + count (white, bottom-right)
- Aspect: getName(), getComponents() (Aspect[] of 2, or null), isPrimal(), getImage()
- ThaumcraftApiHelper.makeCrystal(Aspect, int) → ItemStack (crystal with count)
- Lang keys present: recipe.type.workbench(/shapeless), recipe.type.arcane(/shapeless),
  recipe.type.crucible, recipe.type.infusion, tc.inst, tc.inst.0-5, tc.aspect.name,
  tc.aspect.primal, wandtable.text1
