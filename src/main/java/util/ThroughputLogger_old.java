package util;

import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThroughputLogger_old<T> extends RichFlatMapFunction<T, String> {

    private static final Logger LOG = LoggerFactory.getLogger(ThroughputLogger.class);

    private long totalReceived = 0;
    private long lastTotalReceived = 0;
    private long lastLogTimeMs = -1;
    private int elementSize;
    private long logfreq;
    private long query = 0;

    private static boolean started = false;
    private static long start_time;


    public ThroughputLogger_old(int elementSize, long logfreq) {
        this.elementSize = elementSize;
        this.logfreq = logfreq;
    }


    public ThroughputLogger_old(int elementSize, long logfreq, long query) {
        this.elementSize = elementSize;
        this.logfreq = logfreq;
        this.query = query;
    }

    @Override
    public void flatMap(T element, Collector<String> collector) throws Exception {
        if (!started) { //first run
            //header of result csv file:
            collector.collect("data ingestion rate: "+logfreq+ ", workers: " + getRuntimeContext().getNumberOfParallelSubtasks()
                + ", logging frequency: " + logfreq + " tuples");
            started = true;
            start_time = System.currentTimeMillis();
        }

        totalReceived++;
        if (logfreq > 0 && totalReceived % logfreq == 0) {
            // throughput over entire time
            long now = System.currentTimeMillis();

            // throughput for the last "logfreq" elements
            if (lastLogTimeMs == -1) {
                // init (the first)
                lastLogTimeMs = now;
                lastTotalReceived = totalReceived;

            } else {
                long timeDiff = now - lastLogTimeMs;
                long elementDiff = totalReceived - lastTotalReceived;
                double ex = (1000 / (double) timeDiff);
                String message = "Worker $" + getRuntimeContext().getIndexOfThisSubtask() + "$: Time elapsed: $" + timeDiff + "$ ms, elements/second: $" + (elementDiff * ex) + "$, MB/sec: $" + (elementDiff * ex * elementSize / 1024 / 1024) + "$";
                String mes = "Worker: $" + getRuntimeContext().getIndexOfThisSubtask() + "$: During the last $" + timeDiff + "$ ms, we received $" + elementDiff + "$ elements. That's $" + (elementDiff * ex) + "$ elements/second/core and $" + (elementDiff * ex * elementSize / 1024 / 1024) + "$ MB/sec/core. GB received $" + ((totalReceived * elementSize) / 1024 / 1024 / 1024) + "$ Query $" + query + "$";
                LOG.info(message);
                collector.collect(message);
                lastLogTimeMs = now;
                lastTotalReceived = totalReceived;
            }
        }
    }
}
