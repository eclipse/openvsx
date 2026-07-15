///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.diffplug.spotless:spotless-lib:3.2.0

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.java.ImportOrderStep;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

// Import order: java/javax first, then everything else, then org.eclipse.openvsx,
// then static imports, each group separated by a blank line.
public class ImportSort {

    public static void main(String[] args) throws Exception {
        var step = ImportOrderStep.forJava().createFrom("java|javax", "", "org.eclipse.openvsx", "\\#");

        int changed = 0;
        for (String arg : args) {
            try (Stream<Path> paths = Files.walk(Path.of(arg))) {
                List<Path> javaFiles = paths.filter(p -> p.toString().endsWith(".java")).toList();
                for (Path path : javaFiles) {
                    String original = Files.readString(path, StandardCharsets.UTF_8);
                    String formatted = step.format(original, path.toFile());
                    if (!formatted.equals(original)) {
                        Files.writeString(path, formatted, StandardCharsets.UTF_8);
                        System.out.println("sorted imports: " + path);
                        changed++;
                    }
                }
            }
        }
        System.out.println(changed + " file(s) updated");
    }
}
