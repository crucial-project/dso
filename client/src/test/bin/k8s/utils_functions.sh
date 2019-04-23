#!/usr/bin/env bash

DIR=$(dirname "${BASH_SOURCE[0]}")

TMPLDIR="${DIR}/templates"
CONFIG_FILE="${DIR}/exp.config"
TMPDIR="/tmp"
LOGDIR="${TMPDIR}/log"

config() {
    if [ $# -ne 1 ]; then
        echo "usage: config key"
        exit -1
    fi
    local key=$1
    cat ${CONFIG_FILE} | grep -E "^${key}=" | cut -d= -f2
}

info() {
    local message=$1
    echo >& 2 "["$(date +%s:%N)"] ${message}"
}

log() {
    if [[ $(config verbose) -eq 1 ]]
    then
	local message=$1
	echo >& 2 "["$(date +%s:%N)"] ${message}"
    fi
}

# pod
k8s_create_pod() {
    if [ $# -ne 2 ] && [ $# -ne 3 ]; then
        echo "usage: k8s_create_pod template.yaml context [id]"
        exit -1
    fi
    local template=$1
    local context=$2
    local id=${3:-0} # default id is 0
    local file=${template}-${id}
    local pull=$(config pull-image)
    local image=$(config image)

    # create final template
    cat ${template} |
        sed s,%ID%,${id},g |
	sed s,%IMAGE%,${image},g |
	sed s,%PULL_IMAGE%,${pull},g |
        sed s,%CONTEXT%,${context},g |
	sed s,%CLOUD%,$(config cloud),g |
	sed s,%BUCKET%,$(config bucket),g |
	sed s,%BUCKET_KEY%,$(config bucket_key),g |
	sed s,%BUCKET_SECRET%,$(config bucket_secret),g \
            >${file}

    # create pod
    info $(kubectl --context="${context}" create -f ${file})

    local pod_name=$(k8s_name ${file})
    local pod_status="NotFound"

    # loop until pod is running
    while [ "${pod_status}" != "Running" ]; do
        sleep 1
        pod_status=$(k8s_pod_status ${context} ${pod_name})
	info "pod ${pod_name} status ${pod_status}"
    done
    info "pod ${pod_name} running at ${context}"
}

k8s_delete_pod() {
    if [ $# -ne 2 ] && [ $# -ne 3 ]; then
        echo "usage: k8s_delete_pod template.yaml context [id]"
        exit -1
    fi
    local template=$1
    local context=$2
    local id=${3:-0} # default id is 0
    local file=${template}-${id}
    local pod_name=$(k8s_name ${file})
    local pod_status="Running"

    # loop until pod is down
    while [ "${pod_status}" != "NotFound" ]; do
        kubectl --context="${context}" delete pod ${pod_name} \
            --grace-period=0 --force \
            >&/dev/null
        sleep 1
        pod_status=$(k8s_pod_status ${context} ${pod_name})
    done
    info "pod ${pod_name} deleted at ${context}"
}

k8s_pod_cp(){
    if [ $# -ne 3 ]; then
        echo "usage: k8s_pod_cp src pod dst"
        exit -1
    fi
    local src=$1
    local pod=$2
    local dst=$2
    kubectl cp $1 $2:$3
   
}

k8s_create_all_pods(){
    local context=$(config context)
    local service=$(cat ${CONFIG_FILE} | grep -ioh "/.*:" | sed s,[/:],,g)
    local proxy=""
    for i in $(seq 1 $(config nodes))
    do	
	k8s_create_pod ${TMPLDIR}/${service}.yaml.tmpl ${context} ${i}
    done    
}

k8s_delete_all_pods() {
    local context=$(config context)
    kubectl --context=${context} delete pods --all \
	    --grace-period=0 --force \
	    2>/dev/null &

    # wait for all pods to terminate
    while [ "${empty}" != "1" ]; do
        empty=$(kubectl --context=${context} get pods 2>&1 |
		    grep "No resources found" |
		    wc -l |
		    xargs echo
	     )
    done
}

k8s_name() {
    if [ $# -ne 1 ]; then
        echo "usage: k8s_name file"
        exit -1
    fi
    local file=$1
    grep -E "^  name: " ${file} | awk '{ print $2 }'
}

k8s_pod_status() {
    if [ $# -ne 2 ]; then
        echo "usage: k8s_pod_status context pod_name"
        exit -1
    fi
    local context=$1
    local pod_name=$2

    kubectl --context="${context}" get pod ${pod_name} 2>&1 |
	 grep -oE "(Running|Completed|Terminating|NotFound|ContainerCreating)"
}

# replicaSet
k8s_rs_create() {
    if [ $# -ne 4 ]; then
        echo "usage: k8s_rs_create template.yaml #replicas cpu pattern"
        exit -1
    fi
    local template=$1
    local replicas=$2
    local cpu=$3
    local pattern=$4

    local context=$(config context)
    local file=${template}-0
    local pull=$(config pull-image)
    local image=$(config image)
    local rs_name=$(k8s_name ${template})

    # create template
    cat ${template} |
	sed s,%IMAGE%,${image},g |
	sed s,%PULL_IMAGE%,${pull},g |
        sed s,%REPLICAS%,${replicas},g |
	sed s,%CPU%,${cpu},g \
            >${file}

    info $(kubectl --context="${context}" create -f ${file})
    
    # wait until running
    started=0
    while [ "${started}" != "1" ]; do
        sleep 1
	starting=$(kubectl --context="${context}" get pods 2>&1 |
		       grep $(k8s_name ${file}) |
		       grep -v "Running" |
		       wc -l)
	if [ "${starting}" == "0" ]
	then
	    started=1
	fi
    done

    # wait pattern in log
    for pod in $(kubectl --context=${context} get pods | grep ${rs_name} | awk '{print $1}');
    do
	while [ "$(kubectl --context="${context}" logs ${pod} | grep -e ${pattern})" == "" ];
	do
	    sleep 1
	done
    done
}

k8s_rs_delete() {
    if [ $# -ne 1 ]; then
        echo "usage: k8s_rs_delete template.yaml"
        exit -1
    fi
    local template=$1
    local context=$(config context)
    local file=${template}-0
    local image=$(k8s_name ${file})   

    info $(kubectl --context="${context}" delete -f ${file})
    
    # wait until 
    running=1
    while [ "${running}" != "0" ]; do
        sleep 1
	running=$(kubectl --context="${context}" get pods 2>&1 |
		      grep ${image} |
		      wc -l)
    done
}

k8s_rs_count_pods() {
    if [ $# -ne 1 ]; then
        echo "usage: k8s_rs_count_pods template.yaml"
        exit -1
    fi
    local template=$1
    local context=$(config context)
    local file=${template}-0
    local name=$(k8s_name ${file})
    kubectl --context="${context}" get pod -l app=${name} | grep "Running" | wc -l
}

k8s_rs_cp(){
    if [ $# -ne 3 ]; then
        echo "usage: k8s_rs_cp template.yaml file dst"
        exit -1
    fi
    
    local template=$1
    local file=$2
    local dst=$3
    local context=$(config context)
    local rs_name=$(k8s_name ${template})
    
    for pod in $(kubectl --context=${context} get pods | grep ${rs_name} | awk '{print $1}');
    do
	info "copying ${file} to ${pod}:${dst} at ${context}"
	k8s_pod_cp ${file} ${pod} ${dst}
    done
}

k8s_clean_all(){
    k8s_delete_all_pods $(config context) >&/dev/null
    gsutil rm -r gs://$(config bucket)/* >&/dev/null
    info "cleaned"
}

# job
k8s_create_job() {
    if [ $# -ne 4 ]; then
        echo "usage: k8s_create_job template.yaml parallelism #calls #threads"
        exit -1
    fi
    local template=$1
    local parallelism=$2
    local calls=$3
    local threads=$4
    local context=$(config context)
    local file=${template}-0
    local pull=$(config pull-image)
    local image=$(config image-client)
    local proxy=$(get_proxy):11222

    # create final template
    cat ${template} |
	sed s,%IMAGE%,${image},g |
	sed s,%PULL_IMAGE%,${pull},g |
	sed s,%PARALLELISM%,${parallelism},g |
	sed s,%CALLS%,${calls},g |
	sed s,%THREADS%,${threads},g |
	sed s,%PROXY%,${proxy},g \
            >${file}

    # create job
    info $(kubectl --context="${context}" create -f ${file})

    # loop until job is running (or completed)
    local parallelism=$(cat ${file} | grep parallelism | awk -F':' '{print $2}' | sed 's/^[ \t]*//;s/[ \t]*$//')
    local running=0
    local job_name=$(k8s_name ${file})
    while [ "${parallelism}" != "${running}" ]; do
        sleep 1
	running=$(kubectl --context="${context}" get pods 2>&1 |
		      grep ${job_name} |
		      grep -oE "(Running|Completed)" |
		      wc -l)
    	info "job ${job_name} (${running}/${parallelism})"
    done
    info "job ${job_name} running at ${context}"
}

k8s_delete_job() {
    if [ $# -ne 1 ]; then
        echo "usage: k8s_create_job template.yaml"
        exit -1
    fi
    local template=$1
    local context=$(config context)
    local file=${template}-0
    local job_name=$(k8s_name ${file})
    local image=$(config image)

    kubectl --context="${context}" delete -f ${file} >& /dev/null

    completed=1
    while [ "${completed}" != "0" ]; do
        sleep 1
	completed=$(kubectl --context="${context}" get pods 2>&1 |
		      grep ${image} |
		      wc -l)
    done
    
    info "job ${job_name} deleted"
}

k8s_wait_job() {
    if [ $# -ne 1 ]; then
        echo "usage: k8s_wait_job template.yaml"
        exit -1
    fi
    local template=$1
    local context=$(config context)
    local file=${template}-0

    # loop until job is completed
    local parallelism=$(cat ${file} | grep parallelism | awk -F':' '{print $2}' | sed 's/^[ \t]*//;s/[ \t]*$//')
    local completed=0
    local job_name=$(k8s_name ${file})
    while [ "${parallelism}" != "${completed}" ]; do
        sleep 1
	completed=$(kubectl --context="${context}" get job 2>&1 |
		      grep ${job_name} |
		      awk '{print $3}')
    	info "job ${job_name} (${completed}/${parallelism})"
    done
    info "job ${job_name} completed at ${context}"      
}

k8s_fetch_logs() {
    if [ $# -ne 2 ]; then
        echo "usage: k8s_fetch_log template.yaml pattern"
        exit -1
    fi
    
    local context=$(config context)
    local template=$1
    local pattern=$2
    local file=${template}-0
    local job_name=$(k8s_name ${file})

    mkdir -p ${LOGDIR}
    rm -f ${LOGDIR}/.log-* ${LOGDIR}/log-*

    children=()
    for pod in $(kubectl --context=${context} get pods | grep ${job_name} | awk '{print $1}');
    do
	info "job ${job_name} fetching from ${pod} at ${context}"
	kubectl --context="${context}" logs ${pod} > ${LOGDIR}/.log-${pod} && 
	    cat ${LOGDIR}/.log-${pod} | grep -e ${pattern} > ${LOGDIR}/log-${pod} &
	children+=($!)
    done
    for pid in ${children[@]}; do
	wait $pid
    done
    info "job ${job_name} logs fetched at ${context}"
}

# service
k8s_get_service(){
    local context=$(config context)
    local service=$(echo $(config image) | grep -ioh "/.*:" | sed s,[/:],,g)
    local proxy=$(kubectl --context="${context}" get svc ${service} -o yaml | grep ip | awk '{print $3}')
    while [ "${proxy}" == "" ]; do
	kubectl --context="${context}" apply -f ${TMPLDIR}/${service}-service.yaml.tmpl
	proxy=$(kubectl --context="${context}" get svc ${service} -o yaml | grep ip | awk '{print $3}')
        sleep 1
    done
    info "service ${service} @ ${proxy}"
    echo ${proxy}
}

get_proxy(){
    if [ "$(config cloud)" != "local" ]
    then
    	proxy=$(k8s_get_service)
    else
	proxy="localhost:8080"
    fi
    echo ${proxy}
}
