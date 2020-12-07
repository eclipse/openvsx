FROM gitpod/workspace-postgres:latest

USER gitpod

RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh \
    && sdk install java 11.0.2-open"

RUN curl https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-7.9.3-linux-x86_64.tar.gz --output elasticsearch-linux-x86_64.tar.gz \
    && tar -xzf elasticsearch-linux-x86_64.tar.gz \
    && rm elasticsearch-linux-x86_64.tar.gz
ENV ES_HOME="$HOME/elasticsearch-7.9.3"
