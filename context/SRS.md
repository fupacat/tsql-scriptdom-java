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

