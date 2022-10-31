# This Dockerfile should be built with the root of the repo as context
FROM 760097843905.dkr.ecr.eu-west-1.amazonaws.com/wellcome/sbt_wrapper:1.7.1 as sbt
LABEL maintainer = "Wellcome Collection <digital@wellcomecollection.org>"

WORKDIR /app
COPY ./build.sbt ./
COPY ./project ./project
# make use of layers, don't go fetching all the dependencies for every code change
RUN /run_sbt.sh "project recorder" "update"
COPY ./common ./common
COPY ./recorder ./recorder
# Build a fat jar, as recommended for lambda here:
# https://deepdive.codiply.com/aws-lambda-function-in-scala-with-container-image
# https://aws.amazon.com/blogs/compute/writing-aws-lambda-functions-in-scala/
RUN /run_sbt.sh "project recorder" "assembly"
ENTRYPOINT ["/run_sbt.sh", "project recorder"]

FROM public.ecr.aws/docker/library/eclipse-temurin:11-jre-alpine as runtime

RUN apk add --no-cache bash

COPY --from=sbt /app/target/recorder.jar /opt/docker/
ENTRYPOINT ["java", "-jar", "/opt/docker/recorder.jar"]

FROM public.ecr.aws/lambda/java:11 as lambda
# A Lambda container, to be used to run/test the application in Lambda mode
# See: https://gallery.ecr.aws/lambda/java
# Or to run as an image on Lambda

# Prevent "Cannot list external agents" warning
RUN mkdir -p /opt/extensions

COPY --from=sbt /app/target/recorder.jar ${LAMBDA_TASK_ROOT}/lib/
ENV APP_CONTEXT=remote

FROM lambda as sqs_lambda
CMD ["weco.concepts.recorder.SQSMain::handleRequest"]
