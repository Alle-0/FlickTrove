---
name: FlickTrove Premium Dark
colors:
  primary: '#2dd4bf'
  secondary: '#ff4081'
  tertiary: '#bb86fc'
  background: '#0a0a0a'
  surface: '#121212'
  error: '#ff5252'
  on-primary: '#000000'
  on-secondary: '#e0e0e0'
  on-tertiary: '#e0e0e0'
  on-background: '#e0e0e0'
  on-surface: '#e0e0e0'
  surface-variant: 'rgba(1, 1, 3, 0.70)'
  on-surface-variant: '#e0e0e0'
  surface-bright: '#1a1a1a'
  outline: '#666666'
  inverse-surface: '#e0e0e0'
  inverse-on-surface: '#121212'
typography:
  title-large:
    fontFamily: System Default
    fontSize: 22px
    fontWeight: '700'
    lineHeight: 28px
    letterSpacing: 0em
  body-large:
    fontFamily: System Default
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.03em
  label-small:
    fontFamily: System Default
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.04em
rounded:
  sm: 0.5rem
  DEFAULT: 0.75rem
  md: 0.875rem
  lg: 1rem
  xl: 1.25rem
  2xl: 1.5rem
  full: 9999px
spacing:
  xs: 4px
  sm: 8px
  md: 12px
  lg: 16px
  xl: 24px
  xxl: 36px
  gutter: 16px
---

## Brand & Style
The design system for FlickTrove is built to evoke an immersive, high-stakes cinematic atmosphere. It caters to cinephiles who appreciate a highly polished, premium dark-mode experience. The aesthetic relies heavily on **Glassmorphism** and a **Cyberpunk-inspired Neon Palette**, ensuring that the app's framework is secondary while the vivid movie poster art and the colorful UI accents remain the absolute protagonists. 

The emotional response is one of elegance and modern fluidity. By substituting standard Material ripples with physical bounce animations, and utilizing deep blacks with vibrant neon touches, the system mimics the sophisticated glow of an OLED screen.

## Colors
The palette leverages a strictly dark baseline optimized for OLED displays, with neon accents to provide necessary contrast.

- **Primary (Neon Teal):** `#2dd4bf`. Used for prominent selections, active states, and primary actions.
- **Secondary (Neon Pink) & Tertiary (Neon Purple):** `#ff4081` and `#bb86fc`. Reserved for gradients, charts, and secondary highlights.
- **Backgrounds:** `background` is a deep `#0a0a0a` (or pure `#000000` in AMOLED mode), while `surface` elements sit slightly elevated at `#121212`.
- **Glass/Variant:** `surface-variant` is handled via a deep `#010103` tint at 70% opacity, providing a frosted glass effect that perfectly contrasts with the `on-surface` (`#e0e0e0`) text.

## Typography
The typography system relies heavily on the system default font (typically Roboto or Inter) but establishes hierarchy through extreme weight and letter-spacing variations.

- Display headers and primary titles (e.g., inside the Wrapped stats or major section headers) use very heavy weights (`Black` or `Bold`) combined with tight or wide letter spacing depending on the component, mimicking classic movie title treatments.
- Body copy is kept simple and legible (16px, 400 weight), while small labels (11px) use a Medium weight with generous letter spacing (0.5sp) to remain crisp against the dark backgrounds.

## Layout & Spacing
The grid and spacing model prioritize content breathing room.

- **The 4/8px Rhythm:** Vertical and horizontal spacing relies on a strict 4px/8px rhythm (e.g., 4px, 8px, 12px, 16px, 24px).
- **Horizontal Margins:** A standard 16px or 20px gutter ensures comfortable padding on mobile edges.
- **Immersive Content:** Layouts frequently edge-to-edge for imagery, using safe area paddings specifically to dodge system bars.

## Elevation & Depth
Depth is created through **Glassmorphism (Haze)** and fine borders rather than traditional drop shadows.

- **Level 0 (Background):** Pure `#0a0a0a` or `#000000` for deep immersion.
- **Level 1 (Cards/Surfaces):** A subtle glass effect (`HazeStyle.PremiumDark`) featuring a 16px blur radius and a 70% opaque `#010103` tint, often framed with a 1px ghost border (white at 5% to 10% opacity) to catch the light.
- **Overlays/Modals:** Dialogs use an intensified blur (24px) with a heavier black tint to completely separate them from the content beneath.

## Shapes
The shape language is "Rounded" to soften the aggressive neon-on-black color scheme, making the interface feel premium and approachable.

- **Posters & Media:** `rounded-DEFAULT` (12dp) or `rounded-md` (14dp).
- **Cards & Stats:** `rounded-lg` (16dp) or `rounded-xl` (20dp).
- **Custom Shapes:** Occasional use of `TicketShape` with semi-circular cutouts to emulate a physical movie ticket for special components.

## Components

### Buttons
- Interactive elements abandon the standard Android Ripple for a **Bounce Click** (`bounceClick`), giving a tactile, physics-based scale-down effect (e.g., to 92%).
- Often housed in glassmorphic containers or subtle white-alpha (0.1f - 0.2f) boxes.

### Cards (Media)
Media cards are heavily visual. They are typically borderless, filling the rounded container. Information is either overlaid directly via bottom-up gradients or presented in extremely minimalist rows beneath the art.

### Chips & Tags
Tags (like genres or stats) sit inside highly rounded containers (`14dp` or fully pill-shaped), often with a very subtle white background (`alpha = 0.08f`) and a 1px ghost border. 

### Input Fields
Search and text fields integrate directly into the background or use a low-opacity glass effect, avoiding heavy outlines. Focus is indicated by the primary Neon Teal color.

### Progress Bars
Thin, modern indicators often using gradients drawn from the Cyberpunk Palette (e.g., Teal to Cyan) for a sleek look.

### Lists & Navigation
List items embrace the `bounceClick` modifier. Scrollable areas are tightly spaced but ensure readability, allowing the backdrop imagery to bleed softly under the glassmorphic top/bottom bars.
