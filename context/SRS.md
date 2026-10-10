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
