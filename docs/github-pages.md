# GitHub Pages (API site)

The workflow [`.github/workflows/docs.yml`](../.github/workflows/docs.yml) runs `qualityCheck` and `dokkaHtmlSite` on push to `main`, `master`, or `support-opengl`. **Publishing** to GitHub Pages runs on push to **`main` or `master`** only.

## One-time repository setup

1. Open **Settings → Pages**.
2. Under **Build and deployment**, set **Source** to **GitHub Actions**.
3. After a successful **deploy** job on `master`, open the published URL from the Pages settings.

## URL layout (this repository)

This is a **project** site:

| What | URL |
|------|-----|
| Index | `https://i-redbyte.github.io/redbytefx/` |
| Module API | `https://i-redbyte.github.io/redbytefx/redbytefx-core/…` |
| Guides | `https://i-redbyte.github.io/redbytefx/docs/language-reference.md` |

## Source links in Dokka

Dokka **(source)** links use `https://github.com/i-redbyte/redbytefx/blob/<branch>/…`. CI sets the branch from `GITHUB_REF_NAME` (on `master`, links point at `master`). Locally override with `redbytefx.docsGitRef` in `gradle.properties` (default: `master`).

## Local preview

```bash
./gradlew dokkaHtmlSite
```

Open `build/docs/site/index.html`.
