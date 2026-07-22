# SmartCrop KMP Sample

A Kotlin Multiplatform (Android + iOS) app demonstrating on-device smart image cropping using TensorFlow Lite.

## Concept

The app fetches Rick & Morty characters from the [Rick and Morty API](https://rickandmortyapi.com/documentation#get-all-characters) and uses an on-device TFLite saliency model (U²-Netp) to detect the most visually interesting region of each character image. The feed displays smart-cropped previews instead of naive center crops.

## Architecture

```
shared/
  data/       → Ktor client, API DTOs, repository
  domain/     → Character & CropRegion models
  ml/         → expect/actual SaliencyEngine
  ui/         → Compose Multiplatform screens
androidApp/   → Android entry point, TFLite actual
iosApp/       → iOS entry point, TFLite actual
```

## Screens

- **Home** — Infinite scrollable feed of character images with smart-cropped previews
- **Detail** — Full uncropped image, character metadata, optional saliency overlay

## Stack

- Kotlin Multiplatform + Compose Multiplatform
- Ktor (networking) + kotlinx.serialization
- Coil 3 (multiplatform image loading)
- TensorFlow Lite (on-device ML inference)
- Jetpack Navigation (type-safe routes)
