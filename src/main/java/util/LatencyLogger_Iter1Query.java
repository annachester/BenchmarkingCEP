package util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.cep.PatternFlatSelectFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LatencyLogger_Iter1Query implements FlatMapFunction<Tuple3<ArrayList<KeyedDataPointGeneral>, Long, Integer>, String> {

    private static final Logger LOG = LoggerFactory.getLogger(LatencyLogger_Iter1Query.class);
    private long eventDetectionLatencySum = 0;
    private long patternDetectionLatencySum = 0;
    private long totalLatencySum = 0;
    private long matchedPatternsCount = 0;
    private long lastLogTimeMs = -1;

    public LatencyLogger_Iter1Query() {}
    @Override
    public void flatMap(
        Tuple3<ArrayList<KeyedDataPointGeneral>, Long, Integer> resultTuple,
        Collector<String> collector) throws Exception {
        KeyedDataPointGeneral last = resultTuple.f0.get(resultTuple.f0.size()-1);
        long eventTime = last.getReadTimeStampMs();
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