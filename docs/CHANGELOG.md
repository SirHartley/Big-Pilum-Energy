# Changelog guide

How agents write entries in the root `Changelog`. Read it for every implementation task. Version numbers, release steps and how the release tooling reads the file are in [RELEASE.md](RELEASE.md).

## When to write an entry

- Every implementation task adds or amends its entries in the current unreleased section before merge, in the same commit as the change.
- Read-only work and documentation-only changes add nothing. Say why in the PR when a change needs no entry.
- Check whether the feature has been released: look for it under every version heading without `UNRELEASED`.
  - Not released yet: edit its `Added -` entry so it describes the feature as it now stands.
  - Released: the change gets its own entry.
- Record only completed changes from the actual diff.

## File layout

`Changelog` at the repository root, plain text, no extension. The fixed `Known issues` and `Notes` sections come first (`None` when empty), then the `Changelog` section with versions newest first and a blank line between them.

- Version heading: the version alone on its line, optionally followed by ` - UNRELEASED` (next release) or ` - CURRENT PUBLIC VERSION` (newest published). No `v` prefix, no date, no Markdown. The release tooling reads these headings. Never add a save compatibility line unasked.
- Entries: one change per line, starting `Added - `, `Adjusted - `, `Fixed - ` or `Removed - `. A released section needs at least one.
- A `Note - ` line directly below an entry explains a limit or something the player must do.
- If no unreleased section exists, add one above the newest version. Ask the user which version comes next if it has not been chosen.
- The `1.0.d` and `1.0.c` sections keep the user's GitHub release text.

## Wording

The `house-style` skill's "Changelog entries" section owns the wording and wins over this guide. For this mod:

- Name the weapon as the game does: `Pilum LRM Launcher`, `Pilum LRM Catapult`.
- Numbers as old `->` new: `Adjusted - Pilum LRM Launcher missile speed 125 -> 200`.
- Short lines (under 80 characters), capital after the dash, no full stop on a one-clause line, no semicolons.
- No agent or model names, links, validation logs, commit hashes or claims about testing.
- The user's own lines (`Icon (extremely important)`, `Perfection needs no changes`) stay verbatim. Do not imitate their voice.

## Maintenance

Update this guide when the `Changelog` layout or entry format changes.
