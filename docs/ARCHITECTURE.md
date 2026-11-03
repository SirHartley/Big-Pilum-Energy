# Architecture

Data map for the current implementation. Paths are repository-relative. The mod has no Java code and no mod plugin; everything it does comes from files the game merges at load.

| Reference | Scope |
|---|---|
| [CLAUDE.md](../CLAUDE.md) | Workflow, checks and document upkeep |
| [RELEASE.md](RELEASE.md) | Version numbers, release tooling, package contents and publishing |
| [CHANGELOG.md](CHANGELOG.md) | Change-note format and upkeep for every implementation task |

## Start here

| Change / symptom | Route |
|---|---|
| Pilum speed, damage or any other weapon stat | `data/weapons/weapon_data.csv`; [Weapon rows](#weapon-rows) |
| The mod's icon in the LunaLib settings menu | `data/config/LunaSettingsConfig.json`, `_best_missile.png`; [LunaLib](#lunalib) |
| Mod name, id, version, description | `mod_info.json` (the version is stamped at packaging, see [RELEASE.md](RELEASE.md)) |
| Version Checker | `BigPilumEnergy.version`, registered in `data/config/version/version_files.csv`; owned by [RELEASE.md](RELEASE.md) |

## Data files

| File | What it defines |
|---|---|
| `mod_info.json` | Id `pilumdoublespeed`, name `Big Pilum Energy`, game `0.98a`. No `jars`, no `modPlugin`, no dependencies, not marked `utility` |
| `data/weapons/weapon_data.csv` | Full copies of the vanilla `pilum` and `pilum_large` rows with changed values |
| `data/config/LunaSettingsConfig.json` | LunaLib config for mod id `pilumdoublespeed`: `iconPath` only |
| `_best_missile.png` | 40x40 icon named by `iconPath`; at the mod root, not under `graphics/` |
| `data/config/version/version_files.csv` | Registers `BigPilumEnergy.version` for Version Checker |

## Weapon rows

Both rows are full copies of the vanilla rows from `0.98a-RC8`; only these cells differ:

| Weapon id | Column | Vanilla | Mod |
|---|---|---|---|
| `pilum` (Pilum LRM Launcher) | `proj speed` | 125 | 200 |
| `pilum` | `speedStr` | Very Slow | Slow |
| `pilum_large` (Pilum LRM Catapult) | `proj speed` | 125 | 200 |
| `pilum_large` | `speedStr` | Very Slow | Slow |

- A mod CSV row with the same `id` replaces the whole vanilla row, so every other column must stay a copy of the vanilla value. Recheck the copies against the skill's `data/weapons/weapon_data.csv` after a game update.
- For missiles, `proj speed` is the first stage's max speed. Both launchers fire `type_1_lrm` (`data/weapons/pilum.wpn`, `pilum_large.wpn`); its `engineSpec` sets acceleration and turning but no max speed. The MIRV second stage `pilum_second_stage.proj` has its own `maxSpeed` of 300 and is not changed.
- `flight time` (30) is unchanged, so the first stage can fly further before it expires; `range` (4000) is unchanged.
- `speedStr` is tooltip text only.

## LunaLib

- LunaLib is optional: `mod_info.json` names no dependency, and without LunaLib the config file is never read.
- LunaLib's settings menu lists only mods that have rows in `data/config/LunaSettings.csv`. This mod has none, so `iconPath` is loaded by nothing and the icon does not appear in the menu. Checked in `lunalib.backend.ui.settings.LunaSettingsUIModsPanel.createModsList`, which filters enabled mods by the mod ids in `LunaSettingsLoader.getSettingsData()`, from the `LunaLib.jar` in Industrial.Evolution's `lib/`.
- When the mod is listed, LunaLib calls `Global.getSettings().loadTexture(iconPath)` itself, so the icon does not have to be preloaded in `settings.json` with that LunaLib version.

## Save data

None. The mod changes spec values only; removing it restores vanilla values on the next game start.
