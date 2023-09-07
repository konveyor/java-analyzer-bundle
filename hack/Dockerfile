FROM registry.access.redhat.com/ubi9/ubi
RUN dnf module enable -y maven:3.8 \
 && dnf clean all \
 && dnf -y update \
 && dnf -y install https://dl.fedoraproject.org/pub/epel/epel-release-latest-9.noarch.rpm \
 && dnf -y install maven-openjdk17 git parallel pigz \
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
 && java -classpath $(find /root/.m2 -name lucene-core*.jar) export.java | pigz -k -p$(nproc) > export.gz \
 && rm -rf index \
 && unpigz -p$(nproc) export.gz \
 && cat export | \
    parallel -j150% --pipe --block 100M --cat \
    grep -oaP "'[a-z][a-zA-Z0-9_.-]+\|[a-zA-Z][a-zA-Z0-9_.-]+\|([a-zA-Z0-9_.-]+\|)?(sources|pom|jar|maven-plugin|ear|ejb|ejb-client|java-source|rar|war)\|(jar|war|ear|pom|war|rar)'" {} | \
    cut -d'|' -f1 | \
    sed 's/$/.*/' | \
    sort | \
    uniq > maven.default.index \
 && rm -rf export
