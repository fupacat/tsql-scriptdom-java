package io.github.fupacat;

import io.github.fupacat.SqlVersion;
import io.github.fupacat.ast.TSqlFragment;
import java.util.List;

public class SqlCompatibilityValidator {

    private final SqlVersion targetVersion;

    public SqlCompatibilityValidator() {
        this(SqlVersion.SQL180);
    }

    public SqlCompatibilityValidator(SqlVersion targetVersion) {
        this.targetVersion = targetVersion != null ? targetVersion : SqlVersion.SQL180;
    }

    // TRACEABILITY_MAPPING: REQ-022
    @SuppressWarnings("rawtypes")
    public List validate(TSqlFragment fragment, SqlVersion targetVersion) {
        return SqlCompatibilityInternalVisitor.validate(fragment, targetVersion != null ? targetVersion : this.targetVersion);
    }
}
