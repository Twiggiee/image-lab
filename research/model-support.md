# Image model support requirements

User requests optional Bonsai Image / FLUX.2 Klein fine-tune and LiteRT variants, plus mature-content image options. Exact additional model choices remain unspecified.

Investigate Bonsai Image 4B and its LiteRT conversion using the official google-ai-edge/litert-samples image_generation sample. Keep model packages interchangeable; do not assume that a common FLUX.2 Klein architecture means Gestura's compiled graphs accept different weights directly. Validate tensor signatures, quantization, text encoder, VAE, scheduler and supported accelerators for each package. Distinguish preconverted imports from conversions requiring tooling.

Reference: https://github.com/google-ai-edge/litert-samples/blob/main/samples/litert/image_generation/README.md
Reference: https://huggingface.co/litert-community/FLUX.2-klein-4B-LiteRT

No models downloaded or compatibility tested. Additional mature-content scope can include non-explicit artistic models; no explicit-sexual model sources selected.

## Z-Image

User explicitly requests Z-Image alongside FLUX.2 Klein and Bonsai Image. Include Z-Image as a separate pipeline/backend compatibility target; investigate Turbo variants without assuming the user excludes other variants. Official reference: https://huggingface.co/Tongyi-MAI/Z-Image-Turbo
No working LiteRT conversion or phone compatibility verified in this investigation. Check available conversions, operator coverage, encoder/VAE dependencies, quantization, scheduling and CPU/GPU/NPU partitioning before promising support.

## Clarification and first working baseline

The user clarified that requested additional models are general-purpose models with broad output capabilities. Do not exclude a general-purpose model from compatibility/inference work merely because it can also produce explicit content.

A standalone Bonsai 256x256 baseline now runs on SM-F926B/SM8350 with LiteRT 2.2.0: 26 tokenizer cases passed, four denoising steps and VAE completed in 89.56 seconds using CPU/XNNPACK. Source, logs and sample are in /root/.buddy/work/image-lab. This is a CLI harness, not an installed app. Next investigate validated per-stage CPU/GPU/NPU placements and the Gestura external-weight contract; no mixed-accelerator performance claim is established yet.

## Next user-requested milestone: image editing

The user explicitly selected Gestura-style image editing as the next objective after repository backup and APK validation. Start with one input image plus an instruction, then support two-image composition/reference conditioning where the model package supports it. Preserve the validated text-to-image path.

Do not treat the current Bonsai fixed-shape text-to-image export as an editing model. Editing requires a compatible VAE encoder, correct normalization/packed-latent transforms, rotary/reference positions, and image-conditioned denoiser signatures. The community FLUX package documents a kce family and kv_vae_enc graph; Gestura's newer sidecar-weight package has additional runtime requirements. Verify the selected model contract before implementing the UI/inference path.
