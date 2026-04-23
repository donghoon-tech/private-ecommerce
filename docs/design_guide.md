# Gaseol Link — UI Design Guide

> **Purpose:** This document defines the design system for the Gaseol Link frontend (Vue 3 + Tailwind CSS v4). Reference it whenever creating or modifying any screen. All new UI must follow these rules.

---

## 1. Color Tokens

All colors are defined as CSS variables in `src/style.css`.

| Token | Value | Usage |
|---|---|---|
| `--color-primary` | `#0066FF` | CTA buttons, links, active states, prices |
| `--color-primary-dark` | `#0052CC` | Button hover |
| `--color-primary-light` | `#EEF4FF` | Badge bg, chip bg, subtle highlights |
| `--color-navy` | `#1A1A1A` | Headings, body text |
| `--color-bg` | `#F5F7FA` | Page background |
| `--color-surface` | `#FFFFFF` | Card / panel background |
| `--color-success` | `#00B478` | Completed, confirmed states |
| `--color-warning` | `#FF9900` | Pending, in-progress states |
| `--color-info` | `#54A2FF` | Informational messages |
| `--color-danger` | `#FF4D4D` | Error, delete, negative actions |
| `--color-border` | `#E8ECF0` | Card borders, dividers, inputs |
| `--color-text-secondary` | `#64748B` | Subtitles, labels, meta text |
| `--color-text-muted` | `#94A3B8` | Placeholders, disabled text |

**Rule:** Never hardcode hex values in component files. Always reference CSS variables or use one of the shared utility classes below.

---

## 2. Typography

- **Font family:** `Pretendard` (Korean), `Inter` (fallback)
- **Font import:** loaded via CDN in `index.html` or `style.css`

| Role | Size | Weight | Usage |
|---|---|---|---|
| Page title | 30px (`1.875rem`) | 800 | `<h1>` on each page |
| Section title | 24px | 800 | Card / section headings |
| Sub-section | 18–20px | 700 | Card group titles |
| Body | 16px | 400–500 | Descriptions, form labels |
| Small / meta | 13–14px | 400–600 | Captions, badges, timestamps |
| Price emphasis | 20–32px | 900 | Use `.gl-price` or `.gl-price-lg` |

**Rule:** Prices and key numbers must use `font-weight: 900` and `color: var(--color-primary)`.

---

## 3. Spacing & Shape

| Token | Value |
|---|---|
| `--radius-sm` | `8px` |
| `--radius-md` | `12px` |
| `--radius-lg` | `16px` |
| `--radius-xl` | `20px` |
| `--radius-2xl` | `24px` |
| `--shadow-card` | `0 4px 20px rgba(0,0,0,0.05)` |
| `--shadow-md` | `0 8px 30px rgba(0,0,0,0.08)` |

- Cards use `border-radius: var(--radius-xl)` (or `--radius-2xl` for large panels).
- All cards have `border: 1px solid var(--color-border)` and `box-shadow: var(--shadow-card)`.
- Page content is constrained to `max-width: 1280px; margin: 0 auto; padding: 0 1.5rem`.

---

## 4. Shared UI Components

All components live in `src/components/ui/`. Always import and reuse them — do not reinvent inline.

### `AppButton.vue`

```vue
<AppButton variant="primary" size="md" :loading="false" :block="false">
  Label
</AppButton>
```

| Prop | Options | Default |
|---|---|---|
| `variant` | `primary` `secondary` `outline` `danger` `ghost` | `primary` |
| `size` | `sm` `md` `lg` | `md` |
| `loading` | `boolean` | `false` |
| `block` | `boolean` (full-width) | `false` |
| `type` | `button` `submit` `reset` | `button` |

### `AppInput.vue`

```vue
<AppInput v-model="value" label="Label" placeholder="..." :error="errorMsg">
  <template #prefix><IconComponent /></template>
</AppInput>
```

Slots: `#prefix`, `#suffix` (for icons inside the input field).

### `AppCard.vue`

```vue
<AppCard size="md" :hover="true">content</AppCard>
```

`size`: `md` (padding 1.5rem) | `lg` (padding 2rem). `hover`: adds lift on hover.

### `AppBadge.vue`

```vue
<AppBadge variant="primary">BEST</AppBadge>
```

`variant`: `primary` `success` `warning` `danger` `neutral`

### `AppPageHeader.vue`

```vue
<AppPageHeader title="Page Title" subtitle="Description" :count="42" countLabel="개">
  <template #right><!-- optional right-side slot --></template>
</AppPageHeader>
```

Use at the top of every content page. Renders a bordered, typographically consistent heading row.

### `AppEmptyState.vue`

```vue
<AppEmptyState title="No items" message="Try adding something.">
  <template #icon><svg ...></svg></template>
  <template #action><AppButton>Go browse</AppButton></template>
</AppEmptyState>
```

