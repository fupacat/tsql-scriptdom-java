package io.github.fupacat;

import io.github.fupacat.ast.CreateVectorIndexStatement;
import io.github.fupacat.ast.TSqlFragment;
import io.github.fupacat.ast.WindowClause;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class SqlCompatibilityValidatorTest {

    // TRACEABILITY_MAPPING: REQ-022
    @Test
    public void testCompatibilityValidation() {
        SqlCompatibilityValidator validator = new SqlCompatibilityValidator();

        CreateVectorIndexStatement vectorIndex = new CreateVectorIndexStatement();
        List<ParseError> errorsForSql160 = validator.validate(vectorIndex, SqlVersion.SQL160);
        assertThat(errorsForSql160).isNotEmpty();
        assertThat(errorsForSql160.get(0).message).contains("CREATE VECTOR INDEX");

        List<ParseError> errorsForSql170 = validator.validate(vectorIndex, SqlVersion.SQL170);
        assertThat(errorsForSql170).isEmpty();

        WindowClause windowClause = new WindowClause();
        List<ParseError> errorsForSql150 = validator.validate(windowClause, SqlVersion.SQL150);
        assertThat(errorsForSql150).isNotEmpty();
        assertThat(errorsForSql150.get(0).message).contains("WINDOW clause");

        List<ParseError> errorsForWindow160 = validator.validate(windowClause, SqlVersion.SQL160);
        assertThat(errorsForWindow160).isEmpty();
    }
}
