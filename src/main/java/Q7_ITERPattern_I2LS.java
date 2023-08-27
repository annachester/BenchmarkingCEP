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
 * Iteration Pattern with simple filter condition
 */

public class Q7_ITERPattern_I2LS {
    public static void main(String[] args) throws Exception {

        String className = "Q7_ITER2PatternLS";

        final ParameterTool parameters = ParameterTool.fromArgs(args);
        // Checking input parameters
        if (!parameters.has("input")) {
            throw new Exception("Input Data is not specified");
        }

        String file = parameters.get("input");
        Integer velFilter = parameters.getInt("vel", 175);
        Integer windowSize = parameters.getInt("wsize", 15);
        int iter = parameters.getInt("iter", 3);
        long throughput = parameters.getLong("tput", 50000);
        Integer file_loops = parameters.getInt("file_loops", 1);
        Integer sensors = parameters.getInt("sensors", 1);


        String outputName = className+"/throughput_"+throughput+"_loop_"+file_loops+"_iter_"+iter+"/";
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
        DataStream<KeyedDataPointGeneral> input = env.addSource(new KeyedDataPointParallelSourceFunction(file, file_loops, sensors, ",", throughput))
            .assignTimestampsAndWatermarks(new UDFs.ExtractTimestamp(60000)) // indicate the time field for the matching process
         .keyBy(KeyedDataPointGeneral::getKey); // if this is select only tuples with same key (i.e., same sensor id) match

//        DataStream<String> throughput_messages = input.flatMap(new ThroughputLogger<KeyedDataPointGeneral>(KeyedDataPointParallelSourceFunction.RECORD_SIZE_IN_BYTE, throughput));
        DataStream<String> throughput_messages = input.flatMap(new ThroughputLogger<KeyedDataPointGeneral>(
            KeyedDataPointSourceFunction.RECORD_SIZE_IN_BYTE,
            className, velFilter, windowSize, throughput, file_loops, iter, sensors));

        Pattern<KeyedDataPointGeneral, ?> pattern = Pattern.<KeyedDataPointGeneral>begin("all_same").subtype(VelocityEvent.class).where(
                        new SimpleCondition<VelocityEvent>() {

                            @Override
                            public boolean filter(VelocityEvent event) throws Exception {
                                if ((Double) event.getValue() > velFilter) {
                                    event.setDetectionTimeStampMs(System.currentTimeMillis());
                                    return true;
                                }
                                return false;
                            }
                        }).times(iter).allowCombinations()
                .within(Time.minutes(windowSize));

        PatternStream<KeyedDataPointGeneral> patternStream = CEP.pattern(input, pattern);
        DataStream<String> result = patternStream.flatSelect(new UDFs.GetResultTuple());
        DataStream<String> latencies = patternStream.flatSelect(new LatencyLogger("all_same"));

//        result.writeAsText(outputPath+"result_tuples.csv", FileSystem.WriteMode.OVERWRITE);
//        latencies.writeAsText(outputPath+"latency.csv", FileSystem.WriteMode.OVERWRITE);
//        throughput_messages.writeAsText(outputPath+"throughput.csv", FileSystem.WriteMode.OVERWRITE);

        JobExecutionResult executionResult = env.execute("My Flink Job");
        System.out.println("The job took " + executionResult.getNetRuntime(TimeUnit.MILLISECONDS) + "ms to execute");

    }

}
