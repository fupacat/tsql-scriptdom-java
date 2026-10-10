# Requirements

## Phase 1: AST Code Generation

[REQ-001] AstGen SHALL parse Ast.xml to load all element and enum definitions.

```json
{
  "requirement": "REQ-001",
  "kind": "categorical",
  "precondition": "valid Ast.xml schema resource is present",
  "action": "invoke AstSchemaParser parse",
  "expected": "all elements and enums loaded into memory model",
  "method": "junit",
  "expected_output": "AstModel"
}
```

[REQ-002] AstGen SHALL generate Java source files for at least 500 strongly-typed AST node classes using JavaPoet.

```json
{
  "requirement": "REQ-002",
  "kind": "quantitative",
  "precondition": "parsed AstModel is available",
  "action": "invoke AstCodeGenerator generateNodes",
  "expected": "at least 500 node source files emitted",
  "method": "junit",
  "operator": ">=",
  "value": 500,
  "unit": "files"
}
```

[REQ-003] AstGen SHALL generate the TSqlFragmentVisitor interface containing visit methods for all concrete fragment classes.

```json
{
  "requirement": "REQ-003",
  "kind": "categorical",
  "precondition": "parsed AstModel is available",
  "action": "invoke AstCodeGenerator generateVisitor",
  "expected": "TSqlFragmentVisitor source file emitted",
  "method": "junit",
  "expected_output": "TSqlFragmentVisitor.java"
}
```

[REQ-004] AstGen SHALL generate the TSqlConcreteFragmentVisitor class providing default traversal behavior.

```json
{
  "requirement": "REQ-004",
  "kind": "categorical",
  "precondition": "parsed AstModel is available",
  "action": "invoke AstCodeGenerator generateConcreteVisitor",
  "expected": "TSqlConcreteFragmentVisitor source file emitted",
  "method": "junit",
  "expected_output": "TSqlConcreteFragmentVisitor.java"
}
```

[REQ-005] The Maven build SHALL verify generated AST sources compile cleanly targeting Java 17.

```json
{
  "requirement": "REQ-005",
  "kind": "categorical",
  "precondition": "all AST node sources generated",
  "action": "run maven compile on tsql-scriptdom-astgen",
  "expected": "compilation succeeds with zero errors",
  "method": "maven",
  "expected_output": "BUILD SUCCESS"
}
```

## Phase 2: Base Parser Runtime & Lexer

[REQ-006] The core runtime SHALL provide TSqlTokenType and TSqlParserToken representing T-SQL token categories with source offsets, lines, and columns.

```json
{
  "requirement": "REQ-006",
  "kind": "categorical",
  "precondition": "token text and position coordinates provided",
  "action": "instantiate TSqlParserToken",
  "expected": "token fields accurately reflect coordinates and type",
  "method": "junit",
  "expected_output": "TSqlParserToken"
}
```

[REQ-007] TSqlWhitespaceTokenFilter SHALL filter whitespace and comment tokens while retaining EOF and quoted identifier state.

```json
{
  "requirement": "REQ-007",
  "kind": "categorical",
  "precondition": "raw token stream contains whitespace and comments",
  "action": "invoke TSqlWhitespaceTokenFilter nextToken",
  "expected": "whitespace and comments excluded from filtered stream",
  "method": "junit",
  "expected_output": "Token"
}
```

[REQ-008] TSqlLexerBaseInternal SHALL provide character offset tracking and token creation for ANTLR 2 scanners.

```json
{
  "requirement": "REQ-008",
  "kind": "categorical",
  "precondition": "source text stream is provided to lexer",
  "action": "invoke TSqlLexerBaseInternal makeToken",
  "expected": "token instance created with accurate offset calculations",
  "method": "junit",
  "expected_output": "TSqlParserToken"
}
```

[REQ-009] TSql160ParserBaseInternal SHALL provide LL(2) lookahead predicates, recovery token sets, and parse error collection into ParseError objects.

```json
{
  "requirement": "REQ-009",
  "kind": "categorical",
  "precondition": "parser instance initialized with token stream",
  "action": "invoke TSql160ParserBaseInternal getParseErrors",
  "expected": "parse errors list initialized and accessible",
  "method": "junit",
  "expected_output": "List"
}
```

[REQ-010] The Maven build SHALL verify tsql-scriptdom-core compiles cleanly and passes all runtime tests.

