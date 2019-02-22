#!/usr/bin/env bash

PROJDIR=`realpath $(dirname "${BASH_SOURCE[0]}")/../../../`
TARGETDIR="${PROJDIR}/target"

NAME="infinispan-creson-server"
MAINTAINER="0track"
TAG="latest"
IMAGE="${MAINTAINER}/${NAME}:${TAG}"
IMAGE_ID=$(docker images | grep ${NAME} | head -n 1 | awk '{print $3}')

INSTANCES="1"
CLIENTS="1"
CALLS="10000"

CLASSPATH=${PROJDIR}/target/*:${PROJDIR}/target/lib/*

if [ $# -ne 1 ]; then
    echo "usage: config key"
    exit -1
fi

if [[ "$1" == "-create" ]]
then    
    NSERVERS=3
    echo ">>>>> Starting servers..."
    for i in `seq 1 ${NSERVERS}`
    do
	port=$((11221+i))
	docker run --net host --rm -p ${port}:${port} --env EXTRA="-rf 2" --env CLOUD=local --env PORT=${port} ${IMAGE} 2>&1 > ${TARGETDIR}/.${i}.log &
    done
    up=-1
    while [ ${up} != ${NSERVERS} ]; do
	up=$(cat ${TARGETDIR}/.*.log | grep "LAUNCHED" | wc -l)
	echo -n "."
	sleep 1
    done
    echo " up!"
elif [[ "$1" == "-counters" ]]
then
    CLASS="org.infinispan.creson.benchmarks.count.Counter"
    ARGS="-ea org.infinispan.creson.benchmarks.Benchmark -class ${CLASS} -instances ${INSTANCES} -clients ${CLIENTS} -calls ${CALLS} -verbose"
    echo ">>>>> Counters"
    java -cp ${CLASSPATH} ${ARGS}
elif [[ "$1" == "-blobs" ]]
then    
    echo ">>>>> Blobs"
    CLASS="org.infinispan.creson.benchmarks.intensive.Blob"
    ARGS="-ea org.infinispan.creson.benchmarks.Benchmark -parameters 1000 -class ${CLASS} -instances ${INSTANCES} -clients ${CLIENTS} -calls ${CALLS} -verbose"
    java -cp ${CLASSPATH} ${ARGS}
elif [[ "$1" == "-queues" ]]
then
    echo ">>>>> Queue"
    for c in $(docker ps -q --filter="ancestor=${IMAGE_ID}");
    do
	docker cp ${TARGETDIR}/${NAME}-tests.jar ${c}:/tmp
    done
    sleep 3 # that jars are read
    java -cp ${CLASSPATH} org.infinispan.creson.benchmarks.queue.Benchmark -clients 10 -operations 100
elif [[ "$1" == "-delete" ]]
then
    echo ">>>>> Terminate"
    for c in $(docker ps -q --filter="ancestor=${IMAGE_ID}");
    do
	docker kill ${c}
    done
fi
