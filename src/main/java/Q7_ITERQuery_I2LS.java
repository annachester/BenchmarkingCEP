import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;
import util.*;
import java.util.concurrent.TimeUnit;

/**
 * Run with these parameters:
 * --input ./src/main/resources/QnV.csv
 * Approximate Iteration Solution which presents the number of occurrence per window
 * Note that for accurate solutions an apply function is required that sorts the input and create sequence combinations from the ordered list which can support unbounded iterations patterns
 * if combinations are requested and bounded iterations are used -> use joins
 */

public class Q7_ITERQuery_I2LS {
    public static void main(String[] args) throws Exception {

        String className = "Q7_ITER2QueryLS";

        final ParameterTool parameters = ParameterTool.fromArgs(args);
        // Checking input parameters
        if (!parameters.has("input")) {
            throw new Exception("Input Data is not specified");
        }

        String file = parameters.get("input");
        long throughput = parameters.getLong("tput", 50000);
        int iter = parameters.getInt("iter", 3);
        Integer windowSize = parameters.getInt("wsize", 15);
        Integer velFilter = parameters.getInt("vel", 175);
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

        // preparation, filter and add key and count column
        DataStream<Tuple3<KeyedDataPointGeneral, Integer, Integer>> velStream = input
                .filter(t -> {
                    boolean match = ((Double) t.getValue()) >= velFilter && (t instanceof VelocityEvent);
                    if (match) {
                        t.setDetectionTimeStampMs(System.currentTimeMillis());
                    }
                    return match;
                }) // filer non relevant tuples
                .map(new UDFs.MapKey()) // add artificial key = 1 if no key relationship is provided by your data
                .map(new UDFs.MapCount()) // add a count column to the tuple
                .assignTimestampsAndWatermarks(new UDFs.ExtractTimestampKeyedDataPointGeneral2Int(60000));


        // Window Sum Function over the defined window size
        DataStream<Tuple3<KeyedDataPointGeneral, Integer, Integer>> aggStream = velStream.keyBy(new KeySelector<Tuple3<KeyedDataPointGeneral, Integer, Integer>, Integer>() { // here we use artificial
                    @Override
                    public Integer getKey(Tuple3<KeyedDataPointGeneral, Integer, Integer> tuple3) throws Exception {
                        return tuple3.f1;
                    }
                })
                // .keyBy(new UDFs.DataKeySelectorTuple2Int()) // if this is select only tuples with same key are considered for a match
                .window(SlidingEventTimeWindows.of(Time.minutes(windowSize), Time.minutes(1)))
                /**
                 * if your workload requires accurate information of the all sequences contained in a window, use .apply() sort your input iterable and
                 * collect the different sequences as arraylist from the window (multiple outputs are allowed for UDF window functions)

                .apply(new WindowFunction<Tuple3<KeyedDataPointGeneral, Integer, Integer>, Object, Integer, TimeWindow>() {
                    @Override
                    public void apply(Integer integer, TimeWindow timeWindow, Iterable<Tuple3<KeyedDataPointGeneral, Integer, Integer>> iterable, Collector<Object> collector) throws Exception {

                    }
                })
                 * else: we sum the assigned count for all events assigned to the window
                 */
                .sum(2);

        // last, check if any aggregation fulfill the times condition
        aggStream.filter(new FilterFunction<Tuple3<KeyedDataPointGeneral, Integer, Integer>>() {
            @Override
            public boolean filter(Tuple3<KeyedDataPointGeneral, Integer, Integer> tuple2) throws Exception {
                boolean match = tuple2.f2 >= iter; // here we are approximate, we report that we detected at least the number of specified events, thus, if > iter we know that the window contains multiple sequences
                if (match) {
                    tuple2.f0.setDetectionTimeStampMs(System.currentTimeMillis());
                }
                return match;
            }
        });

      /** Output example:
       *  (Wed Jan 16 12:25:00 CET 2019,R2001396,198.44444444444446, velocity, POINT(8.82536, 50.609455),1,5) -> a single tuple per window with (here) the sum of 5
       *  the output points out that an event occurred, but no details about each any every involved tuple, if this is required use I2
       *  Also we use >= as CEP create multiple sequences if, e.g., 6 events would appear in an interval :
       *  input: e1, e2, e3, e4, e5, e6 may lead to the matches e1, e2, e3, e4, e5 and e1, e2, e3, e4, e6 in CEP (exact combinations depend on the exact time stamps)
       *  ASP will only indicate (Wed Jan 16 12:25:00 CET 2019,R2001396,198.44444444444446, velocity, POINT(8.82536, 50.609455),1,6)
       *  if combination of sequences are required and cannot be archived by keys, write an apply function (UDF), sort your input and generate sequences from the sorted input whenever the sum is larger than the times conditions
       */

        DataStream<String> latencies = aggStream.flatMap(new LatencyLogger_Iter2Query());

//        DataStream<String> latencies = aggStream.flatMap(
//            new FlatMapFunction<Tuple3<KeyedDataPointGeneral, Integer, Integer>, String>() {
//                @Override
//                public void flatMap(
//                    Tuple3<KeyedDataPointGeneral, Integer, Integer> resultTuple,
//                    Collector<String> collector) throws Exception {
//
//                    KeyedDataPointGeneral last = resultTuple.f0;
//                    long eventTime = last.getReadTimeStampMs();
//                    long detectionTime = last.getDetectionTimeStampMs();
//                    long currentTime = System.currentTimeMillis();
//                    long eventDetectionLatency = detectionTime - eventTime;
//                    long patternDetectionLatency = currentTime - detectionTime;
//                    long totalLatency = eventDetectionLatency + patternDetectionLatency;
//                    String latencies = "PatternLastEvent: " + last.getKey() + " - eventDetLat: $" + eventDetectionLatency +
//                        "$, patternDetLat: $" + patternDetectionLatency +"$, totalLatency: $" + totalLatency + "$";
//                    collector.collect(latencies);
//                }
//            });

//        aggStream.writeAsText(outputPath+"result_tuples.csv", FileSystem.WriteMode.OVERWRITE);
//        latencies.writeAsText(outputPath+"latency.csv", FileSystem.WriteMode.OVERWRITE);
//        throughput_messages.writeAsText(outputPath+"throughput.csv", FileSystem.WriteMode.OVERWRITE);

        JobExecutionResult executionResult = env.execute("My FlinkASP Job");
        System.out.println("The job took " + executionResult.getNetRuntime(TimeUnit.MILLISECONDS) + "ms to execute");

    }
}
