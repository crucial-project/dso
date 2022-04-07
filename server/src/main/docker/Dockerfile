FROM quay.io/infinispan/server:${version.infinispan}

ENV VERSION=${project.version}

LABEL version=$VERSION
LABEL description="DSO Server"

ADD target/dso-server-$VERSION.jar server/lib
ADD target/lib/dso-core-$VERSION.jar server/lib
ADD target/lib/guava-*jar server/lib
ADD target/lib/java-uuid-generator-*.jar server/lib
