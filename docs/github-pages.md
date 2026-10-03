# GitHub Pages (API site)

The workflow [`.github/workflows/docs.yml`](../.github/workflows/docs.yml) builds `./gradlew dokkaHtmlSite` and deploys `build/docs/site` to GitHub Pages on push to `main`, `master`, or `support-opengl`.

## One-time repository setup

1. Open **Settings → Pages**.
2. Under **Build and deployment**, set **Source** to **GitHub Actions** (not “Deploy from a branch”).
3. After the first successful workflow run, the site URL appears on the Pages settings screen.

## Published URL layout

This repository is a **project site**, so GitHub always adds the repository name after the host:

`https://<user>.github.io/<repo>/libs/redbytefx/<module>/…`

Example (this repo):

`https://i-redbyte.github.io/redbytefx/libs/redbytefx/redbytefx-core/ru.redbyte.redbytefx/…`

A path like `https://i-redbyte.github.io/libs/redbytefx/…` (without `/redbytefx/` in the middle) would require publishing from a **user/org** Pages repository (`<user>.github.io`), not from the `redbytefx` project alone.

Dokka **(source)** links use `blob/<branch>/…` on GitHub. CI sets the branch from `GITHUB_REF_NAME`; locally override with `-Predbytefx.docsGitRef=support-opengl` in `gradle.properties`.

## Local preview

```bash
./gradlew dokkaHtmlSite
```

Open `build/docs/site/libs/redbytefx/index.html` (or the root redirect at `build/docs/site/index.html`).

## Contents

- Per-module Dokka HTML under `libs/redbytefx/redbytefx-core/`, `libs/redbytefx/redbytefx-gl/`, and related paths.
- Markdown guides at `libs/redbytefx/docs/` (`language-reference.md`, `error-codes.md`).
