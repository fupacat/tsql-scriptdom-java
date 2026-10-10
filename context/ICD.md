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

