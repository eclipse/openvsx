package org.eclipse.openvsx;

import static java.util.Collections.emptyMap;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * TODO: use lombok to reduce boilerplate, it's very needed.
 */
@Configuration
@ConfigurationProperties(prefix = "ovsx")
public class OVSXConfig {

    private AuthConfig auth = new AuthConfig();

    public AuthConfig getAuth() {
        return auth;
    }

    public void setAuth(AuthConfig authConfig) {
        this.auth = authConfig;
    }

    public static class AuthConfig {

        private String provider = "github";

        /**
         * Configuration example:
         * <pre><code>
         * ovsx:
         *   auth:
         *     attribute-names:
         *       github: # provider name
         *         login: field-returned-by-your-provider
         * </code></pre>
         */
        private Map<String, AttributeNames> attributeNames = emptyMap();

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public Map<String, AttributeNames> getAttributeNames() {
            return attributeNames;
        }

        public void setAttributeNames(Map<String, AttributeNames> attributeNames) {
            this.attributeNames = attributeNames;
        }

        public static class AttributeNames {

            public static final AttributeNames EMPTY = new AttributeNames();

            private String avatarUrl;
            private String email;
            private String fullName;
            private String loginName;
            private String providerUrl;

            public String getAvatarUrl() {
                return avatarUrl;
            }

            public void setAvatarUrl(String avatarUrl) {
                this.avatarUrl = avatarUrl;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }

            public String getFullName() {
                return fullName;
            }

            public void setFullName(String fullName) {
                this.fullName = fullName;
            }

            public String getLoginName() {
                return loginName;
            }

            public void setLoginName(String loginName) {
                this.loginName = loginName;
            }

            public String getProviderUrl() {
                return providerUrl;
            }

            public void setProviderUrl(String providerUrl) {
                this.providerUrl = providerUrl;
            }
        }
    }
}
