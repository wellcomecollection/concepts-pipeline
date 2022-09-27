# This Dockerfile should be built with the root of the repo as context
# It is here as a convenience for running the `common` tests - of course there is no `common` application image
FROM 760097843905.dkr.ecr.eu-west-1.amazonaws.com/wellcome/sbt_wrapper:1.7.1
LABEL maintainer = "Wellcome Collection <digital@wellcomecollection.org>"

WORKDIR /app
COPY ./build.sbt ./
COPY ./project ./project
COPY ./common ./common

RUN /run_sbt.sh "project common" "compile"

ENTRYPOINT ["/run_sbt.sh", "project common"]
