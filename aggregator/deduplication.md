# Deduplicating Used Concepts

## Why?
Although the database is perfectly capable of handling requests to add duplicate data, 
the scale of duplication in the full set of used concepts means that it is prudent 
to filter duplicates out in the aggregator, rather than allowing the database to 
deal with it.

In the 2022-08-22 snapshot, there were roughly 3.7 Million concepts, but 
fewer than a quarter of a million distinct concepts.

The overhead in rendering each of these into a bulk request, and posting
them all to Elasticsearch is much greater than that of removing duplicates.

## Time Trials

Running on a macbook using docker compose, and a local copy of the snapshot, thus:
`cat works.json | docker compose run -T aggregator`, I trialled a few approaches.

In each, the Elasticsearch database was already fully populated, so all "updates"
would return "noop". Elasticsearch had ben left running with `docker compose run elasticsearch`

Simply sending all the data to Elasticsearch took over six minutes.
Various methods of deduplication consistently took below four minuts.

## Why do it this way?

I tried a number of different deduplication approaches, settling on the statefulMapConcat approach.

The full snapshot will not be run frequently, but it still needs to run within sensible 
bounds of time and resource usage.

### Baselines

The chosen approach had to beat these two naive baselines:

1 - letting the database deal with it (see above)
2 - gathering all the concepts in a list and calling distinctBy 
    (memory peaked at 1.5GiB, CPU at 300%, took about 4 minutes)

### statefulMapConcat
First was a Flow using statefulMapConcat and a mutable Set.
```
   def deduplicateFlow: Flow[UsedConcept, UsedConcept, NotUsed] = 
    Flow[UsedConcept].statefulMapConcat { () =>
    val seen: MutableSet[String] = MutableSet.empty[String];
        { concept: UsedConcept =>
            val id = concept.identifier.toString
            if (seen.add(id)) Some(concept) else None
        }
    }
```

This drops duplicates and passes through new concepts as they arrive. Memory use peaked at 
about 750MiB.  Time taken was around 3:40.

This approach works well, but relies on mutability.  It slows down as a higher proportion of
concepts are found in the `seen` set, meaning that it takes longer to gather enough concepts
to be worth sending in a bulk request.

One strong advantage to this approach is that it always looks like it is doing something.
It almost immediately indexes the first 50,000, then gradually slows down.  Other techniques
spend a long time getting to the point where it starts indexing, so require extra logging
to make it clear that it is working.

### deduplicate in two stages
Second was a Flow that first deduplicates a smaller batch, then collects and deduplicates 
the results from that.

The best results came from deduplicating in groups of 500,000 before the final deduplication.
Time taken was around 3:30 with memory usage peaking at about 750MiB.

Each 500,000 group resulted in roughly 70,000 distinct concepts.  The resulting 535302 concepts were then 
reduced to 232568.

Working with larger groups proportionally increased the memory usage, but without improving the time
taken.

```
  def deduplicateBatch(size: Int): Flow[UsedConcept, UsedConcept, NotUsed] = {
    def fn(seq: Seq[UsedConcept]): Iterable[UsedConcept] = {
      val s = seq.distinctBy(_.identifier)
      info(s"deduped ${seq.length} records down to ${s.length} records")
      s
    }

    Flow[UsedConcept]
      .grouped(size)
      .via(Flow.fromFunction(fn))
      .mapConcat(identity)
  }

  def deduplicateFlow: Flow[UsedConcept, UsedConcept, NotUsed] = 
    Flow[UsedConcept]
      .via(deduplicateBatch(500000))
      .via(deduplicateBatch(5000000))
  
```

### deduplicate in more stages

I also tried deduplicating in three stages, using smaller groups (100K).  The performance
(both memory and time) was similar to the best two-stage deduplication.

Each 100,000 yielded around 25,000 at the first stage, then about 60,000 at the second stage.
Then the final 578101 were reduced to 232568.

