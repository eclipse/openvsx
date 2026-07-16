import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Shared between ClosingBraceFix.java (jbang) and the Gradle "closingBraceFix" Spotless
// step (server/buildSrc) - edit this one file, both paths pick it up automatically.
//
// jbang-fmt (Eclipse formatter) always leaves the closing brace of a wrapped array
// initializer attached to the last element, e.g. "...class }". This inserts the missing
// line break before "}" only when the initializer actually spans multiple lines;
// single-line initializers are left untouched.
public class ClosingBraceFixCore {

    private record Edit(int contentEnd, int closeBraceOffset, String indent) {}

    public static String fix(String source) {
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(source.toCharArray());
        parser.setResolveBindings(false);
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_21, options);
        parser.setCompilerOptions(options);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        List<Edit> edits = new ArrayList<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(ArrayInitializer node) {
                if (node.expressions().isEmpty()) {
                    return true;
                }
                int openBraceOffset = node.getStartPosition();
                int closeBraceOffset = node.getStartPosition() + node.getLength() - 1;

                int contentEnd = closeBraceOffset;
                while (contentEnd > openBraceOffset
                        && (source.charAt(contentEnd - 1) == ' ' || source.charAt(contentEnd - 1) == '\t')) {
                    contentEnd--;
                }

                int lineOfClose = cu.getLineNumber(closeBraceOffset);
                int lineOfContentEnd = cu.getLineNumber(contentEnd - 1);
                int lineOfOpen = cu.getLineNumber(openBraceOffset);

                boolean wraps = lineOfClose > lineOfOpen;
                boolean closeAttached = lineOfClose == lineOfContentEnd;
                if (wraps && closeAttached) {
                    edits.add(new Edit(contentEnd, closeBraceOffset, indentOfLine(cu, source, lineOfOpen)));
                }
                return true;
            }
        });

        if (edits.isEmpty()) {
            return source;
        }

        edits.sort((a, b) -> b.closeBraceOffset() - a.closeBraceOffset());
        StringBuilder result = new StringBuilder(source);
        for (Edit edit : edits) {
            result.replace(edit.contentEnd(), edit.closeBraceOffset(), "\n" + edit.indent());
        }
        return result.toString();
    }

    private static String indentOfLine(CompilationUnit cu, String source, int line) {
        int lineStart = cu.getPosition(line, 0);
        int i = lineStart;
        while (i < source.length() && (source.charAt(i) == ' ' || source.charAt(i) == '\t')) {
            i++;
        }
        return source.substring(lineStart, i);
    }
}
