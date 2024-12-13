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

    private OAuth2Config oauth2 = new OAuth2Config();

    public OAuth2Config getOauth2() {
        return oauth2;
    }

    public void setOauth2(OAuth2Config oauth2Config) {
        this.oauth2 = oauth2Config;
    }

    public static class OAuth2Config {

        /**
         * The user authentication provider to use.
         */
        private String provider = "github";

        /**
         * Configuration example:
         * <pre><code>
         *ovsx:
         *  oauth2:
         *    attribute-names:
         *      [provider-name]:
         *        avatar-url: string
         *        email: string
         *        full-name: string
         *        login-name: string
         *        provider-url: string
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
