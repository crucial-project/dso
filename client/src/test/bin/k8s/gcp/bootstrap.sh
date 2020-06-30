#!/usr/bin/env bash

DIR=$(dirname "${BASH_SOURCE[0]}")

MACHINE_TYPE=n1-standard-4
NODE_NUMBER=3
GCP_PROJECT=$(gcloud config list --format='value(core.project)')
NETWORK="projects/${GCP_PROJECT}/global/networks/default"

create_cluster() {
    if [ $# -ne 3 ]; then
        echo "usage: create_cluster cluster_name cluster_zone sleep_time"
    fi

    local name=$1
    local zone=$2
    local seconds=$3

    sleep ${seconds}

    gcloud container clusters create ${name} \
	   --disk-size 30\
           --zone ${zone} \
           --num-nodes ${NODE_NUMBER} \
           --machine-type ${MACHINE_TYPE} \
	   --network ${NETWORK} \
           --preemptible \
           --scopes "cloud-platform,service-control,service-management,https://www.googleapis.com/auth/ndev.clouddns.readwrite"

}

fetch_credentials() {
    if [ $# -ne 2 ]; then
        echo "usage: fetch_credentials name zone"
    fi

    local name=$1
    local zone=$2
    
    local count=0
    local seconds=0

    while [ ${count} != 1 ]; do
        # get credential
        gcloud container clusters get-credentials ${name} \
            --zone ${zone}

        # create alias
        kubectl config set-context ${name} \
            --cluster=gke_${GCP_PROJECT}_${zone}_${name} \
            --user=gke_${GCP_PROJECT}_${zone}_${name}

        # check connection now
        count=$(kubectl --context=${name} get pods 2>&1 |
            grep "No resources found." |
            wc -l |
            xargs
        )

        # sleep backoff mechanism
        sleep ${seconds}
        seconds=$((seconds + 1))
    done
}

name="dso"
zone="us-east4"
sleep_time=$((i * 2)) # avoid "gcp db locked" error

create_cluster ${name} ${zone} ${sleep_time}
fetch_credentials ${name} ${zone}

# RBAC
kubectl create clusterrolebinding cluster-admin-binding --clusterrole cluster-admin --user 0track@gmail.com
kubectl apply -f ${DIR}/../templates/service.yaml
kubectl apply -f ${DIR}/../templates/role.yaml