Use whenever a list or section has zero items.

### `AppProductCard.vue`

```vue
<AppProductCard
  :id="p.id" :slug="p.slug"
  :item-name="p.itemName" :unit-price="p.unitPrice"
  :image-url="p.imageUrls?.[0]"
  :seller-name="p.sellerName" :address="p.address"
  @filter-seller="onFilterSeller"
/>
```

Emits `filter-seller(sellerName: string)` when the seller filter button is clicked.

---

## 5. Layout Patterns

### Page Wrapper

```vue
<div class="page-wrap">          <!-- max-width 1280px, padding 2rem 1.5rem -->
  <AppPageHeader ... />
  <!-- content -->
</div>
```

### Auth Pages (Login / Find ID / Find Password)

```vue
<div class="auth-page-bg">       <!-- min-h-screen, centered, bg #F5F7FA -->
  <div class="auth-card">        <!-- max-w 440–460px, white, rounded-2xl, shadow-md -->
    <div class="auth-card__header"> <!-- centered icon + title + subtitle --> </div>
    <form class="auth-form">     <!-- flex-col gap-1rem --> </form>
  </div>
</div>
```

The icon at the top of auth cards uses `background: var(--color-primary-light); color: var(--color-primary); border-radius: 14px`.

### Side-by-side Layout (Home)

```vue
<div style="display:flex; gap:2rem; max-width:1280px; margin:auto; padding:2rem 1.5rem">
  <aside style="width:240px; flex-shrink:0; position:sticky; top:80px">
    <!-- category / filter sidebar -->
  </aside>
  <main style="flex:1; min-width:0">
    <!-- product grid -->
  </main>
</div>
```

### Product Grid

```vue
<div style="display:grid; grid-template-columns:repeat(2,1fr); gap:1.25rem">
  <AppProductCard v-for="p in products" :key="p.id" v-bind="p" />
</div>
```

Breakpoints: 2 cols default → 3 cols at `min-width: 1024px`.

---

## 6. GNB (Global Navigation Bar)

- **Height:** 64px, sticky, `z-index: 100`
- **Logo:** `GL` square badge (`#0066FF` bg) + brand name in `font-weight: 900`
- **Nav links:** `font-weight: 600`, `color: var(--color-text-secondary)` → `var(--color-primary)` on active/hover
- **Icon buttons** (Recent / Wishlist / Cart): 40×40px rounded square, hover bg `--color-primary-light`
- **Badges on icons:** primary badge = `#0066FF`, danger badge = `#FF4D4D`, neutral = `#64748B`
- **Auth links:** text-only, hover bg `--color-primary-light`; logout hover bg `#FFF0F0` + color `--color-danger`

---

## 7. Feedback & State Patterns

### Error / Success Alerts (inline)

```html
<!-- Error -->
<div style="display:flex; gap:.5rem; padding:.75rem 1rem; background:#FFF0F0; border:1px solid #FECACA; border-radius:var(--radius-md); color:var(--color-danger); font-size:.875rem; font-weight:500">
  <svg><!-- warning icon --></svg>
  Error message here
</div>

<!-- Success -->
<div style="background:#E6FBF5; border:1px solid #6EE7D3; color:var(--color-success)"> ... </div>
```

### Loading Spinner

```html
<div style="width:40px; height:40px; border:3px solid var(--color-border); border-top-color:var(--color-primary); border-radius:50%; animation:spin .7s linear infinite"></div>
```

### Animations

```css
/* Entrance - use on page/card mount */
.gl-fade-in  { animation: glFadeIn  0.3s ease-out both; }   /* fade + slide up */
.gl-zoom-in  { animation: glZoomIn  0.25s ease-out both; }  /* scale from 0.97 */
```

Apply `class="gl-zoom-in"` to auth cards and modals.

---

## 8. Do's and Don'ts

| ✅ Do | ❌ Don't |
|---|---|
| Use `--color-primary` (`#0066FF`) for all primary actions | Use `indigo-600` or other Tailwind color for brand actions |
| Use `AppButton`, `AppInput`, `AppCard` components | Write inline `<button class="px-6 py-3 bg-blue-600 ...">` |
| Use `AppPageHeader` at the top of every content page | Write raw `<h1>` without the component |
| Use `AppEmptyState` for zero-data views | Show a plain `<p>` text for empty lists |
| Keep prices bold (`font-weight:900`) in `--color-primary` | Show prices in default weight/color |
| Use `border-radius: var(--radius-xl)` on cards | Use small or square corners |
| Keep page max-width at `1280px` with `1.5rem` side padding | Let content stretch full-width |
| Add `gl-fade-in` or `gl-zoom-in` on page entrance | Mount content with no animation |
