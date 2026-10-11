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
        TSql180Parser delegate = new TSql180Parser(initialQuotedIdentifiersOn, SqlVersion.SQL160);
        return delegate.parse(reader, errors);
    }
}
