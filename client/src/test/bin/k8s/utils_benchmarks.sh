#!/bin/bash

DIR=$(dirname "${BASH_SOURCE[0]}")
source ${DIR}/utils_functions.sh

start_increment(){
    k8s_create_job ${TMPLDIR}/counter-test.yaml.tmpl
}

wait_increment(){
    k8s_wait_job ${TMPLDIR}/counter-test.yaml.tmpl
    k8s_fetch_logs ${TMPLDIR}/counter-test.yaml.tmpl "^[0-9]*:[0-9]*$" 
    k8s_delete_job ${TMPLDIR}/counter-test.yaml.tmpl    
}

start_access(){
    k8s_create_job ${TMPLDIR}/blob-test.yaml.tmpl
}

wait_access(){
    k8s_wait_job ${TMPLDIR}/blob-test.yaml.tmpl
    k8s_fetch_logs ${TMPLDIR}/blob-test.yaml.tmpl "^[0-9]*:[0-9]*$" 
    k8s_delete_job ${TMPLDIR}/blob-test.yaml.tmpl    
}

# utils

compute_throughput(){
    nlogs=$(ls ${LOGDIR}/log-*  | wc -l)
    alog="${LOGDIR}/monitor" # $(wc -l ${LOGDIR}/log-* | head -n $((nlogs-1)) | sort | tail -n 1 | awk '{print $2}') # longuest
    echo ${alog}

    lines=$(cat ${alog} | wc -l | awk '{print $1}')
    beg=1
    end=$((lines))

    start=$(head -n 1 ${alog} | awk -F':' '{print $1}')
    for i in $(seq ${beg} 1 ${end})
    do
    	total=0
	t=$(( ( $(sed -n -e ${i}p ${alog} | tail -n 1 | awk -F':' '{print $1}') - start ) / 1000))
	n=$(head -n $i ${LOGDIR}/monitor | tail -n 1 | awk -F':' '{print $2}')
    	for log in $(ls ${LOGDIR}/log-*)
    	do    	    
    	    tput=$(sed -n -e ${i}p ${log} | awk -F':' '{print $2}')
	    total=$((total+tput))
    	done
    	echo ${t}" "${total}" "${n}
    done        
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
