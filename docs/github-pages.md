# GitHub Pages (API site)

The workflow [`.github/workflows/docs.yml`](../.github/workflows/docs.yml) builds `./gradlew dokkaHtmlSite` and deploys `build/docs/site` to GitHub Pages on push to `main` or `support-opengl`.

## One-time repository setup

1. Open **Settings → Pages**.
2. Under **Build and deployment**, set **Source** to **GitHub Actions** (not “Deploy from a branch”).
3. After the first successful workflow run, the site URL appears on the Pages settings screen (typically `https://<user>.github.io/<repo>/`).

## Local preview

```bash
./gradlew dokkaHtmlSite
```

Open `build/docs/site/index.html` in a browser, or serve the folder with any static file server.

## Contents

- Per-module Dokka HTML under `redbytefx-core/`, `redbytefx-gl/`, and related paths.
- Markdown guides copied from this `docs/` directory (`language-reference.md`, `error-codes.md`).
