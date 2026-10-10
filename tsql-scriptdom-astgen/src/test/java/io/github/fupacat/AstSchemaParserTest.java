package io.github.fupacat;

import org.junit.jupiter.api.Test;
import java.io.InputStream;
import static org.assertj.core.api.Assertions.assertThat;

public class AstSchemaParserTest {

    // TRACEABILITY_MAPPING: REQ-001
    @Test
    public void testParseAstXml() throws Exception {
        AstSchemaParser parser = new AstSchemaParser();
        try (InputStream is = getClass().getResourceAsStream("/Ast.xml")) {
            assertThat(is).isNotNull();
            AstModel model = parser.parse(is);
            assertThat(model).isNotNull();
            assertThat(model.classes).isNotEmpty();
            assertThat(model.classes.size()).isGreaterThanOrEqualTo(500);
            assertThat(model.enums).isNotEmpty();
            assertThat(model.interfaces).isNotEmpty();
        }
    }
}
