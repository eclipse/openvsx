import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.WhileStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// Shared between AddBracesFix.java (jbang) and the Gradle "addBracesFix" Spotless
// step (server/buildSrc) - edit this one file, both paths pick it up automatically.
//
// The Eclipse formatter only re-flows whitespace/newlines around braces that already
// exist; it never inserts braces around a brace-less if/for/while/do body. This runs
// before jbang-fmt (the Eclipse formatter) so that whatever braces it adds here get
// properly indented and brace-positioned by the formatter pass that follows.
//
// "else if" chains are left alone (the else branch stays an IfStatement, not wrapped
// in its own block) so "} else if (...) {" keeps reading as one chain instead of
// turning into "} else { if (...) { ... } }".
public class AddBracesFixCore {

    public static String fix(String source) {
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(source.toCharArray());
        parser.setResolveBindings(false);
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_21, options);
        parser.setCompilerOptions(options);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        List<Statement> bodies = new ArrayList<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(IfStatement node) {
                addIfNotBlock(bodies, node.getThenStatement());
                Statement elseStatement = node.getElseStatement();
                if (elseStatement != null && !(elseStatement instanceof IfStatement)) {
                    addIfNotBlock(bodies, elseStatement);
                }
                return true;
            }

            @Override
            public boolean visit(ForStatement node) {
                addIfNotBlock(bodies, node.getBody());
                return true;
            }

            @Override
            public boolean visit(EnhancedForStatement node) {
                addIfNotBlock(bodies, node.getBody());
                return true;
            }

            @Override
            public boolean visit(WhileStatement node) {
                addIfNotBlock(bodies, node.getBody());
                return true;
            }

            @Override
            public boolean visit(DoStatement node) {
                addIfNotBlock(bodies, node.getBody());
                return true;
            }
        });

        if (bodies.isEmpty()) {
            return source;
        }

        // offset -> number of braces to insert there; a plain offset->char-count map
        // (rather than mutating a StringBuilder per edit) so nested bodies - e.g. a
        // brace-less for-loop whose brace-less body is itself a brace-less if - don't
        // have their recorded offsets invalidated by an earlier insertion shifting the
        // string underneath them.
        Map<Integer, Integer> opens = new TreeMap<>();
        Map<Integer, Integer> closes = new TreeMap<>();
        for (Statement body : bodies) {
            opens.merge(body.getStartPosition(), 1, Integer::sum);
            closes.merge(body.getStartPosition() + body.getLength(), 1, Integer::sum);
        }

        StringBuilder result = new StringBuilder(source.length() + bodies.size() * 2);
        for (int i = 0; i <= source.length(); i++) {
            result.append("}".repeat(closes.getOrDefault(i, 0)));
            result.append("{".repeat(opens.getOrDefault(i, 0)));
            if (i < source.length()) {
                result.append(source.charAt(i));
            }
        }
        return result.toString();
    }

    private static void addIfNotBlock(List<Statement> bodies, Statement statement) {
        if (statement != null && !(statement instanceof Block)) {
            bodies.add(statement);
        }
    }
}
