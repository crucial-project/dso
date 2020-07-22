#!/usr/bin/env bash

DIR=$(dirname "${BASH_SOURCE[0]}")
source ${DIR}/utils_benchmarks.sh

trap "pkill -KILL -P $$; exit 255" SIGINT SIGTERM

usage(){
    echo "usage: -[create|delete|counter|blob|barrier|sbarrier]"
    exit -1
}

if [ $# -ne 1 ]; then
    usage
fi

PARALLELISM=1
CALLS=1
THREADS=1

if [[ "$1" == "-create" ]]
then    
    k8s_rs_create ${TMPLDIR}/replicaset.yaml.tmpl 1 0 "LAUNCHED"
    k8s_rs_cp ${TMPLDIR}/replicaset.yaml.tmpl ${DIR}/../../../../target/dso-client-1.0.jar/ /tmp
    kubectl create -f ${TMPLDIR}/service.yaml
    # kubectl create -f ${TMPLDIR}/autoscaler.yaml
    # kubectl autoscale replicaset dso-server --cpu-percent=50 --min=3 --max=8 # FIXME
elif [[ "$1" == "-delete" ]]
then
    k8s_rs_delete ${TMPLDIR}/replicaset.yaml.tmpl
    kubectl delete -f ${TMPLDIR}/dso-server-service.yaml
    # kubectl delete  hpa dso-server
    # gsutil rm -r gs://$(config bucket)/* >&/dev/null # clean bucket
else
    template=""
    if [[ "$1" == "-counter" ]]
    then
	template=${TMPLDIR}/counter-test.yaml.tmpl
    elif [[ "$1" == "-blob" ]]
    then
	template=${TMPLDIR}/blob-test.yaml.tmpl
    elif [[ "$1" == "-barrier" ]]
    then
	template=${TMPLDIR}/barrier-test.yaml.tmpl
    elif [[ "$1" == "-sbarrier" ]]
    then
	template=${TMPLDIR}/sbarrier-test.yaml.tmpl
    else
	usage
    fi
    start_access ${template} ${PARALLELISM} ${CALLS} ${THREADS}
    # start_monitor
    wait_access ${template}
    # sleep 300
    # stop_monitor
    compute_average_throughput
fi
