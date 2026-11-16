# Image Lab: standalone Android inference baseline

This project contains a small Java CLI harness and three verified Bonsai Image LiteRT graphs. It is an inference experiment, not an installed Android app. It does not use Gestura's app process, proprietary JNI code or downloaded private files.

## What is implemented

- Qwen byte-level BPE and the exact Bonsai chat suffix, checked against 26 published fixture cases on the phone.
- Four-step shifted Euler schedule, SplitMix64/Box-Muller noise and 2x2 packed-latent decoding.
- A shared official LiteRT 2.2.0 Environment and sequential model residency.
- Stage-specific CPU/GPU/NPU requests, explicit shape/type/byte-size checks, finite-value rejection, wall-time logs and PNG export.
- A 1 GiB MemAvailable guard before graph loads and at denoising-step boundaries. This does not predict transient allocation peaks or guarantee protection against the low-memory killer.
- A CANCEL file checked at stage boundaries. It does not interrupt an in-flight native graph call.

The initial correctness baseline requests CPU for all three stages. Other placements are configurable but unverified. Listing an NPU interface does not mean a compatible Qualcomm compiler/dispatch driver is installed or that a graph ran on the NPU. Actual delegation must be established from runtime logs; no automatic claim of heterogeneous inference is made.

## Build

Uses javac and d8 from the existing Buddy toolchain, without building or installing an APK:

```
./build-runner.sh
```

Runtime dependencies: Google's official litert-api 2.2.0 AAR classes, its arm64 JNI/runtime/GPU libraries and Kotlin stdlib 2.2.10. Download provenance is in runtime/*.pom. D8's existing version warns about the newer Kotlin metadata; bytecode compilation succeeds, and the Environment and tokenizer have executed on Android.

## Models

`download_models.py` downloads the three 256x256 graphs, metadata and tokenizer at pinned revision 317d577f5f962eb2d5a3980bca2ce047a8338212. Downloads are resumable; large files are SHA-256 checked with streaming reads. The working set is approximately 4.28 GB on disk. No model is loaded into a Java byte array.

## Device execution

The isolated directory is `/data/local/tmp/buddy-image-lab`. Use the phone tools to stage files from the local loopback transfer server. Example after staging:

```
CLASSPATH=/data/local/tmp/buddy-image-lab/runner.dex \
LD_LIBRARY_PATH=/data/local/tmp/buddy-image-lab/runtime \
app_process -Xmx512m \
-Djava.library.path=/data/local/tmp/buddy-image-lab/runtime \
/system/bin com.buddyapps.imagelab.ImageRunner \
generate /data/local/tmp/buddy-image-lab/models \
/data/local/tmp/buddy-image-lab/output CPU,CPU,CPU \
'a small bonsai tree in a blue ceramic pot, studio lighting' 7
```

Commands `tokenizer` and `vae` run independently. `vae` checks changed-input sensitivity and writes a smoke-test PNG; it is not text-to-image generation. The three backend names for `generate` correspond to text encoder, DiT and VAE respectively. Sequential placement is implemented; simultaneous multi-accelerator execution is not implemented yet.

## Status and checks

See results/ for saved evidence. The runner compiled, and the standalone runtime plus all 26 tokenizer fixtures passed on SM-F926B / API 35. Numerical host checks pass on the JVM. Full generation has now passed: the complete 256x256 four-step CPU pipeline produced results/baseline.png in 89.56 seconds. See results/receipt.json, results/generate-cpu.log and results/summary.json. GPU and NPU graph execution remain unverified.

Read NOTICE.md for source/model attribution. The full model binaries and runtime libraries are excluded from source exports.

## Android APK

A plain-Java app wrapper is under `apk/`. It prepares and hashes the model files, runs inference in a foreground service with a partial wake lock, shows progress and the final image, and records power/idle/thermal conditions. It uses the same verified inference runner through a read-only DEX asset packaged in the signed APK; it downloads no executable code. Native libraries are the official LiteRT runtime, not Gestura's libraries.

Build on the Buddy/Alpine Android toolchain:

```
bash tools/setup-android-toolchain.sh
./tools/build-apk.sh
```

Output: `apk/build/app.apk`. Install it with Android's package installer, prepare the model once, then generate. Model preparation can reuse the local loopback transfer server while developing, otherwise it downloads the pinned public files over HTTPS. Inference runs from app-private storage and needs neither the transfer server nor shell access after preparation.

The first command-line 89.56-second run was subsequently reported by the user to have been power-sharing/in a deep-sleep state. Treat it as a functional check, not a controlled performance benchmark. The APK records conditions and holds a wake lock for the active job; it does not change system power settings.

See BACKUP.md for full-workspace release assets and restoration. GPU/NPU execution, heterogeneous partitioning, other model families and video generation remain future work.

APK 0.1.1 also supports importing an existing model folder using Android's file picker. Tokenizer and fixture files are packaged with the APK; imported graph files must match the pinned model hashes. A DNS/firewall blocker can prevent downloading even when other apps work; allow Image Lab in the blocker or use offline import. No network-blocker settings are changed by this app.
