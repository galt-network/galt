---
name: Sovereign Gold & Obsidian
colors:
  surface: '#131316'
  surface-dim: '#131316'
  surface-bright: '#39393c'
  surface-container-lowest: '#0e0e11'
  surface-container-low: '#1b1b1e'
  surface-container: '#1f1f22'
  surface-container-high: '#2a2a2d'
  surface-container-highest: '#353438'
  on-surface: '#e4e1e6'
  on-surface-variant: '#d2c5ae'
  inverse-surface: '#e4e1e6'
  inverse-on-surface: '#303033'
  outline: '#9b8f7b'
  outline-variant: '#4e4635'
  surface-tint: '#f2bf40'
  primary: '#ffe2a6'
  on-primary: '#3f2e00'
  primary-container: '#f5c242'
  on-primary-container: '#6b5000'
  inverse-primary: '#785a00'
  secondary: '#fbbc2b'
  on-secondary: '#412d00'
  secondary-container: '#dca100'
  on-secondary-container: '#543b00'
  tertiary: '#eded00'
  on-tertiary: '#323200'
  tertiary-container: '#d0d000'
  on-tertiary-container: '#575700'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffdf9b'
  primary-fixed-dim: '#f2bf40'
  on-primary-fixed: '#251a00'
  on-primary-fixed-variant: '#5b4300'
  secondary-fixed: '#ffdea4'
  secondary-fixed-dim: '#fbbc2b'
  on-secondary-fixed: '#261900'
  on-secondary-fixed-variant: '#5d4200'
  tertiary-fixed: '#eaea00'
  tertiary-fixed-dim: '#cdcd00'
  on-tertiary-fixed: '#1d1d00'
  on-tertiary-fixed-variant: '#494900'
  background: '#131316'
  on-background: '#e4e1e6'
  surface-variant: '#353438'
typography:
  display-lg:
    fontFamily: Playfair Display
    fontSize: 56px
    fontWeight: '600'
    lineHeight: 64px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Playfair Display
    fontSize: 36px
    fontWeight: '600'
    lineHeight: 44px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: Playfair Display
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-md:
    fontFamily: Playfair Display
    fontSize: 24px
    fontWeight: '500'
    lineHeight: 32px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.08em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  gutter: 1.5rem
  gutter-mobile: 0.75rem
  margin: 2rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.5rem
---

## Brand & Style

This design system establishes a high-contrast aesthetic that merges the classical heritage of Austrian economics with cutting-edge sovereign technology. Crafted for institutional actors, capital allocators, and individualists building decentralized infrastructure, the visual language balances unyielding digital autonomy with bespoke private-banking pedigree.

The design movement combines **Minimalism** with an **Architectural High-Contrast** baseline:
- Matte obsidian planes and dense charcoal substrates provide an uncompromising foundation.
- Fine, laser-sharp Austrian gold elements serve as structural dividers, key focus rings, and status beacons.
- Restraint rules: gold is an instrument of precision, not sheer decoration. Surfaces emphasize darkness, depth, and spatial discipline to yield absolute focus and gravitas.

## Colors

The palette is engineered exclusively for an immersive, zero-fatigue dark experience.

- **Background & Canvas:** Pure obsidian (`#0A0A0C`) grounds the main viewport, while deep structured container panels rest at `#121215` and `#1A1A1E`. Interactive hover elevations peak at `#222228`.
- **Primary & Accentuation:** `#F5C242` acts as the principal brand signifier—representing sound money, value custody, and active intent. Secondary gold (`#E5A910`) governs steady state indicators and borders, while `#FDFD00` is reserved strictly for high-voltage notifications, system alerts, and active cryptographic transaction states.
- **Neutrals & Text:** Pure stark contrast text rests at `#F5F5F7`, secondary metadata and sub-captions render at `#9E9EA8`, and inactive states/borders dissolve cleanly into `#2A2A32`.

## Typography

