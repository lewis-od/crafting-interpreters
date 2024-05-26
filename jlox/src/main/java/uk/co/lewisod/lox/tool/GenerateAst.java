package uk.co.lewisod.lox.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

// Standalone util to generate Java classes representing nodes in the AST
public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output dir>");
            System.exit(64);
        }
        var outputDirectory = args[0];
        defineAst(outputDirectory, "Expr", List.of(
                "Binary   : Expr left, Token operator, Expr right",
                "Grouping : Expr expression",
                "Literal  : Object value",
                "Unary    : Token operator, Expr right"
        ));
    }

    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
        var path = outputDir + "/" + baseName + ".java";
        try (var writer = new PrintWriter(path, StandardCharsets.UTF_8)) {
            writer.println("package uk.co.lewisod.lox;");
            writer.println();
            writer.println("import java.util.List;");
            writer.println();
            writer.println("public abstract class " + baseName + " {");

            for (var type : types) {
                var parts = type.split(":");
                var className = parts[0].trim();
                var fields = parts[1].trim();
                defineType(writer, baseName, className, fields);
                writer.println();
            }

            writer.println("}");
        }
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        writer.println("  public static class " + className + " extends " + baseName + " {");

        var fields = fieldList.split(", ");
        // Fields
        for (var field : fields) {
            writer.println("    final " + field + ";");
        }

        writer.println();

        // Constructor
        writer.println("    public " + className + "(" + fieldList + ") {");
        for (var field : fields) {
            var name = field.split(" ")[1];
            writer.println("      this." + name + " = " + name + ";");
        }
        writer.println("    }");
        writer.println("  }");
    }
}