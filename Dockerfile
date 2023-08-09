FROM registry.access.redhat.com/ubi9/ubi AS jdtls-download
WORKDIR /jdtls
RUN curl -s -o jdtls.tar.gz https://download.eclipse.org/jdtls/milestones/1.16.0/jdt-language-server-1.16.0-202209291445.tar.gz &&\
	tar -xvf jdtls.tar.gz --no-same-owner &&\
	chmod 755 /jdtls/bin/jdtls &&\
        rm -rf jdtls.tar.gz

FROM registry.access.redhat.com/ubi9/ubi AS maven-index
ARG BUILDENV=local
COPY hack/maven.default.index /maven.default.index
RUN if [ $BUILDENV == "local" ]; then curl -s -o nexus-maven-repository-index.gz https://repo.maven.apache.org/maven2/.index/nexus-maven-repository-index.gz &&\
	zgrep -oaP '[a-z][a-zA-Z0-9_.-]+\|[a-zA-Z][a-zA-Z0-9_.-]+\|([a-zA-Z0-9_.-]+\|)?(sources|pom|jar|maven-plugin|ear|ejb|ejb-client|java-source|rar|war)\|(jar|war|ear|pom|war|rar)' nexus-maven-repository-index.gz | cut -d'|' -f1 | sed 's/$/.*/' | sort | uniq > /maven.default.index &&\
        rm -rf nexus-maven-repository-index.gz; fi

FROM registry.access.redhat.com/ubi9/ubi AS fernflower
RUN dnf install -y maven-openjdk17 wget --setopt=install_weak_deps=False && dnf clean all && rm -rf /var/cache/dnf
RUN wget --quiet https://github.com/JetBrains/intellij-community/archive/refs/tags/idea/231.9011.34.tar.gz -O intellij-community.tar && tar xf intellij-community.tar intellij-community-idea-231.9011.34/plugins/java-decompiler/engine && rm -rf intellij-community.tar
WORKDIR /intellij-community-idea-231.9011.34/plugins/java-decompiler/engine
RUN export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
RUN ./gradlew build -x test && rm -rf /root/.gradle
RUN mkdir /output && cp ./build/libs/fernflower.jar /output

FROM registry.access.redhat.com/ubi9/ubi AS addon-build
RUN dnf install -y maven-openjdk17 && dnf clean all && rm -rf /var/cache/dnf
WORKDIR /app
COPY ./ /app/
RUN export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
RUN JAVA_HOME=/usr/lib/jvm/java-17-openjdk mvn clean install -DskipTests=true

FROM registry.access.redhat.com/ubi9/ubi-minimal
RUN microdnf install -y python39 java-17-openjdk go-toolset maven-openjdk17 && microdnf clean all && rm -rf /var/cache/dnf
RUN go install golang.org/x/tools/gopls@latest
ENV JAVA_HOME /usr/lib/jvm/java-17-openjdk
COPY --from=jdtls-download /jdtls /jdtls/
COPY --from=addon-build /root/.m2/repository/io/konveyor/tackle/java-analyzer-bundle.core/1.0.0-SNAPSHOT/java-analyzer-bundle.core-1.0.0-SNAPSHOT.jar /jdtls/java-analyzer-bundle/java-analyzer-bundle.core/target/
COPY --from=fernflower /output/fernflower.jar /bin/fernflower.jar
COPY --from=addon-build /app/hack/lsp-cli /bin/lsp-cli
COPY --from=maven-index /maven.default.index /usr/local/etc/maven.default.index
CMD [ "/jdtls/bin/jdtls" ]
