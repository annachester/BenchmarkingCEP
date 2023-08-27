#!/usr/bin/env bash

project_path="/mnt/c/Users/Anna/Documents/Master/SS_23/BDSPRO/CEP2ASP_17_07"

startflink="$project_path/flink-1.11.6/bin/start-cluster.sh"
stopflink="$project_path/flink-1.11.6/bin/stop-cluster.sh"
flink="$project_path/flink-1.11.6/bin/flink"
resultFile="$project_path/CEP2ASP/out/CollectEcho_SEQ1.txt"
jar="$project_path/CEP2ASP/out/artifacts/flink_cep_jar/flink-cep.jar"
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
output_path="$project_path/CEP2ASP/out/"

now=$(date +"%T")
today=$(date +%d.%m.%y)
echo "Current time : $today $now" >>$resultFile
echo "----------$today $now------------" >>$resultFile
parallelism=$(grep "parallelism.default" $project_path/flink-1.11.6/conf/flink-conf.yaml | awk '{print $2}')
#echo "Default Parallelism: $parallelism"

# this is the complete setup for scalability testing. Beware of all the loops! Long execution time.
# throughputs 100000 200000 500000 run with file_loops=1
# throughputs 1000000 2000000 5000000 run with file_loops=3
# and throughput 10000000 is tested with file_loops=5
throughputs=(100000 200000 500000 1000000 2000000 5000000 10000000)
loop_settings=(1 1 1 3 3 3 5)
for i in 0 1 2 3 4 5 6; do
  for sensors_mult in 1 2; do # each setting is tested for sensors=parallelism and sensors=parallelism*2
    loop=${loop_settings[i]}
    throughput=${throughputs[i]}
    sensors=$((parallelism * sensors_mult))

    echo "parallelisation:" $parallelism", sensors:" $sensors", throughput:" $throughput

    now=$(date +"%T")
    today=$(date +%d.%m.%y)
    echo "Current time : $today $now" >>$resultFile
    echo "Flink start" >>$resultFile
    $startflink
    START=$(date +%s)
    $flink run -c Q1_SEQPatternLS $jar --input $data_path_Q1 --output $output_path --file_loops $loop --tput $throughput --sensors $sensors --vel 175 --qua 250
    END=$(date +%s)
    DIFF=$((END - START))
    echo "Q1_SEQPattern run "$i " : "$DIFF"s" >>$resultFile
    $stopflink
    echo "------------ Flink stopped ------------" >>$resultFile
    now=$(date +"%T")
    today=$(date +%d.%m.%y)
    echo "Current time : $today $now" >>$resultFile
    echo "Flink start" >>$resultFile
    $startflink
    START=$(date +%s)
    $flink run -c Q1_1_SEQPatternLS $jar --input $data_path_Q9 --output $output_path --file_loops $loop --tput $throughput --sensors $sensors --vel 175 --qua 250
    END=$(date +%s)
    DIFF=$((END - START))
    echo "Q1_SEQPattern run "$i " : "$DIFF"s" >>$resultFile
    $stopflink
    echo "------------ Flink stopped ------------" >>$resultFile
    now=$(date +"%T")
    today=$(date +%d.%m.%y)
    echo "Current time : $today $now" >>$resultFile
    echo "Flink start" >>$resultFile
    $startflink
    START=$(date +%s)
    $flink run -c Q1_2_SEQPatternLS $jar --input $data_path_Q9 --output $output_path --file_loops $loop --tput $throughput --sensors $sensors --vel 175 --qua 250
    END=$(date +%s)
    DIFF=$((END - START))
    echo "Q1_SEQQuery run "$i " : "$DIFF"s" >>$resultFile
    $stopflink
    echo "------------ Flink stopped ------------" >>$resultFile
    now=$(date +"%T")
    today=$(date +%d.%m.%y)
    echo "Current time : $today $now" >>$resultFile
    echo "Flink start" >>$resultFile
    $startflink
    START=$(date +%s)
    $flink run -c Q1_SEQQueryLS $jar --input $data_path_Q1 --output $output_path --file_loops $loop --tput $throughput --sensors $sensors --vel 175 --qua 250
    END=$(date +%s)
    DIFF=$((END - START))
    echo "Q1_SEQQuery run "$i " : "$DIFF"s" >>$resultFile
    $stopflink
    echo "------------ Flink stopped ------------" >>$resultFile

    #AND(2,C1) if keyby is commented AND(2,C2) else
    # for keyby additionally add: --vel 105 --qua 190
    now=$(date +"%T")
    today=$(date +%d.%m.%y)
    echo "Current time : $today $now" >>$resultFile
    echo "Flink start" >>$resultFile
    $startflink
    START=$(date +%s)
    START=$(date +%s)
    $flink run -c Q2_ANDPatternLS $jar --input $data_path_Q2 --output $output_path --file_loops $loop --tput $throughput --sensors $sensors --vel 175 --qua 250
    END=$(date +%s)
    DIFF=$((END - START))
    echo "Q2_ANDPattern run "$i " : "$DIFF"s" >>$resultFile
    $stopflink
    echo "------------ Flink stopped ------------" >>$resultFile
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

    for iter in 3 4 5 6; do #iteration pattern for iter 3 4 5 6
      echo "iter:" $iter
      now=$(date +"%T")
      today=$(date +%d.%m.%y)
      echo "Current time : $today $now" >>$resultFile
      echo "Flink start" >>$resultFile
      $startflink
      START=$(date +%s)
      $flink run -c Q6_ITERPattern_I1LS $jar --input $data_path_Q6 --output $output_path --file_loops $loop --tput $throughput --iter $iter --sensors $sensors --vel 175
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
      $flink run -c Q6_ITERQuery_I1LS $jar --input $data_path_Q6 --output $output_path --file_loops $loop --tput $throughput --iter $iter --sensors $sensors --vel 175
      END=$(date +%s)
      DIFF=$((END - START))
      echo "Q6_ITERQuery_I1 run "$i " : "$DIFF"s" >>$resultFile
      $stopflink
      echo "------------ Flink stopped ------------" >>$resultFile

      now=$(date +"%T")
      today=$(date +%d.%m.%y)
      echo "Current time : $today $now" >>$resultFile
      echo "Flink start" >>$resultFile
      $startflink
      START=$(date +%s)
      $flink run -c Q7_ITERPattern_I2LS $jar --input $data_path_Q7 --output $output_path --file_loops $loop --tput $throughput --iter $iter --sensors $sensors --vel 175
      END=$(date +%s)
      DIFF=$((END - START))
      echo "Q7_ITERPattern_I2 run "$i " : "$DIFF"s" >>$resultFile
      $stopflink
      echo "------------ Flink stopped ------------" >>$resultFile
      now=$(date +"%T")
      today=$(date +%d.%m.%y)
      echo "Current time : $today $now" >>$resultFile
      echo "Flink start" >>$resultFile
      $startflink
      START=$(date +%s)
      $flink run -c Q7_ITERQuery_I2LS $jar --input $data_path_Q7 --output $output_path --file_loops $loop --tput $throughput --iter $iter --sensors $sensors --vel 175
      END=$(date +%s)
      DIFF=$((END - START))
      echo "Q7_ITERQuery_I2 run "$i " : "$DIFF"s" >>$resultFile
      $stopflink
      echo "------------ Flink stopped ------------" >>$resultFile
    done

    now=$(date +"%T")
    today=$(date +%d.%m.%y)
    echo "Current time : $today $now" >>$resultFile
    echo "Flink start" >>$resultFile
    $startflink
    START=$(date +%s)
    $flink run -c Q8_SEQPatternLS $jar --input $data_path_Q8 --output $output_path --file_loops $loop --tput $throughput --sensors $sensors --vel 175 --qua 250
    END=$(date +%s)
    DIFF=$((END - START))
    echo "Q7_ITERPattern_I2 run "$i " : "$DIFF"s" >>$resultFile
    $stopflink
    echo "------------ Flink stopped ------------" >>$resultFile
    now=$(date +"%T")
    today=$(date +%d.%m.%y)
    echo "Current time : $today $now" >>$resultFile
    echo "Flink start" >>$resultFile
    $startflink
    START=$(date +%s)
    $flink run -c Q8_1_SEQPatternLS $jar --input $data_path_Q10 --output $output_path --file_loops $loop --tput $throughput --sensors $sensors --vel 175 --qua 250
    END=$(date +%s)
    DIFF=$((END - START))
    echo "Q7_ITERQuery_I2 run "$i " : "$DIFF"s" >>$resultFile
    $stopflink
    echo "------------ Flink stopped ------------" >>$resultFile
    now=$(date +"%T")
    today=$(date +%d.%m.%y)
    echo "Current time : $today $now" >>$resultFile
    echo "Flink start" >>$resultFile
    $startflink
    START=$(date +%s)
    $flink run -c Q8_2_SEQPatternLS $jar --input $data_path_Q10 --output $output_path --file_loops $loop --tput $throughput --sensors $sensors --vel 175 --qua 250
    END=$(date +%s)
    DIFF=$((END - START))
    echo "Q7_ITERQuery_I2 run "$i " : "$DIFF"s" >>$resultFile
    $stopflink
    echo "------------ Flink stopped ------------" >>$resultFile
  done
done
