FROM gitpod/workspace-postgres:latest

# the following env variable is solely here to invalidate the docker image. We want to rebuild the image from time to time to get the latest base image (which is cached).
ENV DOCKER_BUMP=2

USER gitpod

RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh \
    && sdk install java 17.0.7-tem"

RUN curl https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-8.7.1-linux-x86_64.tar.gz --output elasticsearch-linux-x86_64.tar.gz \
    && tar -xzf elasticsearch-linux-x86_64.tar.gz \
    && rm elasticsearch-linux-x86_64.tar.gz
ENV ES_HOME="$HOME/elasticsearch-8.7.1"
