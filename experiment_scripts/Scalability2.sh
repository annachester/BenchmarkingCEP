#!/usr/bin/env bash

project_path="/mnt/d/TUBerlin/DIMA/BDSPRO"

startflink="$project_path/flink-1.11.6/bin/start-cluster.sh"
stopflink="$project_path/flink-1.11.6/bin/stop-cluster.sh"
flink="$project_path/flink-1.11.6/bin/flink"
resultFile="$project_path/bdaproresults/scalability.txt"
jar="$project_path/annafinal.jar"
#jar="$project_path/CEP2ASP/target/flink-cep-1.0-SNAPSHOT.jar"
#TODO: adjust data paths here!!!
data_path_Q1="$project_path/scripts/seq110%.csv"
data_path_Q2="$project_path/scripts/and10%.csv"
data_path_Q3="$project_path/scripts/or10%.csv"
data_path_Q6="$project_path/scripts/iter110%.csv"
data_path_Q7="$project_path/scripts/iter210%.csv"
data_path_Q8="$project_path/scripts/seq210%.csv"
data_path_Q9="$project_path/scripts/seq110%conti.csv"
data_path_Q10="$project_path/scripts/seq210%conti.csv"
output_path="$project_path/bdaproresults/"

now=$(date +"%T")
today=$(date +%d.%m.%y)
echo "Current time : $today $now" >>$resultFile
echo "----------$today $now------------" >>$resultFile
#Note: you can use this script to run our scalability experiments, below we provide our throughput's 1) for Changing Data Characteristics and 2) for scale out
# 1) Queries: sensors 8: 225000,  sensors 16: 250000, sensors 32:	325000, sensors 64:	325000, sensors 128: 325000
# 1) Pattern: sensors 8: 200000,  sensors 16: 200000, sensors 32:	210000, sensors 64:	300000, sensors 128: 300000
# 2) Query: sensors 128 (2W) 225000, sensors 128 (4W) 225000
# 2) Query: sensors 128 (2W) 130000, sensors 128 (4W) 125000

#parallelism=$(grep "parallelism.default" $project_path/flink-1.11.6/conf/flink-conf.yaml | awk '{print $2}')
parallelism=6
#echo "Default Parallelism: $parallelism"

#throughputs=(100000 10000000)
#loop_settings=(1 2)
for i in 1; do
  for sensors_mult in 1; do
    #loop=${loop_settings[i]}
    #throughput=${throughputs[i]}
    #sensors=$((parallelism * sensors_mult))
    loop=1
    throughput=10000000
    sensors=6

    echo "parallelisation:" $parallelism", sensors:" $sensors", throughput:" $throughput

    now=$(date +"%T")
    today=$(date +%d.%m.%y)
    echo "Current time : $today $now" >>$resultFile
    echo "Flink start" >>$resultFile
    $startflink
    START=$(date +%s)
    $flink run -c Q2_ANDQueryLS $jar --input $data_path_Q2 --output $output_path --file_loops $loop --tput $throughput --sensors $sensors --vel 175 --qua 250
    END=$(date +%s)
    DIFF=$((END - START))
    echo "Q2_ANDQuery run "$i " : "$DIFF"s" >>$resultFile
    $stopflink
    echo "------------ Flink stopped ------------" >>$resultFile


    # OR(2,D1)
    now=$(date +"%T")
    today=$(date +%d.%m.%y)
    echo "Current time : $today $now" >>$resultFile
    echo "Flink start" >>$resultFile
    $startflink
    START=$(date +%s)
    $flink run -c Q3_ORPatternLS $jar --input $data_path_Q3 --output $output_path --file_loops $loop --tput $throughput --sensors $sensors --vel 175 --qua 250
    END=$(date +%s)
    DIFF=$((END - START))
    echo "Q3_ORPattern run "$i " : "$DIFF"s" >>$resultFile
    $stopflink
    echo "------------ Flink stopped ------------" >>$resultFile
    now=$(date +"%T")
    today=$(date +%d.%m.%y)
    echo "Current time : $today $now" >>$resultFile
    echo "Flink start" >>$resultFile
    $startflink
    START=$(date +%s)
    $flink run -c Q3_ORQueryLS $jar --input $data_path_Q3 --output $output_path --file_loops $loop --tput $throughput --sensors $sensors --vel 175 --qua 250
    END=$(date +%s)
    DIFF=$((END - START))
    echo "Q3_ORQuery run "$i " : "$DIFF"s" >>$resultFile
    $stopflink
    echo "------------ Flink stopped ------------" >>$resultFile

    for iter in 4; do
      echo "iter:" $iter
      now=$(date +"%T")
      today=$(date +%d.%m.%y)
      echo "Current time : $today $now" >>$resultFile
      echo "Flink start" >>$resultFile
      $startflink
      START=$(date +%s)
      $flink run -c Q6_ITERPattern_I1LS $jar --input $data_path_Q6 --output $output_path --file_loops $loop --times 1 --tput $throughput --iter 1 --sensors $sensors --vel 175
      END=$(date +%s)
      DIFF=$((END - START))
      echo "Q6_ITERPattern_I1 run "$i " : "$DIFF"s" >>$resultFile
      $stopflink
      echo "------------ Flink stopped ------------" >>$resultFile
      now=$(date +"%T")
      today=$(date +%d.%m.%y)
      echo "Current time : $today $now" >>$resultFile
      echo "Flink start" >>$resultFile
      $startflink
      START=$(date +%s)
      $flink run -c Q6_ITERQuery_I1LS $jar --input $data_path_Q6 --output $output_path --file_loops $loop --times 1 --tput $throughput --iter 1 --sensors $sensors --vel 175
      END=$(date +%s)
      DIFF=$((END - START))
      echo "Q6_ITERQuery_I1 run "$i " : "$DIFF"s" >>$resultFile
      $stopflink
      echo "------------ Flink stopped ------------" >>$resultFile

    done
  done
done
