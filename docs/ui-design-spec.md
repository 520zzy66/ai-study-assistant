# AI Study Assistant UI Design Specification

## 1. Design Direction

The product uses a quiet, focused learning-workspace style:

- Apple-inspired visual restraint and content hierarchy;
- Google-inspired interaction consistency and accessibility;
- Meta-inspired information efficiency for data-heavy workspaces.

The interface must feel calm and useful. Avoid decorative gradients, excessive cards,
heavy shadows, and multiple competing primary actions.

## 2. Brand Palette

| Role | Token | Value | Usage |
|---|---|---|---|
| Page background | `--bg-page` | `#faf9f6` | Main canvas |
| Sidebar background | `--bg-sidebar` | `#f4f3ef` | Navigation and secondary panes |
| Card background | `--bg-card` | `#ffffff` | Raised or independent content |
| Primary | `--color-primary` | `#2d5a27` | Primary action and active state |
| Focus / link accent | `--color-accent` | `#0071e3` | Focus and informational links |
| Primary text | `--text-primary` | `#1c1c1a` | Headings and body text |

Forest green is the only brand action color. Blue is reserved for focus and
informational meaning. Semantic red, amber, and green must not be used as decoration.

## 3. Typography

Use the system sans-serif stack in `theme.css`. Chinese interfaces prefer PingFang SC,
Microsoft YaHei, or Noto Sans SC through the shared stack.

| Level | Token | Size |
|---|---|---|
| Display | `--text-display` | 32px |
| Heading 1 | `--text-heading-1` | 24px |
| Heading 2 | `--text-heading-2` | 20px |
| Heading 3 | `--text-heading-3` | 17px |
| Body | `--text-body` | 16px |
| UI | `--text-ui` | 14px |
| Small | `--text-small` | 13px |
| Micro | `--text-micro` | 12px |

Use no more than three type levels in one local region. Long AI or summary content
uses a line height between 1.65 and 1.8.

## 4. Spacing and Shape

- Use the shared 4px/8px spacing scale only.
- Standard control height: 40px; compact control height: 32px; prominent control: 44px.
- Standard cards use `--radius-lg`; workbench containers use `--radius-xl`.
- Shadows express elevation, not decoration. Flat content should use dividers or whitespace.
- Hover elevation is allowed only for clickable cards.

## 5. Layout Types

### Reading layout

Maximum width: `--content-reading-width` (820px). Use for summaries, plan content,
and long AI responses.

### Work layout

Maximum width: `--content-work-width` (1200px). Use for chat, quiz, and interactive tools.

### Data layout

May use the available width. Use for materials, question banks, history, and wrong-question
tables. Filters and batch actions remain visually attached to their data surface.

## 6. Navigation

The global sidebar owns product-level navigation. A page may add a secondary pane only
for page-local objects such as chat conversations or material folders.

Secondary panes:

- use `--surface-container-low`;
- use a single right divider instead of a floating card;
- keep the primary content visually dominant;
- may collapse on desktop and become a drawer on smaller screens.

## 7. Components and Interaction

- One visually dominant primary action per region.
- Secondary row actions should be placed in a compact overflow menu.
- Every icon-only button requires an accessible label.
- All interactive custom elements require keyboard focus styling.
- Loading, empty, failed, disabled, selected, and processing states are mandatory.
- Motion uses the shared durations and must respect `prefers-reduced-motion`.

## 8. AI Chat

- Conversation history is a quiet secondary pane.
- AI answers are document-like content, not oversized speech bubbles.
- User messages may use a subtle tinted bubble.
- The composer remains centered and visually anchored at the bottom.
- Material association is visible before sending and attached to the sent message.
- Streaming, interruption, stop, retry, copy, and feedback actions remain available.

## 9. Learning Materials

- Folder navigation and the material list form one file-management workbench.
- Search and status filters sit directly above the table.
- AI question answering is the primary row action; detail, summary, mind map, and deletion
  are secondary overflow actions.
- Batch actions appear only after selection.
- Double-click preview and all existing API behavior remain unchanged.

## 10. Responsive and Dark Mode

- Desktop is the primary target for the current product.
- Below 900px, secondary panes stack or become drawers.
- Mobile touch targets should be at least 40px and preferably 44px.
- Components must consume semantic tokens instead of hard-coded light colors so dark mode
  remains functional.

## 11. Dashboard Cards

Homepage cards must be actionable or evidence-based:

- Statistic cards link to the page where the underlying data can be inspected or continued.
- Progress, countdown, and task cards use existing plan/progress APIs when a plan exists.
- Empty cards must expose a direct next action instead of only describing the missing state.
- Recent material and activity cards route to the most relevant workflow, such as chat,
  summary, plan, or history.

## 12. Change Policy

New pages and UI changes must reuse tokens and existing shared components. Introducing a
new color, radius, shadow, or spacing value requires updating this document and `theme.css`.
Business APIs, request payloads, response handling, and user workflows must remain backward
compatible unless the feature specification explicitly changes them.
