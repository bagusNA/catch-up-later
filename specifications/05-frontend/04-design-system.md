---
name: Editorial Reading Room
colors:
  surface: '#fbf9f4'
  surface-dim: '#dbdad5'
  surface-bright: '#fbf9f4'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f3ee'
  surface-container: '#f0eee9'
  surface-container-high: '#eae8e3'
  surface-container-highest: '#e4e2dd'
  on-surface: '#1b1c19'
  on-surface-variant: '#57423d'
  inverse-surface: '#30312e'
  inverse-on-surface: '#f2f1ec'
  outline: '#8a726c'
  outline-variant: '#ddc0ba'
  surface-tint: '#a13e29'
  primary: '#882c18'
  on-primary: '#ffffff'
  primary-container: '#a8432d'
  on-primary-container: '#ffd8cf'
  inverse-primary: '#ffb4a4'
  secondary: '#436465'
  on-secondary: '#ffffff'
  secondary-container: '#c3e6e7'
  on-secondary-container: '#476869'
  tertiary: '#733e00'
  on-tertiary: '#ffffff'
  tertiary-container: '#925410'
  on-tertiary-container: '#ffd9bb'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdad3'
  primary-fixed-dim: '#ffb4a4'
  on-primary-fixed: '#3d0600'
  on-primary-fixed-variant: '#822714'
  secondary-fixed: '#c6e9ea'
  secondary-fixed-dim: '#aacdce'
  on-secondary-fixed: '#002021'
  on-secondary-fixed-variant: '#2b4c4d'
  tertiary-fixed: '#ffdcc1'
  tertiary-fixed-dim: '#ffb778'
  on-tertiary-fixed: '#2e1500'
  on-tertiary-fixed-variant: '#6c3a00'
  background: '#fbf9f4'
  on-background: '#1b1c19'
  surface-variant: '#e4e2dd'
typography:
  display:
    fontFamily: Newsreader
    fontSize: 3.25rem
    fontWeight: '400'
    lineHeight: '1.15'
    letterSpacing: -0.02em
  display-mobile:
    fontFamily: Newsreader
    fontSize: 2.25rem
    fontWeight: '400'
    lineHeight: '1.2'
    letterSpacing: -0.015em
  headline-lg:
    fontFamily: Newsreader
    fontSize: 2.25rem
    fontWeight: '500'
    lineHeight: '1.25'
    letterSpacing: -0.015em
  headline-lg-mobile:
    fontFamily: Newsreader
    fontSize: 1.75rem
    fontWeight: '500'
    lineHeight: '1.3'
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Newsreader
    fontSize: 1.625rem
    fontWeight: '500'
    lineHeight: '1.3'
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Newsreader
    fontSize: 1.25rem
    fontWeight: '600'
    lineHeight: '1.35'
  body-lg:
    fontFamily: Newsreader
    fontSize: 1.25rem
    fontWeight: '400'
    lineHeight: '1.7'
    letterSpacing: 0.005em
  body-md:
    fontFamily: Newsreader
    fontSize: 1.0625rem
    fontWeight: '400'
    lineHeight: '1.65'
    letterSpacing: '0'
  body-sm:
    fontFamily: Newsreader
    fontSize: 0.9375rem
    fontWeight: '400'
    lineHeight: '1.55'
  ui-label-lg:
    fontFamily: Hanken Grotesk
    fontSize: 0.9375rem
    fontWeight: '500'
    lineHeight: '1.4'
    letterSpacing: 0.01em
  ui-label-md:
    fontFamily: Hanken Grotesk
    fontSize: 0.8125rem
    fontWeight: '500'
    lineHeight: '1.35'
    letterSpacing: 0.02em
  ui-label-sm:
    fontFamily: Hanken Grotesk
    fontSize: 0.6875rem
    fontWeight: '600'
    lineHeight: '1.3'
    letterSpacing: 0.04em
  ui-mono:
    fontFamily: JetBrains Mono
    fontSize: 0.8125rem
    fontWeight: '400'
    lineHeight: '1.4'
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  gutter: 1.5rem
  gutter-mobile: 1rem
  margin: 3rem
  margin-tablet: 2rem
  margin-mobile: 1.25rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.75rem
  space-xl: 2.5rem
  space-2xl: 4rem
---

## Brand & Style

This design system embodies the serene, contemplative atmosphere of a private reading library. Designed for deep reading, deliberate archiving, and quiet study, it prioritizes content over decoration. The audience consists of knowledge workers, researchers, avid readers, and thinkers seeking refuge from high-frequency feeds and algorithmic distraction.

The aesthetic fuses literary editorial tradition with modern typographic discipline:
- **Calm & Unhurried:** Visual quietude achieved through deliberate negative space, tactile paper tones, and minimal ornamentation.
- **Editorial Precision:** Typographic structure reminiscent of bespoke print journals and fine bookmaking.
- **Low-Cognitive Friction:** Subtle structural dividers, honest state signifiers, and an absolute rejection of saturated gradients, neon glows, or aggressive micro-animations.

## Colors

The palette is rooted in unbleached paper, warm stone, and classical bookbinding inks:

