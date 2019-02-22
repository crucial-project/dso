#!/usr/bin/env bash

DIR=$(dirname "${BASH_SOURCE[0]}")
source ${DIR}/utils_benchmarks.sh

N_COUNTERS=1000

N_CLIENTS=100
N_OPS=10000
N_PAR=1

random_transfer(){    
    from=$((RANDOM % N_ACCOUNTS))
    to=$((RANDOM % N_ACCOUNTS))
    amount=$((RANDOM))
    transfer ${from} ${to} ${amount}
}

if [[ "$1" == "-create" ]]
then    
    gsutil rm -r gs://$(config bucket)/* >&/dev/null # clean bucket
    k8s_rs_create ${TMPLDIR}/replicaset.yaml.tmpl
    kubectl autoscale replicaset infinispan-creson-server --cpu-percent=100 --min=1 --max=10 # FIXME    
elif [[ "$1" == "-delete" ]]
then
    k8s_rs_delete ${TMPLDIR}/replicaset.yaml.tmpl
    kubectl delete  horizontalpodautoscalers.autoscaling infinispan-creson-server
    gsutil rm -r gs://$(config bucket)/* >&/dev/null # clean bucket
elif [[ "$1" == "-counters" ]]
then    
    for i in $(seq 1 ${N_PAR});
    do
    	increment ${N_CLIENTS} ${N_COUNTERS} ${N_OPS} > ${DIR}/log-${i} &
    done
    over=0
    count=0
    while [ "${over}" != "${N_PAR}" ]; do
    	over=$(grep closed ${DIR}/log-* | wc -l)
    	count=$(k8s_rs_count_pods ${TMPLDIR}/replicaset.yaml.tmpl)
    	date=$(($(date +%s%N)/1000000)) # ms
        echo "${date}:${count}"
	sleep 10
    done
    wait
# elif [[ "$1" == "-clear" ]]
# then    
#     clear_accounts
# elif [[ "$1" == "-run" ]]
# then
#     for i in $(seq 1 ${N_OPS});
#     do
# 	random_transfer
#     done
# elif [[ "$1" == "-concurrent-run" ]]
# then
#     for i in $(seq 1 $((N_OPS/N_PAR)));
#     do
# 	for j in $(seq 1 ${N_PAR});
# 	do
# 	    random_transfer &
# 	done
# 	wait
#     done
# elif [[ "$1" == "-check" ]]
# then
#     total=0
#     for i in $(seq 0 $((N_ACCOUNTS-1)));
#     do
# 	balance=$(get_balance $i)
# 	total=$((total+balance))
#     done	
#     info "Total=${total}"    
fi
