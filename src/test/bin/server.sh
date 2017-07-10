#!/bin/sh

IP=`/sbin/ifconfig eth0 | grep 'inet addr' | cut -d: -f2 | awk '{print $1}'`
SRC="lib/"

java -XX:+UseConcMarkSweepGC -Xms64m -Xmx1024m -Djava.net.preferIPv4Stack=true -Djgroups.tcp.address=${IP} -Dlog4j.configuration=log4j2.xml -cp *:${SRC}/* org.infinispan.creson.Server -server ${IP}:11222 -ec2 -rf 2
