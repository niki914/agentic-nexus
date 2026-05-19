---
name: coding-liquid-glass-ui
description: Design and implement Liquid Glass-style Android and Jetpack Compose interfaces with the current AndroidLiquidGlass Backdrop APIs, focusing on content-first hierarchy, compact floating controls, immersive overlays, interactive scaling, blur, lens, and readability safeguards. Use when building, refactoring, or reviewing glass-like Android UI.
---

# Coding Liquid Glass UI

<skill_purpose>
Create Android and Jetpack Compose interfaces that use liquid glass as a functional foreground layer for navigation, controls, and immersive overlays while keeping content visually primary
</skill_purpose>

<liquid_glass_design_philosophy>
Liquid glass is a content-first interaction system, not a decorative filter. A good implementation reduces persistent chrome, keeps the content canvas visible behind foreground controls, and uses blur, refraction, tint, scaling, and motion to clarify hierarchy. Favor floating, edge-aware, compact surfaces over heavy reserved panels. Let controls expand, scale, or flow only when interaction benefits from that feedback. Tune every surface for legibility before visual intensity
</liquid_glass_design_philosophy>

<source_of_truth>
Use the current AndroidLiquidGlass repository source and the local example files as the only API truth:

- Core published artifact: `io.github.kyant0:backdrop:1.0.6`
  - maven:
  ```xml
  <dependency>
    <groupId>io.github.kyant0</groupId>
    <artifactId>backdrop</artifactId>
    <version>2.0.0-alpha03</version>
  </dependency>
  ```
- Core package: `com.kyant.backdrop`
- Backdrop sources package: `com.kyant.backdrop.backdrops`
- Effects package: `com.kyant.backdrop.effects`
- Example app components package: `com.kyant.backdrop.catalog.components`

The `catalog` components such as `LiquidButton`, `LiquidBottomTabs`, `LiquidBottomTab`, and `LiquidSlider` are example app components. They are useful implementation references, while the stable reusable layer is the `backdrop` module API such as `Backdrop`, `Modifier.drawBackdrop`, `rememberLayerBackdrop`, `Modifier.layerBackdrop`, `rememberCanvasBackdrop`, and `rememberCombinedBackdrop`
</source_of_truth>

<mandatory_example_reference>
Before writing or reviewing code, read the `.kt` files in `examples/` and use them as the authoritative syntax reference for imports, function calls, modifier order, and component usage:

- `examples/LiquidButtonExample.kt` for compact press-responsive glass controls using `drawBackdrop`, `layerBlock`, `vibrancy`, `blur`, and `lens`
- `examples/FloatingOverlayExample.kt` for immersive full-screen content with a floating glass navigation overlay using `rememberLayerBackdrop` and `Modifier.layerBackdrop`
- `examples/LiquidEnumPickerExample.kt` for a text-triggered, drag-through enum picker that keeps the selector compact until interaction

Copy import names and package names from these files when adapting examples into a project. When a project already has its own package name, add that package declaration above the imports and keep the imports unchanged unless the project uses different UI dependencies
</mandatory_example_reference>

<forbidden_distractions>
Forbidden: reasoning about historical versions, deprecated docs, breaking-change logs, or previous API shapes. Treat the current checked-out source and the `.kt` examples as the compatibility target. The skill intentionally avoids a `docs/` directory so agents stay focused on current source-backed usage
</forbidden_distractions>

<implementation_workflow>
1. Identify the surface role: button, floating overlay, picker, slider, bottom tabs, sheet, or badge cluster
2. Choose the smallest structure that preserves content space: floating overlay, inset control, compact rest state, or progressive expansion
3. Create the backdrop source with `rememberLayerBackdrop` for live content sampling, `rememberCanvasBackdrop` for custom drawn content, or `rememberCombinedBackdrop` for mixed global and local sources
4. Apply `Modifier.drawBackdrop` to the foreground surface with a Compose `Shape`, then compose effects in the order `vibrancy`, `blur`, `lens` when all three are needed
5. Put press, drag, and scale feedback into `layerBlock` or foreground content animation so the sampled backdrop remains spatially stable
6. Add `onDrawSurface` tint or fill only to preserve text and icon readability over busy content
7. Verify compactness, legibility, motion continuity, and content visibility in the resting state and active state
</implementation_workflow>

<api_usage_rules>
Use exact imports from the example files. The frequently used Backdrop imports are:

```kotlin
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
```

Inside `effects = { ... }`, call `dp.toPx()` directly because the effect scope provides density. Use Compose `RoundedCornerShape` when a simple current-source-compatible lens shape is enough. Use `onDrawSurface = { drawRect(...) }` for readable fills and `layerBlock = { scaleX = ...; scaleY = ... }` for liquid press or drag feedback
</api_usage_rules>

<review_checklist>
A finished liquid glass UI should satisfy these checks:

- The main content remains visually dominant
- The glass surface occupies less persistent space than an opaque bar or panel
- Blur, lens, tint, and fill improve readability and hierarchy
- Motion explains touch, drag, selection, expansion, or collapse
- Scaling is localized to the foreground surface or content
- Corners and spacing feel integrated with the host layout
- The implementation follows the imports and API calls shown in `examples/*.kt`
</review_checklist>
