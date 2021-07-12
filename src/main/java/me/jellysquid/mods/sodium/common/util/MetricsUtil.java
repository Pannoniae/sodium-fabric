package me.jellysquid.mods.sodium.common.util;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@NotThreadSafe
@Log4j2
public class MetricsUtil {

    private static final Map<String, Triple<Float, Long, Long>> METRICS = new HashMap<>();

    public static void startMetric(String name) {
        if (METRICS.containsKey(name)) {
            Triple<Float, Long, Long> t = METRICS.get(name);
            METRICS.put(name, Triple.of(t.getLeft(), t.getMiddle(), System.currentTimeMillis()));
        } else {
            METRICS.put(name, Triple.of(0f, 0L, System.currentTimeMillis()));
        }
    }

    public static void endMetric(String name) {
        Triple<Float, Long, Long> t = METRICS.get(name);
        if (t == null)
            throw new RuntimeException("Metrics needs to be started first!");
        float f = t.getLeft();
        long m = t.getMiddle();
        long l = t.getRight();
        if (l < 0)
            throw new RuntimeException("Metrics needs to be started first!");
        f = ((f * m) + (float)(System.currentTimeMillis() - l))/(m+1);
        METRICS.put(name, Triple.of(f, m + 1, -1L));
    }

    public static void logMetrics() {
        for (Entry<String, Triple<Float, Long, Long>> entry : METRICS.entrySet()) {
            Triple<Float, Long, Long> t = entry.getValue();
            log.info("Metric {} latency={} ms, count={}", entry.getKey(), t.getLeft(), t.getMiddle());
        }
    }
}
