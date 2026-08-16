# Phase 5 Connectivity Matrix (Phone-Side Evidence)

Automated verification completed on aligned debug builds.

- Unit tests pass (`./gradlew :app:test`) with package ID `default.exposures.ww.app`.
- Package install verified on device (`pm list packages` includes `default.exposures.ww.app`).
- `dumpsys package` confirms `WearMessageListenerService` registration for:
  - `/command/request-rolls-sync`
  - `/command/connectivity-ping`
  - `/exposures`
- `dumpsys activity service com.google.android.gms/.wearable.service.WearableService` shows the phone and watch nodes connected.

Manual UI verification is still expected for gesture/button-driven refresh behavior.
