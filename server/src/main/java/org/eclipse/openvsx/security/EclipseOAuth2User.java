package org.eclipse.openvsx.security;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class EclipseOAuth2User implements OAuth2User {
    private List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("ROLE_PUBLISHER");
    private Map<String, Object> attributes;
    private String sub;
    private String name;
    private String github_handle;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public Map<String, Object> getAttributes() {
        if (this.attributes == null) {
            this.attributes = new HashMap<>();
            this.attributes.put("sub", this.getSub());
            this.attributes.put("name", this.getName());
            this.attributes.put("github_handle", this.getGitHubHandle());
        }
        return attributes;
    }

    public String getSub() {
        return this.sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGitHubHandle() {
        return this.github_handle;
    }

    public void setGitHubHandle(String github_handle) {
        this.github_handle = github_handle;
    }
}