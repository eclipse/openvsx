package org.eclipse.openvsx.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Schema(
    name = "NamespaceDetails",
    description = "Details of a namespace"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NamespaceDetailsJson extends ResultJson implements Serializable {

    public static NamespaceDetailsJson error(String message) {
        var result = new NamespaceDetailsJson();
        result.error = message;
        return result;
    }

    @Schema(description = "Name of the namespace")
    @NotNull
    public String name;

    @Schema(description = "Display name of the namespace")
    public String displayName;

    @Schema(description = "Description of the namespace")
    public String description;

    @Schema(description = "Logo URL of the namespace")
    public String logo;

    @Schema(hidden = true)
    public byte[] logoBytes;

    @Schema(description = "Website URL of the namespace")
    public String website;

    @Schema(description = "Support URL of the namespace")
    public String supportLink;

    @Schema(description = "Map of social network names to their profile URLs")
    public Map<String, String> socialLinks;

    @Schema(description = "Map of extension names to their metadata URLs")
    public List<SearchEntryJson> extensions;

    @Schema(description = "Indicates whether the namespace has an owner")
    @NotNull
    public boolean verified;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamespaceDetailsJson that = (NamespaceDetailsJson) o;
        return verified == that.verified
                && Objects.equals(name, that.name)
                && Objects.equals(displayName, that.displayName)
                && Objects.equals(description, that.description)
                && Objects.equals(logo, that.logo)
                && Arrays.equals(logoBytes, that.logoBytes)
                && Objects.equals(website, that.website)
                && Objects.equals(supportLink, that.supportLink)
                && Objects.equals(socialLinks, that.socialLinks)
                && Objects.equals(extensions, that.extensions);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name, displayName, description, logo, website, supportLink, socialLinks, extensions, verified);
        result = 31 * result + Arrays.hashCode(logoBytes);
        return result;
    }
}
