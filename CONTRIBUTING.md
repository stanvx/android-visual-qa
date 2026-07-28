# Contributing

Thank you for your interest in contributing. The project is currently in
the M0 milestone — repository, contracts, schema, and tests. Contributions
are most welcome in:

- Pure-Kotlin modules (`:core:model`, `:core:geometry`, `:core:testing`).
- Report schema and serialization (versioned, `@Serializable`).
- Capture state machine and command types.
- Documentation, examples, and golden report corpus fixtures.

## Workflow

1. Open an issue describing the change and its motivation.
2. Wait for maintainer sign-off before opening a pull request for
   non-trivial work — schema, permission, Gradle, or public API changes.
3. Use feature branches; keep pull requests focused.
4. Run `./gradlew test` locally before pushing.
5. All commits must be signed off (`git commit -s`).

## Code style

- Kotlin official style.
- Public APIs require KDoc and `@since` markers.
- Schema changes must update both `schemaVersion` and the golden corpus.
- No raw screen text, screenshots, voice content, or accessibility trees
  in logs.

## Reporting security issues

Please email `security@example.invalid` (placeholder — replace with the
upstream address before public use) instead of opening a public issue.
