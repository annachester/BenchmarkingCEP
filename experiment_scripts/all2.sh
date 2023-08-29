#!/usr/bin/env bash

project_path="/mnt/d/TUBerlin/DIMA/BDSPRO"

startflink="$project_path/flink-1.11.6/bin/start-cluster.sh"
stopflink="$project_path/flink-1.11.6/bin/stop-cluster.sh"
flink="$project_path/flink-1.11.6/bin/flink"
resultFile="$project_path/bdaproresults/all.txt"
jar="$project_path/annafinal.jar"
#jar="$project_path/CEP2ASP/target/flink-cep-1.0-SNAPSHOT.jar"
data_path1="$project_path/scripts/seq110%.csv"
data_path2="$project_path/scripts/seq110%conti.csv.csv"
data_path3="$project_path/scripts/seq210%.csv"
data_path4="$project_path/scripts/seq210%conti.csv"
data_path5="$project_path/scripts/and10%.csv"
data_path6="$project_path/scripts/or10%.csv"
data_path7="$project_path/scripts/iter110%.csv"
data_path8="$project_path/scripts/iter210%.csv"
output_path="$project_path/bdaproresults/"

now=$(date +"%T")
today=$(date +%d.%m.%y)
echo "Current time : $today $now" >>$resultFile
echo "----------$today $now------------" >>$resultFile

for loop in 1; do
  for throughput in 200000; do

    file_loops=1

    now=$(date +"%T")
    today=$(date +%d.%m.%y)
    echo "Current time : $today $now" >>$resultFile
    echo "Flink start" >>$resultFile
    $startflink
    echo "loop: $loop"
    echo "throughput: $throughput"
    START=$(date +%s)
    $flink run -c Q8_1_SEQPattern $jar --input $data_path4 --output $output_path --file_loops $file_loops --tput $throughput --vel 150 --qua 250
    END=$(date +%s)
    DIFF=$((END - START))
    echo "Q8_1_SEQPattern - throughput: $throughput, runtime: $DIFF" >>$resultFile
    $stopflink
    echo "------------ Flink stopped ------------" >>$resultFile
    now=$(date +"%T")
    today=$(date +%d.%m.%y)
    echo "Current time : $today $now" >>$resultFile
    echo "Flink start" >>$resultFile
    $startflink
    echo "loop: $loop"
    echo "throughput: $throughput"
    START=$(date +%s)
    $flink run -c Q8_2_SEQPattern $jar --input $data_path4 --output $output_path --file_loops $file_loops --tput $throughput --vel 150 --qua 250
    END=$(date +%s)
    DIFF=$((END - START))
    echo "Q8_2_SEQPattern - throughput: $throughput, runtime: $DIFF" >>$resultFile
    $stopflink
    echo "------------ Flink stopped ------------" >>$resultFile

    now=$(date +"%T")
    today=$(date +%d.%m.%y)
    echo "Current time : $today $now" >>$resultFile
    echo "Flink start" >>$resultFile
    $startflink
    START=$(date +%s)
    START=$(date +%s)
    $flink run -c Q2_ANDPattern $jar --input $data_path5 --output $output_path --file_loops $file_loops --tput $throughput --vel 175 --qua 250
    END=$(date +%s)
    DIFF=$((END - START))
    echo "Q2_ANDPattern - throughput: $throughput, runtime: $DIFF" >>$resultFile
    $stopflink
    echo "------------ Flink stopped ------------" >>$resultFile
    now=$(date +"%T")
    today=$(date +%d.%m.%y)
    echo "Current time : $today $now" >>$resultFile
    echo "Flink start" >>$resultFile
    $startflink
    START=$(date +%s)
    $flink run -c Q2_ANDQuery $jar --input $data_path5 --output $output_path --file_loops $file_loops --tput $throughput --vel 175 --qua 250
    END=$(date +%s)
    DIFF=$((END - START))
    echo "Q2_ANDQuery - throughput: $throughput, runtime: $DIFF" >>$resultFile
    $stopflink
    echo "------------ Flink stopped ------------" >>$resultFile

    now=$(date +"%T")
    today=$(date +%d.%m.%y)
    echo "Current time : $today $now" >>$resultFile
    echo "Flink start" >>$resultFile
    $startflink
    START=$(date +%s)
    $flink run -c Q3_ORPattern $jar --input $data_path6 --output $output_path --file_loops $file_loops --tput $throughput  --vel 175 --qua 250
    END=$(date +%s)
    DIFF=$((END - START))
    echo "Q3_ORPattern - throughput: $throughput, runtime: $DIFF" >>$resultFile
    $stopflink
    echo "------------ Flink stopped ------------" >>$resultFile
    
  done
done
echo "Tasks executed"