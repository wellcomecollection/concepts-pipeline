# Concepts Ingestor

Takes concepts from Library of Congress sources (LoC subject headings and LoC names) and stores them into
the `authoritative-concepts` index in Elasticsearch.

Unlike other services from the `concepts-pipeline`, the `ingestor` service is currently not deployed as a Lambda function in AWS.
This is because it is not possible to index all Library of Congress concepts (12+ million) as part of a single Lambda function run,
which is constrained by a 15-minute execution time limit.

Not having a deployed version of this service is not a pressing issue because it does not need to run regularly or frequently.
Whenever we need to do a full reindex, we can run the service locally while connected to the remote Elasticsearch cluster
(see below for instructions).

If we ever decide to deploy the service, it might be a good idea to follow the architecture of the `ebsco_indexer` service (see [here](https://github.com/wellcomecollection/catalogue-pipeline/blob/main/ebsco_adapter/terraform/indexer_lambda.tf)), which was architected to solve the same issue. It uses an EventSourceMapping to consume messages from SQS via a Lambda function. Each instance of the  
Lambda function only indexes a small batch of items and a maximum concurrency parameter is used to ensure the Elasticsearch cluster is not overwhelmed. 


## Running locally with a local index

From the project root:
```shell
 docker compose run -d --service-ports elasticsearch 
```
Then

```shell
docker compose run ingestor
```

## Running locally with the remote index

If you want to run the application locally, but connected to the remote ES cluster, you can use the following (so long as you have the ability to assume a catalogue-developer role locally).

```shell
./run_with_remote_cluster.sh [pipeline_date] [...application arguments]
```
