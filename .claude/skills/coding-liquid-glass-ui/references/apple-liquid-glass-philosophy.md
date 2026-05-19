# Apple Liquid Glass Philosophy Notes

## Purpose

Use these notes to translate Apple’s Liquid Glass philosophy into practical Android or Compose implementation choices.

## Core philosophy

### 1. Controls and navigation form a top functional layer
Apple positions Liquid Glass on the topmost interface layer for controls and navigation so people can focus on the content beneath it.
The result is a lighter chrome model where bars, sidebars, sheets, and controls feel suspended above the content instead of boxing it in.

### 2. Reduce occupied screen area
Apple’s examples consistently reduce persistent UI occupation.
Floating tab bars, floating sidebars, inset partial-height sheets, and adaptive search placement preserve more of the content canvas.
When possible, navigation recedes, minimizes, or adapts to context rather than staying fully expanded all the time.

### 3. Extend content behind glass
Apple explicitly uses background extension to let imagery and content continue visually beneath sidebars and inspectors.
This keeps the interface immersive without sacrificing clarity.
Translate this idea into Compose by letting content remain visible behind glass overlays instead of introducing opaque structural blocks.

### 4. Use fluid transitions to explain state changes
Liquid Glass is not only translucent.
It is fluid.
Elements emerge from source controls, transition smoothly between compact and expanded states, and maintain continuity during movement.
This gives users a stronger sense of cause and effect.

### 5. Use scaling and concentric geometry to feel native to the container
Apple emphasizes corner concentricity and adaptive geometry.
Controls should feel like they belong to their sheet, toolbar, or display edge.
Scaling behavior should reduce chrome when possible and preserve recognizability during interaction.

### 6. Favor legibility and restraint
Apple recommends removing custom backgrounds that interfere with the system material, avoiding overuse of glass, and testing accessibility settings.
Glass is most effective on the most important functional elements.
Readability remains the gating requirement.

## Practical translation to Android and Compose

### Reduce screen occupation
- Prefer floating overlay bars instead of solid reserved bottom areas
- Prefer inset and partial-height sheets that allow surrounding content to remain visible
- Prefer compact resting states with expansion on interaction or scroll

### Create fluid continuity
- Use spring-based scale, drag, and settle motion for direct manipulation
- Keep transitions visually connected to the source element
- Let shape, position, and emphasis evolve continuously instead of switching abruptly

### Preserve backdrop correctness
- Keep backdrop sampling stable while animating the foreground shape or content
- If the entire sampled region scales with the control, the effect usually feels wrong
- Scope backdrop creation before polishing effect parameters

### Maintain readability
- Add subtle surface fills when text or icons need more contrast
- Use tint to communicate importance, status, or meaning
- Avoid turning every control into glass

## Apple APIs and concepts worth mirroring conceptually
- `backgroundExtensionEffect()` for content continuation under floating structure
- `tabBarMinimizeBehavior` for reduced resting occupancy
- `ToolbarSpacer` and toolbar grouping for clearer action hierarchy
- `glassEffect`, `interactive`, `GlassEffectContainer`, and `glassEffectID` for fluid custom elements and transitions
- Concentric geometry APIs for shape alignment with host surfaces

## Source links
- Apple Developer, Adopting Liquid Glass
  - https://developer.apple.com/documentation/TechnologyOverviews/adopting-liquid-glass
- Apple Developer, Build a SwiftUI app with the new design, WWDC25 Session 323
  - https://developer.apple.com/videos/play/wwdc2025/323/
