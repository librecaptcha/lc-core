FROM adoptopenjdk/openjdk16:latest  AS base-core
ENV JAVA_HOME="/usr/lib/jvm/default-jvm/"
RUN apt update && apt install -y ttf-dejavu
ENV PATH=$PATH:${JAVA_HOME}/bin


FROM base-core
RUN mkdir /lc-core
COPY target/scala-3.3.0/LibreCaptcha.jar /lc-core
WORKDIR /lc-core
RUN mkdir data/

EXPOSE 8888

CMD [ "java", "-jar", "LibreCaptcha.jar" ]
