# Complete workspace backup

The source tree and curated research are versioned in this repository. The full initial workspace (including the 4.28 GB Bonsai model set, original Gestura APKs, decompilations, native libraries, tools and test artifacts) is preserved separately as numbered release-asset chunks.

Release tag: `workspace-backup-2026-11-16`. The source archive excludes Git internals and the backup directory itself. Signing keys and GitHub credentials live outside both project directories and are not uploaded.

To restore, download `snapshot-manifest.json`, `SHA256SUMS` and all `workspace-snapshot.tar.gz.part-*` assets into an empty directory, then:

```
sha256sum -c SHA256SUMS
cat workspace-snapshot.tar.gz.part-* | tar -xz
```

The result contains `image-lab/` and `gestura-research/`. APK work after this initial snapshot is committed to the main branch; the signed APK is a separate release asset. Dependencies and model downloads are also reproducible from the pinned manifests if only the source checkout is needed.
