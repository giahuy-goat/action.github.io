# GAME CORNER media master drop

Production media masters can be kept here for source control/reference:

- `rog_boot_anim_1080p.mp4`
- `rog_sound_effects.wav` or `.mp3`
- `bg_cyberpunk_rog_hd.png`

The Android runtime loads the release copies from `res/raw/` and
`res/drawable/`, because those locations provide typed resource IDs for
`VideoView`, `MediaPlayer`, and XML layouts. Do not duplicate the same large
files in both folders or the APK will include them twice.