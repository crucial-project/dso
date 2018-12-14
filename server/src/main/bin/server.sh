#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CLASSPATH="${DIR}/*:${DIR}/lib/*"
IP="0.0.0.0"
EXTRA="-rf 1"

if [ "$1" == "-ec2" ];
then
    echo "AWS EC2 mode"
    #IP=`/sbin/ifconfig eth0 | grep 'inet addr' | cut -d: -f2 | awk '{print $1}'`
    IP=`hostname -I | sed -e 's/[[:space:]]*$//'`
    PIP=`curl -s checkip.amazonaws.com`
    EXTRA="-proxy ${PIP}:11222 -ec2 -rf 1"
elif [ "$1" == "-vpc" ];
then
    echo "AWS EC2 VPC mode"
    IP=`hostname -I | sed -e 's/[[:space:]]*$//'`
    EXTRA="-ec2 -rf 1"
fi

JVM="-XX:+UseConcMarkSweepGC -Xms64m -Xmx1024m -Djava.net.preferIPv4Stack=true -Djgroups.tcp.address=${IP} -Dlog4j.configurationFile=${DIR}/log4j2.xml"
java -cp ${CLASSPATH} ${JVM} org.infinispan.crucial.Server -server ${IP}:11222 ${EXTRA}