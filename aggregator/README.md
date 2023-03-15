# Concepts Aggregator

The Concepts Aggregator collects in-use Concepts by examining Works from the catalogue API.

## Running
### As a Jar
To build a jar, run sbt:
```shell 
sbt "project aggregator" assembly
```
Given a locally running instance of Elasticsearch, you can then run it locally.
#### Extract all concepts from the latest snapshot
```shell 
APP_CONTEXT=local java -jar target/aggregator.jar
```
#### Extract all concepts from some piped NDJSON

```shell 
APP_CONTEXT=local head works.json | java -jar target/aggregator.jar
```
#### Extract the concepts from a list of work ids
```shell 
APP_CONTEXT=local java -jar target/aggregator.jar uk4kymkq yn8nshmc  
```

### In Docker
The Docker version of the application runs the [AWS RIE])(https://docs.aws.amazon.com/lambda/latest/dg/images-test.html)
To build in docker, run `docker build` from the root of the project.

```shell
docker build -f aggregator/Dockerfile -t concepts-aggregator .
```

Or (better), `docker compose build`.

```shell
  docker compose build aggregator
```

You can then run it locally. First start elasticsearch:
```shell
 docker compose run -d --service-ports elasticsearch 
```

Then start the Lambda emulator
```shell
docker compose run -d --service-ports aggregator
```
Now, you can send requests to the container to mimic a Lambda invocation:

Extract the concepts from one work

```shell
sh scripts/localpost-aggregator.sh gwdn56yp
```

The bulk update runs in a different Lambda.  This makes it easier for the two modes to have
different memory and timeout settings.
```shell
docker compose build aggregator-bulk
docker compose run -d --service-ports aggregator-bulk
curl -XPOST "http://localhost:9002/2015-03-31/functions/function/invocations" -d '{"workId":"all"}'
```

In Real Life, this can be invoked using:
```shell
aws lambda invoke --function-name 2022-08-31-concepts_aggregator_bulk --payload eyJ3b3JrSWQiOiJhbGwifQo= out
```

### Where is the data?
Each of these commands will populate the local Elasticsearch, so you can examine
the results there:

```shell 
curl -XGET http://localhost:9200/_search\?pretty
```


### Connecting to the remote cluster

If you want to run the application locally, but connected to the remote ES cluster, you can use the following (so long as you have the ability to assume a catalogue-developer role locally).

```shell
./run_with_remote_cluster.sh [pipeline_date] [...application arguments]
```
