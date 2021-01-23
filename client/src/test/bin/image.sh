#!/usr/bin/env bash

if [ -z "$1" ]; then
    TAG=latest
else
    TAG="$1"
fi

DOCKER_USER=$(docker info |
    grep Username |
    awk '{print $2}')
if [ -z "${DOCKER_USER}" ]; then
    DOCKER_USER=0track
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
IMAGE=${DOCKER_USER}/dso-client-tests:${TAG}
DOCKERFILE=${DIR}/../docker/Dockerfile

pushd ${DIR}/../../../

# package
mvn clean package -DskipTests

# build image
docker build \
    --no-cache \
    -t "${IMAGE}" -f "${DOCKERFILE}" .

# push image
docker push "${IMAGE}"

popd
