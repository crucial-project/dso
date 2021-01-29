#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

start(){
  ${DIR}/local/test.sh -create
}

stop(){
  ${DIR}/local/test.sh -delete
}

success(){
  echo "ALL PASSED"
  stop
  exit 0
}

fail(){
  echo "FAILURE"
  stop
  exit 1
}

start

echo "Java tests"
${DIR}/local/test.sh -counters
${DIR}/local/test.sh -longs
${DIR}/local/test.sh -blobs
${DIR}/local/test.sh -barrier
${DIR}/local/test.sh -sbarrier
${DIR}/local/test.sh -countdownlatch

echo "Shell tests"
source ${DIR}/aliases.sh
THREADS=4
[[ ! -z $(counter -n test reset; counter -n test increment -1 1 | grep "1") ]] || fail
[[ ! -z $(counter -n test increment -1 1 | grep "2" ) ]] || fail
[[ ! -z $(for i in $(seq 1 1 ${THREADS}); do counter -n test increment -1 1 & done | tail -n 1 | grep $((THREADS+2))) ]] || fail
[[ ! -z $(for i in $(seq 1 1 ${THREADS}); do barrier -n test -p ${THREADS} await & done | grep ${THREADS}) ]] || fail
[[ ! -z $(for k in $(seq 1 1 ${THREADS}); do b=$((1+10*($k-1))); e=$((10*$k)); treemap -n x putAll $(for i in $(seq $b 1 $e); do echo -n "-1 $i=-$i "; done;)& done; wait; treemap -n x lastKey | grep 9) ]] || fail
[[ ! -z $(for k in $(seq 1 1 ${THREADS}); do b=$((1+10*($k-1))); e=$((10*$k)); treemap -n y putAll $(for i in $(seq $b 1 $e); do echo -n "-1 $i=-$i "; done;)& done; wait; treemap -n y reverse -1; treemap -n y lastKey | grep "1") ]] || fail

success
