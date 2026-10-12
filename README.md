# tsql-scriptdom-java

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/)
[![Release](https://img.shields.io/github/v/release/fupacat/tsql-scriptdom-java)](https://github.com/fupacat/tsql-scriptdom-java/releases/latest)
[![Javadoc](https://img.shields.io/badge/Javadoc-GitHub%20Pages-blue.svg)](https://fupacat.github.io/tsql-scriptdom-java/)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=fupacat_tsql-scriptdom-java&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=fupacat_tsql-scriptdom-java)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=fupacat_tsql-scriptdom-java&metric=coverage)](https://sonarcloud.io/summary/new_code?id=fupacat_tsql-scriptdom-java)

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

## 📦 Installation

Add the dependency to your project:

### Maven
```xml
<dependency>
    <groupId>io.github.fupacat</groupId>
    <artifactId>tsql-scriptdom-core</artifactId>
    <version>0.1.1</version>
</dependency>
```

### Gradle
```groovy
implementation 'io.github.fupacat:tsql-scriptdom-core:0.1.1'
```

> **Note:** `tsql-scriptdom-core` has **zero external transitive dependencies**. The ANTLR runtime is completely shaded and relocated internally (`io.github.fupacat.internal.antlr`) to eliminate classpath collisions.

---

## 🚀 Quickstart & Usage

### 1. Basic Parsing
```java
import io.github.fupacat.ParseResult;
import io.github.fupacat.TSqlParser;
import io.github.fupacat.ast.TSqlScript;

String sql = "SELECT u.id, u.username FROM dbo.Users AS u WHERE u.isActive = 1;";

ParseResult<TSqlScript> result = TSqlParser.parse(sql);

if (result.isSuccess()) {
    TSqlScript script = result.fragment();
    System.out.println("Parsed script batches: " + script.Batches.size());
} else {
    result.errors().forEach(err -> 
        System.err.println("Line " + err.line + ", Col " + err.column + ": " + err.message)
    );
}
```

### 2. Inspecting the AST with Visitors
```java
import io.github.fupacat.ast.SelectStatement;
import io.github.fupacat.ast.TSqlConcreteFragmentVisitor;

result.fragment().accept(new TSqlConcreteFragmentVisitor() {
    @Override
    public void visit(SelectStatement selectStatement) {
        System.out.println("Discovered SELECT query: " + selectStatement);
    }
});
```

### 3. Checking SQL Server Version Compatibility
```java
import io.github.fupacat.SqlVersion;

// Check if modern SQL 2022/2025 syntax is valid on a legacy SQL Server 2016 target
ParseResult<TSqlScript> legacyCheck = TSqlParser.parse(
    "SELECT c1, SUM(c2) OVER w FROM dbo.t WINDOW w AS (PARTITION BY c1);",
    SqlVersion.SQL130 // SQL Server 2016
);

if (legacyCheck.hasErrors()) {
    legacyCheck.errors().forEach(e -> System.out.println("Incompatible: " + e.message));
}
```

### 4. Standalone CLI Usage
The packaged JAR can be executed directly as a standalone CLI to validate T-SQL queries or files:
```bash
# Validate inline T-SQL
java -jar tsql-scriptdom-core-0.1.0-SNAPSHOT.jar "SELECT 1 FROM dbo.t;"

# Validate a SQL script file
java -jar tsql-scriptdom-core-0.1.0-SNAPSHOT.jar -f /path/to/script.sql
```

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
  - [x] Ingest upstream test scripts from Microsoft's test suite into parameterized JUnit 5 tests (458 scripts verified, **100.0% pass rate**: 454 positive ASTs + 4 verified negative/deprecated syntax rejection tests, 90/90 JUnit tests passing).
- [x] **Phase 5: Standalone Packaging, CI/CD, & Distribution**
  - [x] Package shaded standalone CLI (`tsql-scriptdom-core`) with zero external runtime dependencies.
  - [x] High-level facade (`TSqlParser`, `ParseResult`) and AST visitor traversal API.
  - [x] CI/CD multi-JDK test matrix (Java 17 & 21), automated release workflow, and GitHub Packages / Maven Central distribution.
- [ ] **Phase 6: SonarQube Language Plugin (`sonar-tsql-plugin`)**
  - [ ] Standalone companion repository consuming `tsql-scriptdom-core`.
  - [ ] Syntax highlighting, CPD duplication, code metrics, and AST-based quality & security rules.

---

## 🔍 Dialect Scope & Syntax Support

### Core SQL Server Engine (Supported)
`tsql-scriptdom-java` provides full coverage of the standard Transact-SQL engine powering:
* **Box SQL Server:** SQL Server 2000 (`SQL80`) through SQL Server 2025 (`SQL180`).
* **Azure SQL:** Azure SQL Database, Azure SQL Managed Instance.
* **Modern Features:** Vector search / distance metrics, vector indexes, DiskANN, JSON enhancements (`JSON_OBJECT`, `JSON_ARRAY`, `JSON_OBJECTAGG`, `JSON_ARRAYAGG`, `RETURNING JSON`), modern windowing, and graph tables.

### Warehouse Dialects (Not Included in Core Engine)
Specialized cloud data warehouse engines implement divergent syntax not present in the standard SQL Server database engine:
* **Microsoft Fabric Data Warehouse (`EngineType.FabricDW`):** Fabric-specific statements such as `CLUSTER BY`, Fabric AI functions (`AI_GENERATE_RESPONSE`, `AI_SUMMARIZE`), and `ALTER EXTERNAL FUNCTION`.
* **Azure Synapse / Azure SQL Data Warehouse (`EngineType.SqlDw`):** MPP distribution syntax such as `DISTRIBUTION = HASH(...)` / `ROUND_ROBIN` in `CREATE MATERIALIZED VIEW`, Azure DW `COPY INTO`, and workload classification for MPP.

These warehouse scripts are omitted from the core engine test suite because they require warehouse-specific ANTLR grammar variants.

### Obsolete Syntax & Negative Testing
Transact-SQL has intentionally removed obsolete constructs across major versions:
* `DISK INIT` (SQL Server 6.5 / 7.0 disk device management, removed in SQL Server 2005).
* `DUMP DATABASE` and `LOAD DATABASE` (Sybase heritage commands, removed in SQL Server 2005/2008).
* `FASTFIRSTROW` table hint (deprecated in SQL Server 2005/2008, removed in SQL Server 2012 / `TSql110`).

In upstream Microsoft SqlScriptDOM, scripts testing these removed constructs (`MiscTests80.sql`, `DumpLoadStatement90Tests.sql`, `MiscDeprecatedIn110Tests.sql`) are marked as negative tests when evaluated against modern parsers (`TSql110` through `TSql180`). Our regression suite validates them as negative tests to guarantee that obsolete syntax is properly flagged and rejected by the modern `TSql180` grammar.

---

## 📄 License & Attribution

This project is licensed under the [MIT License](LICENSE).

`tsql-scriptdom-java` is derived from Microsoft's open-source [SqlScriptDOM](https://github.com/microsoft/SqlScriptDOM), Copyright &copy; Microsoft Corporation, licensed under the MIT License. See [NOTICE.md](NOTICE.md) for full attribution.
