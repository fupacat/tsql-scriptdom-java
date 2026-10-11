# Contributing to tsql-scriptdom-java

Thank you for your interest in contributing to `tsql-scriptdom-java`!

This project is a high-performance, deterministic Java port of Microsoft's [SqlScriptDOM](https://github.com/microsoft/SqlScriptDOM), designed to power T-SQL static analysis, linters, and the SonarQube Community T-SQL plugin.

---

## 🛠️ Prerequisites

To build and contribute to this repository, you will need:
* **JDK 17+** (JDK 17 or JDK 21 recommended)
* **Apache Maven 3.8+**
* **Python 3.8+** (required by `exec-maven-plugin` during source generation)

---

## 🏗️ Building from Source

To compile all modules and run the full regression test suite:

```bash
mvn clean test
```

To produce the shaded distribution JAR:

```bash
mvn clean package -DskipTests
```

The resulting standalone zero-dependency artifact will be located at:
`tsql-scriptdom-core/target/tsql-scriptdom-core-<version>.jar`

---

## 🧪 Testing Guidelines

* **Regression Test Suite:** The regression suite in `tsql-scriptdom-core/src/test/resources/testscripts/` contains upstream Microsoft SQL Server regression tests. Any modifications to the parser must maintain a **100% pass rate** across all test scripts.
* **Negative Tests:** Scripts expected to fail syntax parsing (or testing deprecated features like `DISK INIT` or `FASTFIRSTROW`) must be registered in `TSqlRegressionTest.NEGATIVE_TEST_SCRIPTS`.
* **Unit Tests:** New features or AST node helpers should include dedicated unit tests using JUnit 5 and AssertJ.

---

## 📝 Code Conventions

* **No External Runtime Dependencies:** `tsql-scriptdom-core` must maintain **zero external runtime dependencies**. Any third-party libraries (such as ANTLR 2) must be shaded and relocated into `io.github.fupacat.internal.*`.
* **Visitor Pattern Integrity:** When adding or extending AST nodes, ensure that visitor methods in `TSqlFragmentVisitor` and `TSqlConcreteFragmentVisitor` properly dispatch `visit` and `explicitVisit` calls.
* **Compatibility Mapping:** Features added in specific SQL Server versions must be reflected in `SqlCompatibilityValidator` with appropriate minimum `SqlVersion` boundaries.

---

## 📄 Licensing

By contributing to `tsql-scriptdom-java`, you agree that your contributions will be licensed under the project's [MIT License](LICENSE).
