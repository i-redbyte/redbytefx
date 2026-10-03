# GitHub Pages (API site)

The workflow [`.github/workflows/docs.yml`](../.github/workflows/docs.yml) builds `./gradlew dokkaHtmlSite` and deploys `build/docs/site` to GitHub Pages on push to `main`, `master`, or `support-opengl`.

## One-time repository setup

1. Open **Settings → Pages**.
2. Under **Build and deployment**, set **Source** to **GitHub Actions**.
3. After a successful workflow run, open the published URL from the Pages settings.

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
