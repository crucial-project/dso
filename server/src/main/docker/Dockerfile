FROM openjdk

MAINTAINER otrack

WORKDIR /app

RUN apt-get update && apt-get install -y \
    tar \
    git \
    maven \
    build-essential

ADD https://api.github.com/repos/otrack/infinispan-creson/git/refs/heads/master creson-version.json
RUN git clone https://github.com/otrack/infinispan-creson
RUN mvn -f infinispan-creson clean package -DskipTests
RUN tar zxvf  /app/infinispan-creson/server/target/infinispan-creson-server-9.0.3.Final.tar.gz -C /app
RUN mkdir /app/userlibs

ENV JVMPERF="-XX:+UseConcMarkSweepGC -Xms64m -Xmx2048m"
ENV CLASSPATH="/app/infinispan-creson-server-9.0.3.Final/*:/app/infinispan-creson-server-9.0.3.Final/lib/*"
ENV SERVER="0.0.0.0::11222"
ENV USERLIBS="/app/userlibs"
ENV RF="1"
ENV EXTRA=""

CMD java ${JVMPERF} -Djava.net.preferIPv4Stack=true -Djgroups.tcp.address=${IP} -Dlog4j.configuration=log4j2.xml -cp ${CLASSPATH} org.infinispan.creson.Server -server ${SERVER} -userLibs ${USERLIBS} -rf 1 ${EXTRA}
