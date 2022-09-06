# This Dockerfile should be built with the root of the repo as context
FROM 760097843905.dkr.ecr.eu-west-1.amazonaws.com/wellcome/sbt_wrapper:1.7.1 as sbt
LABEL maintainer = "Wellcome Collection <digital@wellcomecollection.org>"

WORKDIR /app
COPY ./build.sbt ./
COPY ./project ./project
# make use of layers, don't go fetching all the dependencies for every code change
RUN /run_sbt.sh "project aggregator" "update"
COPY ./common ./common
COPY ./aggregator ./aggregator
RUN /run_sbt.sh "project aggregator" "assembly"
ENTRYPOINT ["/run_sbt.sh", "project aggregator"]

FROM public.ecr.aws/docker/library/eclipse-temurin:11-jre-alpine as runtime

RUN apk add --no-cache bash

COPY --from=sbt /app/target/aggregator-lambda.jar /opt/docker/
ENTRYPOINT ["java", "-jar", "/opt/docker/aggregator-lambda.jar"]