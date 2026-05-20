# Upstream Code Navigation

Use these paths to inspect implementation patterns without copying large blocks of code.
Take the smallest relevant idea, then adapt it to the current project.

## Demo and entry points

- `catalog/src/main/java/com/kyant/backdrop/catalog/MainActivity.kt`
  - App entry point for the demo catalog
- `catalog/src/main/java/com/kyant/backdrop/catalog/CatalogDestination.kt`
  - Registers demo destinations and helps locate specific liquid-glass samples

## Glass navigation patterns

- `catalog/src/main/java/com/kyant/backdrop/catalog/components/LiquidBottomTabs.kt`
  - Main glass bottom bar container and interactive motion behavior
- `catalog/src/main/java/com/kyant/backdrop/catalog/components/LiquidBottomTab.kt`
  - Individual glass tab item behavior and rendering

## Sheet and playground patterns

- `catalog/src/main/java/com/kyant/backdrop/catalog/destinations/GlassPlaygroundContent.kt`
  - Demonstrates glass bottom sheet behavior and smoother rounded-corner presentation

## Slider pattern

- `catalog/src/main/java/com/kyant/backdrop/catalog/components/LiquidSlider.kt`
  - Glass slider behavior, highlight treatment, and interaction patterns

## Blur pattern

- `catalog/src/main/java/com/kyant/backdrop/catalog/destinations/ProgressiveBlurContent.kt`
  - Progressive blur demo using runtime-shader-based blur masking

## Backdrop rendering core

- `backdrop/src/main/java/com/kyant/backdrop/DrawBackdropModifier.kt`
  - Core `Modifier.drawBackdrop` implementation

## Effect implementations

- `backdrop/src/main/java/com/kyant/backdrop/effects/Lens.kt`
  - Lens and refraction behavior
- `backdrop/src/main/java/com/kyant/backdrop/effects/Blur.kt`
  - Blur effect implementation
- `backdrop/src/main/java/com/kyant/backdrop/effects/ColorFilter.kt`
  - Color adjustment, vibrancy, and related color-filter logic

## How to use these paths

1. Identify the closest visual pattern
2. Inspect the smallest matching file
3. Extract only the interaction or rendering idea that matters
4. Rebuild it in the target project with local naming, local state, and local layout constraints
5. Verify backdrop scope, readability, and spatial economy after adaptation
