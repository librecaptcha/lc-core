FROM adoptopenjdk/openjdk16:alpine-jre  AS base-core
ENV JAVA_HOME="/usr/lib/jvm/default-jvm/"
RUN apk add --update ttf-dejavu
ENV PATH=$PATH:${JAVA_HOME}/bin


FROM base-core
WORKDIR /lc-core
COPY /build/target/scala-2.13/LibreCaptcha.jar .
RUN mkdir data/

EXPOSE 8888

CMD [ "java", "-jar", "LibreCaptcha.jar" ]
