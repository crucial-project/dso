#!/usr/bin/env bash

DIR=$(dirname "${BASH_SOURCE[0]}")
source ${DIR}/utils_benchmarks.sh

trap "pkill -KILL -P $$; exit 255" SIGINT SIGTERM

# 1 - set-up
# k8s_rs_create ${TMPLDIR}/replicaset.yaml.tmpl 3 6 "LAUNCHED"
# k8s_rs_cp ${TMPLDIR}/replicaset.yaml.tmpl ${DIR}/../../../../target/dso-client-1.0.jar/ /tmp

# 2 - barriers
CALLS=1000
PARALLELISM=(2 50 100 200)
THREADS=1
OUT=${LOGDIR}/barriers.dat
OBJECT=("barrier" "sbarrier")
OBJECT=("sbarrier")
echo -e "\t\tN\tF" > ${OUT}
for p in ${PARALLELISM[@]};
do
    line=${p}"\t" 
    for b in ${OBJECT[@]};
    do
	template=${TMPLDIR}/${b}-test.yaml.tmpl
	start_access ${template} ${p} ${CALLS} ${THREADS}
	wait_access ${template}
	result=$(cat ${LOGDIR}/.log-* | grep -i Average | awk -F':' '{print $2}' | awk -F'[' '{print $1}' | awk '{ i++; s += $1 } END { print s/i }')
	line=${line}"\t"${result}
    done
    echo -e ${line} >> ${OUT}
done

# 3 - Clean-up
k8s_rs_delete ${TMPLDIR}/replicaset.yaml.tmpl
