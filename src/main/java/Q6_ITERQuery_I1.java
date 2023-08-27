import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.functions.FlatJoinFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.*;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;
import util.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import util.UDFs.getArtificalKey;

/**
 * Run with these parameters
 * --input ./src/main/resources/QnV.csv
 * We translate Q6_ITERPatterm_I1 here using ASP OP, i.e., joins and apply the inter-event condition, i.e., increasing values over time, in the join apply.
 * Note that here for the flexible handling of the times parameter (# of iterations) we use a ArrayList, not that this is not the best-permanent solution.
 * If you aim for high performance, each iteration need to be hand-coded and use the POJO class KeyedDataPointGeneral only (no array list).
 * see Q6_ITERQuery_I1T and Q6_ITERQuery_I1T5-9.
 */

public class Q6_ITERQuery_I1 {
    public static void main(String[] args) throws Exception {

        String className = "Q6_ITER1Query";

        final ParameterTool parameters = ParameterTool.fromArgs(args);
        // Checking input parameters
        if (!parameters.has("input")) {
            throw new Exception("Input Data is not specified");
        }

        String file = parameters.get("input");
        long throughput = parameters.getLong("tput", 50000);
        Integer velFilter = parameters.getInt("vel", 179);
        Integer windowSize = parameters.getInt("wsize", 15);
        int iter = parameters.getInt("iter", 3);
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
            className, velFilter, windowSize, throughput, file_loops, iter));

        // we filter directly
        DataStream<Tuple2<KeyedDataPointGeneral, Integer>> velStream = input
                .filter(t -> ((Double) t.getValue()) >= velFilter && (t instanceof VelocityEvent))
                .map(new UDFs.MapKey());

        // iter == 2, we create an array list here to flexible handle the times parameter
        DataStream<Tuple3<ArrayList<KeyedDataPointGeneral>, Long, Integer>> it1 = velStream.join(velStream)
                .where(new getArtificalKey())
                .equalTo(new getArtificalKey())
                .window(SlidingEventTimeWindows.of(Time.minutes(windowSize), Time.minutes(1)))
                .apply(new FlatJoinFunction<Tuple2<KeyedDataPointGeneral, Integer>, Tuple2<KeyedDataPointGeneral, Integer>, Tuple3<ArrayList<KeyedDataPointGeneral>, Long, Integer>>() {
                    @Override
                    public void join(Tuple2<KeyedDataPointGeneral, Integer> d1, Tuple2<KeyedDataPointGeneral, Integer> d2, Collector<Tuple3<ArrayList<KeyedDataPointGeneral>, Long, Integer>> collector) throws Exception {
                        if (d1.f0.getTimeStampMs() < d2.f0.getTimeStampMs() && (Double) d1.f0.getValue() < (Double) d2.f0.getValue()) {
                            // use <= for the timely order of events in a sequence to match FlinkCEP note that FlinkCEP does not form all combination from them
                            ArrayList<KeyedDataPointGeneral> list = new ArrayList<KeyedDataPointGeneral>(2);
                            list.add(0, d1.f0);
                            d2.f0.setDetectionTimeStampMs(System.currentTimeMillis());
                            list.add(1, d2.f0);
                            collector.collect(new Tuple3<>(list, d1.f0.getTimeStampMs(), 1));
                        }
                    }
                })
                .assignTimestampsAndWatermarks(new AssignerWithPeriodicWatermarks<Tuple3<ArrayList<KeyedDataPointGeneral>, Long, Integer>>() {
                    private static final long serialVersionUID = 1L;
                    private long maxOutOfOrderness = 60000;
                    private long currentMaxTimestamp;

                    @Nullable
                    @Override
                    public Watermark getCurrentWatermark() {
                        return new Watermark(currentMaxTimestamp - maxOutOfOrderness);
                    }

                    @Override
                    public long extractTimestamp(Tuple3<ArrayList<KeyedDataPointGeneral>, Long, Integer> tuple4, long l) {
                        long timestamp = tuple4.f1;
                        currentMaxTimestamp = Math.max(timestamp, currentMaxTimestamp);
                        return timestamp;
                    }
                });

        // for iter >= 3 we can use a loop
        for (int i = 3; i <= iter; i++) {

            it1 = it1.join(velStream)
                    .where(new KeySelector<Tuple3<ArrayList<KeyedDataPointGeneral>, Long, Integer>, Integer>() {

                        @Override
                        public Integer getKey(Tuple3<ArrayList<KeyedDataPointGeneral>, Long, Integer> tuple3) throws Exception {
                            return tuple3.f2;
                        }
                    }).equalTo(new getArtificalKey())
                    .window(SlidingEventTimeWindows.of(Time.minutes(windowSize), Time.minutes(1)))
                    .apply(new FlatJoinFunction<Tuple3<ArrayList<KeyedDataPointGeneral>, Long, Integer>, Tuple2<KeyedDataPointGeneral, Integer>, Tuple3<ArrayList<KeyedDataPointGeneral>, Long, Integer>>() {
                        @Override
                        public void join(Tuple3<ArrayList<KeyedDataPointGeneral>, Long, Integer> d1, Tuple2<KeyedDataPointGeneral, Integer> d2, Collector<Tuple3<ArrayList<KeyedDataPointGeneral>, Long, Integer>> collector) throws Exception {
                            if (d1.f0.get(d1.f0.size() - 1).getTimeStampMs() < d2.f0.getTimeStampMs() && (Double) d1.f0.get(d1.f0.size()-1).getValue() < (Double) d2.f0.getValue()) {

                                ArrayList<KeyedDataPointGeneral> list = new ArrayList<KeyedDataPointGeneral>(d1.f0.size());
                                list.addAll(0, d1.f0);
                                d2.f0.setDetectionTimeStampMs(System.currentTimeMillis());
                                list.add(d2.f0);
                                collector.collect(new Tuple3<>(list, d1.f1, 1));
                            }
                        }
                    }).assignTimestampsAndWatermarks(new AssignerWithPeriodicWatermarks<Tuple3<ArrayList<KeyedDataPointGeneral>, Long, Integer>>() {
                        private static final long serialVersionUID = 1L;
                        private long maxOutOfOrderness = 60000;
                        private long currentMaxTimestamp;

                        @Nullable
                        @Override
                        public Watermark getCurrentWatermark() {
                            return new Watermark(currentMaxTimestamp - maxOutOfOrderness);
                        }

                        @Override
                        public long extractTimestamp(Tuple3<ArrayList<KeyedDataPointGeneral>, Long, Integer> tuple4, long l) {
                            long timestamp = tuple4.f1;
                            currentMaxTimestamp = Math.max(timestamp, currentMaxTimestamp);
                            return timestamp;
                        }
                    });
        }

        DataStream<String> latencies = it1.flatMap(new LatencyLogger_Iter1Query());

//        DataStream<String> latencies = it1.flatMap(
//            new FlatMapFunction<Tuple3<ArrayList<KeyedDataPointGeneral>, Long, Integer>, String>() {
//                @Override
//                public void flatMap(
//                    Tuple3<ArrayList<KeyedDataPointGeneral>, Long, Integer> resultTuple,
//                    Collector<String> collector) throws Exception {
//
//                    KeyedDataPointGeneral last = resultTuple.f0.get(resultTuple.f0.size()-1);
//
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


//        it1.writeAsText(outputPath+"result_tuples.csv", FileSystem.WriteMode.OVERWRITE).setParallelism(1);
//        latencies.writeAsText(outputPath+"latency.csv", FileSystem.WriteMode.OVERWRITE).setParallelism(1);
//        throughput_messages.writeAsText(outputPath+"throughput.csv", FileSystem.WriteMode.OVERWRITE).setParallelism(1);

        JobExecutionResult executionResult = env.execute("My FlinkASP Job");
        System.out.println("The job took " + executionResult.getNetRuntime(TimeUnit.MILLISECONDS) + "ms to execute");
    }
}