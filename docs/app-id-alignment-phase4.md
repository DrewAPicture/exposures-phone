# Phase 4 Device Reset/Reinstall (Phone)

Completed migration reset and reinstall for app ID alignment.

- Built debug APK from `fix/phase1-bluetooth-connectivity-phone`.
- Uninstalled prior package IDs from the phone (`com.exposures.phone`, `com.exposures.watch`, `default.exposures.ww.app`).
- Installed fresh debug APK with package `default.exposures.ww.app`.
- Verified install with `pm list packages`.
