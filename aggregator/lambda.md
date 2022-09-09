# Running in Lambda

## Scale

We index something in the region of 15,000 documents per week, however, that will include
some documents being indexed multiple times.
Indexing one document has a peak memory usage of about 180MiB.

Indexing one document from a cold start takes about 2500ms. When warm, about 200ms.

Given a very pessimistic view of pricing 
- giving the sizes a pretty significant buffer - 80K docs per month and 256MiB.
- assuming all documents suffer a cold start each time

This is expected to cost about 85c per month.

Removing the cold start assumption, it's more like 12c.

Triggering a full rebuild each week (1024MiB, 210,000ms) adds an extra cent.



