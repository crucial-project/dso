#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

num-from-string() { # FIXME
    local out i a
    for ((i=0;i<${#1};++i)); do
        printf -v a "%d\n" "'${1:i:1}"
        out+=$((a%10))
    done
    echo "$out"
}

CLASSPATH="${DIR}/*:${DIR}/lib/*"
ARGS="-ea org.crucial.dso.Benchmark "

if [ "${ID}" != "" ]
then
    ARGS=${ARGS}" -id ${ID}"
fi

ARGS=${ARGS}" -class ${CLASS} -instances ${INSTANCES} ${PERSIST} "
ARGS=${ARGS}" -threads ${THREADS} -calls ${CALLS} -parallelism ${PARALLELISM} -server ${PROXY} -verbose -parameters ${PARAMETERS}"

echo "java -Xmx128M -cp ${CLASSPATH} ${ARGS}"
java -cp ${CLASSPATH} ${ARGS}
