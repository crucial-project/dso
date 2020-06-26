FROM openjdk:8-slim

MAINTAINER otrack

ENV NAME=dso-server
ENV VERSION=1.0
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
ADD src/main/resources/jgroups-dso-tcp.xml  .
ADD src/main/resources/jgroups-dso-ec2.xml  .
ADD src/main/resources/jgroups-dso-gcp.xml  .
ADD src/main/resources/jgroups-dso-k8s.xml  .
ADD src/main/resources/log4j2.xml .
ADD src/main/bin/server.sh  .
RUN chmod +x .

CMD ["bash", "/dso-server/server.sh"]
