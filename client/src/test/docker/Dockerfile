FROM 0track/infinispan-creson-server:latest

MAINTAINER otrack

ENV NAME=infinispan-creson-client
ENV VERSION=9.4.1.Final
ENV JAR=$NAME-${VERSION}.jar
ENV TESTJAR=$NAME-${VERSION}-tests.jar
ENV THREADS=1
ENV INSTANCES=1
ENV PERSIST=""
ENV ID=""
ENV CLASS=Counter
ENV CALLS=1
ENV PARALLELISM=1
ENV PROXY=127.0.0.1:11222
ENV PARAMETERS="\"\""

ADD src/test/bin/client-test.sh  .
ADD target/$JAR .
ADD target/$TESTJAR .

CMD ["bash", "/infinispan-creson-server/client-test.sh"]

