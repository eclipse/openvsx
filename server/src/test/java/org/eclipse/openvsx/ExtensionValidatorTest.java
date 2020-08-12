package org.eclipse.openvsx;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.openvsx.entities.ExtensionVersion;
import org.junit.jupiter.api.Test;

public class ExtensionValidatorTest {

    @Test
    public void testInvalidURL() {
        var validator = new ExtensionValidator();
        var extension = new ExtensionVersion();
        extension.setVersion("1");
        extension.setRepository("Foo and bar!");
        var issues = validator.validateMetadata(extension);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0))
                .isEqualTo(new ExtensionValidator.Issue("Invalid URL in field 'repository': Foo and bar!"));
    }

    @Test
    public void testGitProtocol() {
        var validator = new ExtensionValidator();
        var extension = new ExtensionVersion();
        extension.setVersion("1");
        extension.setRepository("git+https://github.com/Foo/Bar.git");
        var issues = validator.validateMetadata(extension);
        assertThat(issues).isEmpty();
    }
    
}