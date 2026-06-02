# Trademark Notice

> Last updated: 2026-06-02 · Status: codename strategy (no formal trademark
> filings yet). See `docs/cs/README.md` §6 for the standalone ServiceOS path.

## Marks

| Mark | Type | Holder |
|---|---|---|
| **2Africa.AI** | House mark / customer-facing brand | 2Africa AI Ltd. (申请中 / pending registration) |
| **AgriOS** | **Internal engineering codename only** — not a brand | Used in source code paths, Java packages, database names, container images. **Not used in customer-facing UI, marketing, or sales materials.** |

## Codename vs Brand boundary

The customer-facing brand is **2Africa.AI**. The string `AgriOS` appears in
this repository for one reason only: it is the internal engineering
codename for the source tree, in the same way that "Chromium" is the
internal codename for Google Chrome. Specifically, `AgriOS` is used in:

* Java package names (e.g. `ai.toafrica.agrios.*`)
* Container image names (e.g. `agrios-backend`)
* Database names (`toafrica_agrios`)
* Git repository name (`2Africa-AgriOS`)
* Internal developer documentation

`AgriOS` is **not** used in:

* The UI logo (which shows `2Africa.AI`)
* User-facing menus, dialogs, error messages
* Marketing copy, landing pages, sales decks
* Press / media announcements
* App store listings

This boundary is consistent with the Apache 2.0 license §3, which grants
no trademark rights. Forks of this codebase are licensed under Apache 2.0
to use the source code, but they **may not** use the `2Africa.AI` mark
nor pass themselves off as `2Africa AgriOS` products.

## Not affiliated with agrios.org / Advance Insight BV

This project is **not affiliated with, endorsed by, or connected to**
**agrios.org** (the Odoo-based African agri-SME ERP operated by
**Advance Insight BV**, Netherlands, with subsidiaries in Kenya and
Uganda).

The two products serve overlapping markets (African agriculture
software). Any similarity in the internal codename `AgriOS` is
coincidental — both names independently derive from the descriptive
construction `Agri` + `OS` ("agriculture operating system"), which is a
common-vocabulary phrase neither party invented.

Users looking for **agrios.org**'s product should visit
<https://agrios.org/>. Users looking for the present project's customer-
facing product should visit (2Africa.AI properties — TBA).

## If you are reading this as a contributor or downstream consumer

If you are forking this code base for your own product:

1. **Do not use `2Africa.AI` in your fork's brand or UI.** Apache 2.0 §3
   does not grant trademark rights.
2. **You may keep `agrios` in code paths** (it's a codename, not a mark).
3. If you ship your fork to customers, choose your own brand name and
   conduct your own trademark clearance (especially against
   `agrios.org` if you target African agriculture markets).
4. Add your own `TRADEMARK.md` declaring your brand and the boundaries
   relative to this upstream project.

## Contact

Trademark / brand questions: see `SECURITY.md` for the project contact
channel. Replies on a best-effort basis; this notice is not a legal
opinion.
