# AI Study Assistant — UI/UX Design Specification

> Version: 2.0 — Deep Craft Edition  
> Role: Senior UI Designer + SaaS Product Design Expert  
> References: Apple Human Interface Guidelines, Material Design 3, Ant Design Design Values  
> Goal: Define a Figma Community-grade, professional, modern, minimal, enterprise-level learning platform.  
> Constraint: AI is a capability, not the visual subject. The first impression must be "professional learning platform", not "AI chatbot".

---

## 1. Design Principles

### 1.1 Product Vision

Build a calm, focused, and credible learning workspace. The UI should feel like a tool that serious learners and professionals want to use every day — clean like Notion, precise like Linear, reliable like GitHub, and polished like Apple.

### 1.2 Design References

| Reference | What to Learn |
|-----------|---------------|
| **Linear** | Minimal chrome, density, subtle borders, fast feel, keyboard-first hints, refined typography. |
| **Notion** | Neutral canvas, content-first, generous whitespace, collapsible sidebars, block-based clarity. |
| **GitHub** | Functional tables, muted color accents, clear metadata, issue-like list patterns. |
| **Apple** | Tactile feedback, consistent spacing, readable typography, subtle depth, 44pt touch targets. |
| **飞书 (Lark)** | Clear information hierarchy, friendly but enterprise-ready, consistent icon language. |
| **Material Design 3** | Tonal color, elevation tokens, shape scale, state layers, purposeful motion. |
| **Ant Design** | Certainty, meaning, growth, naturalness; 8-point grid; enterprise clarity. |

### 1.3 Anti-Patterns (Do NOT Use)

- ChatGPT / Claude / Kimi / 豆包 style chat-centric layouts as the default home.
- Purple / blue gradient "tech" hero backgrounds.
- Glowing AI orb icons, robot mascots, sparkles as primary visuals.
- Centered single-column chat UIs for non-chat pages.
- Dark mode as default (support optional later; default is light and paper-like).
- Arbitrary values (random pixels, random colors, random timings).

### 1.4 Core UX Mantras

1. **Content first, chrome second.** Navigation and decoration should recede; learning content is the star.
2. **One page, one job.** Each page has a clear primary action and clear hierarchy.
3. **Progressive disclosure.** Hide advanced options until needed; default views are simple.
4. **Consistent feedback.** Every action gives immediate, calm feedback (loading, success, empty, error).
5. **Respect the user's attention.** No pop-ups, no bouncing elements, no auto-playing animations.
6. **Every pixel is intentional.** Spacing, color, motion, and typography all serve meaning.

---

## 2. Design System — Deep Craft

### 2.1 Color Tokens

Color follows a **tonal palette** philosophy (Material 3) applied to a single primary hue (Teal) and a neutral Zinc scale. The system uses surface containers, on-colors, and state layers rather than flat hex values.

#### 2.1.1 Primary — Teal Tonal Palette

| Token | Hex | HSL | Usage |
|-------|-----|-----|-------|
| `--teal-0` | `#ffffff` | 168 100% 100% | On-primary text / white |
| `--teal-50` | `#f0fdfa` | 168 80% 98% | Surface container highest, active bg |
| `--teal-100` | `#ccfbf1` | 168 75% 90% | Hover state layer on primary |
| `--teal-200` | `#99f6e4` | 168 70% 78% | Focus ring glow |
| `--teal-300` | `#5eead4` | 168 75% 64% | Decorative highlights |
| `--teal-400` | `#2dd4bf` | 168 70% 50% | Links hover |
| `--teal-500` | `#14b8a6` | 168 75% 40% | Links, secondary accents |
| `--teal-600` | `#0d9488` | 168 80% 31% | **Primary brand / Primary button** |
| `--teal-700` | `#0f766e` | 168 78% 26% | Primary hover / pressed |
| `--teal-800` | `#115e59` | 168 75% 22% | Strong emphasis |
| `--teal-900` | `#134e4a` | 168 70% 18% | Text on light teal surfaces |

#### 2.1.2 Neutral — Zinc Tonal Palette

| Token | Hex | Usage |
|-------|-----|-------|
| `--zinc-0` | `#ffffff` | Cards, dialogs, inputs |
| `--zinc-50` | `#fafafa` | Page background |
| `--zinc-100` | `#f4f4f5` | Hover surfaces, dividers |
| `--zinc-200` | `#e4e4e7` | Borders, separators |
| `--zinc-300` | `#d4d4d8` | Disabled borders, placeholder icons |
| `--zinc-400` | `#a1a1aa` | Tertiary text, inactive icons |
| `--zinc-500` | `#71717a` | Secondary text, metadata |
| `--zinc-600` | `#52525b` | Body text, labels |
| `--zinc-700` | `#3f3f46` | Strong secondary text |
| `--zinc-800` | `#27272a` | Primary text, headings |
| `--zinc-900` | `#18181b` | Maximum emphasis text |

#### 2.1.3 Surface & On-Color Tokens

| Token | Value | Usage |
|-------|-------|-------|
| `--surface-page` | `--zinc-50` | App canvas |
| `--surface-card` | `--zinc-0` | Cards, panels |
| `--surface-container-low` | `--zinc-50` | Subtle grouped backgrounds |
| `--surface-container` | `--zinc-100` | Chips, tags, hover rows |
| `--surface-container-high` | `--zinc-0` | Elevated cards, modals |
| `--surface-container-highest` | `--teal-50` | Active/selected rows |
| `--on-surface` | `--zinc-900` | Text on surfaces |
| `--on-surface-variant` | `--zinc-600` | Secondary text on surfaces |
| `--on-primary` | `--zinc-0` | Text on primary buttons |
| `--outline` | `--zinc-200` | Borders, dividers |
| `--outline-variant` | `--zinc-100` | Subtle separators |

#### 2.1.4 Semantic Colors

| Token | Hex | On-Color | Usage |
|-------|-----|----------|-------|
| `--success` | `#10b981` | `#ffffff` | Success, mastered |
| `--success-container` | `#ecfdf5` | `#065f46` | Success backgrounds |
| `--warning` | `#f59e0b` | `#ffffff` | Pending, warning |
| `--warning-container` | `#fffbeb` | `#92400e` | Warning backgrounds |
| `--error` | `#ef4444` | `#ffffff` | Errors, wrong answers |
| `--error-container` | `#fef2f2` | `#991b1b` | Error backgrounds |
| `--info` | `#6366f1` | `#ffffff` | Informational |
| `--info-container` | `#eef2ff` | `#3730a3` | Info backgrounds |

#### 2.1.5 State Layer Opacity

Following Material 3 state layers, overlays are applied on hover/press/focus rather than changing the base color directly.

| State | Opacity | Usage |
|-------|---------|-------|
| Hover | 8% black overlay | `--state-hover: rgba(0,0,0,0.08)` |
| Pressed / Active | 12% black overlay | `--state-pressed: rgba(0,0,0,0.12)` |
| Focus | 12% primary overlay | `--state-focus: rgba(13,148,136,0.12)` |
| Drag | 16% black overlay | `--state-drag: rgba(0,0,0,0.16)` |
| Disabled | 38% opacity | `opacity: 0.38` |

> On primary-colored surfaces, use **white** overlays instead of black.

---

### 2.2 Typography

Type is the primary carrier of hierarchy. We use a harmonious scale based on a 1.125 (major second) ratio with rounding to the nearest pixel for clean implementation.

#### 2.2.1 Font Family

```css
--font-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
  "Helvetica Neue", Arial, "Noto Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif;
--font-mono: "SF Mono", "Fira Code", "Fira Mono", "Roboto Mono", monospace;
```

#### 2.2.2 Type Scale

| Token | Size | Line Height | Weight | Letter Spacing | Usage |
|-------|------|-------------|--------|----------------|-------|
| `--text-hero` | 32px | 40px | 700 | -0.03em | Login title, empty state hero |
| `--text-display` | 28px | 36px | 700 | -0.02em | Page hero title |
| `--text-heading-1` | 22px | 28px | 600 | -0.02em | Page title |
| `--text-heading-2` | 18px | 24px | 600 | -0.01em | Section title |
| `--text-heading-3` | 15px | 20px | 600 | -0.01em | Card title |
| `--text-heading-4` | 13px | 16px | 600 | 0 | Sub-section, label |
| `--text-body-large` | 15px | 24px | 400 | 0 | Lead paragraphs |
| `--text-body` | 14px | 22px | 400 | 0 | Body text, table content |
| `--text-ui` | 13px | 18px | 500 | 0 | Buttons, menu items, tabs |
| `--text-small` | 12px | 16px | 400 | 0.01em | Captions, timestamps, helper |
| `--text-micro` | 11px | 14px | 600 | 0.02em | Badges, tags, table headers |

#### 2.2.3 Typography Rules

- Headings use **negative letter-spacing** for tighter, more editorial feel.
- Chinese body text minimum weight is **400**; never use 300 for Chinese.
- Line-height for headings: **1.25–1.3**; for body: **1.55–1.6**.
- Maximum readable line length: **65ch** for body text (Apple HIG readability).
- Monospace is reserved for code, file names, batch IDs, file sizes, timestamps.
- **Never use purely uppercase Chinese text.** Uppercase styling is allowed only for English micro labels (table headers, badges).

#### 2.2.4 Font Weight Usage

| Weight | Usage |
|--------|-------|
| 400 | Body text, descriptions, table content |
| 500 | UI labels, buttons, menu items, tabs, emphasized body |
| 600 | Headings, card titles, section labels, table headers |
| 700 | Page hero, display numbers, brand emphasis |

---

### 2.3 Spacing — The 4-Point Grid

The entire UI is built on a **4-point grid** (Ant Design / Material baseline). Every dimension — margin, padding, gap, height, width — must be divisible by 4. This creates visual rhythm and makes handoff effortless.

