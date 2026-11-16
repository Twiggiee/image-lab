# Community FLUX versus Gestura: verified comparison

Sources: the two Hugging Face repository trees saved alongside this document, the downloaded model cards and Gestura 4.0.6 DEX. This comparison does not establish authorship, copying or the history of either conversion.

Both packages use Qwen hidden width 2560, three encoder taps combined to width 7680, 512 text positions, a 128-wide packed latent, FLUX.2 Klein's four-step flow-matching update, and prep/double/single/final stages. Their shared 777,912,320-byte FP16 embedding table has the same LFS SHA-256. Matching names and that identical table are evidence of common model content, not proof that one application was forked from another.

The current public Gestura payload differs materially: all shared graph files have different LFS hashes. It has external W4A16 weight sidecars and multi-geometry signatures. For example, community kc_final is 19,348,608 bytes, while Gestura kc_final is 76,452 bytes plus external weights. A standard standalone TFLite loader cannot simply treat the latter as a complete model. The application must resolve every external-buffer name, offset and length exactly.

The older APK's hard-coded graph catalog also differs from that current public payload. Package versions must remain separate; do not silently combine the APK's inventory with later repository files.

The community card now contains a Snapdragon NPU sweep: some encoder graphs compiled but failed to invoke, GPU inference was faster on the comparable tested graphs, and NPU cached loading was faster. These results are for its documented SM8850/Hexagon-v81 setup, not this user's SM8350 phone. They provide a concrete hypothesis for testing, not a proven reason for Gestura's GPU-only decision.

Bonsai is a third contract: 256 text positions, token-ID input directly to the encoder, three whole graphs and different quantization. It is a suitable independent correctness baseline but is not a weight drop-in replacement for Gestura's split graphs.

Machine-readable filename/size/hash comparison: artifact-comparison.json.

References:
- https://huggingface.co/litert-community/FLUX.2-klein-4B-LiteRT
- https://huggingface.co/ZawShiShawn/gestura-flux2-klein-4b-litert-tflite
- https://github.com/google-ai-edge/litert-samples/tree/main/models/bonsai/bonsai_image_4b
