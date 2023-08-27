import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import util.*;
import java.util.concurrent.TimeUnit;

/**
 * Run with these parameters:
 * --input ./src/main/resources/QnV.csv
 */

public class Q3_ORPattern {
    public static void main(String[] args) throws Exception {

        String className = "Q3_ORPattern";

        final ParameterTool parameters = ParameterTool.fromArgs(args);
        // Checking input parameters
        if (!parameters.has("input")) {
            throw new Exception("Input Data is not specified");
        }

        String file = parameters.get("input");
        Integer velFilter = parameters.getInt("vel",175);
        Integer quaFilter = parameters.getInt("qua",150);
        Integer windowSize = parameters.getInt("wsize",15);
        long throughput = parameters.getLong("tput",50000);
        Integer file_loops = parameters.getInt("file_loops", 1);


        String outputName = className+"/throughput_"+throughput+"_loop_"+file_loops+"/";
        String outputPath;
        if (parameters.has("output")) {
            outputPath = parameters.get("output") + outputName;
        } else {
            outputPath = "./out/" + outputName;
        }

        // sets up the Flink streaming environment and specifies that the time characteristic should be EventTime
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        // This stream of KeyedDataPointGeneral objects is timestamped and watermarked
        DataStream<KeyedDataPointGeneral> input = env.addSource(new KeyedDataPointSourceFunction(file, file_loops, ",", throughput))
            .assignTimestampsAndWatermarks(new UDFs.ExtractTimestamp(60000)); // indicate the time field for the matching process
        // .keyBy(new UDFs.DataKeySelector()); // if this is select only tuples with same key (i.e., same sensor id) match

//        DataStream<String> throughput_messages = input.flatMap(new ThroughputLogger<KeyedDataPointGeneral>(KeyedDataPointSourceFunction.RECORD_SIZE_IN_BYTE, throughput));
        DataStream<String> throughput_messages = input.flatMap(new ThroughputLogger<KeyedDataPointGeneral>(KeyedDataPointSourceFunction.RECORD_SIZE_IN_BYTE,
            className, velFilter, quaFilter, windowSize, throughput, file_loops));

        Pattern<KeyedDataPointGeneral, ?> pattern1 = Pattern.<KeyedDataPointGeneral>begin("last").subtype(QuantityEvent.class).where(
                new SimpleCondition<QuantityEvent>() {
                    @Override
                    public boolean filter(QuantityEvent event1) {
                        Double quantity = (Double) event1.getValue();
                        boolean match = quantity > quaFilter;
                        if (match) {
                            // moment the second event is detected
                            event1.setDetectionTimeStampMs(System.currentTimeMillis());
                        }
                        return match;
                    }
                }).within(Time.minutes(windowSize));

        Pattern<KeyedDataPointGeneral, ?> pattern2 = Pattern.<KeyedDataPointGeneral>begin("last").subtype(VelocityEvent.class).where(
                new SimpleCondition<VelocityEvent>() {
                    @Override
                    public boolean filter(VelocityEvent event2) throws Exception {
                        Double vel = (Double) event2.getValue();
                        boolean match = vel > velFilter;
                        if (match) {
                            // moment the second event is detected
                            event2.setDetectionTimeStampMs(System.currentTimeMillis());
                        }
                        return match;
                    }
                }).within(Time.minutes(windowSize));

        PatternStream<KeyedDataPointGeneral> patternStream1 = CEP.pattern(input, pattern1);
        PatternStream<KeyedDataPointGeneral> patternStream2 = CEP.pattern(input, pattern2);

        /* *
         * We can only apply one pattern to the stream and not use .or() for different subtypes (event types)
         * thus, matches from patternStream and patternStream2 are not related.
         * Below, is just composing results of both entries at the end.
         * */
        DataStream<String> result = patternStream1.flatSelect(new UDFs.GetResultTuple())
                .union(patternStream2.flatSelect(new UDFs.GetResultTuple()));

        DataStream<String> latencies1 = patternStream1.flatSelect(new LatencyLogger("last"));
        DataStream<String> latencies2 = patternStream2.flatSelect(new LatencyLogger("last"));
        DataStream<String> latencies = latencies1.union(latencies2);

//        result.writeAsText(outputPath+"result_tuples.csv", FileSystem.WriteMode.OVERWRITE).setParallelism(1);
//        latencies.writeAsText(outputPath+"latency.csv", FileSystem.WriteMode.OVERWRITE).setParallelism(1);
//        throughput_messages.writeAsText(outputPath+"throughput.csv", FileSystem.WriteMode.OVERWRITE).setParallelism(1);

        JobExecutionResult executionResult = env.execute("My Flink Job");
        System.out.println("The job took " + executionResult.getNetRuntime(TimeUnit.MILLISECONDS) + "ms to execute");
    }

}
