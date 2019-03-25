#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CLASSPATH="${DIR}/*:${DIR}/lib/*"
JVM="-XX:+UseConcMarkSweepGC -Xms64m -Xmx1024m -Djava.net.preferIPv4Stack=true -Djgroups.tcp.address=${IP} -Dlog4j.configurationFile=${DIR}/log4j2.xml"
CONFIG="jgroups-creson-tcp.xml"

if [ "${CLOUD}" == "ec2" ];
then
    echo "AWS EC2 mode"
    CONFIG=jgroups-creson-ec2.xml
    IP=`/sbin/ifconfig eth0 | grep 'inet addr' | cut -d: -f2 | awk '{print $1}'`
    PIP=`curl -s checkip.amazonaws.com`
    EXTRA=${EXTRA}"-proxy ${PIP}:11222 -rf 2"
    CONFIG=jgroups-creson-ec2.xml
elif [ "${CLOUD}" == "gcp" ];
then
    echo "GCP mode"
    CONFIG=jgroups-creson-gcp.xml
    cat ${CONFIG} \
    | sed s,%BUCKET%,${BUCKET},g \
    | sed s,%BUCKET_KEY%,${BUCKET_KEY},g \
    | sed s,%BUCKET_SECRET%,${BUCKET_SECRET},g \
    | sed s,%IP%,${IP},g > tmp
    mv tmp ${CONFIG}
elif [ "${CLOUD}" == "k8s" ];
then
    echo "K8S mode"
    CONFIG=jgroups-creson-k8s.xml
else
    echo "local mode"
fi

mv ${CONFIG} jgroups.xml

echo "java -ea -cp ${CLASSPATH} ${JVM} org.infinispan.creson.Server -server ${IP}:${PORT} ${EXTRA}"
java -ea -cp ${CLASSPATH} ${JVM} org.infinispan.creson.Server -server ${IP}:${PORT} ${EXTRA}
