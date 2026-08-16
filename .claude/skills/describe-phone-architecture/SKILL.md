---
name: describe-phone-architecture
description: Describes the current exposures-phone architecture, module responsibilities, data flow, sync boundaries, and key extension points. Use when asked about phone app architecture, component ownership, or where to implement phone-side changes.
disable-model-invocation: true
---

# Describe Phone Architecture

Use this skill to explain the current `exposures-phone` architecture accurately and consistently.

## Scope

Focus on current implementation details, not future aspirations:

- modules and ownership
- dependency wiring
- navigation/view-model structure
- sync and wearable message boundaries
- capture/upload pipeline

## Module map

- `app`
  - Android entry points, Compose UI screens, navigation, view models, sync/capture orchestration.
- `core-model`
  - domain models and value types shared across modules.
- `core-database`
  - Room schema, DAOs, mappers, and `EquipmentRepository`.
- `core-datalayer`
  - Wear OS Data Layer gateway/client, paths, DTO/json mapping.
- `core-sync`
  - backend sync abstraction (`SyncApi`) and placeholder auth/provider wiring.

## App wiring

- `ExposuresApplication`
  - initializes `DefaultAppContainer`
  - initializes WorkManager
  - schedules periodic upload drain work.
- `DefaultAppContainer`
  - manual DI (no Hilt)
  - provides:
    - `EquipmentRepository`
    - `DataLayerClient`
    - `EquipmentSyncPusher`
    - `SyncApi`
    - `CsvExportCoordinator`
    - upload trigger helper.

## UI architecture

- Single Compose nav graph (`ExposuresNavHost`) with home plus CRUD screens:
  - camera bodies
  - lenses
  - light meters
  - film rolls
- `ExposuresViewModelFactory` manually constructs per-screen view models.
- View models read/write through `EquipmentRepository`; push watch updates via `EquipmentSyncPusher` on equipment edits.

## Phone/watch boundary

- `WearMessageListenerService` is the manifest-registered wearable entry point.
- Handles commands from watch:
  - capture-photo request
  - complete-roll request
  - refresh request (full equipment snapshot push)
  - connectivity ping (ack back to watch).
- Handles exposure payload updates (`/exposures`) from watch and enqueues upload work.

## Capture pipeline

- Watch capture command starts `CaptureForegroundService` with exposure id.
- Service:
  - tries to start camera FGS notification
  - captures photo via CameraX when permitted
  - stores reference-photo metadata
  - publishes capture result/status to watch and photo-status payload.
- Android 14+ guard:
  - if camera FGS startup is rejected, service reports `FAILED` status and exits without crashing app process.

## Refresh behavior (current)

- Watch "Refresh from phone" request now triggers phone push of:
  - camera bodies
  - lenses
  - light meters
  - rolls
- This is used as recovery after reinstall/reset and to rehydrate watch equipment state.

## How to answer architecture questions

When responding:

1. Start with module boundaries.
2. Name exact entry points/classes for the asked flow.
3. Trace data direction (UI -> repo -> data layer or reverse).
4. Call out reliability constraints (FGS eligibility, wearable reachability) when relevant.
5. If asked "where to change X", provide the smallest owning surface first.

