package util;

import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThroughputLogger<T> extends RichFlatMapFunction<T, String> {

    private static final Logger LOG = LoggerFactory.getLogger(ThroughputLogger.class);

    private long totalReceived = 0;
    private long lastTotalReceived = 0;
    private long lastLogTimeMs = -1;
    private final int elementSize;
    private String name;
    private Integer velFilter;
    private Integer quaFilter;
    private Integer windowSize;
    private long throughput;

    private Integer iter;

    private Integer fileLoops;
    private Integer sensors;
    private long query = 0;

    private static boolean started = false;
    private static long start_time;


    public ThroughputLogger(int elementSize, long throughput) {
        this.elementSize = elementSize;
        this.throughput = throughput;
    }


    public ThroughputLogger(int elementSize, long throughput, long query) {
        this(elementSize, throughput);
        this.query = query;
    }

    public ThroughputLogger(int elementSize, String className, Integer velFilter, Integer quaFilter, Integer windowSize, long throughput,
        Integer fileLoops) {
        this.elementSize = elementSize;
        this.name = className;
        this.velFilter = velFilter;
        this.quaFilter = quaFilter;
        this.windowSize = windowSize;
        this.throughput = throughput;
        this.fileLoops = fileLoops;
    }

    public ThroughputLogger(int elementSize, String className, Integer velFilter, Integer windowSize, long throughput,
        Integer fileLoops, int iter) {
        this(elementSize, className, velFilter, null, windowSize, throughput, fileLoops);
        this.iter = iter;
    }

  public ThroughputLogger(int elementSize, String className, Integer velFilter, Integer quaFilter, Integer windowSize, long throughput,
      Integer fileLoops, Integer sensors) {
      this(elementSize, className, velFilter, quaFilter, windowSize, throughput, fileLoops);
      this.sensors = sensors;
  }

    public ThroughputLogger(int elementSize, String className, Integer velFilter, Integer windowSize, long throughput,
        Integer fileLoops, Integer iter, Integer sensors) {
        this(elementSize, className, velFilter, windowSize, throughput, fileLoops, iter);
        this.sensors = sensors;
    }

  @Override
    public void flatMap(T element, Collector<String> collector) throws Exception {
        if (!started) { //first run
            //header of result csv file:
            String settings = "$" + name + "$ - file loops: $" + fileLoops + "$, velThreshold: $" +
                velFilter + "$, quaThreshold: $" + quaFilter + "$, windowSize: $" + windowSize + "$, iter: $" + iter +"$";
            String message = "throughput: $"+throughput+ "$, workers: $"+getRuntimeContext().getNumberOfParallelSubtasks() + "$, logging frequency: $1$ second";
            LOG.info(settings);
            LOG.info(message);
            if(this.sensors != null) {
                LOG.info("sensors: $" + sensors + "$");
            }
            collector.collect(message);
            started = true;
            start_time = System.currentTimeMillis();
        }

        totalReceived++;
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastLogTimeMs;
        if (timeDiff >= 1000) {
            if (lastLogTimeMs == -1) { //init
                lastLogTimeMs = System.currentTimeMillis();
                lastTotalReceived = totalReceived;
            } else {
                long elementDiff = totalReceived - lastTotalReceived;
                double mb_sec = elementDiff * elementSize / 1024.0 / 1024.0;
                String message = "Worker $" + getRuntimeContext().getIndexOfThisSubtask() +
                    "$, elements/second: $" + elementDiff +
                    "$, MB/sec: $" + mb_sec + "$";
                LOG.info(message);
                collector.collect(message);
                lastLogTimeMs = currentTime;
                lastTotalReceived = totalReceived;
            }
        }
    }
}
