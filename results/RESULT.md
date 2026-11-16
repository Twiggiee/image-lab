# First on-phone image baseline

Completed on SM-F926B (SM8350), Android API 35, using official LiteRT 2.2.0. No app installation or Gestura binary was needed.

Prompt: a small bonsai tree in a blue ceramic pot, studio lighting.
Seed: 7. Size: 256x256. Four denoising steps. CPU requested for encoder, DiT and VAE; logs identify XNNPACK delegation.

End-to-end: 89.56 seconds. Encoder write/run/read: 5.69 seconds. DiT steps: 14.77, 14.17, 17.72 and 15.54 seconds. VAE write/run/read: 8.17 seconds. Loading, preprocessing and cleanup are included in the end-to-end total.

Tokenizer matches all 26 published fixture cases. Every graph output and Euler update was checked for finite values. The VAE separately passed a changed-input sensitivity test. The saved PNG visibly matches the neutral test prompt.

The lowest sampled MemAvailable was 2,511,144 KiB. This is a system-wide boundary sample, not a process peak or an uninterrupted memory trace. The phone was charging and the user was watching YouTube, so this is an initial functional timing rather than an isolated benchmark.

Next: measure and validate per-stage accelerator placements, preserve intermediate reference tensors for numerical comparison, and package the runner into an app. NPU registration alone is not proof of an installed working Qualcomm dispatch/compiler stack. The GPU and NPU routes have not yet generated this image.

User correction: the phone was power-sharing and in a deep-sleep state. The 89.56-second result is a functional result, not a reliable performance baseline; exact power/clock state was not captured.
