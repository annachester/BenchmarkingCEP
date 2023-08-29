#!/usr/bin/env bash
root='/mnt/d/TUBerlin/DIMA/BDSPRO/'
startflink=$root'flink-1.11.6/bin/start-cluster.sh'
stopflink=$root'flink-1.11.6/bin/stop-cluster.sh'
flink=$root'flink-1.11.6/bin/flink'
resultFile=$root'bdaproresults/selTest.txt'
jar=$root'annafinal.jar'
data_path1=$root'scripts/seq110%.csv'
data_path2=$root'scripts/seq1200%.csv'
data_path3=$root'scripts/seq11%.csv'
data_path4=$root'scripts/seq101%.csv'
data_path5=$root'scripts/seq110%skew.csv'
output_pathP=$root'bdaproresults/QnV_result_selP'
output_pathQ=$root'bdaproresults/QnV_result_selQ'

#startflink='/home/ziehn-bdapro-ldap/flink-1.11.6/bin/start-cluster.sh'
#stopflink='/home/ziehn-bdapro-ldap/flink-1.11.6/bin/stop-cluster.sh'
#flink='/home/ziehn-bdapro-ldap/flink-1.11.6/bin/flink'
#resultFile='/home/ziehn-bdapro-ldap/CEP2ASP/out/CollectEcho_SEQ1.txt'
#jar='/home/ziehn-bdapro-ldap/CEP2ASP/out/artifacts/flink_cep_jar/flink-cep.jar'
#data_path='/home/ziehn-bdapro-ldap/CEP2ASP/QnV.csv'
# You find the file here: https://gofile.io/d/pjglkV
#output_path='/home/ziehn-bdapro-ldap/CEP2ASP/out/QnV_result.csv'
#SEQ(2) --vel 175 --qua 250 (sel: 5*10^⁻7) is equivalent to Baseline
  #SEQ(2) --vel 150 --qua 200 (sel: 3*10^-5)
  #>=vel >=qua
for loop in 1; do
now=$(date +"%T")
today=$(date +%d.%m.%y)
echo "Current time : $today $now" >>$resultFile
echo "Flink start" >>$resultFile
$startflink
START=$(date +%s)
$flink run -c Q1_SEQPattern $jar --input $data_path1 --output $output_pathP --vel 150 --qua 200 --wsize 20 --tput 500000
END=$(date +%s)
DIFF=$((END - START))
echo "Q1_SEQPattern run "$loop " select 10% : "$DIFF"s" >>$resultFile
$stopflink
echo "------------ Flink stopped ------------" >>$resultFile
now=$(date +"%T")

now=$(date +"%T")
today=$(date +%d.%m.%y)
echo "Current time : $today $now" >>$resultFile
echo "Flink start" >>$resultFile
$startflink
START=$(date +%s)
$flink run -c Q1_SEQQuery $jar --input $data_path1 --output $output_pathQ --vel 150 --qua 200 --wsize 20 --tput 500000
END=$(date +%s)
DIFF=$((END - START))
echo "Q1_SEQQuery run "$loop " select 10% : "$DIFF"s" >>$resultFile
$stopflink
echo "------------ Flink stopped ------------" >>$resultFile
now=$(date +"%T")

now=$(date +"%T")
today=$(date +%d.%m.%y)
echo "Current time : $today $now" >>$resultFile
echo "Flink start" >>$resultFile
$startflink
START=$(date +%s)
$flink run -c Q1_SEQPattern $jar --input $data_path2 --output $output_pathP --vel 150 --qua 200 --wsize 20 --tput 500000
END=$(date +%s)
DIFF=$((END - START))
echo "Q1_SEQPattern run "$loop " select 200% : "$DIFF"s" >>$resultFile
$stopflink
echo "------------ Flink stopped ------------" >>$resultFile
now=$(date +"%T")

#now=$(date +"%T")
#today=$(date +%d.%m.%y)
#echo "Current time : $today $now" >>$resultFile
#echo "Flink start" >>$resultFile
#$startflink
#START=$(date +%s)
#$flink run -c Q1_SEQQuery $jar --input $data_path2 --output $output_pathQ --vel 150 --qua 200 --wsize 20 --tput 500000
#END=$(date +%s)
#DIFF=$((END - START))
#echo "Q1_SEQQuery run "$loop " select 200% : "$DIFF"s" >>$resultFile
#$stopflink
#echo "------------ Flink stopped ------------" >>$resultFile
#now=$(date +"%T")

now=$(date +"%T")
today=$(date +%d.%m.%y)
echo "Current time : $today $now" >>$resultFile
echo "Flink start" >>$resultFile
$startflink
START=$(date +%s)
$flink run -c Q1_SEQPattern $jar --input $data_path3 --output $output_pathP --vel 150 --qua 200 --wsize 20 --tput 500000
END=$(date +%s)
DIFF=$((END - START))
echo "Q1_SEQPattern run "$loop " select 1% : "$DIFF"s" >>$resultFile
$stopflink
echo "------------ Flink stopped ------------" >>$resultFile
now=$(date +"%T")

now=$(date +"%T")
today=$(date +%d.%m.%y)
echo "Current time : $today $now" >>$resultFile
echo "Flink start" >>$resultFile
$startflink
START=$(date +%s)
$flink run -c Q1_SEQQuery $jar --input $data_path3 --output $output_pathQ --vel 150 --qua 200 --wsize 20 --tput 500000
END=$(date +%s)
DIFF=$((END - START))
echo "Q1_SEQQuery run "$loop " select 1% : "$DIFF"s" >>$resultFile
$stopflink
echo "------------ Flink stopped ------------" >>$resultFile
now=$(date +"%T")

done