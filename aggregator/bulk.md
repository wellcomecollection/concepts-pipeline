# On Bundling Concepts for the Bulk API

## Background
Used Concepts are inserted into an Elasticsearch index via the Bulk API.

Elasticsearch documentation is (deliberately) rather vague about sizing bulk 
requests. We, as developers of our specific ES-backed solution, 
must scientifically work out a good size for our data and our clusters.

However, here are some tips: 
In [Tune for indexing speed](https://www.elastic.co/guide/en/elasticsearch/reference/8.3/tune-for-indexing-speed.html)
the ElasticSearch recommendation is to not go over a few tens of MB per request.
and in the outdated [Indexing Performance Tips](
https://www.elastic.co/guide/en/elasticsearch/guide/current/indexing-performance.html#_using_and_sizing_bulk_requests)
It suggests a starting point of 5-15MB

Although this is specifically in [ES for Hadoop](https://www.elastic.co/guide/en/elasticsearch/hadoop/8.3/performance.html),
the default limit for a Hadoop task is 1mb or 1000 docs.

## Application-specific considerations

Bulk updates are expected to be relatively rare, andhe index in question does not 
need to be excessively protected against overloading, as it should not be in use by 
anything else during bulk indexing, and will be only trivially exercised by 
indexing concepts during normal running.  Therefore, a thorough optimisation of 
this process is overkill.  However, we do still need to ensure that anything
we try to index, gets indexed.  So a degree of care needs to be taken over batch sizes.

At time of writing, there are over 3.7 million Concepts in the Works snapshot,
consisting of only about 230,000 unique concepts.

This totals over 250MB without deduplicating or ca. 17MB with.

The largest document is 570B and the mean is about 110.

An appropriate size, to stay within the "few tens of MB" (30MB), would actually encompass
the entire dataset (250,000 * 110)
 
The concept mapping is very simple.  Documents consist of a small number of keyword fields,
and each document is quite small.  So we can hope that Elastic can cope with a relatively 
large number of them.

memory usage in the docker image peaked at 881MiB with today's (22 Aug) snapshot when 
printing out straight to stdout rather than building a bulk document to push.