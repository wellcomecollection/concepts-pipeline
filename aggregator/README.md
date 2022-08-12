# Concepts Aggregator

The Concepts Aggregator collects in-use Concepts by examining Works from the catalogue API.

You can run it locally in Docker to see the lists of Concepts that would be returned by 
a list of Work ids, thus:

```shell
docker build -f aggregator/Dockerfile -t concepts-aggregator .
docker run concepts-aggregator uk4kymkq yn8nshmc  
```
