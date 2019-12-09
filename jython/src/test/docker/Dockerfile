FROM harisekhon/jython

MAINTAINER otrack

ENV NAME=infinispan-creson-jython
ENV VERSION=9.4.16.Final
ENV JAR=$NAME-${VERSION}.jar
ENV SERVER=localhost:11222
ENV CLASSPATH=$JAR:lib/*

WORKDIR $NAME

ADD target/$JAR .
ADD target/lib lib
ADD src/test/bin/simple.sh  .
ADD src/test/python/simple.py  .

RUN chmod +x .

CMD ["sample.py"]

