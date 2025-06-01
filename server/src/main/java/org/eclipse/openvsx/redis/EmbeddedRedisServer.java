package org.eclipse.openvsx.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.stereotype.Component;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

@Component
@ConditionalOnProperty(value = "ovsx.redis.embedded", havingValue = "true")
public class EmbeddedRedisServer {

    private final int port;
    private RedisServer server;

    public EmbeddedRedisServer(RedisStandaloneConfiguration configuration) {
        port = configuration.getPort();
    }

    @PostConstruct
    public void start() throws IOException {
        server = new RedisServer(port);
        server.start();
    }

    @PreDestroy
    public void stop() throws IOException {
        server.stop();
    }
}
