FROM 0track/dso-server:latest

MAINTAINER otrack

ENV NAME=dso-client
ENV VERSION=1.0
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

ADD src/test/bin/client-tests.sh  .
ADD target/$JAR .
ADD target/$TESTJAR .

CMD ["bash", "/dso-server/client-tests.sh"]

