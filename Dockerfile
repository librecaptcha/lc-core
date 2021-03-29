FROM alpine:latest AS base-builder
ARG SBT_VERSION=1.3.13
RUN apk add --no-cache bash
ENV JAVA_HOME="/usr/lib/jvm/default-jvm/"
RUN apk add openjdk11-jre
ENV PATH=$PATH:${JAVA_HOME}/bin
RUN \
	wget -O sbt-$SBT_VERSION.tgz https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz && \
  	tar -xzvf sbt-$SBT_VERSION.tgz && \
  	rm sbt-$SBT_VERSION.tgz

ENV PATH=$PATH:/sbt/bin/


FROM base-builder AS builder
WORKDIR /build
COPY lib/ lib/
COPY project/plugins.sbt project/
COPY build.sbt .
COPY src/ src/
RUN sbt assembly


FROM alpine:latest AS base-core
ENV JAVA_HOME="/usr/lib/jvm/default-jvm/"
RUN apk add openjdk11-jre
ENV PATH=$PATH:${JAVA_HOME}/bin


FROM base-core
WORKDIR /lc-core
COPY --from=builder /build/target/scala-2.13/LibreCaptcha.jar .
RUN mkdir data/

EXPOSE 8888

CMD [ "java", "-jar", "LibreCaptcha.jar" ]