#### 2.3.1 Spacing Tokens

| Token | Value | Usage |
|-------|-------|-------|
| `--space-1` | 4px | Icon-text gap, tight inline spacing |
| `--space-2` | 8px | Compact gaps, small internal padding |
| `--space-3` | 12px | Default internal gaps, form label margin |
| `--space-4` | 16px | Card padding, section gaps, list item padding |
| `--space-5` | 20px | Medium section padding |
| `--space-6` | 24px | Page content padding, dialog padding |
| `--space-8` | 32px | Large section gaps, card grids |
| `--space-10` | 40px | Hero spacing, major section breaks |
| `--space-12` | 48px | Empty state padding, modal large padding |
| `--space-16` | 64px | Maximum page spacing |

#### 2.3.2 Spacing Hierarchy

| Layer | Rule |
|-------|------|
| **Micro** | 4–8px between related items (icon + label, inline buttons). |
| **Component** | 12–16px inside a card or form group. |
| **Section** | 24–32px between distinct sections on a page. |
| **Page** | 48–64px for major page-level vertical rhythm. |

#### 2.3.3 Whitespace Rules

- **Equal breathing room:** card padding should equal or exceed internal element gaps.
- **Proximity law:** related elements are closer together than unrelated groups.
- **Measure twice:** when in doubt, use more whitespace, never less.
- **No 1px or 2px gaps.** Minimum intentional gap is 4px.

---

### 2.4 Radius — Shape Scale

Shape is consistent and purposeful. Use larger radius for larger surfaces.

| Token | Value | Usage |
|-------|-------|-------|
| `--radius-sm` | 6px | Small buttons, tags, input fields, checkboxes |
| `--radius-md` | 8px | Default buttons, cards, small panels, menu items |
| `--radius-lg` | 12px | Cards, dialogs, large panels, dropdowns |
| `--radius-xl` | 16px | Modals, feature cards, drawers |
| `--radius-2xl` | 20px | Large modals, empty state cards |
| `--radius-full` | 9999px | Pills, avatars, status dots, badges |

#### 2.4.1 Shape Rules

- Container radius should relate to its size: small inputs get 6–8px, cards get 12px, modals get 16px.
- Never use `border-radius: 50%` for avatars; use `--radius-full` (9999px).
- Inner elements should have a smaller radius than their container by 4px (e.g. 12px card contains 8px buttons).

---

### 2.5 Shadows & Elevation

Elevation is expressed through subtle shadows and z-index layers. We use 5 levels.

| Level | Token | Shadow | Usage |
|-------|-------|--------|-------|
| 0 | `--shadow-none` | none | Resting surfaces (page bg) |
| 1 | `--shadow-1` | `0 1px 2px rgba(0,0,0,0.04)` | Resting cards, buttons |
| 2 | `--shadow-2` | `0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)` | Card hover, dropdowns |
| 3 | `--shadow-3` | `0 4px 6px -1px rgba(0,0,0,0.06), 0 2px 4px -1px rgba(0,0,0,0.04)` | Popovers, drawers |
| 4 | `--shadow-4` | `0 10px 15px -3px rgba(0,0,0,0.08), 0 4px 6px -2px rgba(0,0,0,0.04)` | Modals, toasts |
| 5 | `--shadow-5` | `0 20px 25px -5px rgba(0,0,0,0.08), 0 10px 10px -5px rgba(0,0,0,0.04)` | Full-screen overlays |

#### 2.5.1 Elevation Rules

- **Content surfaces** (cards, panels) rest at Level 1.
- **Hover** elevates to Level 2.
- **Floating surfaces** (menus, dropdowns) start at Level 3.
- **Blocking surfaces** (modals, drawers) use Level 4 or 5.
- Shadows always use **black** with low opacity; never colored shadows.

#### 2.5.2 Z-Index Scale

| Token | Value | Usage |
|-------|-------|-------|
| `--z-base` | 0 | Page content |
| `--z-sticky` | 100 | Sticky header |
| `--z-dropdown` | 200 | Dropdowns, popovers |
| `--z-drawer` | 300 | Drawers |
| `--z-modal` | 400 | Modals, dialogs |
| `--z-toast` | 500 | Notifications, toasts |
| `--z-tooltip` | 600 | Tooltips |

---

### 2.6 Grid & Layout — Page Proportions

#### 2.6.1 App Layout

```
┌─────────────────┬──────────────────────────────────────────────────────────┐
│                 │                                                          │
│   Sidebar       │                    Header (56px)                         │
│   (240px)       │                                                          │
│                 ├──────────────────────────────────────────────────────────┤
│                 │                                                          │
│                 │                                                          │
│                 │              Content Area (fluid)                        │
│                 │              padding: 24px                               │
│                 │              max-width: 1200px (optional)                │
│                 │                                                          │
│                 │                                                          │
└─────────────────┴──────────────────────────────────────────────────────────┘
```

#### 2.6.2 Layout Measurements

| Element | Value | Notes |
|---------|-------|-------|
| Sidebar width (expanded) | 240px | Fixed, non-scrollable horizontally |
| Sidebar width (collapsed) | 64px | Icon-only mode on tablet |
| Header height | 56px | Sticky top, z-index `--z-sticky` |
| Header horizontal padding | 24px | Desktop; 16px on mobile |
| Content padding | 24px | Desktop; 20px tablet; 16px mobile |
| Content max-width | 1200px | Centered for reading-heavy pages |
| Content full-width | 100% | Tables, dashboards |
| Minimum page height | 100vh | Layout fills viewport |

#### 2.6.3 12-Column Content Grid

Inside the content area, use a **12-column grid**:

| Property | Value |
|----------|-------|
| Columns | 12 |
| Gutter | 16px (default), 24px (loose), 12px (tight) |
| Margin | 0 (content area padding already provides it) |
| Column min-width | ~72px at 1200px container |

Common grid patterns:

| Pattern | Columns | Usage |
|---------|---------|-------|
| 1-column | 12 | Forms, reading, single card |
| 2-column | 6 + 6 | Summary page, plan detail |
| 3-column | 4 + 4 + 4 | Stats cards, feature cards |
| 4-column | 3 + 3 + 3 + 3 | Stats, quick actions |
| 1/3 + 2/3 | 4 + 8 | Plan list + detail, sidebar + main |
| 2/3 + 1/3 | 8 + 4 | Dashboard content + side panel |

#### 2.6.4 Breakpoints

| Name | Range | Sidebar | Content Padding | Notes |
|------|-------|---------|-----------------|-------|
| Mobile | < 768px | Hidden, slide-over | 16px | Single column |
| Tablet | 768px – 1279px | Collapsible icon-only | 20px | 1–2 columns |
| Desktop | 1280px – 1535px | Expanded 240px | 24px | Full layout |
| Large Desktop | ≥ 1536px | Expanded 240px | 32px | Centered max-width |

---

### 2.7 Iconography

Icons are functional, not decorative. They must be clear at small sizes and consistent in weight.

#### 2.7.1 Icon Library

- Use **Element Plus Icons Vue** (`@element-plus/icons-vue`).
- Prefer **outlined / stroke-style** icons. Avoid filled icons unless indicating active state.
- Default stroke weight in Element Plus is consistent; do not override.

#### 2.7.2 Icon Sizes

| Token | Size | Usage |
|-------|------|-------|
| `--icon-xs` | 12px | Inline text icons, tags |
| `--icon-sm` | 14px | Small buttons, form suffixes |
| `--icon-md` | 16px | Default button icons, list items, table actions |
| `--icon-lg` | 20px | Navigation menu items, page headers |
| `--icon-xl` | 24px | Empty state icons, feature illustrations |
| `--icon-2xl` | 32px | Large empty states, hero icons |
| `--icon-3xl` | 48px | Empty state hero, success/error states |

#### 2.7.3 Icon Color Rules

| Context | Color |
|---------|-------|
| Default inline | `--text-secondary` |
| Primary action | `--color-primary` |
| Active / selected | `--color-primary` |
| Disabled | `--text-disabled` |
| Danger | `--color-error` |
| Success | `--color-success` |
| Warning | `--color-warning` |

#### 2.7.4 Icon + Text Alignment

- Icon and text baseline-aligned.
- Gap between icon and text: **8px** (`--space-2`).
- Icon must be vertically centered within the line box.

---

### 2.8 Layout Components — Header & Sidebar

#### 2.8.1 Header

| Property | Value |
|----------|-------|
| Height | 56px |
| Background | `--surface-card` with `backdrop-filter: blur(12px)` |
| Border bottom | 1px solid `--outline-variant` |
| Horizontal padding | 24px desktop, 16px mobile |
| Z-index | `--z-sticky` |
| Shadow on scroll | `--shadow-1` (appears after 1px scroll) |

