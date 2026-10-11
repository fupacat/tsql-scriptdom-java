package io.github.fupacat;

import io.github.fupacat.ast.TSqlFragment;
import java.io.Reader;
import java.util.List;

public class TSql180Parser {

    private final boolean initialQuotedIdentifiersOn;
    private final SqlVersion targetVersion;

    public TSql180Parser() {
        this(true, SqlVersion.SQL180);
    }

    public TSql180Parser(SqlVersion targetVersion) {
        this(true, targetVersion);
    }

    public TSql180Parser(boolean initialQuotedIdentifiersOn) {
        this(initialQuotedIdentifiersOn, SqlVersion.SQL180);
    }

    public TSql180Parser(boolean initialQuotedIdentifiersOn, SqlVersion targetVersion) {
        this.initialQuotedIdentifiersOn = initialQuotedIdentifiersOn;
        this.targetVersion = targetVersion != null ? targetVersion : SqlVersion.SQL180;
    }

    // TRACEABILITY_MAPPING: REQ-021, REQ-023
    @SuppressWarnings({"unchecked", "rawtypes"})
    public TSqlFragment parse(Reader reader, List errors) throws Exception {
        return _parse(reader, errors, targetVersion);
    }

    // TRACEABILITY_MAPPING: REQ-023
    @SuppressWarnings({"unchecked", "rawtypes"})
    public TSqlFragment _parse(Reader reader, List errors, SqlVersion targetVersion) throws Exception {
        if (reader == null) {
            return null;
        }

        TSql180LexerInternal lexer = new TSql180LexerInternal(reader);
        TSqlWhitespaceTokenFilter filter = new TSqlWhitespaceTokenFilter(initialQuotedIdentifiersOn, lexer);
        TSql180ParserInternal parser = new TSql180ParserInternal(filter);

        TSqlFragment fragment = null;
        try {
            fragment = parser.script();
        } catch (antlr.RecognitionException e) {
            parser.getParseErrors().add(new ParseError(102, 0, e.getLine(), e.getColumn(), e.getMessage()));
        } catch (antlr.TokenStreamException e) {
            parser.getParseErrors().add(new ParseError(102, 0, 1, 1, e.getMessage()));
        } catch (Exception e) {
            parser.getParseErrors().add(new ParseError(102, 0, 1, 1, e.getMessage()));
        }

        if (fragment != null && targetVersion != null && targetVersion != SqlVersion.SQL180) {
            SqlCompatibilityValidator validator = new SqlCompatibilityValidator(targetVersion);
            List compatErrors = validator.validate(fragment, targetVersion);
            parser.getParseErrors().addAll(compatErrors);
        }

        if (errors != null) {
            errors.addAll(parser.getParseErrors());
        }

        return fragment;
    }
}
