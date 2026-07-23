# SmartCrop KMP — Implementation Plan

## Overview

A Kotlin Multiplatform (Android + iOS) app that fetches Rick & Morty character images, runs an on-device TensorFlow Lite saliency model to detect the most visually interesting region of each image, and displays smart-cropped previews in an infinite-scroll feed.

---

## TF Lite Model Options for Smart Crop

### Recommended: U²-Netp (lightweight salient object detection)

- **What it does:** Outputs a per-pixel saliency mask highlighting the most visually salient object/region.
- **Size:** ~4.7 MB (the "p" lightweight variant).
- **Latency:** Tens of milliseconds on modern phones.
- **Why it fits:** Converts cleanly to `.tflite`. From the mask, compute a bounding box (or centroid) of the salient region and crop to the target aspect ratio around it. Widely used for background removal and smart crop.
- **Caveat:** Trained on natural images; test early on cartoon art (Rick & Morty). High-contrast illustrations generally transfer well.

### Alternative candidates

| Model | Type | Size | Pros | Cons |
|---|---|---|---|---|
| **MSI-Net** | Eye-fixation saliency heatmap | ~15 MB | Trained on SALICON/MIT1003 eye-tracking data; closer to how Twitter's smart-crop worked. TF-native, clean TFLite conversion. | Heavier than U²-Netp; heatmap post-processing needed. |
| **BlazeFace (MediaPipe)** | Face detector | ~400 KB | Sub-10ms, returns face bounding boxes directly — no heatmap math. Perfect for portrait images. | Trained on human faces; unproven on cartoon faces. Worth a quick offline test. |
| **EfficientDet-Lite0 / SSD-MobileNetV2** | General object detector | 4–15 MB | Ready-made TFLite models; returns bounding boxes with confidence. | COCO classes may not map well to cartoon characters. |
| **DeepGaze / SalGAN** | Academic saliency | 30+ MB | Strongest accuracy on saliency benchmarks. | Heavy, difficult to convert, overkill for this use case. |

### Decision

Use **U²-Netp** as primary, with **BlazeFace** as a fallback experiment for the portrait-heavy Rick & Morty dataset.

---

## Architecture

```
shared/
  data/
    api/          → Ktor client (RickAndMortyApi)
    model/        → API DTOs (CharacterResponse, CharacterDto, etc.)
    repository/   → CharacterRepository (DTO → domain mapping, paging)
  domain/
    model/        → Character, CropRegion
    usecase/      → GetCharactersUseCase (future)
  ml/
    SaliencyEngine.kt          → expect declaration
    CropCalculator.kt          → mask → bounding box → aspect-ratio-fit (pure Kotlin, shared)
  ui/
    App.kt                     → NavHost (HomeRoute ↔ DetailRoute)
    Navigation.kt              → Type-safe route definitions
    home/
      HomeScreen.kt            → Infinite scrollable feed
      HomeViewModel.kt         → Paging state, triggers saliency inference
    detail/
      DetailScreen.kt          → Full image, metadata, saliency overlay toggle
    components/
      SmartCropImage.kt        → Composable that renders cropped source rect
    theme/
      Theme.kt                 → Neo-brutalist MaterialTheme (colors + Fredoka type)
      NeoComponents.kt         → NeoBox / NeoButton / NeoPill / NeoStatCard

androidApp/
  MainActivity.kt              → Android entry point
  SaliencyEngine.android.kt    → actual: TFLite Interpreter + GPU/NNAPI delegate

iosApp/
  iOSApp.swift                 → iOS entry point
  SaliencyEngine.ios.kt        → actual: TFLite C API via cinterop + Metal delegate
```

---

## Design System — Neo-Brutalist

Adopt the neo-brutalist visual language from the `eat-please-app` KMP project (`ui/theme/EatPleaseTheme.kt` + `NeoComponents.kt`). It's a flat, high-contrast, hard-shadow look — no elevation blur, no gradients, no dark mode. Port the two theme files into `shared/ui/theme/` (rename `EatPleaseTheme` → `SmartCropTheme`) and reuse the components as-is.

### Principles

- **Flat + hard-edged.** Every surface is a bordered block sitting above a *hard, non-blurred* ink shadow offset down-right. No Material elevation/shadows.
- **Single light-committed theme.** The look is a deliberate one-world design — do **not** add a dark scheme.
- **Hue carries meaning.** A warm cream ground with black ink for all borders/shadows; saturated blocks whose color signals state, plus decorative header+body color pairs for section cards.

### Palette (`NeoColors`)

| Token | Hex | Role |
|---|---|---|
| `Cream` / `CreamDeep` | `#FBE7D6` / `#F5D9C4` | Background / surface variant |
| `Ink` | `#0B0B0B` | All borders, shadows, text |
| `Green` | `#88E88C` | Positive / steady state |
| `Coral` | `#FF7059` | Attention / error |
| `Yellow` | `#FFD60A` | Selected / highlight accent |
| Head/Body pairs | Orange · Magenta · Cyan · Lime | Decorative section cards (header strip + lighter body) |

