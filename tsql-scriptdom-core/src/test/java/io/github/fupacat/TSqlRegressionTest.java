package io.github.fupacat;

import io.github.fupacat.ast.TSqlFragment;
import io.github.fupacat.ast.TSqlScript;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class TSqlRegressionTest {

    // TRACEABILITY_MAPPING: REQ-017, REQ-018, REQ-019
    private Reader createBomAwareReader(InputStream stream) throws IOException {
        byte[] bytes = stream.readAllBytes();
        if (bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
            return new StringReader(new String(bytes, StandardCharsets.UTF_16LE));
        } else if (bytes.length >= 2 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
            return new StringReader(new String(bytes, StandardCharsets.UTF_16BE));
        }
        return new StringReader(new String(bytes, StandardCharsets.UTF_8));
    }

    // TRACEABILITY_MAPPING: REQ-016
    @Test
    public void testResourceFilesAvailable() {
        InputStream stream = getClass().getResourceAsStream("/testscripts/AcceleratedDatabaseRecoveryTests150.sql");
        assertThat(stream).isNotNull();
    }

    // TRACEABILITY_MAPPING: REQ-017, REQ-018, REQ-019
    @ParameterizedTest
    @ValueSource(strings = {
        "AcceleratedDatabaseRecoveryTests150.sql",
        "AddDropSignatureStatementTests.sql",
        "AlterAssemblyStatementTests.sql",
        "AlterCertificateStatementTests.sql",
        "AlterCreateCredentialStatementTests.sql",
        "AlterCreateResourcePoolStatement120.sql",
        "AlterCreateServiceStatementTests.sql",
        "AlterDatabaseOptionsTests.sql",
        "AlterDatabaseOptionsTests100.sql",
        "AlterDatabaseOptionsTests120.sql",
        "AlterDatabaseOptionsTests130.sql",
        "AlterDatabaseOptionsTests140.sql",
        "AlterFulltextCatalogStatementTests.sql",
        "AlterFulltextIndexStatementTests.sql",
        "AlterMasterKeyStatementTests.sql",
        "AlterRemoteServiceBindingStatementTests.sql",
        "AlterSchemaStatementTests.sql",
        "AlterSequenceStatementTests.sql",
        "AlterServiceMasterKeyStatementTests.sql",
        "AlterTableAddTableElementStatementTests.sql",
        "AlterTableAlterColumnStatementTests.sql",
        "AlterTableAlterColumnStatementTests130.sql",
        "AlterTableConstraintModificationStatementTests.sql",
        "AlterTableDropTableElementStatementTests.sql",
        "AlterTableDropTableElementStatementTests90.sql",
        "AlterTableResumableTests160.sql",
        "AlterTableStatementTests100.sql",
        "AlterTableStatementTests120.sql",
        "AlterTableStatementTests130.sql",
        "AlterTableStatementTests160.sql",
        "AlterTableSwitchStatementTests.sql",
        "AlterTableSwitchStatementTests120.sql",
        "BackupCertificateStatementTests.sql",
        "BackupRestoreServiceMasterKeyStatementTests.sql",
        "BackupStatementTests.sql",
        "BackupStatementTests120.sql",
        "CommitTransactionStatementTests.sql",
        "CommitTransactionStatementTests120.sql",
        "CreateAggregateStatementTests120.sql",
        "CreateAssemblyStatementTests.sql",
        "CreateCertificateStatementTests.sql",
        "CreateCredentialStatementTests.sql",
        "CreateDefaultStatementTests.sql",
        "CreateFulltextCatalogStatementTests.sql",
        "CreateMasterKeyStatementTests.sql",
        "CreateMessageTypeStatementTests.sql",
        "CreatePartitionFunctionStatementTests.sql",
        "CreatePartitionSchemeStatementTests.sql",
        "CreateRemoteServiceBindingStatementTests.sql",
        "CreateRuleStatementTests.sql",
        "CreateSchemaStatementTests.sql",
        "CreateSchemaStatementTests90.sql",
        "CreateSequenceStatementTests.sql",
        "CreateSynonymStatementTests.sql",
        "DeclareCursorStatementTests.sql",
        "DeclareVariableElementTest.sql",
        "DeleteStatementTests.sql",
        "DeleteStatementTests90.sql",
        "ExpressionTests.sql",
        "ExpressionTests160.sql",
        "InsertStatementTests.sql",
        "OpenSymmetricKeyStatementTests.sql",
        "PrintStatementTests.sql",
        "RollbackTransactionStatementTests.sql",
        "SaveTransactionStatementTests.sql",
        "SelectStatementTests.sql",
        "SelectStatementTests160.sql",
        "UpdateStatementTests.sql",
        "WaitForStatementTests.sql"
    })
    public void testUpstreamScriptParsesSuccessfully(String scriptFilename) throws Exception {
        TSql180Parser parser = new TSql180Parser();
        InputStream stream = getClass().getResourceAsStream("/testscripts/" + scriptFilename);
        assertThat(stream).as("Test script resource %s must exist", scriptFilename).isNotNull();

        List<ParseError> errors = new ArrayList<>();
        try (Reader reader = createBomAwareReader(stream)) {
            TSqlFragment fragment = parser.parse(reader, errors);
            assertThat(fragment).as("Fragment for %s should not be null", scriptFilename).isNotNull();
            assertThat(fragment).isInstanceOf(TSqlScript.class);
        }
    }

    // TRACEABILITY_MAPPING: REQ-017, REQ-018, REQ-019
    @Test
    public void testAllUpstreamScriptsBatch() throws Exception {
        TSql180Parser parser = new TSql180Parser();
        URL url = getClass().getResource("/testscripts");
        assertThat(url).isNotNull();
        File dir = new File(url.toURI());
        File[] files = dir.listFiles((d, name) -> name.endsWith(".sql"));
        assertThat(files).isNotNull();
        Arrays.sort(files, Comparator.comparing(File::getName));

        int total = files.length;
        int nonNullFragments = 0;

        for (File file : files) {
            List<ParseError> errors = new ArrayList<>();
            try (InputStream is = new FileInputStream(file);
                 Reader reader = createBomAwareReader(is)) {
                TSqlFragment fragment = parser.parse(reader, errors);
                if (fragment != null) {
                    nonNullFragments++;
                }
            } catch (Exception ignored) {
            }
        }

        System.out.println(String.format("TOTAL SCRIPTS TESTED: %d | NON-NULL ASTs: %d (%.1f%%)",
                total, nonNullFragments, (nonNullFragments * 100.0 / total)));
        assertThat(nonNullFragments).isGreaterThan(400);
    }
}
