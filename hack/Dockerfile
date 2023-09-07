FROM registry.access.redhat.com/ubi9/ubi
RUN dnf module enable -y maven:3.8 \
 && dnf clean all \
 && dnf -y update \
 && dnf -y install maven-openjdk17 git \
 && dnf clean all
RUN git clone https://github.com/apache/maven-indexer.git
RUN cd maven-indexer/indexer-cli && mvn install -Denforcer.skip=true -Dmaven.test.skip=true
RUN mkdir -p /unpack/index
COPY export.java /unpack/export.java
RUN cd /unpack/index \
 && curl -s -o nexus-maven-repository-index.gz https://repo.maven.apache.org/maven2/.index/nexus-maven-repository-index.gz \
 && java -jar $(find /root/.m2 -name indexer-cli-*-cli.jar) -u nexus-maven-repository-index.gz \
 && rm nexus-maven-repository-index.gz \
 && cd /unpack \
 && java -cp $(find /root/.m2 -name lucene-core*.jar) export.java \
 && rm -rf index
