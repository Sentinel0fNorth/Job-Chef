# UI Design System: The Agentic Job Matcher

**Vibe/Aesthetic:** "Executive Tech-Luxury" – A blend of corporate authority and high-tech depth. Think of a perfectly tailored navy suit combined with a cutting-edge workspace.
**Core Philosophy:** The UI must feel like a high-end, extremely stable executive assistant. Use clean, luxurious tones for static reading and profile management. Transition to dark glassmorphism for active AI generation and rapid interactions (swiping). Use negative space aggressively to reduce cognitive load.

## 1. Color Palette (The Dual State)
Never use pure black or pure white outside of text/icons. Avoid generic, bright system blues; stick to deep, desaturated tones.

**Base Theme (Profile, Reading, Vault, Saved List)**
* **Background/Surface:** Warm Cream/Beige (`#F9F8F6` / `oklch(0.975 0.005 85)`)
* **Surface Deep:** Cream Deep (`#F5F3E9` / `oklch(0.955 0.012 85)`)
* **Primary Accent (Success/Ready/Saved):** Boardroom Slate (`#1B263B` / `oklch(0.275 0.035 255)`)
* **Secondary Accent (Processing/Pending):** Cool Steel Grey (`#A8B2C1` / `oklch(0.74 0.018 250)`)
* **Primary Text:** Deep Ink (`#1A1A1A` / `oklch(0.18 0 0)`)
* **Secondary Text (Snippets/Dates):** Ash Grey (`#7D7D7D` / `oklch(0.58 0 0)`)

**Active Theme (Swipe Deck, AI Processing Modals)**
* **Background:** Night Matte Black (`#121212` / `oklch(0.16 0 0)`)
* **Surfaces:** Frosted Glass (White at `10-15%` opacity with background blur)
* **Text:** Pure White (`#FFFFFF`) with soft drop shadows for legibility
* **Accents:** Luminous variations of Boardroom Slate and Crisp White.

## 2. Typography
* **Display Headers (App Title, Major Sections):** Playfair Display. Font weight: Semi-Bold (600).
* **Functional UI & Body (Job details, resumes, buttons):** Inter. Font weight: Regular (400) to Medium (500). Line-height: 1.5x for readability.
* **Data Labels & Section Headers:** Use ALL CAPS with wide letter-spacing (`0.18em`) for sub-labels and screen indices (e.g., `01 — VAULT`). Size: ~11-12sp.

## 3. Shapes, Texture, & Components
* **Navigation:** Persistent Bottom Navigation with three main destinations: `VAULT`, `SEARCH` (Active/Deck), and `SAVED`.
* **Section Headers:** Screen-top headers following the `XX — TITLE` pattern (e.g., `04 — SHORTLIST`) in wide-spaced caps.
* **Cards (The Swipe Deck & Saved Items):**
  * Border Radius: `24dp` to `32dp` (Smooth, continuous curves).
  * Light Theme: Subtle elevation or `1px` solid border (`#E5E5E5`).
  * Dark Theme: Heavy glassmorphism (background blur) with a `1px` semi-transparent white inner border.
* **Match Score Pill:** Top-right alignment on job cards. Circular or Pill-shaped with a background that contrasts the card surface (e.g., Slate on Cream or White on Glass).
* **Status Indicators:** 
  * `READY`: Solid indicator dot (Slate) + Bold text.
  * `TAILORING`: Muted indicator dot (Steel) + Regular text.
* **Buttons:**
  * Shape: Floating Pill shapes (`50dp` radius).
  * Primary: Solid Boardroom Slate with Pure White text.
  * Secondary/Outline: `1px` border of Slate or White.
* **Search Brief (Discovery):** Stacked, high-contrast input cards for `TARGET ROLE`, `LOCATION`, and `KEYWORDS`.
* **Tailoring Lab (Split View):** A vertical split layout where the top half shows the "Source" (Job Description) and the bottom half shows the "Agent" (Tailored Resume). 
  * Features tabs for switching between `RESUME` and `COVER LETTER`.
  * Includes a `REGENERATE` action and a prominent `EXPORT` floating button.
* **Selection State:** Use a distinct background (e.g., light grey/blue tint) or a "star/sparkle" icon to indicate the "Active" resume in the Vault or Search screens.

## 4. Interaction & Motion Guidelines
* **State Transitions:** Shifting from the "Vault" (Light) to the "Swipe Deck" (Dark) should feature a smooth, cross-fade animation (`300ms`), signaling a shift from reading to action.
* **The Deck:** Swiping should feel physically heavy and tactile. No bouncy, cartoonish animations.
* **Z-Index & Depth:** Rely on blur and translucency rather than flat drop shadows to create depth, especially when the AI is "tailoring" a resume over a blurred background of the job description.
* **Empty States:** When a list is empty, display a muted, minimalist wireframe icon centered on the screen with a gentle, professional message.