Scripts for gathering stats about the catalogue, in order to help with scaling the appication appropriately.

Results from looking at the 2022-09-08 snapshot:

# Most Concepts:

The work with the most distinct concepts is ghzw8qfa with 146.

This was found by running (log level DEBUG):
`docker compose run aggregator | grep 'concepts from' > concept-counts.txt`
Then 
`cat concept-counts.txt| sed 's/.*extracted//' | sed 's/concepts.*//'| sort -u`
Then eyeballing the result to find the biggest number

# Biggest Work
The biggest work is q2tanzjw at 475KiB

Found by running:
`cat  ~/Downloads/works.json | sh scripts/biggest-doc.sh`

(This takes a while, awking a 1.3GiB file to find the longest line is not a speedy process)