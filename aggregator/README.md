# Concepts Aggregator

The Concepts Aggregator collects in-use Concepts by examining Works from the catalogue API.

To build in docker, run docker build from the root of the project

```shell
docker build -f aggregator/Dockerfile -t concepts-aggregator .
```

You can then run it locally to see the lists of Concepts that would be returned by 
a list of Work ids, thus:

```shell
docker run concepts-aggregator uk4kymkq yn8nshmc  
```

Or with some line-delimited JSON piped to STDIN, thus:

```shell
head works.json | docker run -i concepts-aggregator
```

Or, to fetch the latest API Snapshot and extract from there, with no inputs, thus:

```shell
docker run concepts-aggregator
```