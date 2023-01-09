FROM registry.access.redhat.com/ubi9/ubi AS jdtls-download
WORKDIR /jdtls
RUN curl -o jdtls.tar.gz https://download.eclipse.org/jdtls/milestones/1.16.0/jdt-language-server-1.16.0-202209291445.tar.gz &&\
	tar -xvf jdtls.tar.gz --no-same-owner &&\
	chmod 755 /jdtls/bin/jdtls

FROM registry.access.redhat.com/ubi9/ubi AS addon-build
RUN dnf install -y maven
RUN dnf install -y java-17-openjdk
WORKDIR /app
COPY ./ /app/
RUN export JAVA_HOME=/usr/lib/jvm/java-17-openjdk 
RUN mvn clean install -DskipTests=true

FROM registry.access.redhat.com/ubi8/ubi-minimal
RUN microdnf install -y python39 java-17-openjdk go-toolset
RUN microdnf clean all
RUN go install golang.org/x/tools/gopls@latest
ENV JAVA_HOME /etc/alternatives/jre
COPY --from=jdtls-download /jdtls /jdtls/
COPY --from=addon-build /app/java-analyzer-bundle.core/target/java-analyzer-bundle.core-1.0.0-SNAPSHOT.jar /jdtls/java-analyzer-bundle/java-analyzer-bundle.core/target/
COPY --from=addon-build /app/hack/lsp-cli /bin/lsp-cli
CMD [ "/jdtls/bin/jdtls" ]







