package scripts;

import io.github.fupacat.*;
import java.io.InputStream;
import java.nio.file.Paths;

public class AstGenRunner {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: AstGenRunner <outputDirectory>");
            System.exit(1);
        }
        AstSchemaParser parser = new AstSchemaParser();
        AstModel model;
        try (InputStream is = AstGenRunner.class.getResourceAsStream("/schema/Ast.xml")) {
            if (is == null) {
                try (InputStream is2 = AstGenRunner.class.getResourceAsStream("/Ast.xml")) {
                    model = parser.parse(is2);
                }
            } else {
                model = parser.parse(is);
            }
        }
        AstCodeGenerator generator = new AstCodeGenerator();
        int count = generator.generate(model, Paths.get(args[0]));
        System.out.println("Generated AST classes: " + count);
    }
}
