#!/usr/bin/env bash

DIR=$(dirname "${BASH_SOURCE[0]}")

region=eu-west-3

usage(){
    echo "usage: -[create|delete]"
    exit -1
}

if [ $# -ne 1 ];
then
    usage
fi

create_cluster() {
    eksctl create cluster \
	   --region ${region} \
	   --name aws-dso \
	   --version 1.12 \
	   --nodegroup-name standard-workers \
	   --node-type t3.medium \
	   --nodes 1 \
	   --nodes-min 1 \
	   --nodes-max 1 \
	   --node-ami auto
    kubectl create clusterrolebinding cluster-admin-binding --clusterrole cluster-admin --user 0track@gmail.com
    kubectl apply -f ${DIR}/../templates/dso-server-service.yaml
    kubectl apply -f ${DIR}/../templates/role.yaml    
}

delete_cluster() {
    eksctl delete cluster --region=eu-west-3 --name=aws-dso
}


if [[ "$1" == "-create" ]]
then
    create_cluster
elif [[ "$1" == "-delete" ]]
then
     delete_cluster
else
    usage
fi

# RBAC