The typographic hierarchy delivers an intentional juxtaposition:
1. **Editorial Authority (Playfair Display):** Used for significant headers, portfolio balances, and institutional milestones. It invokes the classical liberal tradition of sound money and the intellectual heritage of Vienna and the Enlightenment.
2. **Operational Ergonomics (Plus Jakarta Sans):** Drives all analytical workflows, UI copy, and body narratives. Highly legible across extreme pixel densities with neutral geometric purity.
3. **Cryptographic Rigor (JetBrains Mono):** Drives transactional hashes, Bitcoin values, status tags, block heights, and telemetry parameters to emphasize mechanical verifiability.

## Layout & Spacing

The layout model is anchored on an 8pt architectural rhythm using a fluid 12-column grid on desktop and a compact 4-column structure on mobile devices:

- **Desktop (1024px+):** Max-width containment at 1440px with generous margins (`margin: 2rem`) and distinct column gutters (`gutter: 1.5rem`) allowing dense multi-pane financial charts and transaction streams to sit without cognitive strain.
- **Tablet (768px - 1023px):** Adapts to an 8-column layout, collapsing peripheral telemetry into collapsible off-canvas sheets.
- **Mobile / PWA (<768px):** Zero-waste margins (`margin-mobile: 1rem`) with a fixed bottom-anchored haptic navigation zone. Primary sovereign metrics sit top-level, while data grids reflow into vertically stacked card units.

## Elevation & Depth

Visual hierarchy rejects blurry dropshadows in favor of **structural borders and tonal stratification**:

- **Layer 0 (Canvas):** Flat obsidian `#0A0A0C`.
- **Layer 1 (Cards & Structural Frames):** Matte charcoal `#121215` enclosed in crisp 1px borders colored `#2A2A32`.
- **Layer 2 (Active Panels / Modals):** Elevated substrate `#1A1A1E` framed by an ultra-thin 1px gold hairline border (`rgba(245, 194, 66, 0.35)`).
- **Luminosity (Status & Accents):** Soft amber and gold halos (`box-shadow: 0 0 16px rgba(245, 194, 66, 0.15)`) accompany active sovereign beacons, verification badges, and primary key states, communicating computational vitality against dead-black darkness.

## Shapes

The design system enforces architectural firmness through **Soft (0.25rem)** geometric radiuses:
- Base cards, data containers, button elements, and dialogs utilize an authoritative, barely-softened 4px (`0.25rem`) corner radius.
- Badges, status nodes, and sovereign crypto pill tags employ a full rounded loop (`rounded-full`) to delineate system indicators from structural, sharp-cornered interactive surfaces.
- All form inputs retain the signature 4px radius with razor-sharp focus bounds.

## Components

### Buttons
- **Primary Sovereign:** Solid `#F5C242` gold surface, `#0A0A0C` high-contrast typography in bold sans-serif, 4px border radius. Hover introduces a subtle warm glow (`box-shadow: 0 0 12px rgba(245, 194, 66, 0.35)`).
- **Secondary (Ghost Obsidian):** Transparent base with a 1px border in `#2A2A32` transitioning to `#E5A910` on hover, paired with `#F5F5F7` text.
- **Destructive/Critical:** Matte black fill with crisp crimson hairline border (`#FF453A`) and muted crimson text.

### Sovereign Badges & Chips
- Monospaced JetBrains Mono typography at `label-sm` with letter spacing.
- Bound in 1px borders (`rgba(245, 194, 66, 0.3)`) over `#1A1A1E`.
- Integrated pulsing 6px gold/amber micro-LED dot indicators indicating live node status or on-chain finality.

### Cards & Data Panels
- High-contrast charcoal surfaces (`#121215` / `#1A1A1E`) bound by 1px subtle divider lines (`#2A2A32`).
- Modular header areas that display classical serif values stacked directly above monospaced micro-captions.

### Form Inputs
- Background `#0E0E11` with a restful 1px border of `#222228`.
- Focus state: Instantly transitions border to `#F5C242` accompanied by a 1px subtle inner hairline, completely free of generic browser outlines.

### Checkboxes & Radio Controls
- Geometric square and circular frames with sharp definition; selected states fill with `#F5C242` and display pitch-black `#0A0A0C` vector check indicators.

### Mobile Navigation (PWA)
- Bottom-docked translucent obsidian bar (`rgba(10, 10, 12, 0.85)` with `backdrop-filter: blur(16px)`).
- 1px top border of `#2A2A32` with gold indicators marking the current sovereign workspace.