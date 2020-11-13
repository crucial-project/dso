#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
TMP_DIR=/tmp/$(whoami)
CODE_DIR=${TMP_DIR}/code
CONFIG_FILE=${DIR}/config.properties

if [ ! -f ${CONFIG_FILE} ];
then
    echo "${CONFIG_FILE} is missing."
    exit 0
fi

config() {
    if [ $# -ne 1 ]; then
        echo "usage: config key"
        exit -1
    fi
    local key=$1
    cat ${CONFIG_FILE} | grep -E "^${key}=" | cut -d= -f2
}
