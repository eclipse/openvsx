import com.diffplug.spotless.FormatterStep;

import java.io.Serializable;

// Wraps ClosingBraceFixCore (shared with the jbang path, see scripts/ClosingBraceFix.java)
// as a Spotless FormatterStep for use in the Gradle "spotless { java { ... } }" block.
public final class ClosingBraceFixStep {

    private ClosingBraceFixStep() {
    }

    public static FormatterStep create() {
        return FormatterStep.create("closingBraceFix", "unused", (Serializable state) -> ClosingBraceFixCore::fix);
    }
}
