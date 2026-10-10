# Decisions

## ADR-001
- Status: [RESOLVED]
- Decision: Target Java 17 LTS as baseline language level using multi-module Maven build with JUnit 5 and AssertJ.

## ADR-002
- Status: [RESOLVED]
- Decision: Use JavaPoet (com.squareup:javapoet) to parse Ast.xml schema and generate strongly-typed AST node classes and visitor hierarchy.

## ADR-003
- Status: [RESOLVED]
- Decision: Omit ScriptGenerator formatting subsystem to focus strictly on AST inspection and static analysis for linters and SonarQube.

## ADR-004
- Status: [RESOLVED]
- Decision: Standardize on ANTLR 2.7.7 runtime with TSql160 (SQL Server 2022) as unified superset grammar for deterministic LL(2) lookahead parsing.

## ADR-005
- Status: [RESOLVED]
- Decision: Implement TSqlWhitespaceTokenFilter and TSqlParserToken on ANTLR 2.7.7 TokenStream preserving exact source offsets and quoted identifier toggles.

## ADR-006
- Status: [RESOLVED]
- Decision: Collapse upstream C# multi-tiered version inheritance (TSql80 through TSql150) into a single direct TSql160ParserBaseInternal runtime class extending antlr.LLkParser.

## ADR-007
- Status: [RESOLVED]
- Decision: Adapt TSql160.g and TSqlTokenTypes.g for ANTLR 2 Java target, outputting generated parser and token types into target/generated-sources.

## ADR-008
- Status: [RESOLVED]
- Decision: Implement public TSql160Parser facade providing parse methods that return TSqlFragment ASTs and populate ParseError diagnostics.
