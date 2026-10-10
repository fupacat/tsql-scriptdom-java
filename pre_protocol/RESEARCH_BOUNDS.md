# Research Bounds

- Upstream Source: Microsoft SqlScriptDOM (Ast.xml, TSql160.g, TSqlTokenTypes.g under MIT License).
- Target Language: Java 17+ bytecode target, 100% pure Java runtime with zero native binaries.
- AST Generation Scope: Parse Ast.xml to generate ~500 AST node types and full visitor interfaces.
- Excluded Scope: ScriptGenerator (AST-to-SQL text emission) is intentionally out of scope.
- Grammars: Initial grammar target is restricted to TSql160 (SQL Server 2022) superset.
- Test Framework: JUnit Jupiter 5.10.2 + AssertJ 3.25.3.
