# This Dockerfile should be built with the root of the repo as context
FROM 760097843905.dkr.ecr.eu-west-1.amazonaws.com/wellcome/sbt_wrapper:1.7.1 as sbt
LABEL maintainer = "Wellcome Collection <digital@wellcomecollection.org>"

WORKDIR /app

COPY ./build.sbt ./
COPY ./project ./project
COPY ./aggregator ./aggregator

RUN /run_sbt.sh "project aggregator" "stage"

ENTRYPOINT ["/run_sbt.sh", "project aggregator"]

FROM public.ecr.aws/docker/library/eclipse-temurin:11-jre-alpine

RUN apk add --no-cache bash

COPY --from=sbt /app/aggregator/target/universal/stage /opt/docker

ENTRYPOINT ["/opt/docker/bin/aggregator"]
