#!/usr/bin/env bash

DIR=$(dirname "${BASH_SOURCE[0]}")
source ${DIR}/utils_benchmarks.sh

trap "pkill -KILL -P $$; exit 255" SIGINT SIGTERM

template=${TMPLDIR}/counter-test.yaml.tmpl
THREADS=1
CALLS=10000
REPLICAS=(3 6 9 12)
OUT=${LOGDIR}/counters.dat
echo -n "" > ${OUT}

for r in ${REPLICAS[@]};
do
    line=${r}

    k8s_rs_create ${TMPLDIR}/replicaset.yaml.tmpl ${r} 2 "LAUNCHED"
    k8s_rs_cp ${TMPLDIR}/replicaset.yaml.tmpl ${DIR}/../../../../target/dso-client-1.0.jar/ /tmp

    prev=0; cur=1; diff=1; p=32    
    while [ ${diff} -gt 0 ] && [ ${p} -le 400 ];
    do
    	start_access ${template} ${p} ${CALLS} ${THREADS}
    	wait_access ${template}

	prev=${cur}
    	cur=$(cat ${LOGDIR}/.log-* | grep -i Average | awk -F':' '{print $2}' | awk -F'=' '{print $2}' | sed s,\],,g | awk '{ s += $1 } END { print int(s) }')
	diff=$(echo $(($((cur-prev))/1000))) # stop condition 
	info ${r}" "${p}" "${cur}" ("${diff}")"

    	p=$(echo "(3*${p})/2" | bc)
    done
    
    k8s_rs_delete ${TMPLDIR}/replicaset.yaml.tmpl
    
    line=${line}"\t"${prev}
    echo -e ${line} >> ${OUT}
done
