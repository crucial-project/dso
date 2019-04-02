FROM 0track/infinispan-creson-server:latest

MAINTAINER otrack

ENV NAME=infinispan-creson-client
ENV VERSION=9.4.1.Final
ENV CLIENTS=1
ENV INSTANCES=1
ENV CALLS=1
ENV PROXY=127.0.0.1:11222
ENV CLASS=Counter
ENV EXTRA=""
ENV JAR=$NAME-${VERSION}.jar
ENV TESTJAR=$NAME-${VERSION}-tests.jar

ADD src/test/bin/client-test.sh  .
ADD target/$JAR .
ADD target/$TESTJAR .

CMD ["bash", "/infinispan-creson-server/client-test.sh"]

