import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import util.*;
import java.util.concurrent.TimeUnit;

/**
 * Run with these parameters: they have matching event time
 * --input ./src/main/resources/QnV.csv
 */

public class Q3_ORQueryLS {
    public static void main(String[] args) throws Exception {

        String className = "Q3_ORQueryLS";

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
        Integer sensors = parameters.getInt("sensors", 1);


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
        DataStream<KeyedDataPointGeneral> input = env.addSource(new KeyedDataPointParallelSourceFunction(file, file_loops, sensors, ",", throughput))
            .assignTimestampsAndWatermarks(new UDFs.ExtractTimestamp(60000)) // indicate the time field for the matching process
         .keyBy(KeyedDataPointGeneral::getKey); // if this is select only tuples with same key (i.e., same sensor id) match

//        DataStream<String> throughput_messages = input.flatMap(new ThroughputLogger<KeyedDataPointGeneral>(KeyedDataPointParallelSourceFunction.RECORD_SIZE_IN_BYTE, throughput));
        DataStream<String> throughput_messages = input.flatMap(new ThroughputLogger<KeyedDataPointGeneral>(
            KeyedDataPointSourceFunction.RECORD_SIZE_IN_BYTE,
            className, velFilter, quaFilter, windowSize, throughput, file_loops, sensors));

        DataStream<KeyedDataPointGeneral> quaStream = input.filter(t -> ((Double) t.getValue()) > quaFilter && (t instanceof QuantityEvent));

        DataStream<KeyedDataPointGeneral> velStream = input.filter(t -> ((Double) t.getValue()) > velFilter && (t instanceof VelocityEvent));

        DataStream<KeyedDataPointGeneral> result = quaStream.union(velStream)
                // window is not required for union, if applied a .apply() function is necessary
                .windowAll(SlidingEventTimeWindows.of(Time.minutes(windowSize), Time.minutes(1)))
                .apply(new AllWindowFunction<KeyedDataPointGeneral, KeyedDataPointGeneral, TimeWindow>() {
                    @Override
                    public void apply(TimeWindow timeWindow, Iterable<KeyedDataPointGeneral> iterable, Collector<KeyedDataPointGeneral> collector) throws Exception {
                        iterable.forEach(t -> {
                            t.setDetectionTimeStampMs(System.currentTimeMillis());
                            collector.collect(t);
                        });
                    }
                });

        /* TODO: didnt manage to include thi method in the LatencyLogger because it clashed types
            with other FlatMap function with requires Tuple2<KeyedDataPointGeneral, KeyedDataPointGeneral> resultTuple
            so had to create another LatencyLogger2 */

        DataStream<String> latencies = result.flatMap(new LatencyLogger_ORQuery());

//        result.writeAsText(outputPath+"result_tuples.csv", FileSystem.WriteMode.OVERWRITE);
//  //      latencies.writeAsText(outputPath+"latency.csv", FileSystem.WriteMode.OVERWRITE);
//        throughput_messages.writeAsText(outputPath+"throughput.csv", FileSystem.WriteMode.OVERWRITE);

        JobExecutionResult executionResult = env.execute("My FlinkASP Job");
        System.out.println("The job took " + executionResult.getNetRuntime(TimeUnit.MILLISECONDS) + "ms to execute");

    }


}