- **Surface Tones:** `#F9F7F2` acts as the foundational reading sheet, paired with `#F0EDE4` for recessed containers and `#E3DFD5` for muted borders. Darker elements deploy warm charcoal and graphite (`#23211E` for primary text, `#57534E` for secondary labels) to avoid the harsh glare of stark black on pure white.
- **Primary Accent (`#A8432D` - Deep Vermilion / Terracotta):** Reserved for intentional interactions: active reading progress, bookmarks, primary callouts, and key focus indicators. Never used for large background fills.
- **Secondary Accent (`#4A6B6C` - Muted Slate Teal):** Designates processing states, archive statuses, and low-priority system meta.
- **Tertiary Accent (`#C47D38` - Warm Ochre / Amber):** Applied to warning notices, saved highlights, and sync alerts.
- **Error & Destructive:** Handled via deep rust (`#8F2D1F`), keeping warnings legible while remaining tonally harmonious with the paper aesthetic.

## Typography

Typographic hierarchy enforces an intentional separation between prose reading content and application chrome:

- **Editorial Long-Form:** Set in **Newsreader**, an authoritative literary serif optimized for sustained on-screen reading. It features relaxed tracking and generous vertical rhythm (`1.65`–`1.7` line heights) to replicate fine book formatting.
- **Interface & Chrome:** Handled by **Hanken Grotesk**, a clean, humanist geometric sans-serif that remains crisp, unobtrusive, and structurally distinct from article text.
- **Code & Timestamps:** Rendered in **JetBrains Mono** at small scales for article metadata, word counts, and estimated reading durations.

## Layout & Spacing

The layout model balances catalog browsing with dedicated distraction-free reading modes:

- **Grid Architecture:** 
  - **Desktop (>= 1024px):** 12-column layout for dashboard views; restricted single-column container (optimal measure of `65ch` / max width `720px`) for active reading views.
  - **Tablet (768px – 1023px):** 8-column layout, margin `2rem`.
  - **Mobile (< 768px):** 4-column layout, margin `1.25rem`.
- **Vertical Rhythm:** Rooted in consistent `0.5rem` (8px) baseline units. Long-form article elements employ `space-lg` between paragraphs and `space-xl` prior to subheadings to sustain reading cadence.

## Elevation & Depth

This design system avoids high-blur, heavy drop shadows in favor of paper-layer physical realism:

- **Tonal Tiers:** Depth is articulated through shifting background tones. The base viewport sits at `#F9F7F2`; floating panels, drawers, and modal sheets sit on `#FFFFFF`; recessed sidebars and secondary wells use `#F0EDE4`.
- **Low-Contrast Outlines:** Structural containers employ hairline boundaries (`1px solid rgba(35, 33, 30, 0.08)` or `#E3DFD5`). 
- **Ambient Elevation:** When elements must float over text (e.g., selection popovers, context menus, search palettes), use an ambient, low-opacity shadow tinted with warm graphite: `0 4px 16px rgba(35, 33, 30, 0.06), 0 1px 2px rgba(35, 33, 30, 0.04)`.

## Shapes

The design system employs a soft, understated corner profile (`roundedness: 1`). 

- Default inputs, cards, and buttons use `0.25rem` (4px).
- Modals, popovers, and reading-card containers scale to `0.5rem` (8px).
- Status tags and badge elements utilize `0.25rem` to avoid hyper-curved "pill" shapes, maintaining a tailored, bookplate-like quality across all viewports.

## Components

### Buttons
- **Primary:** Filled `#A8432D` with `#FFFFFF` text. Minimal `0.25rem` radius, `0.5rem 1rem` padding, typography set to `ui-label-md`. Hover state darkens to `#8F2D1F`.
- **Secondary / Quiet:** Transparent background, `1px solid #E3DFD5`, text `#23211E`. Hover triggers background `#F0EDE4`.
- **Ghost:** No border, text `#57534E`, turns `#23211E` on hover with a subtle `#F0EDE4` tint.

### Reading Cards
- Constructed with `#FFFFFF` background and a `1px solid rgba(35, 33, 30, 0.07)` border. No shadow in default state.
- Card titles utilize `headline-sm` (Newsreader). Excerpts use `body-sm`.
- Hover introduces a subtle border color shift toward `#A8432D` (40% opacity) and a delicate warm shadow.

### Chips & Tags
- Used for publication sources, reading times, and topic collections.
- Neutral fill (`#F0EDE4`), text `#57534E`, font `ui-label-sm` in uppercase with subtle letter-spacing.
- Selected state shifts to `#A8432D` text on `#F9EFEA` background with a `#A8432D` hairline border.

### Input Fields & Search
- Inputs use flat `#FFFFFF` backgrounds bordered by `1px solid #E3DFD5`. 
- Focus state removes default browser outlines in favor of a crisp `1px solid #A8432D` border and an ultra-subtle `0 0 0 2px rgba(168, 67, 45, 0.12)` halo.
- Typography: `ui-label-lg` with `#23211E` for input text and `#8C877E` for placeholder text.

### Checkboxes & Radio Controls
- Square checkbox (`0.25rem` radius) and circular radio borders in `1.5px solid #8C877E`.
- Active/Checked states fill with `#A8432D` and feature an unbleached `#F9F7F2` checkmark/indicator.

### Article Reader Chrome
- **Progress Indicator:** A discrete `2px` top-edge bar tracking scroll position in `#A8432D`.
- **Margin Notes & Quotes:** Left border `2px solid #C47D38` with indented `body-md` italic text.
- **Selection Tooltip:** A minimal dark graphite (`#23211E`) floating pill containing actions (Highlight, Note, Share) rendered in crisp `#F9F7F2` iconography.
