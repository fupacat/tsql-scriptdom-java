package io.github.fupacat;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class SqlVersionTest {

    // TRACEABILITY_MAPPING: REQ-020
    @Test
    public void testSqlVersionCompatibilityLevels() {
        assertThat(SqlVersion.SQL80.getCompatibilityLevel()).isEqualTo(80);
        assertThat(SqlVersion.SQL90.getCompatibilityLevel()).isEqualTo(90);
        assertThat(SqlVersion.SQL100.getCompatibilityLevel()).isEqualTo(100);
        assertThat(SqlVersion.SQL110.getCompatibilityLevel()).isEqualTo(110);
        assertThat(SqlVersion.SQL120.getCompatibilityLevel()).isEqualTo(120);
        assertThat(SqlVersion.SQL130.getCompatibilityLevel()).isEqualTo(130);
        assertThat(SqlVersion.SQL140.getCompatibilityLevel()).isEqualTo(140);
        assertThat(SqlVersion.SQL150.getCompatibilityLevel()).isEqualTo(150);
        assertThat(SqlVersion.SQL160.getCompatibilityLevel()).isEqualTo(160);
        assertThat(SqlVersion.SQL170.getCompatibilityLevel()).isEqualTo(170);
        assertThat(SqlVersion.SQL180.getCompatibilityLevel()).isEqualTo(180);
    }
}
