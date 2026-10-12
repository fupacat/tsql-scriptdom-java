# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Code of Conduct (`CODE_OF_CONDUCT.md`) based on Contributor Covenant v2.1.
- Issue template chooser configuration (`.github/ISSUE_TEMPLATE/config.yml`).
- Keep a Changelog baseline (`CHANGELOG.md`).
- JaCoCo test code coverage and automated SonarCloud analysis workflow.
- Dependabot configuration for automated Maven and GitHub Actions updates.
- CodeQL SAST security analysis workflow for Java.

### Changed
- Standardized `LICENSE` header for automated SPDX MIT detection, preserving upstream Microsoft notices in `NOTICE.md`.

## [0.1.0] - 2026-10-10

### Added
- Pure Java port of Microsoft ScriptDOM (`TSqlParser`, `TSqlFragment`, and ~500+ strongly-typed AST nodes).
- Full `TSqlFragmentVisitor` and `TSqlConcreteFragmentVisitor` visitor hierarchy.
- Complete regression test suite with 100% pass rate across upstream Microsoft test scripts.
- Automated code generator (`tsql-scriptdom-astgen`) using JavaPoet and upstream `Ast.xml`.
- Zero runtime external dependency shaded distribution (`tsql-scriptdom-core`).
- Automated release workflow with publishing to Maven Central and GitHub Packages.
