FROM openjdk:8-slim

MAINTAINER otrack

ENV NAME=infinispan-creson-server
ENV VERSION=9.3.5.Final
ENV CLOUD=gcp
ENV BUCKET=undefined
ENV BUCKET_KEY=undefined
ENV BUCKET_SECRET=undefined
ENV IP=127.0.0.1
ENV PORT=11222
ENV JAR=$NAME-${VERSION}.jar
ENV TESTJAR=$NAME-${VERSION}-tests.jar
ENV EXTRA="-rf 1"

WORKDIR $NAME

ADD target/$JAR .
ADD target/$TESTJAR .
ADD target/lib lib
ADD src/main/resources/jgroups-creson-tcp.xml  .
ADD src/main/resources/jgroups-creson-ec2.xml  .
ADD src/main/resources/jgroups-creson-gcp.xml  .
ADD src/main/resources/log4j2.xml .
ADD src/main/bin/server.sh  .
RUN chmod +x .

CMD ["bash", "/infinispan-creson-server/server.sh"]

