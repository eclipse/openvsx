FROM registry.access.redhat.com/ubi9/nodejs-20-minimal:1-63.1725851021 as builder

USER root

RUN microdnf -y --nodocs --setopt=install_weak_deps=0 install \
    git \
    wget \
  && microdnf clean all

USER 1001
  
RUN npm install -g ovsx \ 
    && ovsx --version
