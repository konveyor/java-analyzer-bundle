FROM registry.access.redhat.com/ubi9/ubi AS jdtls-download
WORKDIR /jdtls
RUN curl -s -o jdtls.tar.gz https://download.eclipse.org/jdtls/milestones/1.16.0/jdt-language-server-1.16.0-202209291445.tar.gz &&\
	tar -xvf jdtls.tar.gz --no-same-owner &&\
	chmod 755 /jdtls/bin/jdtls &&\
        rm -rf jdtls.tar.gz

FROM registry.access.redhat.com/ubi9/ubi AS maven-index
COPY hack/maven.default.index /maven.default.index
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

FROM registry.access.redhat.com/ubi9/ubi-minimal as gopls-build
RUN microdnf install -y go-toolset && microdnf clean all && rm -rf /var/cache/dnf
RUN go install golang.org/x/tools/gopls@latest

FROM registry.access.redhat.com/ubi9/ubi-minimal
RUN microdnf install -y python39 maven-openjdk17 java-17-openjdk-devel golang-bin --nodocs --setopt=install_weak_deps=0 && microdnf clean all && rm -rf /var/cache/dnf
ENV JAVA_HOME /usr/lib/jvm/java-17-openjdk
COPY --from=gopls-build /root/go/bin/gopls /root/go/bin/gopls
COPY --from=jdtls-download /jdtls /jdtls/
COPY --from=addon-build /root/.m2/repository/io/konveyor/tackle/java-analyzer-bundle.core/1.0.0-SNAPSHOT/java-analyzer-bundle.core-1.0.0-SNAPSHOT.jar /jdtls/java-analyzer-bundle/java-analyzer-bundle.core/target/
COPY --from=fernflower /output/fernflower.jar /bin/fernflower.jar
COPY --from=maven-index /maven.default.index /usr/local/etc/maven.default.index
CMD [ "/jdtls/bin/jdtls" ]