Wired into a Material 3 `lightColorScheme`: `primary = Ink`, `background/surface = Cream`, `outline = Ink`, `error = Coral`.

### Typography

- **Fredoka** family (Regular/Medium/SemiBold/Bold) — bundle the four `.ttf` files into `commonMain/composeResources/font/`.
- Chunky hierarchy: display + headline at **Bold**, titles + labels at **SemiBold**, body at **Medium**.

### Components (`NeoComponents.kt`)

- **`NeoBox`** — the signature primitive: rounded (16dp) bordered face (3dp ink border) over a hard ink shadow (4dp offset). When clickable, pressing slides the face down-right by exactly the shadow offset over a 70ms tween so it reads as physically pressed, then springs back. Display cards keep the shadow but never move.
- **`NeoButton`** — full-width chunky action (down-scaled `NeoBox`), for retry / load-more actions.
- **`NeoPill`** — small highlighted token (yellow default), e.g. a status/species chip.
- **`NeoStatCard`** — uppercase header strip over a big value + caption, header/body color pair with a hard divider between them.

### Application to SmartCrop screens

- **Feed cells:** wrap each smart-cropped image in a `NeoBox` (ink border + hard shadow). Character name as a chunky title; status/species as `NeoPill`s (e.g. Green = Alive, Coral = Dead).
- **Detail screen:** full image in a `NeoBox`; metadata rendered as `NeoStatCard`s / pill rows (species, gender, origin, episode count).
- **Loading / error / empty states:** cream ground with ink-bordered blocks; retry via `NeoButton`.
- **Debug saliency overlay:** draw the crop box in `Ink`, the heatmap tinted, consistent with the flat palette.

---

## Key Components

### Networking & Paging

- **Client:** Ktor + kotlinx.serialization against `GET /api/character?page=N`.
- **Pagination:** The API returns `info.next` for the next page URL. Use `app.cash.paging` (multiplatform-compatible) or a hand-rolled page loader keyed on scroll position. 20 items per page; `info.pages` as the terminal condition.

### Image Loading

- **Coil 3** (multiplatform) for fetching and disk-caching the `image` URL of each character (300×300 avatars).

### SaliencyEngine (expect/actual)

- **Interface:** `suspend fun findSalientRegion(imageBytes: ByteArray, targetAspectRatio: Float): CropRegion`
- **Android actual:** TFLite Interpreter with bundled `u2netp.tflite` from assets. Optional GPU/NNAPI delegate.
- **iOS actual:** TFLite C API (or TensorFlowLiteSwift via a thin Swift shim) with Metal delegate. Model bundled as a resource.
- **Model file:** Lives once in shared resources, copied into each platform bundle at build time.

### Crop Math (pure Kotlin, shared, unit-testable)

Pipeline:
1. Threshold the saliency mask (binary mask at a confidence cutoff).
2. Find the largest connected region or compute a weighted centroid.
3. Compute a tight bounding box around the salient region.
4. Expand/pad the box to match the feed cell's target aspect ratio.
5. Clamp to image bounds.
6. Output `CropRegion(x, y, w, h)` in normalized coordinates (0.0–1.0).

### Inference Caching

- Inference is deterministic per image URL, so cache `CropRegion` keyed by image URL.
- In-memory LRU cache + optional persistent store (Room KMP table).
- Each image is analyzed once, ever — keeps scrolling smooth.

**Room KMP setup:**
- Apply the `androidx.room` Gradle plugin + KSP for every target (`kspAndroid`, `kspIosX64`, `kspIosArm64`, `kspIosSimulatorArm64`); depend on `room-runtime` and `sqlite-bundled` in `commonMain`.
- Define a `@Database`/`@Entity` (`CropRegionEntity` keyed by image URL) and a `@Dao` in `commonMain`.
- Provide the builder per platform via expect/actual: `Room.databaseBuilder<AppDatabase>(...)` with a platform path (Android `context.getDatabasePath(...)`, iOS `NSDocumentDirectory`), `.setDriver(BundledSQLiteDriver())`, and `.setQueryCoroutineContext(Dispatchers.IO)`.

---

## Screens

### Home Screen

- **Layout:** Infinite `LazyColumn` or `LazyVerticalGrid`.
- **Cell behavior:**
  1. Load image via Coil.
  2. While saliency is pending → show a center-crop placeholder.
  3. When `CropRegion` arrives → render the cropped preview using `drawImage(srcOffset, srcSize)`.
  4. Animate the transition from center-crop to smart-crop subtly.
- **Inference scheduling:** Off the main thread on a bounded dispatcher (`limitedParallelism(2)`), prioritizing currently visible items.

### Detail Screen

