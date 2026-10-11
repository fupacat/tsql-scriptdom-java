package io.github.fupacat;

import io.github.fupacat.ast.TSqlFragment;
import io.github.fupacat.ast.TSqlScript;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * High-level entry point and facade for parsing Transact-SQL (T-SQL) into strongly-typed
 * Abstract Syntax Trees (ASTs).
 *
 * <p>Example usage:
 * <pre>{@code
 * ParseResult<TSqlScript> result = TSqlParser.parse("SELECT 1 AS col FROM dbo.tbl;");
 * if (result.isSuccess()) {
 *     TSqlScript script = result.fragment();
 *     script.accept(new MyCustomVisitor());
 * } else {
 *     result.errors().forEach(err -> System.err.println(err.message));
 * }
 * }</pre>
 */
public class TSqlParser {

    private final boolean initialQuotedIdentifiersOn;
    private final SqlVersion targetVersion;

    public TSqlParser() {
        this(true, SqlVersion.SQL180);
    }

    public TSqlParser(SqlVersion targetVersion) {
        this(true, targetVersion);
    }

    public TSqlParser(boolean initialQuotedIdentifiersOn, SqlVersion targetVersion) {
        this.initialQuotedIdentifiersOn = initialQuotedIdentifiersOn;
        this.targetVersion = targetVersion != null ? targetVersion : SqlVersion.SQL180;
    }

    /**
     * Parses a T-SQL string using the default SQL Server 2025 (SQL180) superset engine.
     */
    public static ParseResult<TSqlScript> parse(String sql) {
        return parse(sql, SqlVersion.SQL180);
    }

    /**
     * Parses a T-SQL string and validates compatibility against the specified {@link SqlVersion}.
     */
    public static ParseResult<TSqlScript> parse(String sql, SqlVersion version) {
        Objects.requireNonNull(sql, "SQL string must not be null");
        return new TSqlParser(version).parseScript(new StringReader(sql));
    }

    /**
     * Parses T-SQL from a {@link Reader} using the default SQL Server 2025 (SQL180) superset engine.
     */
    public static ParseResult<TSqlScript> parse(Reader reader) {
        return parse(reader, SqlVersion.SQL180);
    }

    /**
     * Parses T-SQL from a {@link Reader} and validates compatibility against the specified {@link SqlVersion}.
     */
    public static ParseResult<TSqlScript> parse(Reader reader, SqlVersion version) {
        Objects.requireNonNull(reader, "Reader must not be null");
        return new TSqlParser(version).parseScript(reader);
    }

    /**
     * Instance parse method for reading a full T-SQL script from a {@link Reader}.
     */
    @SuppressWarnings("unchecked")
    public ParseResult<TSqlScript> parseScript(Reader reader) {
        List<ParseError> errors = new ArrayList<>();
        TSql180Parser underlying = new TSql180Parser(initialQuotedIdentifiersOn, targetVersion);
        try {
            TSqlFragment fragment = underlying.parse(reader, errors);
            TSqlScript script = fragment instanceof TSqlScript s ? s : null;
            return new ParseResult<>(script, errors);
        } catch (Exception ex) {
            errors.add(new ParseError(102, 0, 1, 1, "Parsing exception: " + ex.getMessage()));
            return new ParseResult<>(null, errors);
        }
    }

    /**
     * Instance parse method for reading a full T-SQL script from a {@link String}.
     */
    public ParseResult<TSqlScript> parseScript(String sql) {
        Objects.requireNonNull(sql, "SQL string must not be null");
        return parseScript(new StringReader(sql));
    }

    public boolean isInitialQuotedIdentifiersOn() {
        return initialQuotedIdentifiersOn;
    }

    public SqlVersion getTargetVersion() {
        return targetVersion;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean initialQuotedIdentifiersOn = true;
        private SqlVersion targetVersion = SqlVersion.SQL180;

        public Builder initialQuotedIdentifiers(boolean on) {
            this.initialQuotedIdentifiersOn = on;
            return this;
        }

        public Builder sqlVersion(SqlVersion version) {
            this.targetVersion = version != null ? version : SqlVersion.SQL180;
            return this;
        }

        public TSqlParser build() {
            return new TSqlParser(initialQuotedIdentifiersOn, targetVersion);
        }
    }

    /**
     * Standalone CLI entry point for command-line syntax verification.
     */
    public static void main(String[] args) {
        if (args.length == 0 || "-h".equals(args[0]) || "--help".equals(args[0])) {
            System.out.println("tsql-scriptdom-java - Canonical Transact-SQL Parser & AST Engine");
            System.out.println("Usage:");
            System.out.println("  java -jar tsql-scriptdom-core.jar \"<sql-string>\"");
            System.out.println("  java -jar tsql-scriptdom-core.jar -f <path-to-sql-file>");
            return;
        }
        try {
            String sql;
            if ("-f".equals(args[0]) && args.length > 1) {
                sql = java.nio.file.Files.readString(java.nio.file.Path.of(args[1]));
            } else {
                sql = String.join(" ", args);
            }
            ParseResult<TSqlScript> result = parse(sql);
            if (result.isSuccess()) {
                System.out.println("SUCCESS: T-SQL parsed cleanly (0 errors).");
                if (result.fragment() != null && result.fragment().Batches != null) {
                    System.out.println("Batches: " + result.fragment().Batches.size());
                }
            } else {
                System.err.println("FAILED: Found " + result.errors().size() + " parse error(s):");
                for (ParseError err : result.errors()) {
                    System.err.println("  Line " + err.line + ", Col " + err.column + ": " + err.message);
                }
                System.exit(1);
            }
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
            System.exit(1);
        }
    }
}
