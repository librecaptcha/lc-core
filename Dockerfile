FROM eclipse-temurin:17-jre-jammy AS base-builder
ARG SBT_VERSION=1.7.1
ENV JAVA_HOME="/usr/lib/jvm/default-jvm/"
ENV PATH=$PATH:${JAVA_HOME}/bin
RUN \
        apt update && \
        apt install -y wget && \
	wget -O sbt-$SBT_VERSION.tgz https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz && \
        tar -xzvf sbt-$SBT_VERSION.tgz && \
        rm sbt-$SBT_VERSION.tgz

ENV PATH=$PATH:/sbt/bin/


FROM base-builder AS sbt-builder
WORKDIR /build
COPY lib/ lib/
COPY project/plugins.sbt project/
COPY build.sbt .
RUN sbt assembly

FROM sbt-builder as builder
COPY src/ src/
RUN sbt assembly

FROM eclipse-temurin:17-jre-jammy  AS base-core
ENV JAVA_HOME="/usr/lib/jvm/default-jvm/"
RUN apt update && apt install -y fonts-dejavu
ENV PATH=$PATH:${JAVA_HOME}/bin


FROM base-core
WORKDIR /lc-core
COPY --from=builder /build/target/scala-3.4.1/LibreCaptcha.jar .
RUN mkdir data/

EXPOSE 8888

CMD [ "java", "-jar", "LibreCaptcha.jar" ]
