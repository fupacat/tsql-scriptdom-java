package io.github.fupacat;

import io.github.fupacat.ast.TSqlFragment;

import java.io.Reader;
import java.util.List;

/**
 * Public parser facade for SQL Server 2022 (version 160) T-SQL statements.
 */
public class TSql160Parser {

    private final boolean initialQuotedIdentifiersOn;

    public TSql160Parser() {
        this(true);
    }

    public TSql160Parser(boolean initialQuotedIdentifiersOn) {
        this.initialQuotedIdentifiersOn = initialQuotedIdentifiersOn;
    }

    // TRACEABILITY_MAPPING: REQ-011, REQ-012, REQ-013, REQ-014, REQ-015, REQ-016, REQ-017, REQ-018, REQ-019
    @SuppressWarnings({"unchecked", "rawtypes"})
    public TSqlFragment parse(Reader reader, List errors) throws Exception {
        if (reader == null) {
            return null;
        }

        TSql160LexerInternal lexer = new TSql160LexerInternal(reader);
        TSqlWhitespaceTokenFilter filter = new TSqlWhitespaceTokenFilter(initialQuotedIdentifiersOn, lexer);
        TSql160ParserInternal parser = new TSql160ParserInternal(filter);

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

        if (errors != null) {
            errors.addAll(parser.getParseErrors());
        }

        return fragment;
    }
}
