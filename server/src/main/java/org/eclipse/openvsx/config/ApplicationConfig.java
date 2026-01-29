package org.eclipse.openvsx.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application configuration for OpenVSX.
 * Reads configuration from application.yml under the 'ovsx' prefix.
 * 
 * Properties:
 * - ovsx.webui.url: Base URL for the web UI (fallback URL)
 * - ovsx.server.url: Base URL for the API server (preferred URL)
 */
@Component
@ConfigurationProperties(prefix = "ovsx")
public class ApplicationConfig {
    
    private WebUI webui = new WebUI();
    private Server server = new Server();
    
    /**
     * Web UI configuration.
     */
    public static class WebUI {
        private String url;
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
    }
    
    /**
     * Server (API) configuration.
     * Takes precedence over WebUI URL.
     */
    public static class Server {
        private String url;
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
    }
    
    public WebUI getWebui() {
        if (webui == null) {
            webui = new WebUI();
        }
        return webui;
    }
    
    public void setWebui(WebUI webui) {
        this.webui = webui;
    }
    
    public Server getServer() {
        if (server == null) {
            server = new Server();
        }
        return server;
    }
    
    public void setServer(Server server) {
        this.server = server;
    }
}