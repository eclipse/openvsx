package org.eclipse.openvsx.security;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.openvsx.OVSXConfig;
import org.eclipse.openvsx.OVSXConfig.OAuth2Config.AttributeNames;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class AuthUserFactory {

    protected static final Map<String, AttributeNames> DEFAULTS = new HashMap<>();

    public static class MissingProvider extends Exception {
        public MissingProvider(String provider) { super("Missing configuration: ovsx.auth.attribute-names." + provider); }
    }

    static {
        var github = new AttributeNames();
        github.setAvatarUrl("avatar_url");
        github.setEmail("email");
        github.setFullName("name");
        github.setLoginName("login");
        github.setProviderUrl("html_url");
        DEFAULTS.put("github", github);
    }

    protected final OVSXConfig config;

    public AuthUserFactory(OVSXConfig config) {
        this.config = config;
    }

    /**
     * @param provider The configured OAuth2 provider from which the user object came from.
     * @param user The OAuth2 user object to get attributes from.
     * @return An {@link AuthUser} instance with attributes set according to the current configuration.
     * @throws MissingProvider if an attribute name mapping is missing for the given provider.
     */
    public AuthUser createAuthUser(String provider, OAuth2User user) throws MissingProvider {
        var attr = getAttributeNames(provider);
        return new DefaultAuthUser(
            user.getName(),
            getAttribute(user, attr.getAvatarUrl()),
            getAttribute(user, attr.getEmail()),
            getAttribute(user, attr.getFullName()),
            getAttribute(user, attr.getLoginName()),
            provider,
            getAttribute(user, attr.getProviderUrl())
        );
    }

    protected <T> T getAttribute(OAuth2User oauth2User, String attribute) {
        return attribute == null ? null : oauth2User.getAttribute(attribute);
    }

    /**
     * @param provider The provider to get the attribute mappings for.
     * @return The relevant attribute mappings.
     */
    protected AttributeNames getAttributeNames(String provider) throws MissingProvider {
        var attributeNames = config.getOauth2().getAttributeNames().get(provider);
        if (attributeNames == null) attributeNames = DEFAULTS.get(provider);
        if (attributeNames == null) throw new MissingProvider(provider);
        return attributeNames;
    }
}
