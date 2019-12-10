FROM python:3.5-slim-buster

MAINTAINER otrack

RUN pip install JPype1
RUN mkdir -p /usr/share/man/man1 # FIXME
RUN apt-get update && apt-get install -y openjdk-11-jre-headless

ENV NAME=infinispan-creson-python
ENV VERSION=9.4.16.Final
ENV JAR=$NAME-${VERSION}.jar
ENV SERVER=localhost:11222
ENV CLASSPATH=$JAR:lib/*

WORKDIR $NAME

ADD target/$JAR .
ADD target/lib lib
ADD src/test/python/sample.py  .

RUN chmod +x .

CMD ["python", "sample.py"]

