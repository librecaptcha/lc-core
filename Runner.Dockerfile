FROM adoptopenjdk/openjdk16:latest  AS base-core
ENV JAVA_HOME="/usr/lib/jvm/default-jvm/"
RUN apt update && apt install -y ttf-dejavu
ENV PATH=$PATH:${JAVA_HOME}/bin


FROM base-core
RUN mkdir /lc-core
COPY target/scala-3.2.1/LibreCaptcha.jar /lc-core
WORKDIR /lc-core
ENV UID_TO_SET 1001552021
RUN mkdir data/ && \
    groupadd --gid $UID_TO_SET lc-core && useradd --uid $UID_TO_SET -l -g lc-core lc-core && \
	  chown -R lc-core:lc-core /lc-core && chmod -R g+w /lc-core

EXPOSE 8888
USER lc-core

CMD [ "java", "-jar", "LibreCaptcha.jar" ]
