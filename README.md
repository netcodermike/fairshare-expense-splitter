# FairShare Expense Splitter

Record shared expenses in exact cents, calculate equal shares, and propose payments that settle everyone’s balance.

[![Build and verify](https://github.com/netcodermike/fairshare-expense-splitter/actions/workflows/verify.yml/badge.svg)](https://github.com/netcodermike/fairshare-expense-splitter/actions/workflows/verify.yml)

**Language:** Kotlin · **Platform:** Windows, macOS, Linux desktop · **App type:** Desktop group expense app

First-release learning implementation, with explicit scope below. This is one independently runnable app, not a placeholder in a combined repository.

## Features

- 1–100 named participants, equal-split expenses, and descriptions.
- Exact integer-cent accounting with deterministic remainder distribution.
- Net balances and creditor/debtor settlement suggestions.
- Persistent local ledger, remove-last action, and text export.

## Requirements

JDK 17, Gradle 8.9, and network access on the first build to download Kotlin 2.0.21 from the configured Gradle/Maven repositories. No binary Gradle wrapper is bundled.

## Quick start

```sh
git clone https://github.com/netcodermike/fairshare-expense-splitter.git
cd fairshare-expense-splitter
gradle run
# Optional separate ledger path:
gradle run --args="trip.properties"
# Build an application distribution:
gradle installDist
```

Commands run from the repository root. Lines marked for a particular operating system apply only there. Language tools are prerequisites, not installed by the app. Close desktop windows normally; terminal/server apps stop with Ctrl+C unless their usage notes say otherwise.

## Example workflow

Keep Ada, Ben, and Cy, record Ada paying 10.00 for dinner, and inspect the balances. Add Ben paying 5.01 for lunch. The proposed payments should bring every balance to zero. Close/reopen, export the report, and remove the last expense.

## Architecture, configuration, and data

`src/main/kotlin/Main.kt` contains amount parsing, balances, settlement, Properties persistence, and a Swing form. Each expense credits its payer and debits equal shares. Remainder cents are assigned to the earliest participants in list order. A debtor-to-creditor greedy pass clears balances; it does not claim globally minimal transfers. Amounts are 0.01–10,000,000.00, participant count ≤100, and expense count ≤10,000. The default store is ledger.properties. Saves use same-directory atomic replacement when supported, with ordinary replacement fallback. Tests cover remainder conservation, settlement clearing, decimal validation, and save/load.

## Verification

Build with the quick-start command where needed, then run:

```sh
gradle run --args="--self-test" --no-daemon
```

Manual acceptance: Record uneven-cent expenses, verify total balances sum to zero, restart, undo the last expense, export text, and test invalid payer/amount inputs.

See [validation record](docs/VALIDATION.md) for observed checks and their limits. CI results verify only what the workflow runs; they do not establish native GUI correctness or production readiness.

## Limitations and next steps

One currency with two decimal places; no currency conversion, interest, tax, financial advice, unequal splits, participant changes after expenses, or partial repayments. Participant order determines who gets remainder cents. All expenses are shared by everyone. Progress is plaintext, with one writer expected. Export overwrites the chosen file. Settlement suggestions are bookkeeping, not payment execution.

## Troubleshooting

Use JDK 17 and Gradle 8.9 for the pinned Kotlin plugin. A headless session can run --self-test but not Swing. Amounts with nonzero fractions smaller than a cent are rejected. Payer spelling must match a participant. Corrupt ledgers fail explicitly; restore a backup or choose a new path.

## Source map

- `build.gradle.kts`
- `settings.gradle.kts`
- `src/main/kotlin/Main.kt`

## Development and license

Read [contributing](CONTRIBUTING.md) before changing the app, and [security/privacy](SECURITY.md) before supplying private data. Use synthetic fixtures for tests. No telemetry, paid API key, or account is required by the app itself. Dependencies, when present, are stated in Requirements.

[MIT license](LICENSE), copyright 2026 netcodermike.
