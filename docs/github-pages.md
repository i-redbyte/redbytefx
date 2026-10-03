# GitHub Pages (API site)

The workflow [`.github/workflows/docs.yml`](../.github/workflows/docs.yml) always runs `qualityCheck` and `dokkaHtmlSite` on push to `main`, `master`, or `support-opengl`. **Publishing** to GitHub Pages runs only on **`main` or `master`** (the `github-pages` environment usually allows those branches only).

To refresh the live site from `support-opengl`, merge into `master` (or temporarily add `support-opengl` under **Settings → Environments → github-pages → Deployment branches**).

## One-time repository setup

1. Open **Settings → Pages**.
2. Under **Build and deployment**, set **Source** to **GitHub Actions**.
3. After a deploy job succeeds on `master`/`main`, open the published URL from the Pages settings.

## URL layout (this repository)

This is a **project** site. GitHub serves it under the repository name:

| What | URL |
|------|-----|
| Index | `https://i-redbyte.github.io/redbytefx/` |
| Module API | `https://i-redbyte.github.io/redbytefx/redbytefx-core/…` |
| Guides | `https://i-redbyte.github.io/redbytefx/docs/language-reference.md` |

You cannot get `https://i-redbyte.github.io/libs/redbytefx/…` (without `/redbytefx/` in the path) from this repo’s Pages alone. That layout needs a **user/org** site repository named `<user>.github.io`, with content under `libs/redbytefx/` and a deploy step that pushes there (separate from this project workflow).

## Source links in Dokka

Dokka **(source)** links use `https://github.com/i-redbyte/redbytefx/blob/<branch>/…`. CI uses `GITHUB_REF_NAME`; locally set `redbytefx.docsGitRef` in `gradle.properties` (default in repo: `support-opengl` until docs track `master`).

## Local preview

```bash
./gradlew dokkaHtmlSite
```

Open `build/docs/site/index.html`.
