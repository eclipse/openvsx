FROM gitpod/workspace-postgres:latest

# the following env variable is solely here to invalidate the docker image. We want to rebuild the image from time to time to get the latest base image (which is cached).
ENV DOCKER_BUMP=2

USER gitpod

RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh \
    # Install Java 18 for the Java extension to function properly
    && sdk install java 18.0.1.1-open \
    && sdk install java 11.0.2-open"

RUN curl https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-7.11.0-linux-x86_64.tar.gz --output elasticsearch-linux-x86_64.tar.gz \
    && tar -xzf elasticsearch-linux-x86_64.tar.gz \
    && rm elasticsearch-linux-x86_64.tar.gz
ENV ES_HOME="$HOME/elasticsearch-7.11.0"

RUN curl -L https://github.com/prometheus/prometheus/releases/download/v2.38.0/prometheus-2.38.0.linux-amd64.tar.gz --output prometheus-linux-amd64.tar.gz \
    && tar -xzf prometheus-linux-amd64.tar.gz \
    && rm prometheus-linux-amd64.tar.gz
ENV PROMETHEUS_HOME="$HOME/prometheus-2.38.0.linux-amd64"
ENV PROMETHEUS_PORT="9090"

RUN curl https://dl.grafana.com/enterprise/release/grafana-enterprise-9.1.1.linux-amd64.tar.gz --output grafana-enterprise-linux-amd64.tar.gz \
    && tar -xzf grafana-enterprise-linux-amd64.tar.gz \
    && rm grafana-enterprise-linux-amd64.tar.gz
ENV GRAFANA_HOME="$HOME/grafana-9.1.1"