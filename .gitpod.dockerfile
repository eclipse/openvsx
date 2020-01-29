FROM gitpod/workspace-postgres:latest

USER gitpod

RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh \
    && sdk default java 11.0.5-open"

RUN curl https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-6.8.4.tar.gz --output elasticsearch-6.8.4.tar.gz \
    && tar -xzf elasticsearch-6.8.4.tar.gz
ENV ES_HOME="$HOME/elasticsearch-6.8.4"
