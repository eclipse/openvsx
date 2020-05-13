FROM gitpod/workspace-postgres:latest

USER gitpod

RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh \
    && sdk install java 11.0.7-open"

RUN curl https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-6.8.6.tar.gz --output elasticsearch-6.8.6.tar.gz \
    && tar -xzf elasticsearch-6.8.6.tar.gz \
    && rm elasticsearch-6.8.6.tar.gz
ENV ES_HOME="$HOME/elasticsearch-6.8.6"
