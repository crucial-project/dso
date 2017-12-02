FROM openjdk

MAINTAINER otrack

WORKDIR /app

RUN apt-get update && apt-get install -y \
    tar \
    git \
    maven \
    build-essential

ADD https://api.github.com/repos/otrack/creson/git/refs/heads/master creson-version.json
RUN git clone https://github.com/otrack/creson
RUN mvn -f creson clean package -DskipTests
RUN tar zxvf /app/creson/server/target/infinispan-creson-server-9.0.3-Final.tar.gz -C /app
RUN mkdir /app/userlibs

ENV EXTRA ""

CMD ["sh", "-c", "/app/infinispan-creson-server-9.0.3-Final/bin/server.sh -userLibs=/app/userlibs ${EXTRA}"]
