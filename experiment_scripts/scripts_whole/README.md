# Scripts

all.sh:
runs all classes for given throughputs (currently 200.000 and 1.000.000, because only local machine), for iteration patterns, iter parameter is 4.

Iterations.sh:
runs all iter classes for iter parameters from 3-7 (throughputs 100000-500000).

Scalability.sh
runs very extensive scalability experiments. Many loops. Could never be performed so far.
All classes with ending "LS" (large scale) are called, which run with ParallelSourceFunction.
- throughputs 100000 200000 500000 run with file_loops=1
- throughputs 1000000 2000000 5000000 run with file_loops=3
- throughput 10000000 is tested with file_loops=5

Baseline.sh
original Baseline script from Arianes project.

