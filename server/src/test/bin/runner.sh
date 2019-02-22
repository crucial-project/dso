#!/usr/bin/env bash

PROJDIR=`realpath $(dirname "${BASH_SOURCE[0]}")/../../../`
TARGETDIR="${PROJDIR}/target"

NAME="infinispan-creson-server"
MAINTAINER="0track"
TAG="latest"
IMAGE="${MAINTAINER}/${NAME}:${TAG}"
IMAGE_ID=$(docker images | grep ${NAME} | head -n 1 | awk '{print $3}')

# 1. Deploy
NSERVERS=3
echo ">>>>> Starting servers..."
for i in `seq 1 ${NSERVERS}`
do
    docker run --rm -p $((11221+i)):11222 ${IMAGE} 2>&1 > ${TARGETDIR}/.${i}.log &
done
up=-1
while [ ${up} != ${NSERVERS} ]; do
    up=$(cat ${TARGETDIR}/.*.log | grep "LAUNCHED" | wc -l)
    echo -n "."
    sleep 1
done
echo " up!"

# 2. Run
CLASSPATH=${PROJDIR}/target/*:${PROJDIR}/target/lib/*

echo ">>>>> Counters"
java -cp ${CLASSPATH} org.infinispan.creson.benchmarks.count.Benchmark -clients 10 -counters 100 -increments 1

echo ">>>>> Queue"
for c in $(docker ps -q --filter="ancestor=${IMAGE_ID}");
do
    docker cp ${TARGETDIR}/${NAME}-tests.jar ${c}:/tmp
done
sleep 3 # that jars are read
java -cp ${CLASSPATH} org.infinispan.creson.benchmarks.queue.Benchmark -clients 10 -operations 100

# 4. Terminate
echo ">>>>> Terminate"
for c in $(docker ps -q --filter="ancestor=${IMAGE_ID}");
do
    docker kill ${c}
done
