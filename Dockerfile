FROM registry.access.redhat.com/ubi9/ubi AS jdtls-download
WORKDIR /jdtls
RUN curl -o jdtls.tar.gz https://download.eclipse.org/jdtls/milestones/1.16.0/jdt-language-server-1.16.0-202209291445.tar.gz &&\
	tar -xvf jdtls.tar.gz --no-same-owner &&\
	chmod 755 /jdtls/bin/jdtls

FROM registry.access.redhat.com/ubi9/ubi AS maven-index
RUN curl -o nexus-maven-repository-index.gz https://repo.maven.apache.org/maven2/.index/nexus-maven-repository-index.gz &&\
	gunzip -k nexus-maven-repository-index.gz &&\
	grep -oaP '[a-z][a-zA-Z0-9_.-]+\|[a-zA-Z][a-zA-Z0-9_.-]+\|([a-zA-Z0-9_.-]+\|)?(sources|pom|jar|maven-plugin|ear|ejb|ejb-client|java-source|rar|war)\|(jar|war|ear|pom|war|rar)' nexus-maven-repository-index | cut -d'|' -f1 | sed 's/$/.*/' | sort | uniq > /maven.default.index

FROM registry.access.redhat.com/ubi9/ubi AS fernflower
RUN dnf install -y maven-openjdk17 wget
RUN wget https://github.com/JetBrains/intellij-community/archive/refs/tags/idea/231.9011.34.tar.gz -O intellij-community.tar && tar xf intellij-community.tar
WORKDIR /intellij-community-idea-231.9011.34/plugins/java-decompiler/engine
RUN export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
RUN ./gradlew build -x test
RUN mkdir /output && cp ./build/libs/fernflower.jar /output

FROM registry.access.redhat.com/ubi9/ubi AS addon-build
RUN dnf install -y maven-openjdk17
WORKDIR /app
COPY ./ /app/
RUN export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
RUN JAVA_HOME=/usr/lib/jvm/java-17-openjdk mvn clean install -DskipTests=true

FROM registry.access.redhat.com/ubi9/ubi-minimal
RUN microdnf install -y python39 java-17-openjdk go-toolset maven-openjdk17
RUN microdnf clean all
RUN go install golang.org/x/tools/gopls@latest
ENV JAVA_HOME /usr/lib/jvm/java-17-openjdk
COPY --from=jdtls-download /jdtls /jdtls/
COPY --from=addon-build /root/.m2/repository/io/konveyor/tackle/java-analyzer-bundle.core/1.0.0-SNAPSHOT/java-analyzer-bundle.core-1.0.0-SNAPSHOT.jar /jdtls/java-analyzer-bundle/java-analyzer-bundle.core/target/
COPY --from=fernflower /output/fernflower.jar /bin/fernflower.jar
COPY --from=addon-build /app/hack/lsp-cli /bin/lsp-cli
COPY --from=maven-index /maven.default.index /maven.default.index
CMD [ "/jdtls/bin/jdtls" ]
