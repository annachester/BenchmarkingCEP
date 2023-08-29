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
  for throughput in 1000000; do
    file_loops=1

    echo "loop: $loop" >>$resultFile
    
    now=$(date +"%T")
    today=$(date +%d.%m.%y)
    echo "Current time : $today $now" >>$resultFile
    echo "Flink start" >>$resultFile
    $startflink
    START=$(date +%s)
    $flink run -c Q3_ORQuery $jar --input $data_path6 --output $output_path --file_loops $file_loops --tput $throughput  --vel 175 --qua 250
    END=$(date +%s)
    DIFF=$((END - START))
    echo "Q3_ORQuery - throughput: $throughput, runtime: $DIFF" >>$resultFile
    $stopflink
    echo "------------ Flink stopped ------------" >>$resultFile

    for iter in 4; do
      echo "iter: $iter"
      now=$(date +"%T")
      today=$(date +%d.%m.%y)
      echo "Current time : $today $now" >>$resultFile
      echo "Flink start" >>$resultFile
      $startflink
      START=$(date +%s)
      $flink run -c Q6_ITERPattern_I1 $jar --input $data_path7 --output $output_path --file_loops $file_loops --tput $throughput --times 1 --iter 1 --vel 175
      END=$(date +%s)
      DIFF=$((END - START))
      echo "Q6_ITERPattern_I1 - iter: $iter, throughput: $throughput, runtime: $DIFF" >>$resultFile
      $stopflink
      echo "------------ Flink stopped ------------" >>$resultFile
      now=$(date +"%T")
      today=$(date +%d.%m.%y)
      echo "Current time : $today $now" >>$resultFile
      echo "Flink start" >>$resultFile
      $startflink
      START=$(date +%s)
      $flink run -c Q6_ITERQuery_I1 $jar --input $data_path7 --output $output_path --file_loops $file_loops --tput $throughput --times 1 --iter 1 --vel 175
      END=$(date +%s)
      DIFF=$((END - START))
      echo "Q6_ITERQuery_I1 - iter: $iter, throughput: $throughput, runtime: $DIFF" >>$resultFile
      $stopflink
      echo "------------ Flink stopped ------------" >>$resultFile

      now=$(date +"%T")
      today=$(date +%d.%m.%y)
      echo "Current time : $today $now" >>$resultFile
      echo "Flink start" >>$resultFile
      $startflink
      START=$(date +%s)
      $flink run -c Q7_ITERPattern_I2 $jar --input $data_path8 --output $output_path --file_loops $file_loops --tput $throughput --iter $iter --vel 175
      END=$(date +%s)
      DIFF=$((END - START))
      echo "Q7_ITERPattern_I2 - iter: $iter, throughput: $throughput, runtime: $DIFF" >>$resultFile
      $stopflink
      echo "------------ Flink stopped ------------" >>$resultFile
      now=$(date +"%T")
      today=$(date +%d.%m.%y)
      echo "Current time : $today $now" >>$resultFile
      echo "Flink start" >>$resultFile
      $startflink
      START=$(date +%s)
      $flink run -c Q7_ITERQuery_I2 $jar --input $data_path8 --output $output_path --file_loops $file_loops --tput $throughput --iter $iter --vel 175
      END=$(date +%s)
      DIFF=$((END - START))
      echo "Q7_ITERQuery_I2 - iter: $iter, throughput: $throughput, runtime: $DIFF" >>$resultFile
      $stopflink
      echo "------------ Flink stopped ------------" >>$resultFile
    done
  done
done
echo "Tasks executed"