**Header structure:**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ [Menu]  Page Title                    [Search?] [Notify] [Avatar Name ▼]   │
└─────────────────────────────────────────────────────────────────────────────┘
```

- **Left:** hamburger toggle (tablet/mobile), page title (`--text-heading-1`).
- **Right:** optional global search, notification bell, user dropdown.
- User dropdown trigger: avatar (32px) + nickname + chevron-down (12px).
- Header title uses `--text-heading-1` (22px / 600).

#### 2.8.2 Sidebar

| Property | Value |
|----------|-------|
| Width | 240px expanded, 64px collapsed |
| Background | `--surface-card` |
| Right border | 1px solid `--outline-variant` |
| Vertical padding | 16px top, 16px bottom |
| Horizontal padding | 16px |

**Sidebar structure:**

```
┌────────────────────────┐
│  [Logo] AI Study       │  ← 56px height
├────────────────────────┤
│                        │
│  ◆ 首页                │
│  📄 学习资料            │
│                        │
│  AI 功能               │  ← group label
│    📝 AI 总结           │
│    💬 AI 问答           │
│    ✏️ AI 出题           │
│    📅 学习计划          │
│                        │
│  📕 错题本              │
│  🕐 历史记录            │
│                        │
├────────────────────────┤
│  [Avatar] Mini Profile │  ← optional bottom area
└────────────────────────┘
```

**Menu item specs:**

| Property | Value |
|----------|-------|
| Height | 40px |
| Padding | 0 12px |
| Border radius | 8px |
| Gap (icon ↔ text) | 12px |
| Font | `--text-ui` (13px / 500) |
| Icon size | 20px |

**Menu item states:**

| State | Background | Text | Icon |
|-------|------------|------|------|
| Default | transparent | `--text-secondary` | `--text-secondary` |
| Hover | `--surface-container` | `--text-primary` | `--text-primary` |
| Active | `--surface-container-highest` | `--color-primary` | `--color-primary` |
| Active indicator | 3px primary bar, 20px height, left edge | — | — |
| Disabled | transparent | `--text-disabled` | `--text-disabled` |

**Sub-menu item:**
- Height: 36px
- Padding-left: 44px (indent under group icon)
- Font: `--text-ui`

**Group label:**
- Font: `--text-micro` (11px / 600)
- Color: `--text-tertiary`
- Uppercase (English) or normal (Chinese)
- Padding: 16px 12px 8px

---

### 2.9 Motion & Transition

Motion should be fast, purposeful, and never attention-seeking. Animations are used to explain state changes, not to decorate.

#### 2.9.1 Duration Scale

| Token | Value | Usage |
|-------|-------|-------|
| `--duration-instant` | 0ms | No animation (preferred for snappy tools) |
| `--duration-fast` | 100ms | Micro-interactions: color change, border change |
| `--duration-normal` | 150ms | Hover, focus, button press, opacity |
| `--duration-emphasis` | 200ms | Card hover, dropdown open, sidebar collapse |
| `--duration-complex` | 300ms | Modal open, drawer slide, page transition |

#### 2.9.2 Easing Curves

| Token | Curve | Usage |
|-------|-------|-------|
| `--ease-default` | `cubic-bezier(0.4, 0, 0.2, 1)` | Default transitions (Material standard) |
| `--ease-in` | `cubic-bezier(0.4, 0, 1, 1)` | Exiting elements |
| `--ease-out` | `cubic-bezier(0, 0, 0.2, 1)` | Entering elements |
| `--ease-elastic` | `cubic-bezier(0.34, 1.56, 0.64, 1)` | Subtle bounce (rare, e.g. toggle) |

#### 2.9.3 Transition Property Rules

- Prefer animating only `transform` and `opacity` for 60fps performance.
- For color/border/shadow changes, use `--duration-fast` or `--duration-normal`.
- Avoid animating `width`, `height`, `top`, `left`, `margin`.

#### 2.9.4 Common Transition Patterns

| Element | Transition |
|---------|------------|
| Button hover | `background-color 150ms var(--ease-default)` |
| Button active | `transform: scale(0.98)` + `background-color 100ms` |
| Card hover | `transform: translateY(-1px)` + `box-shadow 150ms` |
| Input focus | `border-color 100ms, box-shadow 100ms` |
| Menu item hover | `background-color 100ms` |
| Modal open | `opacity 200ms var(--ease-out), transform 200ms var(--ease-out)` |
| Drawer open | `transform 200ms var(--ease-out)` |
| Dropdown open | `opacity 100ms, transform 100ms` |
| Skeleton shimmer | `opacity 1.5s infinite` |

---

### 2.10 Button

Buttons are the primary call-to-action surfaces. They must be immediately recognizable, tactile, and consistent.

#### 2.10.1 Button Sizes

| Size | Height | Padding | Font | Icon Size | Radius |
|------|--------|---------|------|-----------|--------|
| Small | 28px | 0 12px | `--text-small` (12px) | 12px | 6px |
| Default | 36px | 0 16px | `--text-ui` (13px) | 16px | 8px |
| Large | 44px | 0 20px | `--text-ui` (13px) | 18px | 10px |

#### 2.10.2 Button Variants

| Variant | Background | Border | Text | Hover (state layer) | Active |
|---------|------------|--------|------|---------------------|--------|
| Primary | `--color-primary` | none | `--on-primary` | white overlay 8% | scale 0.98, white overlay 12% |
| Secondary | `--surface-card` | 1px `--outline` | `--text-primary` | black overlay 8% | scale 0.98 |
| Tertiary / Text | transparent | none | `--text-secondary` | `--surface-container` | scale 0.98 |
| Ghost | transparent | none | `--color-primary` | `--teal-50` | scale 0.98 |
| Danger | `--color-error` | none | `--on-primary` | white overlay 8% | scale 0.98 |
| Danger Secondary | `--surface-card` | 1px `--color-error` | `--color-error` | `--color-error-bg` | scale 0.98 |

#### 2.10.3 Icon Button

| Size | Dimensions | Radius | Icon Size |
|------|------------|--------|-----------|
| Small | 28px × 28px | 6px | 14px |
| Default | 32px × 32px | 8px | 16px |
| Large | 40px × 40px | 10px | 20px |

#### 2.10.4 Button States

| State | Rule |
|-------|------|
| Default | Resting at elevation 1 if on page bg. |
| Hover | State layer + optional shadow-2 for cards. |
| Active / Pressed | `transform: scale(0.98)` + deeper state layer. |
| Focus | 2px ring `--color-primary` with 2px offset. |
| Disabled | `opacity: 0.38`, `cursor: not-allowed`, no hover. |
| Loading | Spinner replaces left icon (or appears left), button stays same size, disabled interactions. |

#### 2.10.5 Button Group

- Gap between grouped buttons: **8px**.
- Primary action on the **right** (destructive actions on the far right with confirmation).
- In dialogs: primary right-aligned, secondary to its left.

---

### 2.11 Input / Form

Form controls must feel responsive and accessible. Every input is a clear invitation to act.

#### 2.11.1 Input Sizes

| Size | Height | Padding | Font | Radius |
|------|--------|---------|------|--------|
| Small | 32px | 0 12px | `--text-small` | 6px |
| Default | 36px | 0 12px | `--text-body` | 8px |
| Large | 44px | 0 16px | `--text-body` | 10px |

#### 2.11.2 Input States

| State | Border | Background | Shadow |
|-------|--------|------------|--------|
| Default | 1px `--outline` | `--surface-card` | none |
| Hover | 1px `--zinc-300` | `--surface-card` | none |
| Focus | 1px `--color-primary` | `--surface-card` | `0 0 0 3px var(--color-primary-ring)` |
| Error | 1px `--color-error` | `--surface-card` | `0 0 0 3px rgba(239,68,68,0.1)` |
| Disabled | 1px `--outline` | `--surface-container-low` | none |
| Filled | 1px `--outline` | `--surface-card` | none |

#### 2.11.3 Textarea

- Min-height: 80px.
- Padding: 12px.
- Border radius: 8px.
- Auto-resize up to 240px, then scroll.
- Line-height: 1.6.

#### 2.11.4 Label & Helper Text

| Element | Font | Color | Margin |
|---------|------|-------|--------|
| Label | `--text-ui` (13px / 500) | `--text-secondary` | bottom 6px |
| Required marker | `--text-error` | `--color-error` | left 2px |
| Helper text | `--text-small` | `--text-tertiary` | top 6px |
| Error text | `--text-small` | `--color-error` | top 6px |

#### 2.11.5 Prefix / Suffix

- Prefix/suffix icon color: `--text-tertiary`.
- Padding-left/right when icon present: 36px.
- Suffix text (e.g. unit): `--text-small` `--text-secondary`.

#### 2.11.6 Checkbox / Radio / Switch

- Checkbox size: 16px × 16px, radius 4px.
- Radio size: 16px × 16px, full radius.
- Switch width: 44px, height: 24px, radius full.
- Checked: `--color-primary` background with white check/knob.
- Focus ring: 2px `--color-primary`.

#### 2.11.7 Select / Dropdown

- Same height/padding as input.
- Dropdown panel: radius 10px, shadow-3.
- Item height: 36px.
- Item hover: `--surface-container`.
- Item selected: `--surface-container-highest` + `--color-primary` text.

---

### 2.12 Card

Cards are the primary content containers. They must be uniform, scannable, and tactile.

#### 2.12.1 Card Sizes

| Token | Padding | Min Height | Usage |
|-------|---------|------------|-------|
| `--card-compact` | 16px | auto | List items, small stats |
| `--card-default` | 20px | auto | Standard content cards |
| `--card-comfortable` | 24px | auto | Feature cards, modals |
| `--card-spacious` | 32px | auto | Empty states, hero cards |

#### 2.12.2 Card Structure

```
┌─────────────────────────────────────┐
│ [Icon] Card Title          [Action] │  ← Header (optional)
│ Description line                    │
├─────────────────────────────────────┤
│                                     │
│           Card Content              │
│                                     │
├─────────────────────────────────────┤
│ [Secondary]        [Primary]        │  ← Footer (optional)
└─────────────────────────────────────┘
```

#### 2.12.3 Card Specs

| Property | Value |
|----------|-------|
| Background | `--surface-card` |
| Border | 1px solid `--outline` |
| Border radius | 12px |
| Shadow | `--shadow-1` |
| Hover shadow (if clickable) | `--shadow-2` |
| Hover transform (if clickable) | `translateY(-1px)` |
| Transition | `box-shadow 150ms, transform 150ms` |

#### 2.12.4 Card Header

- Padding-bottom: 16px.
- Border-bottom: 1px solid `--outline-variant` (only if content follows).
- Title: `--text-heading-3`.
- Action area: right-aligned, gap 8px.

#### 2.12.5 Card Footer

- Padding-top: 16px.
- Border-top: 1px solid `--outline-variant`.
- Actions right-aligned by default.

---

### 2.13 Table

Tables are for dense, scannable data. They must be readable and actionable.

#### 2.13.1 Table Dimensions

| Property | Value |
|----------|-------|
| Header height | 44px |
| Row height | 48px |
| Cell padding | 12px 16px |
| Header padding | 12px 16px |
| Font (header) | `--text-micro` (11px / 600 / uppercase English) |
| Font (cell) | `--text-body` (14px / 400) |
| Header color | `--text-tertiary` |
| Row border | 1px solid `--outline-variant` (bottom only) |
| Header border | 1px solid `--outline` (bottom only) |

#### 2.13.2 Table Header

- Background: transparent or `--surface-container-low`.
- Text: uppercase for English column titles, normal for Chinese.
- Sort icon: 12px, `--text-tertiary`, active state `--color-primary`.
- Checkbox column width: 44px.

#### 2.13.3 Table Row States

| State | Background |
|-------|------------|
| Default | transparent |
| Hover | `--surface-hover` |
| Selected | `--surface-active` |
| Selected hover | blend of active + hover state layer |
| Disabled | `--surface-container-low`, opacity 0.6 |

#### 2.13.4 Table Cells

- Primary text left-aligned.
- Numbers and dates right-aligned or centered based on context.
- Status tags centered or left-aligned.
- Actions column right-aligned, icons with 8px gap.

#### 2.13.5 Table Empty State

- Rendered inside table body.
- Padding: 48px.
- Icon 48px, title, description, optional action.

#### 2.13.6 Pagination

- Height: 36px.
- Position: right-aligned below table.
- Margin-top: 16px.
- Use Element Plus pagination with primary color `--color-primary`.

---

### 2.14 Tag / Badge / Chip

Small labels for status, categories, and metadata.

#### 2.14.1 Tag Specs

| Property | Value |
|----------|-------|
| Height | 22px |
| Padding | 0 8px |
| Border radius | 6px |
| Font | `--text-micro` (11px / 600) |
| Gap in group | 8px |

#### 2.14.2 Tag Variants

| Variant | Background | Border | Text |
|---------|------------|--------|------|
| Default | `--surface-container` | none | `--text-secondary` |
| Primary | `--teal-50` | none | `--teal-700` |
| Success | `--color-success-bg` | none | `--color-success` |
| Warning | `--color-warning-bg` | none | `--color-warning` |
| Error | `--color-error-bg` | none | `--color-error` |
| Info | `--color-info-bg` | none | `--color-info` |
| Outlined | transparent | 1px `--outline` | `--text-secondary` |

#### 2.14.3 Badge

- Dot size: 8px.
- Count badge: min-width 18px, height 18px, radius full.
- Font: `--text-micro` (11px / 600), white text.
- Background: `--color-error` for notifications, `--color-primary` for counts.

---

### 2.15 Empty State

Empty states must be helpful, never punitive. They guide the user to the next action.

#### 2.15.1 Empty State Layout

```
┌──────────────────────────────────────────┐
│                                          │
│              [ Icon 48px ]               │
│                                          │
│           暂无学习资料                   │
│     上传你的第一份资料，开始学习之旅       │
│                                          │
│         [ + 上传资料 ]                   │
│                                          │
└──────────────────────────────────────────┘
```

#### 2.15.2 Empty State Specs

| Property | Value |
|----------|-------|
| Container padding | 48px (compact), 64px (spacious) |
| Icon size | 48px default, 64px for page-level |
| Icon color | `--text-tertiary` |
| Title | `--text-heading-3` |
| Description | `--text-small`, `--text-secondary`, max-width 320px |
| Action margin-top | 20px |

#### 2.15.3 Empty State Variants

| Context | Icon | Action |
|---------|------|--------|
| No materials | Document | Upload button |
| No chat history | ChatDotRound | Start chat button |
| No quiz results | EditPen | Generate quiz button |
| No wrong questions | CircleCheck | Celebrate / back to quiz |
| No plan | Calendar | Create plan button |
| Search no results | Search | Clear filters link |

#### 2.15.4 Rules

- Always explain **why it's empty** and **what to do next**.
- Use the same icon library; no custom illustrations unless provided.
- Action is optional but preferred when a clear next step exists.

---

### 2.16 Loading

Loading states must reduce perceived wait time and prevent layout shift.

#### 2.16.1 Loading Patterns

| Pattern | Usage |
|---------|-------|
| Skeleton | Page initial load, card lists, tables. |
| Spinner | Inline button loading, small area refresh. |
| Progress bar | File upload, AI generation, plan creation. |
| Inline shimmer | Search suggestions, dynamic content. |

#### 2.16.2 Skeleton Specs

| Property | Value |
|----------|-------|
| Base color | `--surface-container` |
| Shimmer color | `--surface-hover` |
| Border radius | 4px for lines, 8px for cards, full for avatars |
| Line height | 14px (body), 20px (heading) |
| Animation | `opacity 0.5 ↔ 1` or translateX shimmer |
| Duration | 1.5s infinite |

#### 2.16.3 Spinner Specs

| Size | Usage |
|------|-------|
| 12px | Inline text loading |
| 16px | Button spinner, small areas |
| 20px | Card loading overlay |
| 32px | Page section loading |
| 48px | Full-page loading |

#### 2.16.4 Progress Bar

| Property | Value |
|----------|-------|
| Height | 6px |
| Radius | 3px (full if capped) |
| Track color | `--surface-container` |
| Fill color | `--color-primary` |
| Buffer / striped | Optional for indeterminate state |

#### 2.16.5 Loading Button

- Show spinner left of label.
- Keep button width stable (reserve spinner space).
- Disable clicks during loading.
- Spinner color: white on primary, `--text-secondary` on secondary.

#### 2.16.6 AI Generation Loading

For AI summary / quiz / plan generation, show a **multi-step progress indicator**:

```
[====>          ]  35%  正在分析文档...
步骤：读取文档 → 提取重点 → 生成内容 → 格式化输出
```

- Use progress bar + status text + optional step list.
- Disable the generate button during generation.
- Show skeleton preview of the result area.

---

### 2.17 Dialog / Drawer / Notification

#### 2.17.1 Dialog

| Property | Value |
|----------|-------|
| Background | `--surface-card` |
| Border radius | 16px |
| Padding | 24px |
| Shadow | `--shadow-4` |
| Backdrop | `rgba(0,0,0,0.35)` + `backdrop-filter: blur(2px)` |
| Max width | 360px small, 480px default, 640px large |
| Header font | `--text-heading-2` |

#### 2.17.2 Drawer

| Property | Value |
|----------|-------|
| Position | right |
| Width | 420px default, 560px wide, 360px narrow |
| Background | `--surface-card` |
| Border radius | 16px 0 0 16px |
| Shadow | `--shadow-4` |
| Header height | 56px |
| Content padding | 24px |

#### 2.17.3 Notification / Toast

| Property | Value |
|----------|-------|
| Position | top-right, 16px offset |
| Width | 360px max |
| Border radius | 12px |
| Padding | 16px |
| Shadow | `--shadow-4` |
| Icon size | 20px |
| Auto-dismiss | 4s (success/info), persistent (error) |

---

### 2.18 Focus & Accessibility

- **Minimum touch target:** 36 × 36px (Apple HIG recommendation; 44px ideal).
- **Focus ring:** 2px solid `--color-primary`, 2px offset.
- **Focus visible:** only show focus ring on keyboard navigation, not mouse click.
- **Color contrast:** minimum 4.5:1 for normal text, 3:1 for large text/UI components.
- **Don't rely on color alone:** pair status colors with icons or text.
- **Reduced motion:** respect `prefers-reduced-motion: reduce` and disable non-essential animations.

---

## 3. Information Architecture

### 3.1 Navigation Structure

```
┌─────────────────────────────────────────────────────────────┐
│  Logo                                                       │
├─────────────────────────────────────────────────────────────┤
│  首页              /dashboard                                │
│  学习资料          /material                                 │
│  ─────────────────────────────────────────                  │
│  AI 功能                                                    │
│    AI 总结         /ai/summary                              │
│    AI 问答         /ai/chat                                 │
│    自动出题         /ai/quiz                                │
│    学习计划         /ai/plan                                │
│  ─────────────────────────────────────────                  │
│  错题本            /quiz/wrong                              │
│  历史记录          /history                                 │
└─────────────────────────────────────────────────────────────┘
```

Top-right user menu:

```
用户头像 + 昵称
├─ 用户中心   /profile
├─ 设置      /settings
└─ 退出登录
```

### 3.2 Page Relationship Diagram

```
                      ┌─────────────┐
                      │   /login    │
                      └──────┬──────┘
                             │ 登录成功
                             ▼
                      ┌─────────────┐
                      │  App Shell  │
                      │   Layout    │
                      └──────┬──────┘
                             │
        ┌────────┬───────────┼───────────┬────────┐
        ▼        ▼           ▼           ▼        ▼
   /dashboard  /material   /ai/*     /quiz/wrong /history
        │           │       │            │          │
        │           ▼       ▼            │          │
        │       /material/:id (drawer)   │          │
        │       /ai/summary              │          │
        │       /ai/chat                 │          │
        │       /ai/quiz                 │          │
        │       /ai/plan                 │          │
        │                                │          │
        ▼                                ▼          ▼
   /profile (user menu)            /settings (user menu)
```

### 3.3 Page Hierarchy

| Level | Page | Description |
|-------|------|-------------|
| L0 | Login | Public entry, no sidebar. |
| L1 | Layout | Authenticated shell: sidebar + header + router-view. |
| L2 | Dashboard | Home, overview, quick actions. |
| L2 | Material | List + upload + detail drawer. |
| L2 | AI Summary | Generate and read summaries. |
| L2 | AI Chat | RAG Q&A conversation. |
| L2 | AI Quiz | Generate quiz + take quiz + review. |
| L2 | Study Plan | Plan list + plan detail. |
| L2 | Wrong Question | Filterable wrong-question bank. |
| L2 | History | Cross-module activity history. |
| L2 | Profile | User info, edit profile. |
| L2 | Settings | Preferences, password, notifications. |

### 3.4 Key User Flows

#### Flow A: First-time user uploads a document and summarizes it

1. `/login` → `/dashboard`
2. Click "上传资料" (or navigate `/material`)
3. Upload file → see processing status → file becomes "Ready"
4. Click "AI 总结" on the material row → navigate `/ai/summary?materialId={id}`
5. Click "生成总结" → see loading → read summary

#### Flow B: User asks questions about a document

1. Navigate `/ai/chat`
2. Select material from dropdown
3. Type question → submit
4. Receive streaming answer with source citations
5. Continue conversation or start new

#### Flow C: User takes a quiz

1. Navigate `/ai/quiz`
2. Select material → click "生成练习"
3. Answer questions (single-choice / judge / short-answer)
4. Submit → see score + explanations
5. Wrong questions auto-added to `/quiz/wrong`

#### Flow D: User reviews wrong questions

1. Navigate `/quiz/wrong`
2. Filter by material / mastered status
3. Review question + explanation
4. Mark as mastered → card removed or badge updated

---

## 4. Page Designs

### 4.1 Login Page (`/login`)

#### Purpose
Authenticate the user. Set a calm, premium first impression.

#### Layout
- Full viewport, light gray background (`--surface-page`).
- Centered card, max-width 400px.
- Left side (≥1280px): optional abstract illustration / product value panel, 40% width.
- Card contains: logo, title, tabs (login/register), form, submit button, forgot password link.

#### Components
- Logo mark + product name at top of card.
- Tabs: 登录 / 注册.
- Form fields: 邮箱 / 用户名, 密码, 确认密码 (register only).
- Primary button: "登录" / "注册".
- Inline validation under each field.

#### Interactions
- Press Enter in password field submits form.
- Tab switch resets validation but preserves email.
- Login success: toast + redirect to `/dashboard`.
- Login failure: inline error below button.

#### Information Hierarchy
1. Logo + product name (top, small).
2. Title "欢迎回来" (large).
3. Tabs (medium).
4. Form fields (primary content).
5. Submit button.
6. Secondary links (small).

#### Why This Design
A centered login card is familiar and task-focused. Light background keeps it professional. No AI visual cues — just a clean SaaS login.

---

### 4.2 Dashboard (`/dashboard`)

#### Purpose
Give the user an at-a-glance overview of their learning state and the fastest path to action.

#### Layout
- Page header: greeting + date/subtitle.
- Stats row: 4 cards in a grid.
- Content grid below: 2/3 + 1/3 split.
  - Left (2/3): Quick actions + Recent activity / Continue learning.
  - Right (1/3): Today's plan + Shortcuts.

#### Components
- **Welcome header**: dynamic greeting based on time of day.
- **Stat cards**: 资料数, 问答次数, 练习数, 错题数. Each with icon, value, label, trend indicator (optional).
- **Quick actions**: 4 action cards (上传资料, AI 问答, AI 出题, 学习计划).
- **Continue learning**: list of recent materials with progress bars.
- **Today's tasks**: from current study plan, checkable mini-list.
- **Recent activity**: timeline of recent AI actions.

#### Interactions
- Click stat card → navigate to corresponding page.
- Click quick action card → navigate.
- Click recent material → open material detail drawer or navigate `/ai/chat?materialId={id}`.
- Task checkbox → mark plan task complete (API call).

#### Information Hierarchy
1. Greeting (personal, high).
2. Stats (scannable numbers).
3. Quick actions (primary tasks).
4. Recent / Continue learning (context).
5. Today's plan (motivation).

#### Why This Design
Dashboards fail when they become busy. By separating "state" (stats) from "action" (quick actions) and "context" (recent/plan), the user immediately knows where to go. The design avoids a chat-first layout.

---

### 4.3 学习资料 (`/material`)

#### Purpose
Manage learning materials: upload, browse, search, delete, and select materials for AI features.

#### Layout
- Page header: title + description + "上传资料" primary button.
- Toolbar: search input (left), filter tags/status (center), view toggle list/grid (right).
- Main area:
  - List view: table with columns (文件名, 类型, 状态, 上传时间, 操作).
  - Grid view: cards with file icon, name, status badge, actions.
- Right drawer for material detail / preview.

#### Components
- Upload button → opens dialog or drag-and-drop zone.
- Status tags: 处理中 (warning), 可用 (success), 失败 (error).
- Row actions: 查看, AI 总结, AI 问答, 删除.
- Empty state for no materials.
- Drag-and-drop upload zone: dashed border, centered icon + text.

#### Interactions
- Upload: select file or drag file → show progress → show processing → update to ready.
- Click row → open detail drawer.
- Click "AI 总结" → navigate `/ai/summary?materialId={id}`.
- Click "AI 问答" → navigate `/ai/chat?materialId={id}`.
- Delete → confirm dialog → remove row.
- Search debounced 300ms.

#### Information Hierarchy
1. Page title + primary action.
2. Search / filters.
3. Material list (filename is primary).
4. Status + metadata.
5. Actions (secondary).

#### Why This Design
The material page is a file management surface. Table view is scannable; grid view is friendly for fewer files. Keeping AI actions one click away makes the material the center of the workflow, not the chat.

---

### 4.4 AI 总结 (`/ai/summary`)

#### Purpose
Generate and read AI-powered summaries of learning materials.

#### Layout
- Page header: title + description.
- Two-column layout (when material selected):
  - Left (320px): material selector + metadata.
  - Right (fluid): summary content area.
- If no material selected: empty state with selector.

#### Components
- Material selector: dropdown or compact list.
- "生成总结" primary button (disabled if no material or already generating).
- Summary output: rendered Markdown in a clean reading card.
- Regenerate / copy / download actions (secondary toolbar above content).
- Skeleton while generating.

#### Interactions
- Select material → load existing summary if available.
- Click "生成总结" → show progress → render Markdown.
- Regenerate → confirm if overwriting existing.
- Copy → clipboard toast.
- Download → export Markdown file.

#### Information Hierarchy
1. Material selector (context).
2. Summary content (primary).
3. Action toolbar (secondary).
4. Metadata / tags (tertiary).

#### Why This Design
The summary page is a reading experience. By placing the material selector on the left and the content on the right, it feels like a document editor / reader rather than a chat. Markdown is rendered cleanly with good typography.

---

### 4.5 AI 问答 (`/ai/chat`)

#### Purpose
Have a focused, RAG-powered Q&A conversation about a selected material.

#### Layout
- Three-panel layout:
  - Left sidebar (260px): conversation history list + "新对话" button.
  - Center (fluid): chat thread.
  - Right context panel (280px, collapsible): selected material info + cited sources.
- Or simplified two-panel: left history + center chat, with source citations inline.

#### Components
- Conversation list item: title + last message preview + timestamp.
- Chat thread: alternating user / assistant bubbles.
- User message: right-aligned, subtle background.
- Assistant message: left-aligned, white card, Markdown rendering.
- Source citations: small chip buttons below assistant message, clickable → highlight source in right panel.
- Input box: fixed bottom, textarea + send button. Auto-resize up to 5 lines.
- Material selector at top of chat.

#### Interactions
- Select material → start new conversation context.
- Type question → Enter to send (Shift+Enter for newline).
- Streaming response: word-by-word reveal.
- Click citation → open source snippet in right panel.
- New conversation → clear thread but keep material.
- Delete conversation from history list.

#### Information Hierarchy
1. Chat thread (content is king).
2. Input box (always accessible).
3. Material context (top bar).
4. History (left, collapsible).
5. Source citations (inline, secondary).

#### Why This Design
This is the only chat-like page in the app. By framing it with a material selector and source citations, it feels like "research Q&A" rather than "AI chatbot". The design is closer to a document research tool than a messaging app.

---

### 4.6 自动出题 (`/ai/quiz`)

#### Purpose
Generate practice questions from a material, take the quiz, and review results.

#### Layout
- Page header: title + description.
- Step-based flow:
  - Step 1: Select material + configure question counts (default 5单选, 3判断, 2简答) + difficulty.
  - Step 2: Answer questions (single page with numbered list).
  - Step 3: Review results (score circle + per-question breakdown).
- State is managed within the page.

#### Components
- Material selector.
- Question count inputs (number steppers).
- Difficulty segmented control: 简单 / 中等 / 困难.
- "生成练习" button.
- Question card: number badge, question text, options (radio / checkbox for multi-select if added / text area for short answer).
- Submit button at bottom.
- Result header: score circle, correct/incorrect counts.
- Explanation panel per question after submit.

#### Interactions
- Generate → loading → render questions.
- Answer radio/text → enable submit when all required answered.
- Submit → show scoring animation → switch to review mode.
- Wrong answers auto-sync to错题本.
- "再来一组" → regenerate or reset.
- "查看错题本" link.

#### Information Hierarchy
1. Configuration (step 1).
2. Question list (step 2).
3. Score + explanations (step 3).

#### Why This Design
Quizzes are task-based. A clear step flow reduces cognitive load. Showing explanations immediately after submission supports learning. Keeping configuration on the same page makes iteration fast.

---

### 4.7 错题本 (`/quiz/wrong`)

#### Purpose
Help users review and master questions they answered incorrectly.

#### Layout
- Page header: title + "今日复习" count.
- Toolbar: filter by material, filter by status (全部 / 未掌握 / 已掌握).
- Main area: card grid or list.
- Each card: question preview, your answer, correct answer, explanation, source material, "标记已掌握" button.

#### Components
- Filter dropdowns.
- Wrong question card with:
  - Status badge.
  - Question text (truncated).
  - Your answer vs correct answer.
  - "查看解析" expand button.
  - "标记已掌握" ghost button.
- Empty state when all mastered.

#### Interactions
- Filter change → reload list.
- Expand card → show full explanation.
- Mark mastered → card fades / badge changes / moves to "已掌握" tab.
- Click material name → navigate to material or chat.

#### Information Hierarchy
1. Header + progress indicator.
2. Filters.
3. Question cards (question first).
4. Answer comparison.
5. Explanation.
6. Actions.

#### Why This Design
Wrong questions are learning content, not just records. Cards make each question feel self-contained. Answer comparison is visually distinct (red vs green) to reinforce learning.

---

### 4.8 学习计划 (`/ai/plan`)

#### Purpose
Create and track AI-generated study plans.

#### Layout
- Page header: title + "新建计划" button.
- Two-column layout:
  - Left (360px): plan list (card selector).
  - Right (fluid): plan detail view.
- Plan detail:
  - Goal header + exam date + daily hours.
  - Progress bar (overall completion).
  - Task list grouped by day/week.
  - Each task: checkbox, title, estimated time, source material link.

#### Components
- Plan list card: title, date range, progress bar, status.
- "新建计划" dialog: goal input, exam date picker, daily hours slider, material multi-select.
- Generate loading state with progress steps.
- Task checkbox.
- Empty state when no plan selected.

#### Interactions
- Click plan in list → load detail.
- New plan → fill form → generate → add to list → select it.
- Check task → update progress.
- Delete plan → confirm.
- Regenerate plan → replace tasks.

#### Information Hierarchy
1. Current plan goal (context).
2. Overall progress.
3. Task groups (days/weeks).
4. Individual tasks.
5. Plan list (left, navigation).

#### Why This Design
Study plans need both overview (progress) and detail (tasks). A two-column layout mirrors email/calendar apps, making it familiar. Task checkboxes provide immediate satisfaction and progress tracking.

---

### 4.9 历史记录 (`/history`)

#### Purpose
Provide a unified timeline of user activity across AI features.

#### Layout
- Page header: title + tabs (全部 / AI 问答 / AI 出题 / AI 总结 / 学习计划).
- Filter bar: material selector, date range.
- Main area: timeline or table.
- Each item: icon (feature type), title, material, timestamp, action link.

#### Components
- Tabs for feature type.
- Timeline items with left icon rail.
- "查看详情" link per item (opens relevant page).
- Empty state.

#### Interactions
- Tab change → filter list.
- Click item → navigate to relevant detail (chat, quiz result, summary, plan).
- Pagination or infinite scroll.

#### Information Hierarchy
1. Tabs (filter).
2. Timeline entries (time descending).
3. Action links.

#### Why This Design
History is for lookup. A timeline is scannable and naturally chronological. Linking each entry back to its source page keeps it useful.

---

### 4.10 用户中心 (`/profile`)

#### Purpose
View and edit user profile information.

#### Layout
- Page header: title.
- Card with profile form.
- Sections: 头像, 昵称, 邮箱, 用户名, 注册时间.

#### Components
- Avatar upload (click to change).
- Editable nickname.
- Read-only email/username.
- "保存" primary button.

#### Interactions
- Edit nickname → save → toast success.
- Upload avatar → preview → save.

#### Information Hierarchy
1. Avatar + name (identity).
2. Form fields.
3. Save action.

#### Why This Design
A simple, focused profile page. No unnecessary fields. Edit-in-place keeps interaction minimal.

---

### 4.11 设置 (`/settings`)

#### Purpose
Manage account preferences and security.

#### Layout
- Page header: title.
- Settings list card with sections:
  - 账号安全 (change password).
  - 通知偏好.
  - 外观 (light/dark/auto — optional).
  - 关于 / 版本.

#### Components
- Section headers.
- Form fields for password change.
- Toggle switches for preferences.
- "保存" buttons per section.

#### Interactions
- Password change: old + new + confirm → save.
- Toggles immediate save or batch save.

#### Information Hierarchy
1. Account security (most important).
2. Preferences.
3. About.

#### Why This Design
Settings is a standard SaaS page. Grouping by concern keeps it scannable. We do not overload it with AI-specific settings.

---

## 5. Desktop Wireframes (ASCII)

### 5.1 Login Page

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│                                                                              │
│                          ┌─────────────────────────────┐                     │
│                          │  ◆  AI Study Assistant      │                     │
│                          │                             │                     │
│                          │  欢迎回来                    │                     │
│                          │  登录后继续你的学习           │                     │
│                          │                             │                     │
│                          │  [ 登录 ] [ 注册 ]          │                     │
│                          │                             │                     │
│                          │  邮箱 / 用户名               │                     │
│                          │  ┌─────────────────────┐    │                     │
│                          │  │                     │    │                     │
│                          │  └─────────────────────┘    │                     │
│                          │                             │                     │
│                          │  密码                        │                     │
│                          │  ┌─────────────────────┐    │                     │
│                          │  │                     │    │                     │
│                          │  └─────────────────────┘    │                     │
│                          │                             │                     │
│                          │  [        登录        ]    │                     │
│                          │                             │                     │
│                          │  忘记密码？                  │                     │
│                          └─────────────────────────────┘                     │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 App Shell (Layout)

```
┌────────┬─────────────────────────────────────────────────────────────────────┐
│ Logo   │  页面标题                              [🔔] [头像 昵称 ▼]         │
├────────┤                                                                     │
│ 首页   │                                                                     │
│ 学习资料│                                                                     │
├────────┤                                                                     │
│ AI 功能│                                                                     │
│   总结 │                                                                     │
│   问答 │                        [ Router View ]                            │
│   出题 │                                                                     │
│   计划 │                                                                     │
├────────┤                                                                     │
│ 错题本 │                                                                     │
│ 历史记录│                                                                     │
└────────┴─────────────────────────────────────────────────────────────────────┘
```

### 5.3 Dashboard

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ 欢迎回来，Alex                                    2026年6月18日 星期三       │
│ 今天也要好好学习，继续加油吧                                                   │
├──────────────────────────────────────────────────────────────────────────────┤
│ ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐                       │
│ │ 📄 12    │  │ 💬 48    │  │ ✏️ 86    │  │ 📕 7     │                       │
│ │ 学习资料  │  │ AI 对话   │  │ 练习题目  │  │ 待复习错题│                       │
│ └──────────┘  └──────────┘  └──────────┘  └──────────┘                       │
├──────────────────────────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────┐  ┌──────────────────────────────┐   │
│ │ 快速开始                              │  │ 今日计划                      │   │
│ │                                      │  │                              │   │
│ │ [📤 上传资料] [💬 AI 问答]           │  │ □ 复习第三章                 │   │
│ │ [✏️ AI 出题]  [📅 学习计划]          │  │ □ 完成 5 道错题              │   │
│ │                                      │  │ □ 阅读总结文档               │   │
│ └──────────────────────────────────────┘  └──────────────────────────────┘   │
│ ┌──────────────────────────────────────┐  ┌──────────────────────────────┐   │
│ │ 继续学习                              │  │ 最近动态                      │   │
│ │                                      │  │                              │   │
│ │ ▶ 高等数学.pdf            [━━━─]     │  │ 14:32 生成了 AI 总结          │   │
│ │ ▶ 数据结构笔记.md         [━━━━]     │  │ 13:10 完成了一次练习          │   │
│ │ ▶ 英语阅读材料.pdf        [━━──]     │  │ 11:05 提问了 3 个问题         │   │
│ │                                      │  │                              │   │
│ └──────────────────────────────────────┘  └──────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 5.4 学习资料 (`/material`)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ 学习资料                              [搜索资料...] [状态▼] [⬜/☰] [+ 上传资料]│
├──────────────────────────────────────────────────────────────────────────────┤
│ 文件名              类型      状态        上传时间            操作             │
│ ───────────────────────────────────────────────────────────────────────────  │
│ 📄 高等数学.pdf      PDF      可用       2026-06-17        [总结][问答][⋯]   │
│ 📄 数据结构.md       Markdown 可用       2026-06-16        [总结][问答][⋯]   │
│ 📄 英语听力.mp3      Audio    处理中      2026-06-15        [─]               │
│ 📄 旧笔记.txt        Text     失败       2026-06-10        [重试][删除]       │
│                                                                              │
│                      [  1  2  3  ... 10  >  ]                                │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 5.5 AI 总结 (`/ai/summary`)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ AI 知识总结                                                                   │
│ 选择资料，生成结构化的知识总结                                                 │
├──────────────┬───────────────────────────────────────────────────────────────┤
│              │                                                               │
│ 选择资料 ▼   │   [复制] [重新生成] [下载]                                    │
│              │                                                               │
│ ◉ 高等数学.pdf│ ┌─────────────────────────────────────────────────────────┐   │
│ ○ 数据结构.md  │ │ # 高等数学 · 核心知识点                                    │   │
│ ○ 英语听力.mp3 │ │                                                         │   │
│              │ │ ## 1. 极限与连续                                          │   │
│ 资料信息      │ │ ...                                                       │   │
│ 页数: 120     │ │                                                         │   │
│ 大小: 2.4MB   │ │ ## 2. 导数与微分                                          │   │
│ 状态: 可用    │ │ ...                                                       │   │
│              │ │                                                         │   │
│ [生成总结]   │ │ ### 学习建议                                               │   │
│              │ │ ...                                                       │   │
│              │ └─────────────────────────────────────────────────────────┘   │
└──────────────┴───────────────────────────────────────────────────────────────┘
```

### 5.6 AI 问答 (`/ai/chat`)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ AI 文档问答                                       当前资料: [高等数学.pdf ▼]  │
├──────────────┬───────────────────────────────────────────────┬───────────────┤
│ 新对话        │                                               │ 资料信息       │
│              │  User: 请解释泰勒公式                            │ 高等数学.pdf   │
│ 今天的讨论    │                                               │ 120 页         │
│  ─ 泰勒公式  │  ─────────────────────────────────────────    │ 状态: 可用     │
│  ─ 极限应用  │  Assistant:                                    │                │
│              │  泰勒公式是用多项式逼近函数...                    │ 引用来源       │
│ 昨天的讨论    │                                               │ [1] 第 45 页   │
│  ─ 导数定义  │  [1] 相关内容来自资料第 45 页                   │ [2] 第 52 页   │
│              │                                               │                │
│              │  User: 能举个例子吗？                            ├───────────────┤
│              │                                               │                │
│              │  Assistant:                                    │                │
│              │  例如 sin(x) 在 x=0 附近...                     │                │
│              │                                               │                │
│              ├───────────────────────────────────────────────┤                │
│              │ [ 输入你的问题...                 ] [发送]     │                │
│              └───────────────────────────────────────────────┘                │
└──────────────┴───────────────────────────────────────────────┴───────────────┘
```

### 5.7 自动出题 (`/ai/quiz`)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ 自动出题                                                                      │
├──────────────────────────────────────────────────────────────────────────────┤
│ 步骤 1: 选择资料与配置                                                         │
│                                                                              │
│ 选择资料       [高等数学.pdf ▼]                                               │
│ 单选题数量     [ 5 ▲▼]                                                        │
│ 判断题数量     [ 3 ▲▼]                                                        │
│ 简答题数量     [ 2 ▲▼]                                                        │
│ 难度           [ 简单 | 中等 | 困难 ]                                         │
│                                                                              │
│              [      生成练习      ]                                           │
├──────────────────────────────────────────────────────────────────────────────┤
│ 步骤 2: 答题                                                                  │
│                                                                              │
│ Q1. 下列哪个函数在 x=0 处连续？                                               │
│   ○ A. f(x)=1/x                                                               │
│   ● B. f(x)=sin(x)                                                            │
│   ○ C. f(x)=1/x²                                                              │
│                                                                              │
│ Q2. 判断: 可导一定连续。                                                      │
│   ● 正确  ○ 错误                                                              │
│                                                                              │
│ Q3. 简述拉格朗日中值定理。                                                    │
│   ┌────────────────────────────────────────┐                                  │
│   │ ...                                    │                                  │
│   └────────────────────────────────────────┘                                  │
│                                                                              │
│              [      提交答案      ]                                           │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 5.8 错题本 (`/quiz/wrong`)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ 错题本                                  [全部资料▼] [全部状态▼]               │
├──────────────────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────────────────┐  │
│ │ ❌ 未掌握   高等数学.pdf                                                  │  │
│ │                                                                         │  │
│ │ Q: 下列哪个函数在 x=0 处不可导？                                          │  │
│ │ 你的答案: A                正确答案: C                                    │  │
│ │                                                                         │  │
│ │ [查看解析]                                              [标记已掌握]      │  │
│ └─────────────────────────────────────────────────────────────────────────┘  │
│ ┌─────────────────────────────────────────────────────────────────────────┐  │
│ │ ✅ 已掌握   数据结构.md                                                   │  │
│ │                                                                         │  │
│ │ Q: 二叉树的高度定义是什么？                                               │  │
│ │ 你的答案: 正确           正确答案: 正确                                   │  │
│ │                                                                         │  │
│ │ [查看解析]                                              [取消掌握]        │  │
│ └─────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 5.9 学习计划 (`/ai/plan`)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ 学习计划                                          [+ 新建计划]               │
├──────────────────────┬───────────────────────────────────────────────────────┤
│ 我的计划              │ 考研数学复习计划                                       │
│                      │ 考试日期: 2026-12-25   每日学习: 3 小时              │
│ ● 考研数学复习计划   │                                                      │
│    ████████░░ 65%    │ 总体进度 [████████████░░░░] 65%                      │
│ ○ 英语词汇冲刺       │                                                      │
│    ████░░░░░░ 30%    │ 第 1 周                                              │
│ ○ 数据结构复习       │   □ 完成极限与连续章节                                │
│    ██░░░░░░░░ 15%    │   ☑ 导数基础练习                                    │
│                      │   □ 错题回顾                                          │
│                      │                                                      │
│                      │ 第 2 周                                              │
│                      │   □ 微分中值定理                                      │
│                      │   □ 完成 20 道选择题                                  │
│                      │                                                      │
│                      │ [重新生成] [删除计划]                                │
└──────────────────────┴───────────────────────────────────────────────────────┘
```

### 5.10 历史记录 (`/history`)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ 历史记录                                          [全部 ▼] [资料▼] [日期▼]  │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  今天                                                                        │
│  💬  你提问了 "泰勒公式应用场景"          高等数学.pdf    14:32  [查看]      │
│  ✏️  完成了一次练习 (8/10)                数据结构.md     13:10  [查看]      │
│                                                                              │
│  昨天                                                                        │
│  📝  生成了 AI 总结                        英语听力.mp3   16:45  [查看]      │
│  📅  创建了学习计划 "考研数学复习"         高等数学.pdf    09:20  [查看]      │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 5.11 用户中心 (`/profile`)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ 用户中心                                                                      │
├──────────────────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────────────────┐  │
│ │                                                                         │  │
│ │                         [ 👤 头像 ]                                     │  │
│ │                       点击更换头像                                      │  │
│ │                                                                         │  │
│ │  昵称              ┌─────────────────┐                                  │  │
│ │                    │ Alex            │                                  │  │
│ │                    └─────────────────┘                                  │  │
│ │  邮箱              alex@example.com   (不可修改)                        │  │
│ │  用户名            alex2024           (不可修改)                        │  │
│ │  注册时间          2026-01-15                                         │  │
│ │                                                                         │  │
│ │                       [  保存修改  ]                                    │  │
│ │                                                                         │  │
│ └─────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 5.12 设置 (`/settings`)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ 设置                                                                          │
├──────────────────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────────────────┐  │
│ │ 账号安全                                                                 │  │
│ │ 当前密码    ┌─────────────────┐                                          │  │
│ │ 新密码      ┌─────────────────┐                                          │  │
│ │ 确认密码    ┌─────────────────┐                                          │  │
│ │                           [ 修改密码 ]                                   │  │
│ ├─────────────────────────────────────────────────────────────────────────┤  │
│ │ 通知偏好                                                                 │  │
│ │  [开关] 学习提醒                                                         │  │
│ │  [开关] 每周学习报告                                                     │  │
│ │                           [ 保存偏好 ]                                   │  │
│ ├─────────────────────────────────────────────────────────────────────────┤  │
│ │ 外观                                                                     │  │
│ │ 主题  [浅色 ○] [自动 ○] [深色 ○]                                        │  │
│ ├─────────────────────────────────────────────────────────────────────────┤  │
│ │ 关于                                                                     │  │
│ │ AI Study Assistant v0.0.1                                                │  │
│ └─────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Implementation Spec for Claude Code

### 6.1 Constraints

- **Framework:** Vue 3 + Vite + Element Plus.
- **State:** Pinia.
- **HTTP:** Axios (existing `/api` instance).
- **Icons:** Element Plus Icons Vue (already installed).
- **No new major dependencies.** Use existing markdown-it for rendering.
- **Preserve all existing backend API contracts.** Do not change request/response shapes.
- **Reuse existing `api/` modules** (`ai.js`, `material.js`, `quiz.js`, `history.js`) and add new ones only if needed.

### 6.2 File Structure

```
frontend/src/
├── api/                    # keep existing
├── assets/                 # add logo, empty-state illustrations
├── components/
│   ├── common/
│   │   ├── AppButton.vue
│   │   ├── AppCard.vue
│   │   ├── AppEmpty.vue
│   │   ├── AppLoading.vue
│   │   ├── AppModal.vue
│   │   ├── AppTag.vue
│   │   └── AppToast.vue
│   ├── layout/
│   │   ├── AppSidebar.vue
│   │   ├── AppHeader.vue
│   │   └── AppShell.vue
│   └── business/
│       ├── MaterialTable.vue
│       ├── MaterialCard.vue
│       ├── MaterialUploader.vue
│       ├── ChatThread.vue
│       ├── ChatInput.vue
│       ├── QuizQuestion.vue
│       ├── QuizResult.vue
│       ├── WrongQuestionCard.vue
│       ├── StudyPlanDetail.vue
│       ├── StudyPlanList.vue
│       └── ActivityTimeline.vue
├── composables/            # keep existing useMarkdown.js
├── router/
│   └── index.js            # add /profile, /settings
├── stores/
│   ├── user.js             # keep existing
│   └── ui.js               # optional sidebar collapse, theme
├── styles/
│   ├── theme.css           # replace with new tokens
│   ├── global.css          # keep utility classes
│   └── components.css      # component overrides
└── views/
    ├── Login.vue
    ├── Dashboard.vue
    ├── Material.vue
    ├── AiSummary.vue
    ├── AiChat.vue
    ├── AiQuiz.vue
    ├── AiPlan.vue
    ├── WrongQuestion.vue
    ├── History.vue
    ├── Profile.vue
    └── Settings.vue
```

### 6.3 CSS Variables

Replace `theme.css` with the following root variables. Map Element Plus variables to these tokens.

```css
:root {
  /* Primary Teal */
  --color-primary: #0d9488;
  --color-primary-hover: #0f766e;
  --color-primary-light: #f0fdfa;
  --color-primary-ring: rgba(13, 148, 136, 0.12);

  /* Neutrals */
  --color-text-primary: #18181b;
  --color-text-secondary: #52525b;
  --color-text-tertiary: #a1a1aa;
  --color-text-disabled: #d4d4d8;

  --surface-page: #fafafa;
  --surface-card: #ffffff;
  --surface-container-low: #fafafa;
  --surface-container: #f4f4f5;
  --surface-container-high: #ffffff;
  --surface-active: #f0fdfa;
  --surface-hover: #f4f4f5;

  --outline: #e4e4e7;
  --outline-variant: #f4f4f5;

  /* State layers */
  --state-hover: rgba(0, 0, 0, 0.08);
  --state-pressed: rgba(0, 0, 0, 0.12);
  --state-focus: rgba(13, 148, 136, 0.12);

  /* Semantic */
  --color-success: #10b981;
  --color-success-bg: #ecfdf5;
  --color-warning: #f59e0b;
  --color-warning-bg: #fffbeb;
  --color-error: #ef4444;
  --color-error-bg: #fef2f2;
  --color-info: #6366f1;
  --color-info-bg: #eef2ff;

  /* Spacing */
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 20px;
  --space-6: 24px;
  --space-8: 32px;
  --space-10: 40px;
  --space-12: 48px;
  --space-16: 64px;

  /* Radius */
  --radius-sm: 6px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;
  --radius-2xl: 20px;
  --radius-full: 9999px;

  /* Shadows */
  --shadow-1: 0 1px 2px rgba(0,0,0,0.04);
  --shadow-2: 0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04);
  --shadow-3: 0 4px 6px -1px rgba(0,0,0,0.06), 0 2px 4px -1px rgba(0,0,0,0.04);
  --shadow-4: 0 10px 15px -3px rgba(0,0,0,0.08), 0 4px 6px -2px rgba(0,0,0,0.04);
  --shadow-5: 0 20px 25px -5px rgba(0,0,0,0.08), 0 10px 10px -5px rgba(0,0,0,0.04);

  /* Z-index */
  --z-base: 0;
  --z-sticky: 100;
  --z-dropdown: 200;
  --z-drawer: 300;
  --z-modal: 400;
  --z-toast: 500;
  --z-tooltip: 600;

  /* Typography */
  --font-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
    "Helvetica Neue", Arial, "Noto Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif;
  --font-mono: "SF Mono", "Fira Code", "Fira Mono", "Roboto Mono", monospace;

  /* Layout */
  --sidebar-width: 240px;
  --sidebar-collapsed-width: 64px;
  --header-height: 56px;

  /* Motion */
  --duration-fast: 100ms;
  --duration-normal: 150ms;
  --duration-emphasis: 200ms;
  --duration-complex: 300ms;
  --ease-default: cubic-bezier(0.4, 0, 0.2, 1);
  --ease-in: cubic-bezier(0.4, 0, 1, 1);
  --ease-out: cubic-bezier(0, 0, 0.2, 1);
  --ease-elastic: cubic-bezier(0.34, 1.56, 0.64, 1);
}
```

### 6.4 Element Plus Overrides

Apply these overrides globally to align Element Plus with the Design System.

```css
:root {
  --el-color-primary: #0d9488;
  --el-color-primary-light-3: #14b8a6;
  --el-color-primary-light-5: #5eead4;
  --el-color-primary-light-7: #99f6e4;
  --el-color-primary-light-8: #ccfbf1;
  --el-color-primary-light-9: #f0fdfa;
  --el-color-primary-dark-2: #0f766e;

  --el-text-color-primary: #18181b;
  --el-text-color-regular: #52525b;
  --el-text-color-secondary: #a1a1aa;
  --el-text-color-placeholder: #d4d4d8;

  --el-bg-color: #ffffff;
  --el-bg-color-page: #fafafa;

  --el-border-color: #e4e4e7;
  --el-border-color-light: #f4f4f5;
  --el-border-color-lighter: #f4f4f5;

  --el-fill-color: #f4f4f5;
  --el-fill-color-light: #fafafa;
  --el-fill-color-lighter: #ffffff;

  --el-border-radius-base: 8px;
  --el-border-radius-small: 6px;
  --el-border-radius-round: 9999px;

  --el-menu-active-color: #0d9488;
  --el-menu-hover-bg-color: #f4f4f5;

  --el-card-border-color: #e4e4e7;
  --el-card-border-radius: 12px;

  --el-tag-border-radius: 6px;
}
```

### 6.5 Route Additions

Add to `router/index.js`:

```js
{
  path: 'profile',
  name: 'Profile',
  component: () => import('@/views/Profile.vue'),
  meta: { title: '用户中心' }
},
{
  path: 'settings',
  name: 'Settings',
  component: () => import('@/views/Settings.vue'),
  meta: { title: '设置' }
}
```

Connect the user dropdown in `AppHeader` to `/profile` and `/settings`.

### 6.6 Responsive Behavior

| Breakpoint | Sidebar | Content Padding | Notes |
|------------|---------|-----------------|-------|
| ≥1280px | Fixed 240px | 24px | Full desktop layout. |
| 768–1279px | Collapsible icon-only | 20px | Hamburger toggle in header. |
| <768px | Hidden slide-over | 16px | Single-column stack. |

For MVP, prioritize desktop. Tablet/mobile can be a single-column stack with the sidebar hidden behind a hamburger.

### 6.7 Accessibility

- Minimum touch target: 36×36px.
- Focus rings: 2px `--color-primary` with 2px offset.
- Color alone must not convey meaning; pair with icons/text.
- All form inputs must have associated labels.
- Modal/Dialog must trap focus and close on Escape.
- Use semantic HTML (`<nav>`, `<main>`, `<header>`, `<section>`).
- Respect `prefers-reduced-motion: reduce`.

### 6.8 Animation & Motion

- Keep motion subtle and purposeful.
- Default transition: `all 0.15s var(--ease-default)` for hover/focus.
- Page transitions: none for MVP.
- Loading skeletons: pulse opacity over 1.5s.
- Chat streaming: text reveal, no layout shift.
- Button press: scale 0.98 on active.
- Card hover: translateY(-1px) + shadow-2, duration 150ms.

### 6.9 Empty States (Global)

Every list/table must have a coherent empty state:

```
┌─────────────────────────────┐
│                             │
│        [ Icon 48px ]        │
│                             │
│      暂无学习资料           │
│   上传你的第一份资料        │
│   开始学习之旅              │
│                             │
│    [ + 上传资料 ]           │
│                             │
└─────────────────────────────┘
```

- Icon: `--text-tertiary`.
- Title: `--text-heading-3`.
- Description: `--text-secondary`, max-width 320px, centered.
- Action: primary button when an action is obvious.

### 6.10 Error States

- Inline field errors: red text below input.
- Page-level errors: centered card with error icon, message, and "重试" button.
- Network errors: handled by existing Axios interceptor (toast). Do not redesign.

### 6.11 New API Needs (Backend Unchanged)

The redesign should work with existing APIs. However, the following frontend-only data may need local state or minor backend additions later; for this design, assume they can be derived:

- Dashboard stats: derive from existing `/material/list`, `/history/chat`, `/history/quiz`, `/ai/quiz/wrong`.
- Today's plan tasks: derive from `/ai/plan` response.
- User profile: use existing `/user/profile` (check backend endpoint; if missing, display nickname from existing user store).

If backend endpoints for profile/settings do not exist, create placeholder UI that gracefully degrades (read-only fields).

---

## 7. Page-to-API Mapping

| Page | Existing API | Notes |
|------|--------------|-------|
| Login | `AuthController` | Keep existing login/register flow. |
| Dashboard | derived | Aggregate counts client-side. |
| Material | `material.js` | list, upload, delete, detail. |
| AI Summary | `generateSummary` | render Markdown. |
| AI Chat | `askQuestion` / `askQuestionStream` | render streaming Markdown. |
| AI Quiz | `generateQuiz`, `submitAnswers` | step flow. |
| Wrong Question | `getWrongQuestions`, `markWrongQuestionMastered` | card list. |
| Study Plan | `generatePlan` | plan detail page. |
| History | `history.js` | timeline view. |
| Profile | `UserController` | if available. |
| Settings | `UserController` | password change if available. |

---

## 8. Quality Checklist Before Development

- [ ] All colors come from Design System tokens.
- [ ] All spacing is a multiple of 4px.
- [ ] No page uses a chat-first layout except `/ai/chat`.
- [ ] No AI robot / purple gradient / sparkle visuals.
- [ ] Every interactive element has hover, focus, active, disabled, and loading states.
- [ ] Every list has an empty state.
- [ ] Every form has validation and error feedback.
- [ ] Sidebar navigation matches Information Architecture exactly.
- [ ] New routes `/profile` and `/settings` are added and linked from user menu.
- [ ] Existing backend API contracts are not modified.
- [ ] Design is implementable with Vue3 + Element Plus + existing dependencies only.
- [ ] All transitions respect `prefers-reduced-motion`.
- [ ] Minimum touch target is 36×36px.
- [ ] Focus rings are visible and consistent.

---

## 9. Summary

This specification defines a Figma Community-grade, professional, minimal, enterprise-grade redesign for AI Study Assistant. It synthesizes Apple HIG's tactile clarity, Material 3's tonal color and elevation system, and Ant Design's deterministic 4/8-point grid and enterprise values.

The design treats AI as a capability embedded into a learning workflow — not as the visual identity of the product. Every spacing value, color token, transition timing, and component measurement is specified so that Claude Code can implement the Vue3 frontend without returning to design decisions.
