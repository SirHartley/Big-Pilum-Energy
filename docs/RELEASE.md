# Release

How Big-Pilum-Energy is packaged and published. Nothing here runs unless a release is asked for: no version bumps, tags or published releases for ordinary work. Changelog entry rules are in [CHANGELOG.md](CHANGELOG.md); the data map is in [ARCHITECTURE.md](ARCHITECTURE.md).

## Pieces

| Piece | Path | Role |
|---|---|---|
| Changelog | `Changelog` | Source of the version and the release notes. The newest version heading is the release |
| Mod metadata | `mod_info.json` | Version stamped into the ZIP's copy |
| Version file | `BigPilumEnergy.version` | Online Version Checker file on `master`, registered in `data/config/version/version_files.csv`; stamped at packaging, committed back to `master` after a release |
| Release tool | `tools/ReleaseBuild.java` | `package`, run with `java <file>`; default package, no `build` command since there is nothing to compile |
| Tool checks | `tools/ReleaseBuildChecks.java` | Headless checks of changelog parsing, stamping and file filtering |
| Release workflow | `.github/workflows/release.yml` | Manual only: preview without a version, publish with one |

The tool is the house `ReleaseBuild` template with the jar build, library and source hash parts removed. Keep `FOLDER`, `VERSION_FILE`, `RELEASES` and `REQUIRED` at the top of `ReleaseBuild` aligned with the workflow and the version file.

## Versions

Versions use `major.minor.letter`: the first number for a major release, the second for changes players will notice, the letter for fixes and small patches. Published: `1.0.c`, `1.0.d`. The newest `Changelog` section is marked `- UNRELEASED` until it ships.

## Package

From the repository root, with JDK 17 or newer:

```text
java tools/ReleaseBuild.java package          # preview
java tools/ReleaseBuild.java package 1.0.e    # release
```

Run the checks first (as the workflow does):

```text
mkdir -p out/release-tools
javac --release 17 -encoding UTF-8 -d out/release-tools tools/ReleaseBuild.java tools/ReleaseBuildChecks.java
java -cp out/release-tools ReleaseBuildChecks
```

- Preview packages the newest changelog section, even if `UNRELEASED`.
- Release requires the version (no `v`) to equal the newest changelog heading, and that section must not be `UNRELEASED`.
- Ships Git-tracked files only: `mod_info.json`, `BigPilumEnergy.version`, `Changelog`, `_best_missile.png`, license and credit files, `data/` (`csv json shader ship wpn proj system variant skin`), `graphics/` (`png jpg jpeg`), `sounds/` (`ogg wav`). Docs, tools, IDE (`.idea/`, `*.iml`), agent and workflow files, and any path under a dot, `unused`, `unedited`, `tools` or `docs` directory are left out. Stage new files before a local preview.
- Fails if a required file is missing: `Changelog`, `mod_info.json`, the version file, `_best_missile.png`, `data/weapons/weapon_data.csv`, `data/config/LunaSettingsConfig.json`, `data/config/version/version_files.csv`.
- Output in `out/release/` (gitignored): `Big.Pilum.Energy-<version>.zip` (one `Big.Pilum.Energy/` folder, sorted, fixed timestamps), `release-notes.md`, the stamped version file and `version.txt`.
- Releases up to `1.0.d` were packed by hand as `Big.Pilum.Energy<version>.zip` with a `Big Pilum Energy<version>/` folder. The game finds the mod by id, so the new folder name only means players delete the old folder when they update.

## Publish

1. Finish the newest `Changelog` section and remove `UNRELEASED`.
2. Run the checks and `package <version>` locally as the preview check.
3. Merge `dev` into `master`.
4. Start the Release workflow on `master` with `version` set: Actions tab, Release, Run workflow, or `gh workflow run release.yml --ref master -f version=<version>`. GitHub only offers a manual workflow once the file is on the default branch (`master`).
5. The workflow runs the checks, packages, creates the release and its tag on the packaged commit, then commits the stamped `BigPilumEnergy.version` to `master`.
6. Check the release ZIP and notes, and that `master`'s version file names the new version. Merge `master` back into `dev`.

The workflow has no push, pull request, tag or schedule trigger. Without a version it only uploads a preview artifact. It refuses to overwrite an existing release, and run on an existing version tag it publishes that tag. It downloads nothing.

## Maintenance

Update this guide when `ReleaseBuild`, `ReleaseBuildChecks`, `.github/workflows/release.yml` or the version file change.
