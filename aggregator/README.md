# Concepts Aggregator

The Concepts Aggregator collects in-use Concepts by examining Works from the catalogue API.

To build in docker, run docker build from the root of the project

```shell
docker build -f aggregator/Dockerfile -t concepts-aggregator .
```

Or (better), docker compose build from here.

```shell
  docker compose build
```

You can then run it locally. First start elasticsearch:
```shell
 docker compose run -d --service-ports elasticsearch 
```

Then, to see the lists of Concepts that would be returned by 
a list of Work ids, thus:

```shell
docker compose run aggregator uk4kymkq yn8nshmc  
```

Or with some line-delimited JSON piped to STDIN, thus:

```shell
head works.json | docker compose run -T aggregator
```

Or, to fetch the latest API Snapshot and extract from there, with no inputs, thus:

```shell
docker compose run aggregator
```

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
