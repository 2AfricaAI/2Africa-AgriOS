# Trademark Notice

> Last updated: 2026-06-02 · Status: compound-mark strategy + agrios.org
> disclaimer. No formal registrations filed yet; this is policy
> documentation, not legal advice.

## Marks

| Mark | Type | Holder | Where it appears |
|---|---|---|---|
| **2Africa.AI** | House mark (parent company) | 2Africa AI Ltd. (pending registration) | All customer-facing material, lead position of the compound mark |
| **AgriOS** | Product mark | 2Africa AI Ltd. (pending registration) | Compound mark only — never standalone in marketing |
| **2Africa.AI AgriOS** | Compound brand (canonical public form) | 2Africa AI Ltd. | UI logo, README, marketing, sales decks, app stores |

## Brand strategy — compound mark

The customer-facing product brand is **always shown in its compound
form**:

```
2Africa.AI AgriOS
^^^^^^^^^^ ^^^^^^^
house mark product mark
```

* `2Africa.AI` is the dominant house mark. It is always present and
  visually weighted equal-or-stronger than the product mark.
* `AgriOS` is the product mark. It identifies which 2Africa.AI product
  this is (vs future siblings: 2Africa.AI RetailOS / FactoryOS /
  TravelOS / AgriCloud / MarketOS).
* **The product mark `AgriOS` should not appear standalone** in
  customer-facing material. If a heading or label only has room for one
  word, prefer the house mark `2Africa.AI`.

### Why the compound mark instead of "codename only"

The earlier policy (in commit `bea1462` / Sprint 48b) treated `AgriOS`
as internal-codename-only. After review, the compound-mark strategy
was adopted instead because:

1. **`AgriOS` is descriptive**, not arbitrary — it is the common
   construction `Agri` + `OS` (`agriculture operating system`). Multiple
   parties can legitimately use a descriptive term for their own
   products, the way multiple companies can sell "potato chips".
2. **House mark + product mark together** is the industry standard for
   software product families (e.g. Apple iOS, Google Cloud, Microsoft
   Azure). The `2Africa.AI` prefix provides clear source identification
   to consumers.
3. **agrios.org's prior use** is mitigated by the prominent
   `2Africa.AI` lead: a reasonable consumer encountering
   `2Africa.AI AgriOS` is unlikely to confuse it with `agrios.org`.

## Where `AgriOS` (standalone) does appear

`AgriOS` continues to be used as an internal-engineering identifier
inside the codebase. These usages are not customer-facing branding:

* Java package names (`ai.toafrica.agrios.*`)
* Container image names (`agrios-backend`)
* Database names (`toafrica_agrios`)
* Git repository name (`2Africa-AgriOS`)
* Internal developer documentation
* GitHub Release titles (technical audience)

These are engineering identifiers that historically predate the brand
finalization, and changing them now would cost ~3-5 working days with
zero customer-visible benefit. They will be aligned in a future major
version if business reasons demand.

## Not affiliated with agrios.org / Advance Insight BV

This project is **not affiliated with, endorsed by, or connected to**
**agrios.org** (the Odoo-based African agri-SME ERP operated by
**Advance Insight BV**, Netherlands, with subsidiaries in Kenya and
Uganda).

The product marks differ in both visual presentation and source
identification: the present project ships under
**`2Africa.AI AgriOS`** (compound mark with the `2Africa.AI` house
mark prominently leading). `agrios.org` ships under their own brand
without the `2Africa.AI` prefix.

Users looking for **agrios.org**'s product should visit
<https://agrios.org/>. Users looking for the present project's
customer-facing product should look for the **2Africa.AI** parent
brand.

## License vs trademark separation

This codebase is open source under Apache 2.0 (proposed; see the
top-level LICENSE file once it lands). Apache 2.0 §3 grants license to
the source code but **does not grant trademark rights**.

If you fork this code base:

1. **You may keep `agrios` in code paths** — that is the engineering
   codename, not the brand.
2. **You may not use the marks `2Africa.AI`, `2Africa.AI AgriOS`, or
   any confusingly similar combination** in your fork's brand, UI,
   marketing, or naming. Pick your own brand.
3. Conduct your own trademark clearance against `agrios.org` (and any
   other regional incumbents) if you target African agriculture
   markets.
4. Add your own `TRADEMARK.md` declaring your fork's brand and the
   boundaries relative to this upstream project.

## Contact

Trademark / brand questions: see `SECURITY.md` for the project contact
channel. Replies on a best-effort basis. This document is policy
documentation, not legal advice; consult IP counsel for binding
guidance.
