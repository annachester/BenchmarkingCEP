import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import util.*;

import java.util.concurrent.TimeUnit;

// relaxed contiguity
public class Q1_1_SEQPattern {
    public static void main(String[] args) throws Exception {

      String className = "Q1_1_SEQPattern";

      final ParameterTool parameters = ParameterTool.fromArgs(args);
      // Checking input parameters
      if (!parameters.has("input")) {
        throw new Exception("Input Data is not specified");
      }

      String file = parameters.get("input");
      Integer velFilter = parameters.getInt("vel",150);
      Integer quaFilter = parameters.getInt("qua",175);
      Integer windowSize = parameters.getInt("wsize",15);
      long throughput = parameters.getLong("tput",100000);
      Integer file_loops = parameters.getInt("file_loops", 1);
        Integer sensors = parameters.getInt("sensors", 8);


      String outputName = className+"/throughput_"+throughput+"_loop_"+file_loops+"/";
      String outputPath;
      if (parameters.has("output")) {
        outputPath = parameters.get("output") + outputName;
      } else {
        outputPath = "./out/" + outputName;
      }

      StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
      env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

      // Anna: making the csv into a datastream
      DataStream<KeyedDataPointGeneral> input = env.addSource(new KeyedDataPointSourceFunction(file, file_loops, ",", throughput))
          .assignTimestampsAndWatermarks(new UDFs.ExtractTimestamp(60000)); // indicate the time field for the matching process
      // .keyBy(KeyedDataPointGeneral::getKey); // if this is select only tuples with same key (i.e., same sensor id) match

      DataStream<String> throughput_messages = input.flatMap(new ThroughputLogger<KeyedDataPointGeneral>(
          KeyedDataPointSourceFunction.RECORD_SIZE_IN_BYTE,
          className, velFilter, quaFilter, windowSize, throughput, file_loops));

      Pattern<KeyedDataPointGeneral, ?> pattern = Pattern.<KeyedDataPointGeneral>begin("first").subtype(
          VelocityEvent.class).where(
          new SimpleCondition<VelocityEvent>() {
            @Override
            public boolean filter(VelocityEvent event1) {
              Double velocity = (Double) event1.getValue();
              return velocity > velFilter;
            }
          }).followedBy("last").subtype(QuantityEvent.class).where(
          new IterativeCondition<QuantityEvent>() {
            @Override
            public boolean filter(QuantityEvent event2, Context<QuantityEvent> ctx) throws Exception {
              Double quantity = (Double) event2.getValue();
              if (quantity > quaFilter){
                double distance = 0.0;
                for (KeyedDataPointGeneral event : ctx.getEventsForPattern("first")) {
                  distance = UDFs.checkDistance(event, event2);
                  boolean match = distance < 10.0;
                  if (match) {
                    // moment the last event of the sequence is detected
                    event2.setDetectionTimeStampMs(System.currentTimeMillis());
                  }
                  return match;
                }
              }
              return false;
            }

          }
      ).within(Time.minutes(windowSize));

      PatternStream<KeyedDataPointGeneral> patternStream = CEP.pattern(input, pattern);
      DataStream<String> result = patternStream.flatSelect(new UDFs.GetResultTuple());
      DataStream<String> latencies = patternStream.flatSelect(new LatencyLogger("last"));


//      result.writeAsText(outputPath+"result_tuples.csv", FileSystem.WriteMode.OVERWRITE);
//      latencies.writeAsText(outputPath+"latency.csv", FileSystem.WriteMode.OVERWRITE);
//      throughput_messages.writeAsText(outputPath+"throughput.csv", FileSystem.WriteMode.OVERWRITE);

      JobExecutionResult executionResult = env.execute("My Flink Job");
      System.out.println("The job took " + executionResult.getNetRuntime(TimeUnit.MILLISECONDS) + "ms to execute");
    }
  }

