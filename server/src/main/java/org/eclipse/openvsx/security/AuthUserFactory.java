package org.eclipse.openvsx.security;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class AuthUserFactory {

    public AuthUser createAuthUser(String providerId, OAuth2User oauth2User) {
        return new DefaultAuthUser(
            oauth2User.getName(),
            oauth2User.getAttribute("avatar_url"),
            oauth2User.getAttribute("email"),
            oauth2User.getAttribute("name"),
            oauth2User.getAttribute("login"),
            providerId,
            oauth2User.getAttribute("html_url")
        );
    }
}
