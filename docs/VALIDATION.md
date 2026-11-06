# Validation

Authoring date: November 6, 2026. Host: Windows. Target: Windows, macOS, Linux desktop.

## Local result

Kotlin/Gradle toolchains are unavailable on the authoring host. CI builds and exercises the exact-cent model and file persistence.

## Reproducible checks

The GitHub Actions workflow builds/runs the following on `ubuntu-latest`:

```sh
gradle build --no-daemon
gradle run --args="--self-test" --no-daemon
```

## Manual acceptance

Record uneven-cent expenses, verify total balances sum to zero, restart, undo the last expense, export text, and test invalid payer/amount inputs.

A workflow file is not evidence of a passing run. Observed CI run links and source commits are recorded after publication. Native GUI, sound, sleep/resume, and platform permissions require direct target-platform acceptance even when model tests pass.

## Observed CI result

Passed on November 6, 2026: [target-platform build/check](https://github.com/netcodermike/fairshare-expense-splitter/actions/runs/42198923981).

Verified source commit: `3f0113420d18601cd5a7aaf01e983922f5a125bd`. This evidence identifies the exact tested source; a later documentation-only commit does not change that source.

The commands listed above passed. Native GUI/manual acceptance is not claimed by this CI result.
