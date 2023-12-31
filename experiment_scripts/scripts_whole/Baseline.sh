#!/usr/bin/env bash

startflink='/home/ziehn-bdapro-ldap/flink-1.11.6/bin/start-cluster.sh'
stopflink='/home/ziehn-bdapro-ldap/flink-1.11.6/bin/stop-cluster.sh'
flink='/home/ziehn-bdapro-ldap/flink-1.11.6/bin/flink'
resultFile='/home/ziehn-bdapro-ldap/CEP2ASP/out/CollectEcho_baseline.txt'
jar='/home/ziehn-bdapro-ldap/CEP2ASP/out/artifacts/basline_ariane/basline-ariane.jar'
jar='/home/ziehn-bdapro-ldap/CEP2ASP/flink-cep-1.0-SNAPSHOT.jar'
data_path='/home/ziehn-bdapro-ldap/CEP2ASP/QnV.csv'
 You find the file here: https://gofile.io/d/pjglkV
output_path='/home/ziehn-bdapro-ldap/CEP2ASP/out/QnV_result_baseline'

## original baseline script

now=$(date +"%T")
today=$(date +%d.%m.%y)
echo "Current time : $today $now" >>$resultFile
echo "----------$today $now------------" >>$resultFile
for loop in 1 2 3 4 5 6 7 8 9 10; do
  #SEQ(2)
  now=$(date +"%T")
  today=$(date +%d.%m.%y)
  echo "Current time : $today $now" >>$resultFile
  echo "Flink start" >>$resultFile
  $startflink
  START=$(date +%s)
  $flink run -c Q1_SEQPattern $jar --input $data_path2 --output $output_path --tput 110000
  END=$(date +%s)
  DIFF=$((END - START))
  echo "Q1_SEQPattern run "$loop " : "$DIFF"s" >>$resultFile
  $stopflink
  echo "------------ Flink stopped ------------" >>$resultFile
  now=$(date +"%T")
  today=$(date +%d.%m.%y)
  echo "Current time : $today $now" >>$resultFile
  echo "Flink start" >>$resultFile
  $startflink
  START=$(date +%s)
  $flink run -c Q1_SEQQuery $jar --input $data_path2 --output $output_path --tput 110000
  END=$(date +%s)
  DIFF=$((END - START))
  echo "Q1_SEQQuery run "$loop " : "$DIFF"s" >>$resultFile
  $stopflink
  echo "------------ Flink stopped ------------" >>$resultFile
  # AND(2,C1) if keyby is commented AND(2,C2) else
  # for keyby additionally add: --vel 105 --qua 190
  now=$(date +"%T")
  today=$(date +%d.%m.%y)
  echo "Current time : $today $now" >>$resultFile
  echo "Flink start" >>$resultFile
  $startflink
  START=$(date +%s)
  START=$(date +%s)
  $flink run -c Q2_ANDPattern $jar --input $data_path2 --output $output_path --tput 110000
  END=$(date +%s)
  DIFF=$((END - START))
  echo "Q2_ANDPattern run "$loop " : "$DIFF"s" >>$resultFile
  $stopflink
  echo "------------ Flink stopped ------------" >>$resultFile
  now=$(date +"%T")
  today=$(date +%d.%m.%y)
  echo "Current time : $today $now" >>$resultFile
  echo "Flink start" >>$resultFile
  $startflink
  START=$(date +%s)
  $flink run -c Q2_ANDQuery $jar --input $data_path2 --output $output_path --tput 110000
  END=$(date +%s)
  DIFF=$((END - START))
  echo "Q2_ANDQuery run "$loop " : "$DIFF"s" >>$resultFile
  $stopflink
  echo "------------ Flink stopped ------------" >>$resultFile
  # OR(2,D1)
  now=$(date +"%T")
  today=$(date +%d.%m.%y)
  echo "Current time : $today $now" >>$resultFile
  echo "Flink start" >>$resultFile
  $startflink
  START=$(date +%s)
  $flink run -c Q3_ORPattern $jar --input $data_path2 --output $output_path --vel 245 --qua 315 --tput 100000
  END=$(date +%s)
  DIFF=$((END - START))
  echo "Q3_ORPattern run "$loop " : "$DIFF"s" >>$resultFile
  $stopflink
  echo "------------ Flink stopped ------------" >>$resultFile
  now=$(date +"%T")
  today=$(date +%d.%m.%y)
  echo "Current time : $today $now" >>$resultFile
  echo "Flink start" >>$resultFile
  $startflink
  START=$(date +%s)
  $flink run -c Q3_ORQuery $jar --input $data_path2 --output $output_path --vel 245 --qua 315 --tput 100000
  END=$(date +%s)
  DIFF=$((END - START))
  echo "Q3_ORQuery run "$loop " : "$DIFF"s" >>$resultFile
  $stopflink
  echo "------------ Flink stopped ------------" >>$resultFile
  # OR(3,D2)
  now=$(date +"%T")
  today=$(date +%d.%m.%y)
  echo "Current time : $today $now" >>$resultFile
  echo "Flink start" >>$resultFile
  $startflink
  START=$(date +%s)
  $flink run -c Q4_NestedORPattern $jar --input $data_path1 --inputAQ $data_path3 --output $output_path --tput 17500 --iter 36
  END=$(date +%s)
  DIFF=$((END - START))
  echo "Q4_NestedORPattern run "$loop " : "$DIFF"s" >>$resultFile
  $stopflink
  echo "------------ Flink stopped ------------" >>$resultFile
  now=$(date +"%T")
  today=$(date +%d.%m.%y)
  echo "Current time : $today $now" >>$resultFile
  echo "Flink start" >>$resultFile
  $startflink
  START=$(date +%s)
  $flink run -c Q4_NestedORQuery $jar --input $data_path1 --inputAQ $data_path3 --output $output_path --tput 110000 --iter 36
  END=$(date +%s)
  DIFF=$((END - START))
  echo "Q4_NestedORQuery run "$loop " : "$DIFF"s" >>$resultFile
  $stopflink
  echo "------------ Flink stopped ------------" >>$resultFile
  # NOT(3)
  now=$(date +"%T")
  today=$(date +%d.%m.%y)
  echo "Current time : $today $now" >>$resultFile
  echo "Flink start" >>$resultFile
  $startflink
  START=$(date +%s)
  $flink run -c Q5_NOTPattern $jar --input $data_path1 --inputAQ $data_path3 --output $output_path --tput 25000 --iter 36
  END=$(date +%s)
  DIFF=$((END - START))
  echo "Q5_NOTPattern run "$loop " : "$DIFF"s" >>$resultFile
  $stopflink
  echo "------------ Flink stopped ------------" >>$resultFile
  now=$(date +"%T")
  today=$(date +%d.%m.%y)
  echo "Current time : $today $now" >>$resultFile
  echo "Flink start" >>$resultFile
  $startflink
  START=$(date +%s)
  $flink run -c Q5_NOTQuery $jar --input $data_path1 --inputAQ $data_path3 --output $output_path --tput 110000 --iter 36
  END=$(date +%s)
  DIFF=$((END - START))
  echo "Q5_NOTQuery run "$loop " : "$DIFF"s" >>$resultFile
  $stopflink
  echo "------------ Flink stopped ------------" >>$resultFile
  # ITER(3,I1)
  now=$(date +"%T")
  today=$(date +%d.%m.%y)
  echo "Current time : $today $now" >>$resultFile
  echo "Flink start" >>$resultFile
  $startflink
  START=$(date +%s)
  $flink run -c Q6_ITERPattern_I1 $jar --input $data_path2 --output $output_path --tput 100000 --times 3 --vel 205
  END=$(date +%s)
  DIFF=$((END - START))
  echo "Q6_ITERPattern_I1 run "$loop " : "$DIFF"s" >>$resultFile
  $stopflink
  echo "------------ Flink stopped ------------" >>$resultFile
  now=$(date +"%T")
  today=$(date +%d.%m.%y)
  echo "Current time : $today $now" >>$resultFile
  echo "Flink start" >>$resultFile
  $startflink
  START=$(date +%s)
  $flink run -c Q6_ITERQuery_I1T $jar --input $data_path2 --output $output_path --tput 115000 --times 3 --vel 205
  END=$(date +%s)
  DIFF=$((END - START))
  echo "Q6_ITERQuery_I1T run "$loop " : "$DIFF"s" >>$resultFile
  $stopflink
  echo "------------ Flink stopped ------------" >>$resultFile
  # ITER(3,I2)
  now=$(date +"%T")
  today=$(date +%d.%m.%y)
  echo "Current time : $today $now" >>$resultFile
  echo "Flink start" >>$resultFile
  $startflink
  START=$(date +%s)
  $flink run -c Q7_ITERPattern_I2 $jar --input $data_path2 --output $output_path --tput 85000 --times 3
  END=$(date +%s)
  DIFF=$((END - START))
  echo "Q7_ITERPattern_I2 run "$loop " : "$DIFF"s" >>$resultFile
  $stopflink
  echo "------------ Flink stopped ------------" >>$resultFile
  now=$(date +"%T")
  today=$(date +%d.%m.%y)
  echo "Current time : $today $now" >>$resultFile
  echo "Flink start" >>$resultFile
  $startflink
  START=$(date +%s)
  $flink run -c Q7_ITERQuery_I2 $jar --input $data_path2 --output $output_path --tput 110000 --times 3
  END=$(date +%s)
  DIFF=$((END - START))
  echo "Q7_ITERQuery_I2 run "$loop " : "$DIFF"s" >>$resultFile
  $stopflink
  echo "------------ Flink stopped ------------" >>$resultFile
done
echo "Tasks executed"
