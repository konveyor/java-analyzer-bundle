# JDK21 manual installation as there seem to be no packages for JDK21 yet
FROM registry.access.redhat.com/ubi9/ubi AS jdk21
WORKDIR /jdk21
RUN dnf install -y wget tar --setopt=install_weak_deps=False && \
    wget https://download.java.net/java/GA/jdk21.0.2/f2283984656d49d69e91c558476027ac/13/GPL/openjdk-21.0.2_linux-x64_bin.tar.gz && \
    tar -xzf openjdk-21.0.2_linux-x64_bin.tar.gz

# Download JDT language server
FROM registry.access.redhat.com/ubi9/ubi AS jdtls-download
WORKDIR /jdtls
RUN curl -s -o jdtls.tar.gz https://download.eclipse.org/jdtls/milestones/1.38.0/jdt-language-server-1.38.0-202408011337.tar.gz &&\
	tar -xvf jdtls.tar.gz --no-same-owner &&\
	chmod 755 /jdtls/bin/jdtls &&\
        rm -rf jdtls.tar.gz
COPY jdtls-bin-override/jdtls.py /jdtls/bin/jdtls.py

# Prepare Maven index for public artifact detection
FROM registry.access.redhat.com/ubi9/ubi AS maven-index
COPY hack/maven.default.index /maven.default.index

# Build fernflower - use Java17 (cannot be built with Java21)
FROM registry.access.redhat.com/ubi9/ubi AS fernflower
RUN dnf install -y maven-openjdk17 wget --setopt=install_weak_deps=False && dnf clean all && rm -rf /var/cache/dnf
RUN wget --quiet https://github.com/JetBrains/intellij-community/archive/refs/tags/idea/231.9011.34.tar.gz -O intellij-community.tar && tar xf intellij-community.tar intellij-community-idea-231.9011.34/plugins/java-decompiler/engine && rm -rf intellij-community.tar
WORKDIR /intellij-community-idea-231.9011.34/plugins/java-decompiler/engine
RUN export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
RUN ./gradlew build -x test && rm -rf /root/.gradle
RUN mkdir /output && cp ./build/libs/fernflower.jar /output

# Build this LS plugin
FROM registry.access.redhat.com/ubi9/ubi AS addon-build
COPY --from=jdk21 /jdk21/jdk-21.0.2 /usr/lib/jvm/jdk-21.0.2
RUN export JAVA_HOME=/usr/lib/jvm/jdk-21.0.2
RUN dnf install -y maven --setopt=install_weak_deps=False && dnf clean all && rm -rf /var/cache/dnf
WORKDIR /app
COPY ./ /app/
RUN export JAVA_HOME=/usr/lib/jvm/jdk-21.0.2
RUN JAVA_HOME=/usr/lib/jvm/jdk-21.0.2 mvn clean install -DskipTests=true

# Install go
FROM registry.access.redhat.com/ubi9/ubi-minimal as gopls-build
RUN microdnf install -y go-toolset && microdnf clean all && rm -rf /var/cache/dnf
RUN go install golang.org/x/tools/gopls@latest

#
FROM registry.access.redhat.com/ubi9/ubi-minimal
# Java 1.8 is required for backwards compatibility with older versions of Gradle
RUN microdnf install -y python39 java-1.8.0-openjdk-devel maven golang-bin tar gzip zip --nodocs --setopt=install_weak_deps=0 && microdnf clean all && rm -rf /var/cache/dnf
# Manually installed jdk21
COPY --from=jdk21 /jdk21/jdk-21.0.2 /usr/lib/jvm/jdk-21.0.2
ENV JAVA_HOME=/usr/lib/jvm/jdk-21.0.2
# Specify Java 1.8 home for usage with gradle wrappers
ENV JAVA8_HOME /usr/lib/jvm/java-1.8.0-openjdk
ENV M2_HOME /usr/local/apache-maven-3.9.5

# Copy "download sources" gradle task. This is needed to download project sources.
RUN mkdir /root/.gradle
COPY ./gradle/build.gradle /root/.gradle/task.gradle

COPY --from=gopls-build /root/go/bin/gopls /root/go/bin/gopls
COPY --from=jdtls-download /jdtls /jdtls/
COPY --from=addon-build /root/.m2/repository/io/konveyor/tackle/java-analyzer-bundle.core/1.0.0-SNAPSHOT/java-analyzer-bundle.core-1.0.0-SNAPSHOT.jar /jdtls/java-analyzer-bundle/java-analyzer-bundle.core/target/
COPY --from=fernflower /output/fernflower.jar /bin/fernflower.jar
COPY --from=maven-index /maven.default.index /usr/local/etc/maven.default.index
RUN ln -sf /root/.m2 /.m2 && chgrp -R 0 /root && chmod -R g=u /root
CMD [ "/jdtls/bin/jdtls" ]
