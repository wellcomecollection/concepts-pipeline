# Running in Lambda

## Scale

We index something in the region of 15,000 documents per week, however, that will include
some documents being indexed multiple times.
Indexing one document has a peak memory usage of about 180MiB.  However, a low 
memory lambda can be very slow when fetching secrets, leading to very long cold starts.

Given a 2048MB Lambda, indexing one document from a cold start takes about 7100ms. 
When warm, about 350ms.

Given a very pessimistic view of pricing 
- giving the sizes a pretty significant buffer - 80K docs per month and 2048MB.
- assuming all documents suffer a cold start each time

This is expected to cost about less than 20USD per month.

Removing the cold start assumption, it's more like 1USD.

Triggering a full rebuild each week (2048MiB,  442000ms) adds an extra 0.06USD.



