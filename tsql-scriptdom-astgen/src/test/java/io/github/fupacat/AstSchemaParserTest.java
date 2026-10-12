package io.github.fupacat;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    // TRACEABILITY_MAPPING: REQ-001
    @Test
    public void testRejectsDocTypeDeclaration() {
        AstSchemaParser parser = new AstSchemaParser();
        String xml = """
                <!DOCTYPE Ast [
                  <!ENTITY xxe SYSTEM "file:///etc/passwd">
                ]>
                <Ast>
                  <Class Name="A" Summary="s">
                    <Member Name="M" Type="string" Summary="&xxe;"/>
                  </Class>
                </Ast>
                """;
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> parser.parse(inputStream))
                .isInstanceOf(org.xml.sax.SAXParseException.class)
                .hasMessageContaining("DOCTYPE");
    }
}
