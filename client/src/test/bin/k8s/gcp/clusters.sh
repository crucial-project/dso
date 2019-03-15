#!/usr/bin/env bash

DIR=$(dirname "${BASH_SOURCE[0]}")

ALL_CLUSTERS=($(cat ${DIR}/clusters.txt))

to_region() {
    local cluster=$1
    local region=$(echo ${cluster} | sed 's/\([a-z]*-[a-z]*[0-9]\).*/\1/')
    echo ${region}
}

find_location() {
    region=$(to_region $1)
    case $region in
    asia-east1)
        location="TW" # taiwan
        ;;
    asia-northeast1)
        location="JP" # japan
        ;;
    asia-south1)
        location="IN" # india
        ;;
    asia-southeast1)
        location="SG" # singapore
        ;;
    australia-southeast1)
        location="AU" # australia
        ;;
    europe-north1)
        location="FI" # finland
        ;;
    europe-west1)
        location="BE" # belgium
        ;;
    europe-west2)
        location="GB" # united kingdom
        ;;
    europe-west3)
        location="DE" # germany
        ;;
    europe-west4)
        location="NL" # netherlands
        ;;
    northamerica-northeast1)
        location="QC" # quebec, canada (should be CA, but there's also california below)
        ;;
    southamerica-east1)
        location="BR" # brazil
        ;;
    us-central1)
        location="IA" # iowa
        ;;
    us-west1)
        location="OR" # oregon
        ;;
    us-west2)
        location="CA" # california
        ;;
    us-east1)
        location="SC" # south carolina
        ;;
    us-east4)
        location="VA" # northern virginia
        ;;
    *)
        exit -1
        ;;
    esac
    echo $location
}

find_region() {
    case $1 in
    TW)
        region="asia-east1" # taiwan
        ;;
    JP)
        region="asia-northeast1" # japan
        ;;
    IN)
        region="asia-south1" # india
        ;;
    SG)
        region="asia-southeast1" # singapore
        ;;
    AU)
        region="australia-southeast1" # australia
        ;;
    FI)
        region="europe-north1" # finland
        ;;
    BE)
        region="europe-west1" # belgium
        ;;
    GB)
        region="europe-west2" # united kingdom
        ;;
    DE)
        region="europe-west3" # germany
        ;;
    NL)
        region="europe-west4" # netherlands
        ;;
    QC)
        region="northamerica-northeast1" # quebec, canada (should be CA, but there's also california below)
        ;;
    BR)
        region="southamerica-east1" # brazil
        ;;
    IA)
        region="us-central1" # iowa
        ;;
    OR)
        region="us-west1" # oregon
        ;;
    CA)
        region="us-west2" # california
        ;;
    SC)
        region="us-east1" # south carolina
        ;;
    VA)
        region="us-east4" # northern virginia
        ;;
    *)
        exit -1
        ;;
    esac
    echo $region
}
