# Image Lab

An Android APK and standalone Java runner for on-device image inference, with a working Bonsai Image 4B baseline and research into Gestura's image/video pipelines.

## Verified now

- The installed **Image Lab 0.1.1 APK** generated a 256×256 image through all four denoising steps and the VAE on SM-F926B / Android API 35.
- All 26 published tokenizer fixtures passed.
- The APK's PNG is **byte-identical** to the earlier standalone runner's result for the same prompt and seed.
- Inference uses app-private model files, official LiteRT 2.2.0 libraries and a foreground service; it does not depend on an external shell runner or Gestura's libraries.
- Current validated placement is CPU/XNNPACK. GPU/NPU and coordinated accelerator execution remain future work.

See [the APK validation record](results/apk/RESULT.md), [receipt](results/apk/receipt.json), [recorded power/thermal conditions](results/apk/conditions.json) and [sample image](results/apk/baseline.png).

The earlier standalone run took 89.56 seconds, but the user reported power-sharing/deep-sleep conditions. The APK run took 101.96 seconds with a wake lock held and thermal status 2. Both are **functional checks, not controlled performance benchmarks**.

## Install and use

1. Install the signed `app.apk` from this private repository's release assets.
2. Allow Image Lab in any DNS/firewall app that blocks new applications.
3. Tap **Prepare model** to retrieve and verify the pinned 4.28 GB model package. During development this can reuse the loopback transfer server; otherwise the app uses the public HTTPS download URLs.
4. Enter a prompt and seed, then tap **Generate image**.

The app can also import an existing model folder with Android's file picker. That folder must contain `dit_256_int4b32.tflite`, `textenc_int4.tflite` and `vae_dec_256_fp32.tflite` matching the pinned hashes. Small tokenizer/fixture assets are packaged in the APK. Offline import is implemented but not yet integration-tested.

Once preparation is complete, inference needs neither network access nor shell access. Cancellation takes effect at the next stage boundary; it does not interrupt an in-flight native graph call.

## Build the APK on Buddy/Alpine

```
bash tools/setup-android-toolchain.sh
./tools/build-apk.sh
```

Output: `apk/build/app.apk`. The small bootstrap assets and official runtime dependencies are fetched automatically; building the APK does not require downloading the large model weights. Signing keys are kept outside the repository.

The interface uses plain Java and Android framework views. The verified inference engine is packaged as a read-only DEX asset with the official native LiteRT libraries. The app downloads no executable code.

## Standalone runner

```
python3 tools/fetch_runtime.py
python3 download_models.py
./build-runner.sh
```

The model downloader is pinned to Bonsai revision `317d577f5f962eb2d5a3980bca2ce047a8338212`, resumes partial downloads and verifies large files using streaming SHA-256. Model files are loaded by native file path, never copied wholesale into Java arrays.

The device harness uses `/data/local/tmp/buddy-image-lab` when staged through the phone tools. Commands `tokenizer`, `vae` and `generate` test the tokenizer, decoder sensitivity and complete generation respectively. The three stage backend requests are configurable, but only the CPU route is validated.

## Memory, lifecycle and limits

The engine shares a LiteRT Environment and closes each graph before loading the next. It validates signature names, tensor shapes/types/byte sizes and finite outputs. A 1 GiB MemAvailable admission guard is checked at model loads and denoising boundaries; this does not predict transient allocation peaks or guarantee protection from Android's low-memory killer.

The APK holds a partial wake lock during user-started work and records interactive, idle, power-save, power-source and thermal conditions. It does not change system power settings. Process-death recovery and background lifecycle behavior have not yet been exhaustively tested.

Z-Image, additional general-purpose models, heterogeneous CPU/GPU/NPU scheduling and video inference are planned, not implemented.

## Research and complete backup

[Research notes](research/) cover the recovered Gestura pipelines, community model differences and model-support requirements. The research does not establish that one converter forked another.

The full initial workspace, including models, original Gestura APKs, recovered source, native libraries and tools, is stored as numbered private release assets. See [BACKUP.md](BACKUP.md) for checksum verification and restoration. Ongoing APK code and test evidence are committed on `main`; the full snapshot predates later APK fixes.

Read [NOTICE.md](NOTICE.md) for upstream source/model attribution. Model/runtime binaries and build output are excluded from ordinary Git history.
