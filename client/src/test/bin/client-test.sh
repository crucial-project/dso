#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CLASSPATH="${DIR}/*:${DIR}/lib/*"
ARGS="-ea org.infinispan.creson.Benchmark -class ${CLASS} -instances ${INSTANCES} -clients ${CLIENTS} -calls ${CALLS} -server ${PROXY} -verbose ${EXTRA}"

echo "java -cp ${CLASSPATH} ${ARGS}"
java -cp ${CLASSPATH} ${ARGS}
