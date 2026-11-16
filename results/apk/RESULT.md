# APK validation

Image Lab 0.1.1 (versionCode 2) was installed with user confirmation on SM-F926B / Android API 35. Model preparation initially failed because the user's DNS/network blocker denied the new app; after the user allowed Image Lab, model preparation and verification completed normally.

The installed APK generated a 256x256 Bonsai image from the neutral baseline prompt at seed 7. All four denoising steps and all 26 tokenizer fixtures passed, and the image was displayed in the app. Native execution used official packaged LiteRT libraries and app-private model storage. No app_process shell runner was used for generation.

Elapsed generation time: 101.96 seconds. The phone was interactive, not idle, not in power-save mode, plugged into power, with a partial wake lock held. Initial thermal status was 2. This is a functional check under uncontrolled thermal/background conditions, not a benchmark.

PNG byte equality with the earlier CLI result: True. See receipt.json, conditions.json and events.jsonl for evidence. GPU/NPU and offline folder import are not yet integration-tested.

Timing scope: the receipt timer starts at the engine call, after app-side model verification and DEX setup. It is not a tap-to-image benchmark.
