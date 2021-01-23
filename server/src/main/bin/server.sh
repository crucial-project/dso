#!/bin/bash

# see Dockerfile for env. variables 

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CLASSPATH="${DIR}/*:${DIR}/lib/*"
CONFIG="jgroups-dso-tcp.xml"

if [[ "${CLOUD}" == "" ]];
then
    CLOUD="$1"
    IP='127.0.0.1'
    PORT='11222'
    CONFIG='jgroups-dso-udp.xml'
fi

if [[ "$2" != "" ]];
then
    PORT=$2
fi

if [[ "${CLOUD}" == "ec2" ]];
then
    echo "AWS EC2 mode"
    CONFIG=jgroups-dso-ec2.xml
    #IP=`/sbin/ifconfig eth0 | grep 'inet addr' | cut -d: -f2 | awk '{print $1}'`
    IP=`hostname -I | sed -e 's/[[:space:]]*$//'`
    PIP=`curl -s checkip.amazonaws.com`
    EXTRA="${EXTRA} -proxy ${PIP}:11222 -ec2"
elif [[ "${CLOUD}" == "vpc" ]];
then
    echo "AWS EC2 VPC mode"
    CONFIG=jgroups-dso-ec2.xml
    IP=`hostname -I | sed -e 's/[[:space:]]*$//'`
    EXTRA="${EXTRA} -ec2"
elif [[ "${CLOUD}" == "gcp" ]];
then
    echo "GCP mode"
    CONFIG=jgroups-dso-gcp.xml
    cat ${CONFIG} \
    | sed s,%BUCKET%,${BUCKET},g \
    | sed s,%BUCKET_KEY%,${BUCKET_KEY},g \
    | sed s,%BUCKET_SECRET%,${BUCKET_SECRET},g \
    | sed s,%IP%,${IP},g > tmp
    mv tmp ${CONFIG}
elif [[ "${CLOUD}" == "k8s" ]];
then
    echo "K8S mode"
    CONFIG=jgroups-dso-k8s.xml
else
    echo "local mode"
fi

cp ${CONFIG} jgroups.xml

JVM="${JVM_EXTRA} -Djava.net.preferIPv4Stack=true -Djgroups.tcp.address=${IP} --add-opens java.base/jdk.internal.loader=ALL-UNNAMED"
CMD="java -ea -cp \"${CLASSPATH}\" ${JVM} org.crucial.dso.Server -server ${IP}:${PORT} ${EXTRA}"
echo ${CMD}
bash -c "$CMD"