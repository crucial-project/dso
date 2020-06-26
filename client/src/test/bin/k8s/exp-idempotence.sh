#!/usr/bin/env bash

DIR=$(dirname "${BASH_SOURCE[0]}")
source ${DIR}/utils_benchmarks.sh

trap "pkill -KILL -P $$; exit 255" SIGINT SIGTERM

replicas=1
cpus=6

clients=64
template=${TMPLDIR}/idempotence-test.yaml.tmpl
calls=50000
threads=1

rm -f ${LOGDIR}/.log* ${LOGDIR}/.crash* ${LOGDIR}/log*
OUT=${LOGDIR}/idempotence.dat
echo -n "" > ${OUT}

# # deploy rs
k8s_rs_create ${TMPLDIR}/replicaset.yaml.tmpl ${replicas} ${cpus} "LAUNCHED"
k8s_rs_cp ${TMPLDIR}/replicaset.yaml.tmpl ${DIR}/../../../../target/dso-client-1.0.jar/ /tmp

# # # run w. fault injection
k8s_create_pods ${template} ${clients} ${calls} ${threads}
start_monitor
# sleep 20
# k8s_crash_pods ${template} 32
k8s_wait_pods ${template} ${clients}
k8s_fetch_log_pods ${template} "^[0-9]*:[0-9]*$"
stop_monitor
k8s_delete_pods ${template} ${clients}

# delete rs
# k8s_rs_delete ${TMPLDIR}/replicaset.yaml.tmpl

# measure
compute_realtime_throughput >> ${OUT}
