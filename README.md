# tsql-scriptdom-java

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/)
[![Maven Central](https://img.shields.io/badge/Maven%20Central-Snapshot-orange.svg)](#)

A pure Java port of Microsoft's **ScriptDOM** (`Microsoft.SqlServer.TransactSql.ScriptDom`), the canonical Transact-SQL (T-SQL) parser and Abstract Syntax Tree (AST) engine powering SQL Server Data Tools (SSDT), Azure Data Studio, and SQL Server Management Studio (SSMS).

Built to provide deterministic, linear-time $O(N)$ T-SQL parsing for the Java ecosystem—specifically designed to power an open-source **SonarQube Community T-SQL plugin**, database migration linters (Flyway, Liquibase), and static analysis tools.

---

## 🎯 Motivation: Why ScriptDOM for Java?

1. **The SonarQube Gap:** SonarQube’s official T-SQL analyzer is closed-source and gated behind the paid commercial Developer Edition ($$$). SonarQube Community Edition has no native T-SQL analyzer.
2. **The ANTLR 4 Pitfall:** Community ANTLR 4 T-SQL grammars (`antlr/grammars-v4`) suffer from catastrophic backtracking and memory bloat under ANTLR 4's dynamic $ALL(*)$ algorithm when parsing complex, ambiguous T-SQL statements. Furthermore, generic parse trees require thousands of lines of fragile manual glue before inspection rules can run.
3. **Canonical Microsoft Engine:** Microsoft open-sourced [microsoft/SqlScriptDOM](https://github.com/microsoft/SqlScriptDOM) under the **MIT License**. It features:
   * A hand-tuned, deterministic **$LL(2)$ lookahead** parser (via ANTLR 2.7.5).
   * A declarative XML schema (`Ast.xml`) defining ~500+ strongly-typed AST nodes (`SelectStatement`, `TableReference`, etc.).
   * A complete visitor pattern hierarchy (`TSqlFragmentVisitor`).
   * Over 1,100 production test cases directly from Microsoft SQL Server engineering.

---

## 🏛️ Architecture & Project Structure

The project is structured as a multi-module Maven build targeting **Java 17+**:

```
tsql-scriptdom-java/
├── pom.xml                     <-- Parent POM
├── tsql-scriptdom-astgen/      <-- Code generator parsing Ast.xml -> Java AST & Visitors
│   ├── src/main/java/          <-- Java AST generator implementation (using JavaPoet)
│   └── src/main/resources/     <-- Ast.xml (upstream schema)
└── tsql-scriptdom-core/        <-- Runtime parser & AST library
    ├── src/main/java/          <-- Parser facade, base token filters, runtime helpers
    └── src/main/resources/
        ├── grammar/            <-- TSql160.g (ANTLR v2) & TSqlTokenTypes.g
        └── schema/             <-- Ast.xml
```

### Key Engineering Decisions

* **Scope Reduction:** Upstream ScriptDOM contains 750 files dedicated to `ScriptGenerator` (formatting ASTs back into SQL text). Because static analysis and linters only **inspect** ASTs and never regenerate T-SQL scripts, `ScriptGenerator` is omitted. This reduces porting complexity by over 50%.
* **Superset Parser Architecture (`TSql180`):** The parser engine is powered by Microsoft's latest **`TSql180` grammar (SQL Server 2025 / Azure SQL bleeding-edge)**, including Vector search, vector indexes, DiskANN, JSON enhancements, and windowing.
* **Compatibility Validation (`SqlCompatibilityValidator`):** Rather than bundling 12 separate ANTLR grammars (~1,000,000 lines of generated code and 25MB of JAR bloat), the library uses a single high-performance superset parser combined with an AST visitor `SqlCompatibilityValidator` that checks compatibility against any target `SqlVersion` (SQL Server 2000 / 80 through SQL Server 2025 / 180).
* **Standard ANTLR 2 Runtime:** Powered by `antlr:antlr:2.7.7`, ensuring 100% pure Java execution with zero native binary or platform dependencies.

---

## 🗺️ Roadmap & Milestones

- [x] **Phase 0:** Project scoping, upstream architectural audit, and repo initialization.
- [x] **Phase 1: AST Code Generation (`tsql-scriptdom-astgen`)**
  - [x] Port `AstGen` to Java using JavaPoet.
  - [x] Generate ~500 strongly-typed AST classes from `Ast.xml`.
  - [x] Generate `TSqlFragmentVisitor` and `TSqlConcreteFragmentVisitor`.
- [x] **Phase 2: Base Parser Runtime & Lexer**
  - [x] Port `TSqlTokenFilter` (comment and whitespace handling).
  - [x] Port `TSqlLexerBaseInternal` and `TSql160ParserBaseInternal` (predicate helpers and validation tables).
- [x] **Phase 3: Grammar Translation & Superset Parser (`TSql180.g`)**
  - [x] Adapt C# semantic actions to Java in `TSql180.g` and `TSqlTokenTypes.g`.
  - [x] Compile superset parser via ANTLR 2.7.7.
  - [x] Implement public facade `TSql180Parser` and backwards-compatible `TSql160Parser`.
  - [x] Implement `SqlVersion` enum (`SQL80` to `SQL180`) and `SqlCompatibilityValidator`.
- [x] **Phase 4: Upstream Regression Verification**
  - [x] Ingest upstream test scripts from Microsoft's test suite into parameterized JUnit 5 tests (489 scripts ingested, 95.1% AST yield, 86/86 JUnit tests passing).
- [ ] **Phase 5: Standalone Packaging & SonarQube Language Plugin**
  - [ ] Package standalone CLI / Maven Central artifact for downstream consumption.
  - [ ] Develop `sonar-tsql-plugin` providing syntax highlighting, CPD duplication, code metrics, and an initial catalog of quality & security rules.

---

## 📄 License & Attribution

This project is licensed under the [MIT License](LICENSE).

`tsql-scriptdom-java` is derived from Microsoft's open-source [SqlScriptDOM](https://github.com/microsoft/SqlScriptDOM), Copyright &copy; Microsoft Corporation, licensed under the MIT License. See [NOTICE.md](NOTICE.md) for full attribution.
