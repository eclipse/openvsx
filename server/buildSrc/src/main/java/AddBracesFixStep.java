import com.diffplug.spotless.FormatterStep;

import java.io.Serializable;

// Wraps AddBracesFixCore (shared with the jbang path, see scripts/AddBracesFix.java)
// as a Spotless FormatterStep for use in the Gradle "spotless { java { ... } }" block.
public final class AddBracesFixStep {

    private AddBracesFixStep() {
    }

    public static FormatterStep create() {
        return FormatterStep.create("addBracesFix", "unused", (Serializable state) -> AddBracesFixCore::fix);
    }
}
