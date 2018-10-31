#!/usr/bin/env bash

PROJDIR=`realpath $(dirname "${BASH_SOURCE[0]}")/../../../`
TARGETDIR="${PROJDIR}/target"
ARTIFACT="infinispan-crucial-server"
VERSION="9.0.3.Final"

# 1. Build
mvn -f ${PROJDIR} package -DskipTests
cat ${PROJDIR}/src/main/docker/Dockerfile \
    | sed s,RUN\ apt-get.*,,g \
    | sed s,ADD\ https.*,,g \
    | sed s,RUN\ git\ clone.*,,g \
    | sed s,RUN\ mvn.*,COPY\ ${ARTIFACT}-${VERSION}.tar.gz\ /app,g \
    | sed s,RUN\ tar.*,RUN\ tar\ zxvf\ /app/${ARTIFACT}-${VERSION}.tar.gz\ -C\ /app,g \
    | sed s,Xmx2048m,Xmx128m,g > ${TARGETDIR}/Dockerfile.debug
docker build -f ${TARGETDIR}/Dockerfile.debug -t ${ARTIFACT}:debug ${TARGETDIR}

# 2. Deploy
NSERVERS=3
echo ">>>>> Starting servers..."
for i in `seq 1 ${NSERVERS}`
do
    docker run -p $((11221+i)):11222 ${ARTIFACT}:debug 2>&1 > ${TARGETDIR}/.${i}.log &
done
up=-1
while [ ${up} != ${NSERVERS} ]; do
    up=$(cat ${TARGETDIR}/.*.log | grep "LAUNCHED" | wc -l)
    echo -n "."
    sleep 1
done
echo " up!"

# 3. Run
CLASSPATH=${PROJDIR}/target/*:${PROJDIR}/target/lib/*

echo ">>>>> Counters"
java -cp ${CLASSPATH} org.infinispan.creson.benchmarks.count.Benchmark -clients 10 -counters 100 -increments 10000

echo ">>>>> Queue"
for c in `docker ps -a -q --filter="ancestor=$(docker images | grep ${ARTIFACT} | awk '{print $3}')"`;
do
    docker cp ${TARGETDIR}/${ARTIFACT}-tests.jar ${c}:/app/userlibs
done
sleep 3 # that jars are read
java -cp ${CLASSPATH} org.infinispan.creson.benchmarks.queue.Benchmark -clients 10 -operations 100

# 4. Terminate
echo ">>>>> Terminate"
for c in `docker ps -a -q --filter="ancestor=$(docker images | grep ${ARTIFACT} | awk '{print $3}')"`;
do
    docker kill ${c}
    docker rm ${c}
done