```json
{
  "requirement": "REQ-010",
  "kind": "categorical",
  "precondition": "all core runtime sources present",
  "action": "run maven compile on tsql-scriptdom-core",
  "expected": "compilation succeeds with zero errors",
  "method": "maven",
  "expected_output": "BUILD SUCCESS"
}
```

## Phase 3: Grammar Translation & Parser Facade

[REQ-011] TSqlTokenTypes.g SHALL compile into TSqlTokenTypes.java defining constant IDs for all T-SQL tokens.

```json
{
  "requirement": "REQ-011",
  "kind": "categorical",
  "precondition": "valid TSqlTokenTypes.g grammar present",
  "action": "compile TSqlTokenTypes grammar",
  "expected": "TSqlTokenTypes interface generated with token constants",
  "method": "junit",
  "expected_output": "TSqlTokenTypes.java"
}
```

[REQ-012] TSql160.g SHALL compile via ANTLR 2 into TSql160ParserInternal.java implementing the full TSql160 grammar.

```json
{
  "requirement": "REQ-012",
  "kind": "categorical",
  "precondition": "valid TSql160.g grammar present",
  "action": "compile TSql160 grammar",
  "expected": "TSql160ParserInternal generated by ANTLR 2",
  "method": "junit",
  "expected_output": "TSql160ParserInternal.java"
}
```

[REQ-013] The core runtime SHALL provide TSql160Parser accepting Reader or script text and returning TSqlFragment AST fragments.

```json
{
  "requirement": "REQ-013",
  "kind": "categorical",
  "precondition": "valid T-SQL statement text provided to parser",
  "action": "invoke TSql160Parser parse",
  "expected": "non-null TSqlFragment AST produced",
  "method": "junit",
  "expected_output": "TSqlFragment"
}
```

[REQ-014] TSql160Parser SHALL record syntax and parse errors into ParseError diagnostic collections.

```json
{
  "requirement": "REQ-014",
  "kind": "categorical",
  "precondition": "invalid T-SQL script with syntax errors provided",
  "action": "invoke TSql160Parser parse",
  "expected": "syntax errors populated into errors list",
  "method": "junit",
  "expected_output": "ParseError"
}
```

[REQ-015] The Maven build SHALL verify complete compilation and execute parser test cases successfully.

```json
{
  "requirement": "REQ-015",
  "kind": "categorical",
  "precondition": "all Phase 3 parser and grammar sources present",
  "action": "run maven compile on tsql-scriptdom-core",
  "expected": "compilation succeeds with zero errors",
  "method": "maven",
  "expected_output": "BUILD SUCCESS"
}
```

## Phase 4: Upstream Regression Verification

[REQ-016] Test resources SHALL include upstream Microsoft SqlScriptDOM test scripts.

```json
{
  "requirement": "REQ-016",
  "kind": "categorical",
  "precondition": "upstream SqlScriptDOM repository available",
  "action": "load test script resource files",
  "expected": "test script resources accessible on classpath",
  "method": "junit",
  "expected_output": "test script stream"
}
```

[REQ-017] The test suite SHALL execute parameterized regression tests parsing upstream T-SQL test scripts with TSql160Parser.

```json
{
  "requirement": "REQ-017",
  "kind": "categorical",
  "precondition": "upstream test script resources loaded",
  "action": "execute parameterized parse across test scripts",
  "expected": "test scripts evaluated and parsed",
  "method": "junit",
  "expected_output": "test executions"
}
```

[REQ-018] TSql160Parser SHALL successfully parse common and version-specific baseline test scripts producing non-null TSqlScript ASTs.

```json
{
  "requirement": "REQ-018",
  "kind": "categorical",
  "precondition": "valid upstream baseline test scripts provided",
  "action": "invoke TSql160Parser parse",
  "expected": "non-null TSqlScript AST without unhandled exceptions",
  "method": "junit",
  "expected_output": "TSqlScript"
}
```

[REQ-019] The Maven build SHALL verify that the regression test suite passes cleanly under Maven Surefire.

```json
{
  "requirement": "REQ-019",
  "kind": "categorical",
  "precondition": "regression tests present in test directory",
  "action": "run maven test on tsql-scriptdom-core",
  "expected": "all tests execute and pass",
  "method": "maven",
  "expected_output": "BUILD SUCCESS"
}
```



