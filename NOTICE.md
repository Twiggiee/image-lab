# Attribution

Host-side tokenizer and numerical routines were adapted into Java from BonsaiMath.kt and QwenTokenizer.kt by Daisuke Majima, copyright 2026, Apache License 2.0. Reference source: https://github.com/john-rocky/LiteRT-Models/tree/main/bonsai_image

Changes: standalone Java Android CLI, per-stage backend selection, shared LiteRT Environment, explicit signature/shape/type/finite validation, memory admission, cancellation at stage boundaries, structured logs and PNG output. No original Gestura code or native binaries are used in this runner.

LiteRT runtime: Google AI Edge LiteRT 2.2.0, retrieved from Google's Maven repository; see reference/LiteRT-LICENSE and the original AAR third-party notices. Kotlin standard library 2.2.10 is an unmodified runtime dependency.

Model: prism-ml Bonsai Image ternary 4B, LiteRT conversion from litert-community/Bonsai-Image-ternary-4B at revision 317d577f5f962eb2d5a3980bca2ce047a8338212. Model attribution and conversion documentation are preserved in reference/bonsai-model-card.md. File hashes and sizes are in reference/bonsai-download-manifest.json. Model weights are downloaded separately and are not part of the source archive.

The APK wrapper and model-file preparation/import code were added for Image Lab. Small tokenizer/fixture files shipped in the APK are verified against the pinned model manifest; their upstream model attribution and license remain applicable.
