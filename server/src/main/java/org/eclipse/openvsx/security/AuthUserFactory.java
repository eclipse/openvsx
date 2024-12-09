package org.eclipse.openvsx.security;

import java.util.NoSuchElementException;

import org.eclipse.openvsx.OVSXConfig;
import org.eclipse.openvsx.OVSXConfig.AuthConfig.AttributeNames;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class AuthUserFactory {

    private static final AttributeNames GITHUB_ATTRIBUTES = new AttributeNames();

    static {
        GITHUB_ATTRIBUTES.setAvatarUrl("avatar_url");
        GITHUB_ATTRIBUTES.setEmail("email");
        GITHUB_ATTRIBUTES.setFullName("name");
        GITHUB_ATTRIBUTES.setLoginName("login");
        GITHUB_ATTRIBUTES.setProviderUrl("html_url");
    }

    private final OVSXConfig config;

    public AuthUserFactory(
        OVSXConfig config
    ) {
        this.config = config;
    }

    public AuthUser createAuthUser(String providerId, OAuth2User oauth2User) {
        var attributeNames = getAttributeNames(providerId);
        return new DefaultAuthUser(
            oauth2User.getName(),
            oauth2User.getAttribute(attributeNames.getAvatarUrl()),
            oauth2User.getAttribute(attributeNames.getEmail()),
            oauth2User.getAttribute(attributeNames.getFullName()),
            oauth2User.getAttribute(attributeNames.getLoginName()),
            providerId,
            oauth2User.getAttribute(attributeNames.getProviderUrl())
        );
    }

    /**
     * @param provider The provider to get the attribute mappings for.
     * @return The relevant attribute mappings.
     */
    private AttributeNames getAttributeNames(String provider) {
        var attributeNames = config.getAuth().getAttributeNames().get(provider);
        if (attributeNames == null) {
            return switch (provider) {
                case "github" -> GITHUB_ATTRIBUTES;
                default -> throw new NoSuchElementException("No attributes found for provider: " + provider);
            };
        }
        return attributeNames;
    }
}
