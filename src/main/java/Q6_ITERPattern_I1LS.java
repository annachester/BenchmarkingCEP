import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
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
 * This CEP pattern uses the FlinkCEP times operator and applies and inter-event condition, i.e., increasing values over time
 */
public class Q6_ITERPattern_I1LS {
    public static void main(String[] args) throws Exception {

        String className = "Q6_ITER1PatternLS";

        final ParameterTool parameters = ParameterTool.fromArgs(args);
        // Checking input parameters
        if (!parameters.has("input")) {
            throw new Exception("Input Data is not specified");
        }

        String file = parameters.get("input");
        long throughput = parameters.getLong("tput", 50000);
        Integer velFilter = parameters.getInt("vel", 175);
        Integer windowSize = parameters.getInt("wsize", 15);
        int iter = parameters.getInt("iter", 3);
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
                        new IterativeCondition<VelocityEvent>() {
                            @Override
                            public boolean filter(VelocityEvent event1, Context<VelocityEvent> ctx) throws Exception {
                                if ((Double) event1.getValue() < velFilter) {
                                    return false;
                                }
                                for (VelocityEvent event : ctx.getEventsForPattern("all_same")) {
                                    if ((Double) event1.getValue() < (Double) event.getValue()) {
                                        return false;
                                    }
                                }
                                event1.setDetectionTimeStampMs(System.currentTimeMillis());
//                                event1.setLatency(); //TODO: suspect: this doesnt output same result as later eventDetectionLatency
//                                System.out.println(event1.getLatency());
                                return true;
                            }
                        }).times(iter).allowCombinations()
                .within(Time.minutes(windowSize));

        PatternStream<KeyedDataPointGeneral> patternStream = CEP.pattern(input, pattern);
        DataStream<String> result = patternStream.flatSelect(new UDFs.GetResultTuple());

        DataStream<String> latencies = patternStream.flatSelect(new LatencyLogger("all_same")); //cant access last event directly

//        result.writeAsText(outputPath+"result_tuples.csv", FileSystem.WriteMode.OVERWRITE);
//        latencies.writeAsText(outputPath+"latency.csv", FileSystem.WriteMode.OVERWRITE);
//        throughput_messages.writeAsText(outputPath+"throughput.csv", FileSystem.WriteMode.OVERWRITE);

        JobExecutionResult executionResult = env.execute("My Flink Job");
        System.out.println("The job took " + executionResult.getNetRuntime(TimeUnit.MILLISECONDS) + "ms to execute");
    }
}
