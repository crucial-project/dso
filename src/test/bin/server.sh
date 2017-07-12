#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
IP=`/sbin/ifconfig eth0 | grep 'inet addr' | cut -d: -f2 | awk '{print $1}'`
PIP=`curl -s checkip.amazonaws.com`
CLASSPATH="${DIR}/*:${DIR}/lib/*"

java -XX:+UseConcMarkSweepGC -Xms64m -Xmx2048m -Djava.net.preferIPv4Stack=true -Djgroups.tcp.address=${IP} -Dlog4j.configuration=log4j2.xml -cp ${CLASSPATH} org.infinispan.creson.Server -server ${IP}:11222 -proxy ${PIP}:11222 -ec2 -rf 2
