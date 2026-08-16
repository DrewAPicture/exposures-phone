# App ID Alignment Notes (Phone)

- Runtime package ID: `default.exposures.ww.app`.
- Kotlin namespace stays `com.exposures.phone`.

## Debug install commands

- Install phone app:
  - `adb -s <phone-serial> install -r app/build/outputs/apk/debug/app-debug.apk`
- Verify package on phone:
  - `adb -s <phone-serial> shell pm list packages | rg default.exposures.ww.app`

## Data reset for migration testing

- Remove aligned package from phone:
  - `adb -s <phone-serial> uninstall default.exposures.ww.app`
