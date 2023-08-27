package util;

import java.util.List;
import java.util.Map;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.cep.PatternFlatSelectFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LatencyLogger implements PatternFlatSelectFunction<KeyedDataPointGeneral, String>,
    FlatMapFunction<Tuple2<KeyedDataPointGeneral, KeyedDataPointGeneral>, String> {

    private static final Logger LOG = LoggerFactory.getLogger(LatencyLogger.class);

    private String lastEvent;

    private long eventDetectionLatencySum = 0;
    private long patternDetectionLatencySum = 0;
    private long totalLatencySum = 0;
    private long matchedPatternsCount = 0;

    private long lastLogTimeMs = -1;

    public LatencyLogger() {}

    public LatencyLogger(String lastEvent) {
        this.lastEvent = lastEvent;
    }

    // for Patterns
    @Override
    public void flatSelect(Map<String, List<KeyedDataPointGeneral>> map, Collector<String> collector) throws Exception {

        KeyedDataPointGeneral last;
        if (lastEvent.equals("last")) {
            last = map.get(lastEvent).get(0); //list of one element
        } else if (lastEvent.equals("all_same")) { //used for iteration patterns
            last = map.get("all_same").get(map.get("all_same").size()-1);
        } else {
            System.out.println("Last event name unknown!");
            last = null;
        }

        if (last != null) {
            log_latency(last, collector);
        } else {
            LOG.info("last element was null. No latencies could be recorded.");
        }
    }

    // For queries
    @Override
    public void flatMap(Tuple2<KeyedDataPointGeneral, KeyedDataPointGeneral> resultTuple, Collector<String> collector) throws Exception {
        KeyedDataPointGeneral last = resultTuple.f1;
        log_latency(last, collector);
    }

    public void log_latency(KeyedDataPointGeneral last, Collector<String> collector) {
        long eventTime = last.getReadTimeStampMs(); //named ingestion-time in report
        long detectionTime = last.getDetectionTimeStampMs();
        long currentTime = System.currentTimeMillis();
        long eventDetectionLatency = detectionTime - eventTime;
        long patternDetectionLatency = currentTime - detectionTime;
        long totalLatency = eventDetectionLatency + patternDetectionLatency;

        this.totalLatencySum += totalLatency;
        this.eventDetectionLatencySum += eventDetectionLatency;
        this.patternDetectionLatencySum += patternDetectionLatency;
        this.matchedPatternsCount += 1;

        if (lastLogTimeMs == -1) { //init
            lastLogTimeMs = currentTime;
            LOG.info("Starting Latency Logging for matched patterns with frequency 1 second.");
        }

        long timeDiff = currentTime - lastLogTimeMs;
        if (timeDiff >= 1000) {
            String message = "eventDetLatSum: $" + eventDetectionLatencySum + "$, patternDetLatSum: $" +
                patternDetectionLatencySum + "$, totalLatencySum: $" + totalLatencySum +
                "$, matchedPatternsSum: $" + matchedPatternsCount + "$";
            LOG.info(message);
            collector.collect(message);
            lastLogTimeMs = currentTime;
            totalLatencySum = 0;
            patternDetectionLatencySum = 0;
            eventDetectionLatencySum = 0;
            matchedPatternsCount = 0;
        }
    }
}