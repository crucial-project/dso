#!/bin/bash

DIR=$(dirname "${BASH_SOURCE[0]}")
source ${DIR}/utils_functions.sh

start_access(){
    if [ $# -ne 4 ]; then
        echo "usage: start_access template.yaml parallelism #calls #threads"
        exit -1
    fi
    local template=$1
    local parallellsm=$2
    local calls=$3
    local threads=$4
    k8s_create_job ${template} ${parallellsm} ${calls} ${threads}
}

wait_access(){
    if [ $# -ne 1 ]; then
        echo "usage: wait_access template.yaml"
        exit -1
    fi
    local template=$1
    k8s_wait_job ${template}
    k8s_fetch_logs ${template} "^[0-9]*:[0-9]*$" 
    k8s_delete_job ${template}
}

# utils

compute_throughput(){
    nlogs=$(ls ${LOGDIR}/log-*  | wc -l)
    alog=$(find ${LOGDIR} -iname "log*" -type f | xargs wc -l | sort -rn | grep -v ' total$' | head -1 | awk '{print $2}')
    lines=$(cat ${alog} | wc -l | awk '{print $1}')
    beg=1
    end=$((lines))

    # avg per time unit
    # start=$(head -n 1 ${alog} | awk -F':' '{print $1}')
    # for i in $(seq ${beg} 1 ${end})
    # do
    # 	total=0
    # 	t=$(( ( $(sed -n -e ${i}p ${alog} | tail -n 1 | awk -F':' '{print $1}') - start ) / 1000))
    # 	n=$(head -n $i ${LOGDIR}/monitor | tail -n 1 | awk -F':' '{print $2}')
    # 	for log in $(ls ${LOGDIR}/log-*)
    # 	do    	    
    # 	    tput=$(sed -n -e ${i}p ${log} | awk -F':' '{print $2}')
    # 	    total=$((total+tput))
    # 	done
    # 	echo -e ${t}"\t"${total}"\t"${n}
    # done

    # avg over all the run
    for log in $(ls ${LOGDIR}/.log-*)
    do
	grep "Average time" ${log} | awk '{print $3}' 
    done  | awk 'BEGIN {SUM=0}; {SUM=SUM+$0}; END {printf "%.10f\n", SUM/NR}'
}

start_monitor(){
    rm -f ${LOGDIR}/monitor
    touch ${LOGDIR}/.lock
    while [ -f ${LOGDIR}/.lock ]; do
    	count=$(k8s_rs_count_pods ${TMPLDIR}/replicaset.yaml.tmpl)
    	date=$(($(date +%s%N)/1000000)) # ms
        echo "${date}:${count}" >> ${LOGDIR}/monitor
    	sleep 1
    done &	
}

stop_monitor(){
    rm -f ${LOGDIR}/.lock
}
