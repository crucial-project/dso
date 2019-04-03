#!/usr/bin/env bash

DIR=$(dirname "${BASH_SOURCE[0]}")
source ${DIR}/utils_benchmarks.sh

trap "pkill -KILL -P $$; exit 255" SIGINT SIGTERM

usage(){
    echo "usage: -[create|delete|counters|blobs]"
    exit -1
}

if [ $# -ne 1 ]; then
    usage
fi

if [[ "$1" == "-create" ]]
then    
    k8s_rs_create ${TMPLDIR}/replicaset.yaml.tmpl
    k8s_rs_cp ${TMPLDIR}/replicaset.yaml.tmpl ${DIR}/../../../../target/infinispan-creson-client-9.4.1.Final.jar/ /tmp
    # kubectl create -f ${TMPLDIR}/autoscaler.yaml
    # kubectl autoscale replicaset infinispan-creson-server --cpu-percent=50 --min=3 --max=8 # FIXME
elif [[ "$1" == "-delete" ]]
then
    k8s_rs_delete ${TMPLDIR}/replicaset.yaml.tmpl
    # kubectl delete  hpa infinispan-creson-server
    # gsutil rm -r gs://$(config bucket)/* >&/dev/null # clean bucket
else
    template=""
    if [[ "$1" == "-counters" ]]
    then
	template=${TMPLDIR}/counter-test.yaml.tmpl
    elif [[ "$1" == "-blobs" ]]
    then
	template=${TMPLDIR}/blobs-test.yaml.tmpl
    elif [[ "$1" == "-barrier" ]]
    then
	template=${TMPLDIR}/barrier-test.yaml.tmpl
    else
	usage
    fi
    start_access ${template}
    # start_monitor
    wait_access ${template}
    # sleep 300
    # stop_monitor
    # compute_throughput
fi


