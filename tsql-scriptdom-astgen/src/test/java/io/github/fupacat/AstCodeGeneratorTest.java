package io.github.fupacat;

import org.junit.jupiter.api.Test;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.tools.StandardJavaFileManager;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;

public class AstCodeGeneratorTest {

    private static Path sharedDir;

    // TRACEABILITY_MAPPING: REQ-002
    private synchronized Path getGeneratedDirectory() throws Exception {
        if (sharedDir == null) {
            sharedDir = Files.createTempDirectory("ast_gen_test_output");
            AstSchemaParser parser = new AstSchemaParser();
            AstModel model;
            try (InputStream is = AstCodeGeneratorTest.class.getResourceAsStream("/Ast.xml")) {
                model = parser.parse(is);
            }
            AstCodeGenerator generator = new AstCodeGenerator();
            generator.generate(model, sharedDir);
        }
        return sharedDir;
    }

    // TRACEABILITY_MAPPING: REQ-002
    @Test
    public void testGenerateAstNodes() throws Exception {
        Path outputDir = getGeneratedDirectory();
        Path astPackageDir = outputDir.resolve("io/github/fupacat/ast");
        assertThat(astPackageDir).isDirectory();

        List<Path> files;
        try (var stream = Files.list(astPackageDir)) {
            files = stream.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
        assertThat(files.size()).isGreaterThanOrEqualTo(500);
        assertThat(astPackageDir.resolve("StatementList.java")).isRegularFile();
        assertThat(astPackageDir.resolve("ExecuteStatement.java")).isRegularFile();
    }

    // TRACEABILITY_MAPPING: REQ-003
    @Test
    public void testGenerateVisitorInterface() throws Exception {
        Path outputDir = getGeneratedDirectory();
        Path visitorFile = outputDir.resolve("io/github/fupacat/ast/TSqlFragmentVisitor.java");
        assertThat(visitorFile).isRegularFile();
        String content = Files.readString(visitorFile);
        assertThat(content).contains("public interface TSqlFragmentVisitor");
        assertThat(content).contains("visit(StatementList node)");
        assertThat(content).contains("explicitVisit(StatementList node)");
    }

    // TRACEABILITY_MAPPING: REQ-004
    @Test
    public void testGenerateConcreteVisitor() throws Exception {
        Path outputDir = getGeneratedDirectory();
        Path concreteVisitorFile = outputDir.resolve("io/github/fupacat/ast/TSqlConcreteFragmentVisitor.java");
        assertThat(concreteVisitorFile).isRegularFile();
        String content = Files.readString(concreteVisitorFile);
        assertThat(content).contains("public class TSqlConcreteFragmentVisitor implements TSqlFragmentVisitor");
        assertThat(content).contains("visit(StatementList node)");
        assertThat(content).contains("node.acceptChildren(this)");
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    public void testCompileGeneratedNodes() throws Exception {
        Path outputDir = getGeneratedDirectory();
        Path astPackageDir = outputDir.resolve("io/github/fupacat/ast");
        List<File> javaFiles;
        try (var stream = Files.walk(astPackageDir)) {
            javaFiles = stream.filter(p -> p.toString().endsWith(".java"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        }
        assertThat(javaFiles).isNotEmpty();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).isNotNull();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(javaFiles);
            Path classesDir = Files.createTempDirectory("compiled_ast_classes");
            List<String> options = List.of("-d", classesDir.toString(), "--release", "17");
            boolean success = compiler.getTask(null, fileManager, diagnostics, options, null, units).call();
            if (!success) {
                diagnostics.getDiagnostics().forEach(d -> System.err.println(d.toString()));
            }
            assertThat(success).isTrue();
        }
    }
}
