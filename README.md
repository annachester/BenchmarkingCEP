# Benchmarking Cloud-Optimized Complex Event Processing Solutions

## folder descriptions:

### src/main
java classes for all patterns for FlinkCEP and CEP2ASP
overview available in NewPatternCatalogue.md (in java folder)
java classes with name suffix "LS" are for large scale experiments with parallelism.

### out
executable jar file (in artifacts folder)
also some sample output data (csv format) to see the structure of result tuples, latency log and throughput log. Only a sample not related to experiments.

### flink_configs
flink_conf.yaml files for each of the different parameter settings we wanted to try out for the scalability experiments
example: 8_4_32 refers to the following settings:
taskmanager.numberOfTaskSlots: 8
taskmanager.numberOfTaskManagers: 4
parallelism.default: 32

### experiment_scripts
bash scripts running for experiments, they were split into various subscripts
(/scripts_whole contains a previous whole version before splitting, generally more extensive)

### generator_scripts
scripts used for data generator

### plotting
python scripts used for data preprocessing and plotting
plots for both experiments (check README.md in that folder)

target/ 
can be ignored (maven setup stuff)

Qnv.csv:
original dataset (provided by ariane), before any modification through data generator.