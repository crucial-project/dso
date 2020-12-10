#!/usr/bin/env bash

PROJDIR=`realpath $(dirname "${BASH_SOURCE[0]}")/../../../../`
TARGETDIR="${PROJDIR}/target"

NSERVERS=1
NAME="dso-server"
MAINTAINER="0track"
TAG="latest"
IMAGE="${MAINTAINER}/${NAME}:${TAG}"
IMAGE_ID=$(docker images | grep ${NAME} | head -n 1 | awk '{print $3}')

INSTANCES="100"
THREADS="4"
CALLS="1000"
EXTRA="-verbose"

CLIENT="dso-client"
VERSION=$(cat ${PROJDIR}/pom.xml | grep version | head -n 1 | tr -d '[:blank:]' | sed s,\</*version\>,,g)

if [ $# -ne 1 ]; then
    echo "usage: -[create|blobs|counters|longs|countdownlatch|barrier|sbarrier|delete]"
    exit -1
fi

if [[ "$1" == "-create" ]]
then
    echo ">>>>> Starting servers..."
    rm -f ${TARGETDIR}/*.log
    for i in `seq 1 ${NSERVERS}`
    do
	port=$((11221+i))
	docker run --net host --rm \
	  --env JVM_EXTRA="-XX:+UseG1GC -Xms64m -Xmx1024m -Dlog4j.configuration=log4j-debug.properties" \
	  --env EXTRA="-rf 2" \
	  --env CLOUD=local \
	  --env PORT=${port} \
	  ${IMAGE} 2>&1 > ${TARGETDIR}/${i}.log &
    done
    up=0
    while [ ${up} != ${NSERVERS} ]; do
	up=$(cat ${TARGETDIR}/*.log | grep "LAUNCHED" | wc -l)
	echo -n "."
	sleep 1
    done
    echo " up!"
elif [[ "$1" == "-delete" ]]
  then
    echo ">>>>> Terminate"
    for c in $(docker ps -q --filter="ancestor=${IMAGE_ID}");
    do
	docker kill ${c}
    done
else
  if [[ "$1" == "-counters" ]]
  then
    echo ">>>>> Counters"
    CLASS="org.crucial.dso.AtomicCounter"
  elif [[ "$1" == "-longs" ]]
  then
    echo ">>>>> Longs"
    CLASS="org.crucial.dso.AtomicLong"
  elif [[ "$1" == "-blobs" ]]
  then
    echo ">>>>> Blobs"
    EXTRA="-parameters 100 "${EXTRA}
    CLASS="org.crucial.dso.Blob"
  elif [[ "$1" == "-barrier" ]]
  then
    echo ">>>>> Barrier"
    CLASS="org.crucial.dso.CyclicBarrier"
    INSTANCES=1
  elif [[ "$1" == "-sbarrier" ]]
  then
    echo ">>>>> Scalable Barrier"
    CLASS="org.crucial.dso.ScalableCyclicBarrier"
    INSTANCES=1
  elif [[ "$1" == "-countdownlatch" ]]
  then
    echo ">>>>> CountDownLatch"
    CLASS="org.crucial.dso.CountDownLatch"
    CALLS=1
    INSTANCES=1
  fi
  CLASSPATH=${PROJDIR}/target/*:${PROJDIR}/target/lib/*
  ARGS="-ea -Dlog4j2.configuration=log4j.xml org.crucial.dso.Benchmark -class ${CLASS} -instances ${INSTANCES} -threads ${THREADS} -calls ${CALLS}"
  echo "java -cp ${CLASSPATH} ${ARGS} ${EXTRA}"
  java -cp ${CLASSPATH} ${ARGS} ${EXTRA}
fi
