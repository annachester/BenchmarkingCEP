import java.util.concurrent.TimeUnit;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import util.KeyedDataPointGeneral;
import util.KeyedDataPointParallelSourceFunction;
import util.KeyedDataPointSourceFunction;
import util.LatencyLogger;
import util.QuantityEvent;
import util.ThroughputLogger;
import util.UDFs;
import util.VelocityEvent;

/**
* Run with these parameters:
 * --input ./src/main/resources/QnV.csv
 * --output ./out/Q1_SEQPattern/
 */

public class Q1_SEQPatternLS {
    public static void main(String[] args) throws Exception {
        String className = "Q1_SEQPatternLS";

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
//        DataStream<KeyedDataPointGeneral> input = env.addSource(new KeyedDataPointParallelSourceFunction(file, file_loops, sensors, ",", throughput))
        DataStream<KeyedDataPointGeneral> input = env.addSource(new KeyedDataPointParallelSourceFunction(file, file_loops, sensors,",", throughput))
                .assignTimestampsAndWatermarks(new UDFs.ExtractTimestamp(60000)) // indicate the time field for the matching process
         .keyBy(KeyedDataPointGeneral::getKey); // if this is select only tuples with same key (i.e., same sensor id) match


//        DataStream<String> throughput_messages = input.flatMap(new ThroughputLogger<KeyedDataPointGeneral>(KeyedDataPointParallelSourceFunction.RECORD_SIZE_IN_BYTE, throughput));
        DataStream<String> throughput_messages = input.flatMap(new ThroughputLogger<KeyedDataPointGeneral>(
            KeyedDataPointSourceFunction.RECORD_SIZE_IN_BYTE,
            className, velFilter, quaFilter, windowSize, throughput, file_loops, sensors));

        Pattern<KeyedDataPointGeneral, ?> pattern = Pattern.<KeyedDataPointGeneral>begin("first").subtype(VelocityEvent.class).where(
                new SimpleCondition<VelocityEvent>() {
                    @Override
                    public boolean filter(VelocityEvent event1) {
                        Double velocity = (Double) event1.getValue();
                        return velocity > velFilter;
                    }
                }).followedByAny("last").subtype(QuantityEvent.class).where(
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

        // pattern are detected here
        PatternStream<KeyedDataPointGeneral> patternStream = CEP.pattern(input, pattern);

        DataStream<String> result = patternStream.flatSelect(new UDFs.GetResultTuple());
        DataStream<String> latencies = patternStream.flatSelect(new LatencyLogger("last"));

//        result.writeAsText(outputPath+"result_tuples.csv", WriteMode.OVERWRITE);//.setParallelism(1);
//        latencies.writeAsText(outputPath+"latency.csv", WriteMode.OVERWRITE);//.setParallelism(1);
//        throughput_messages.writeAsText(outputPath+"throughput.csv", WriteMode.OVERWRITE);//.setParallelism(1);

        JobExecutionResult executionResult = env.execute("My Flink Job");
        System.out.println("The job took " + executionResult.getNetRuntime(TimeUnit.MILLISECONDS) + "ms to execute");
    }
}