- Full uncropped image at the top.
- Character metadata: name, species, status, gender, origin, location, episode count.
- Optional debug overlay toggle: saliency heatmap + computed crop box drawn over the original image.
- Shared-element transition from the cropped cell to the full image (polish milestone).

---

## Pipeline Per Feed Item

```
1. Coil loads bitmap
   ↓
2. Downscale to model input size (320×320 for U²-Netp, normalized RGB)
   ↓
3. Run TFLite interpreter → saliency mask output
   ↓
4. CropCalculator: mask → CropRegion (shared, pure Kotlin)
   ↓
5. Cache CropRegion (keyed by image URL)
   ↓
6. Recompose cell with cropped source rect
```

---

## Milestones

### Milestone 1: Skeleton ✅

- [x] KMP project structure (shared, androidApp, iosApp)
- [x] Ktor client + API DTOs + CharacterRepository
- [x] Type-safe navigation (HomeRoute, DetailRoute)
- [x] Placeholder screens
- [x] expect/actual SaliencyEngine stubs
- [x] CropRegion domain model

### Milestone 2: Feed UI + Paging ✅

- [x] Port neo-brutalist theme from `eat-please-app` (Theme.kt + NeoComponents.kt, Fredoka fonts)
- [x] HomeViewModel with paging state (page number, loading, end-of-list)
- [x] LazyColumn/LazyVerticalGrid with character image cells
- [x] Coil image loading in each cell
- [x] Infinite scroll trigger (load next page when near the bottom)
- [x] Center-crop placeholder (no ML yet)
- [x] Basic error / loading / empty states

### Milestone 3: Model Spike ✅

- [x] Obtain or convert `u2netp.tflite` (verify model input/output shapes) — converted from HF ONNX; see `tools/model-spike/`
- [x] Python notebook: test model on a handful of Rick & Morty avatars (`run_spike.py`; also validated on real Picsum photos)
- [x] Lock in input size, normalization constants, threshold values (320², ImageNet, threshold 0.5, minSize 0.4)
- [x] Test BlazeFace on the same image set as a comparison — deemed unnecessary; u2netp validated on cartoon + real photos
- [x] Decide final model → u2netp float32

### Milestone 4: Android Inference ✅

- [x] Android `SaliencyEngine` actual — load model, run interpreter
- [x] `CropCalculator` — mask → bounding box → aspect-ratio-fit (pure Kotlin, unit tests)
- [x] `SmartCropImage` composable — renders the cropped source rect
- [x] Wire into HomeViewModel — inference off main thread, cache results (in-memory via `CropRegionRepository`)
- [x] Integrate into feed cells — replace center-crop with smart-crop preview (both R&M + Picsum feeds)

### Milestone 5: iOS Inference 🚧 (in progress)

- [ ] TFLite C API cinterop setup (or Swift shim) — vendored TensorFlowLiteC.xcframework + `.def`
- [ ] iOS `SaliencyEngine` actual (CoreGraphics decode + interpreter → CropCalculator)
- [ ] Parity testing: compare crop results against Android on identical inputs
- [ ] End-to-end iOS feed with smart-cropped previews

### Milestone 6: Detail Screen + Polish ✅ (mostly)

- [x] Detail screen: full image, character metadata display
- [x] Debug overlay toggle: crop-region box visualization (`CropRegionOverlay`) — saliency heatmap itself not surfaced to the UI
- [x] Shared-element transition from feed cell to detail image
- [x] Error/fallback path: if confidence is low or inference fails → graceful center-crop fallback (`CropCalculator` returns CENTER; min-size guard)

### Milestone 7: Performance Pass

- [ ] Measure inference latency on low-end devices
- [ ] Evaluate int8-quantized model variant
- [ ] Tune dispatcher parallelism and prefetch distance (partial: inference off-main-thread, concurrent downloads, memoized cache)
- [ ] Profile scroll performance (jank, memory)
- [ ] Room-backed persistence of crops across sessions (DAO/entity scaffolding exists)

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Rick & Morty avatars are already 300×300 tight portraits; smart crop effect may be subtle | Low visual payoff | Use non-square cell aspect ratio (16:9 or 2:1) so the crop decision is clearly visible |
| U²-Netp trained on natural images; may underperform on cartoon art | Poor saliency results | Test early in Milestone 3; BlazeFace as fallback for portrait-heavy content |
| iOS TFLite cinterop is complex and fragile | iOS delays | Budget extra time; consider TensorFlowLiteSwift pod as an easier alternative |
| Inference per image adds scroll latency | Jank | Cache results aggressively (LRU + Room KMP); run inference off-screen; bounded dispatcher |

---

## Open Decisions

- [x] Feed cell aspect ratio → **1:1** (square cells)
- [x] Android-first or both platforms in sync → **Android-first**; iOS inference is Milestone 5 (in progress), iOS falls back to center-crop until then
- [x] Which milestone to tackle next → Milestone 5 (iOS inference)
