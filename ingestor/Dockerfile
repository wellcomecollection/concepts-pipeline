# This Dockerfile should be built with the root of the repo as context
FROM 760097843905.dkr.ecr.eu-west-1.amazonaws.com/wellcome/sbt_wrapper:1.7.1 as sbt
LABEL maintainer = "Wellcome Collection <digital@wellcomecollection.org>"

WORKDIR /app
COPY ./build.sbt ./
COPY ./project ./project
COPY ./common ./common
COPY ./ingestor ./ingestor

RUN /run_sbt.sh "project ingestor" "assembly"

ENTRYPOINT ["/run_sbt.sh", "project ingestor"]

FROM public.ecr.aws/docker/library/eclipse-temurin:11-jre-alpine

RUN apk add --no-cache bash

COPY --from=sbt /app/target/ingestor.jar /opt/docker/

ENTRYPOINT ["/opt/docker/bin/ingestor"]

FROM public.ecr.aws/lambda/java:11 as lambda
# A Lambda container, to be used to run/test the application in Lambda mode
# See: https://gallery.ecr.aws/lambda/java
# Or to run as an image on Lambda

# Prevent "Cannot list external agents" warning
RUN mkdir -p /opt/extensions

COPY --from=sbt /app/target/ingestor.jar ${LAMBDA_TASK_ROOT}/lib/
ENV AGGREGATOR_APP_CONTEXT=remote
CMD ["weco.concepts.ingestor.LambdaMain::handleRequest"]
