FROM openjdk:8-slim

MAINTAINER otrack

ENV NAME=infinispan-creson-server
ENV VERSION=9.4.1.Final
ENV CLOUD=local
ENV BUCKET=undefined
ENV BUCKET_KEY=undefined
ENV BUCKET_SECRET=undefined
ENV IP=127.0.0.1
ENV PORT=11222
ENV JAR=$NAME-${VERSION}.jar
ENV EXTRA="-rf 1"
ENV JVM_EXTRA="-XX:+UseConcMarkSweepGC -Xms64m -Xmx1024m"

WORKDIR $NAME

ADD target/$JAR .
ADD target/lib lib
ADD src/main/resources/jgroups-creson-tcp.xml  .
ADD src/main/resources/jgroups-creson-ec2.xml  .
ADD src/main/resources/jgroups-creson-gcp.xml  .
ADD src/main/resources/jgroups-creson-k8s.xml  .
ADD src/main/resources/log4j2.xml .
ADD src/main/bin/server.sh  .
RUN chmod +x .

CMD ["bash", "/infinispan-creson-server/server.sh"]
