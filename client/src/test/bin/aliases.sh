#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJDIR=${DIR}/../../..

source ${DIR}/utils.sh

export SERVER=$(config crucial.server)
export CLASSPATH=${PROJDIR}/target/*:${PROJDIR}/target/lib/*

counter(){
    java org.crucial.dso.client.Interpreter -s ${SERVER} counter $@
}

list(){
    java org.crucial.dso.client.Interpreter -s ${SERVER} list $@
}

map(){
    java org.crucial.dso.client.Interpreter -s ${SERVER} map $@
}

treemap(){
    java org.crucial.dso.client.Interpreter -s ${SERVER} treemap $@
}

barrier(){
    java org.crucial.dso.client.Interpreter -s ${SERVER} barrier $@
}

pipe(){
    java org.crucial.dso.client.Interpreter -s ${SERVER} pipe $@
}

export -f counter
export -f list
export -f map
export -f treemap
export -f barrier
export -f pipe
