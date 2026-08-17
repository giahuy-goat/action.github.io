# GAME CORNER v1.5.2-15200

Native Android Kotlin source for the GAME CORNER launcher, landscape dashboard,
boot animation, and floating performance HUD.

## Build

Open this directory in Android Studio and run:

```bash
./gradlew :app:assembleRelease
```

The app targets Android API 35, supports Android 8.0+, and uses version code
`15200` / version name `1.5.2-15200`.

## Build on GitHub

Upload the contents of this folder to a GitHub repository. The included
`.github/workflows/build.yml` runs automatically on `main`/`master` pushes, or
can be started manually from the **Actions** tab with **Build GAME CORNER APK**.
After the run completes, download the release APK from the workflow's
**Artifacts** section.

## Media pack

Place the final production files at:

```text
app/src/main/res/drawable/bg_cyberpunk_rog_hd.png
app/src/main/res/raw/rog_boot_anim_1080p.mp4
app/src/main/res/raw/rog_sound_effects.wav
```

The Gradle configuration uses `androidResources.noCompress` for MP4, WAV, MP3,
and PNG so media bytes are preserved in the APK. The included development media
pack is generated as a visual/functional sample because the original video demo
and source assets were not supplied. Replace those three files with the
production Full HD assets to match the reference exactly and tune the final APK
size around 65 MB.

## Platform notes

On first launch, GAME CORNER guides the user through notification, overlay, and
Usage Access permissions one at a time. The overlay permission is required for
the detached HUD to appear above a running game.

`PackageManager` detects launchable installed apps. Use **ADD GAME TO OPTIMIZE**
to select one or more games; the selection is saved on the device and the real
game icon is shown in the library. Tap a game card to make it the target before
using the launch slider.

The floating HUD includes animated open/close transitions, a pulsing diamond
launcher, neon wing accents, animated mode buttons, 12 utility controls, and a
crosshair overlay. OEM-only operations such as true bypass charging and
hardware refresh-rate control require vendor APIs/root access; the UI reports
that capability boundary instead of claiming a system change that Android
cannot perform from a normal app.

The primary app icon and in-app logo use the supplied ROG image at
`app/src/main/res/drawable/app_logo.jpg`.