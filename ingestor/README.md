# Concepts Ingestor

Take concepts from Library of Congress sources, put it in elasticsearch.

From the project root:
```shell
 docker compose run -d --service-ports elasticsearch 
```
Then

```shell
docker compose run ingestor
```

### Connecting to the remote cluster

If you want to run the application locally, but connected to the remote ES cluster, you can use the following (so long as you have the ability to assume a catalogue-developer role locally).

```shell
./run_with_remote_cluster.sh [pipeline_date] [...application arguments]
```
