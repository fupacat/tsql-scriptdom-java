# Interfaces

```json
{
  "kind": "interface",
  "id": "IF-001",
  "path": "tsql-scriptdom-astgen/src/main/java/io/github/fupacat/AstSchemaParser.java",
  "symbol": "AstSchemaParser.parse",
  "signature": {
    "parameters": [
      {
        "name": "inputStream",
        "kind": "positional_or_keyword",
        "annotation": "InputStream",
        "default": null
      }
    ],
    "return": "AstModel",
    "async": false
  },
  "dependencies": [
    "java.io.InputStream"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-002",
  "path": "tsql-scriptdom-astgen/src/main/java/io/github/fupacat/AstCodeGenerator.java",
  "symbol": "AstCodeGenerator.generate",
  "signature": {
    "parameters": [
      {
        "name": "model",
        "kind": "positional_or_keyword",
        "annotation": "AstModel",
        "default": null
      },
      {
        "name": "outputDirectory",
        "kind": "positional_or_keyword",
        "annotation": "Path",
        "default": null
      }
    ],
    "return": "int",
    "async": false
  },
  "dependencies": [
    "java.nio.file.Path"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-003",
  "path": "tsql-scriptdom-core/src/main/java/io/github/fupacat/TSqlWhitespaceTokenFilter.java",
  "symbol": "TSqlWhitespaceTokenFilter.nextToken",
  "signature": {
    "parameters": [],
    "return": "Token",
    "async": false
  },
  "dependencies": [
    "antlr.Token",
    "antlr.TokenStream"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-004",
  "path": "tsql-scriptdom-core/src/main/java/io/github/fupacat/TSql160ParserBaseInternal.java",
  "symbol": "TSql160ParserBaseInternal.getParseErrors",
  "signature": {
    "parameters": [],
    "return": "List",
    "async": false
  },
  "dependencies": [
    "antlr.LLkParser",
    "java.util.List"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-005",
  "path": "tsql-scriptdom-core/src/main/java/io/github/fupacat/TSql160Parser.java",
  "symbol": "TSql160Parser.parse",
  "signature": {
    "parameters": [
      {
        "name": "reader",
        "kind": "positional_or_keyword",
        "annotation": "Reader",
        "default": null
      },
      {
        "name": "errors",
        "kind": "positional_or_keyword",
        "annotation": "List",
        "default": null
      }
    ],
    "return": "TSqlFragment",
    "async": false
  },
  "dependencies": [
    "io.github.fupacat.ast.TSqlFragment",
    "java.io.Reader",
    "java.util.List"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-006",
  "path": "tsql-scriptdom-core/src/main/java/io/github/fupacat/TSql180Parser.java",
  "symbol": "TSql180Parser.parse",
  "signature": {
    "parameters": [
      {
        "name": "reader",
        "kind": "positional_or_keyword",
        "annotation": "Reader",
        "default": null
      },
      {
        "name": "errors",
        "kind": "positional_or_keyword",
        "annotation": "List",
        "default": null
      }
    ],
    "return": "TSqlFragment",
    "async": false
  },
  "dependencies": [
    "io.github.fupacat.ast.TSqlFragment",
    "java.io.Reader",
    "java.util.List"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-007",
  "path": "tsql-scriptdom-core/src/main/java/io/github/fupacat/SqlCompatibilityValidator.java",
  "symbol": "SqlCompatibilityValidator.validate",
  "signature": {
    "parameters": [
      {
        "name": "fragment",
        "kind": "positional_or_keyword",
        "annotation": "TSqlFragment",
        "default": null
      },
      {
        "name": "targetVersion",
        "kind": "positional_or_keyword",
        "annotation": "SqlVersion",
        "default": null
      }
    ],
    "return": "List",
    "async": false
  },
  "dependencies": [
    "io.github.fupacat.SqlVersion",
    "io.github.fupacat.ast.TSqlFragment",
    "java.util.List"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-008",
  "path": "tsql-scriptdom-core/src/main/java/io/github/fupacat/SqlVersion.java",
  "symbol": "SqlVersion.getCompatibilityLevel",
  "signature": {
    "parameters": [],
    "return": "int",
    "async": false
  },
  "dependencies": [],
  "exchanges": []
}
```


