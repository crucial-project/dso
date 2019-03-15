FROM 0track/infinispan-creson-server:latest

MAINTAINER otrack

ENV CLIENTS=1
ENV INSTANCES=1
ENV CALLS=1
ENV PROXY=127.0.0.1:11222
ENV CLASS=Counter
ENV EXTRA=

ADD src/test/bin/client-test.sh  .

CMD ["bash", "/infinispan-creson-server/client-test.sh"]

