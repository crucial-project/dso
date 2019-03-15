#!/usr/bin/env bash

DIR=$(dirname "${BASH_SOURCE[0]}")
source ${DIR}/utils_benchmarks.sh

trap "pkill -KILL -P $$; exit 255" SIGINT SIGTERM

if [ $# -ne 1 ]; then
    echo "usage: [-create, -delete, -counters]"
    exit -1
fi

if [[ "$1" == "-create" ]]
then    
    # gsutil rm -r gs://$(config bucket)/* >&/dev/null # clean bucket
    k8s_rs_create ${TMPLDIR}/replicaset.yaml.tmpl
    # kubectl create -f ${TMPLDIR}/autoscaler.yaml
    # kubectl autoscale replicaset infinispan-creson-server --cpu-percent=50 --min=3 --max=8 # FIXME
elif [[ "$1" == "-delete" ]]
then
    k8s_rs_delete ${TMPLDIR}/replicaset.yaml.tmpl
    kubectl delete  hpa infinispan-creson-server
    # gsutil rm -r gs://$(config bucket)/* >&/dev/null # clean bucket
elif [[ "$1" == "-counters" ]]
then
    start_increment
    start_monitor
    wait_increment
    # sleep 300
    stop_monitor
    compute_throughput
elif [[ "$1" == "-blobs" ]]
then
    start_access
    start_monitor
    wait_access
    # sleep 300
    stop_monitor
    compute_throughput
fi
