# Contributing to 2Africa AgriOS

First off, **thank you** for considering contributing to AgriOS. This project's
mission is to digitize African agriculture in a way that respects data
sovereignty, works offline, and stays open. Every contributor helps push that
mission forward.

## Code of Conduct

This project follows the [Contributor Covenant 2.1](./CODE_OF_CONDUCT.md). By
participating you agree to its terms.

## How to contribute

There are many ways to help:

- **Report bugs** — open an issue with steps to reproduce
- **Suggest features** — open an issue describing the use case
- **Improve documentation** — typos, clarifications, examples
- **Translate the UI** — we currently ship English / 中文 / Swahili
- **Fix bugs / add features** — pick an open issue and submit a PR
- **Write tests** — bring up coverage on under-tested areas

## Before you start coding

1. **Open an issue first** for any non-trivial change. This avoids you spending
   a weekend on something we'd rather solve differently.
2. **Search existing issues** to see if someone is already working on it.
3. **Comment "I'd like to take this"** on the issue you want to work on; a
   maintainer will assign it to you.

## Development setup

```bash
# Clone
git clone https://github.com/2AfricaAI/2Africa-AgriOS.git
cd 2Africa-AgriOS

# Backend (Java 17 + Maven + Docker)
cd backend
docker compose up -d   # starts MySQL + Redis + MinIO + the API
docker compose logs -f backend

# Frontend (Node 20+)
cd ../frontend
npm install
npm run dev
```

See [DEPLOY.md](./DEPLOY.md) for production deployment.

## Pull-request checklist

- [ ] Branch from `main` (e.g. `feature/excel-import-warehouses`)
- [ ] Commit messages use the [Conventional Commits](https://www.conventionalcommits.org) style
  (e.g. `feat(import): add warehouse Excel import`)
- [ ] Each commit is signed off with a DCO line (`Signed-off-by: Your Name <email>`).
  Add it automatically with `git commit -s`.
- [ ] All existing tests pass (`mvn test` and `npm test`)
- [ ] New code includes tests where reasonable
- [ ] Public APIs documented in Swagger annotations
- [ ] No secrets / credentials in source
- [ ] All Java / Vue files include the SPDX header
  (`// SPDX-License-Identifier: Apache-2.0`) at the top

## Developer Certificate of Origin (DCO)

We use the [Developer Certificate of Origin](https://developercertificate.org/)
in lieu of a CLA. Every commit must be signed off (`git commit -s`), which
adds a `Signed-off-by` line affirming that you have the right to contribute
the change under the project's Apache 2.0 license.

## Reporting security vulnerabilities

Please **do not** open a public issue for security problems. Email
`security@2africa.ai` — see [SECURITY.md](./SECURITY.md) for details.

## Style guide

### Java (backend)

- Java 17, Spring Boot 3, MyBatis-Plus
- 4-space indent, Lombok where it cuts boilerplate
- Service methods are transactional only when they need to be
- Controllers stay thin; logic in services
- Use `BusinessException` for user-facing errors with i18n keys

### Vue 3 (frontend)

- `<script setup>` + Composition API
- Element Plus + Tailwind-style utility classes (sparingly)
- Every UI string goes through `t('...')` — never hard-code
- API calls go through `src/api/<resource>.js`
- One Vue file per page; reusable bits become components in `src/components/`

### Commit messages

Examples:

```
feat(qc): add expiry-date column to QC inspection list
fix(packing): handle FEFO ties when batches share same expiry
docs(readme): clarify M-Pesa stub vs real-integration status
refactor(auth): move JWT bootstrap into framework/security
test(plot): add CRUD smoke test
chore(deps): bump Spring Boot to 3.2.5
```

The first word after `:` is the imperative verb — "add", not "added" or "adds".

## License of contributions

By submitting a PR you agree that your contribution is licensed under
[Apache 2.0](./LICENSE), the same license as the rest of the project. The DCO
sign-off confirms this. No CLA required.

## Community channels

- GitHub Issues: feature requests, bug reports
- GitHub Discussions: open-ended questions, architecture proposals
- Email: `community@2africa.ai`

Welcome to AgriOS. We're glad you're here.
