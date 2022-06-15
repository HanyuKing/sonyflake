import com.why.tools.Setting;
import com.why.tools.Sonyflake;
import com.why.tools.SonyflakeUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.*;
import java.util.concurrent.*;

import static com.why.tools.Constant.BitLenSequence;


/**
 * @Author Hanyu.Wang
 * @Date 2022/6/14 19:14
 * @Description
 * @Version 1.0
 **/
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SonyflakeTest {

    private Sonyflake sonyflake;
    private long startTime;
    private long machineID;

    @BeforeAll
    public void init() throws Exception {
        this.machineID = SonyflakeUtil.lower16BitPrivateIP();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        Date now = calendar.getTime();

        this.startTime = SonyflakeUtil.toSonyflakeTime(calendar);

        this.sonyflake = new Sonyflake(
                Setting.builder()
                        .checkMachineID(null)
                        .machineID(null) // () -> machineID
                        .startTime(now)
                        .build()
        );

    }

    public long nextID() {
        try {
            return this.sonyflake.nextID();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void sleep(long msec) {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        long start = System.currentTimeMillis();
        while (true) {
            if (start + msec <= System.currentTimeMillis()) {
                return;
            }
        }
    }

    @Test
    public void testGetOnce() {
        long sleepTime = 50;

        sleep( sleepTime * 10);

        long id = nextID();

        Map<String, Long> parts = SonyflakeUtil.Decompose(id);
        System.out.println(parts);

        long actualMSB = parts.get("msb");
        if (actualMSB != 0) {
            throw new RuntimeException(String.format("unexpected msb: %d", actualMSB));
        }

        long actualTime = parts.get("time");
        if (actualTime < sleepTime || actualTime > sleepTime+1) {
            throw new RuntimeException(String.format("unexpected time: %d", actualTime));
        }

        long actualSequence = parts.get("sequence");
        if (actualSequence != 0) {
            throw new RuntimeException(String.format("unexpected sequence: %d", actualSequence));
        }

        long actualMachineID = parts.get("machine-id");
        if (actualMachineID != machineID) {
            throw new RuntimeException(String.format("unexpected machine id: %d", actualMachineID));
        }

        System.out.println("Sonyflake id:" + id);
        System.out.println("decompose:" + parts);
    }

    public long currentTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        return SonyflakeUtil.toSonyflakeTime(calendar);
    }

    @Test
    public void TestSonyflakeFor10Sec() {
        long numID = 0;
        long lastID = 0;
        long maxSequence = 0;

        long initial = currentTime();
        long current = initial;

        while (current - initial < 1000) {
            long id = nextID();
            current = currentTime();
            numID++;

            Map<String, Long> parts = SonyflakeUtil.Decompose(id);

            if (id <= lastID) {
                throw new RuntimeException("duplicated id");
            }

            long actualMSB = parts.get("msb");
            if (actualMSB != 0) {
                throw new RuntimeException(String.format("unexpected msb: %d", actualMSB));
            }

            long actualTime = parts.get("time");
            long overtime = startTime + actualTime - current;
            if (overtime > 0) {
                throw new RuntimeException(String.format("unexpected overtime: %d", overtime));
            }

            long actualSequence = parts.get("sequence");
            if (maxSequence < actualSequence) {
                maxSequence = actualSequence;
            }

            long actualMachineID = parts.get("machine-id");
            if (actualMachineID != machineID) {
                throw new RuntimeException(String.format("unexpected machine id: %d", actualMachineID));
            }
        }

        if (maxSequence != (1 << BitLenSequence) - 1) {
            throw new RuntimeException(String.format("unexpected max sequence: %d", maxSequence));
        }

        System.out.println("max sequence:" + maxSequence);
        System.out.println("number of id:" + numID);
    }

    @Test
    public void TestSonyflakeInParallel() throws Exception {
        int nCPU = Runtime.getRuntime().availableProcessors();
        Map<Long, Object> map = new ConcurrentHashMap<>();
        Object NULL = new Object();

        int numID = 10000;

        List<Future> futures = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(nCPU);

        long start = System.currentTimeMillis();

        for (int i = 0; i < nCPU; i++) {
            futures.add(
                    executorService.submit(() -> {
                        for (int i1 = 0; i1 < numID; i1++) {
                            long id = sonyflake.nextID();
                            if (map.containsKey(Long.valueOf(id))) {
                                throw new RuntimeException("duplicated id");
                            }
                            map.put(Long.valueOf(id), NULL);
                        }
                    })
            );
        }

        for (Future future : futures) {
            future.get();
        }

        System.out.println("cost: " + (System.currentTimeMillis() - start));

        assert map.size() == nCPU * numID;
    }

    @Test
    public void TestSonyflakeInConcurrent() throws Exception {
        int nCPU = Runtime.getRuntime().availableProcessors();
        int nThread = nCPU * 4;

        Map<Long, Object> map = new ConcurrentHashMap<>();
        Object NULL = new Object();

        int numID = 10000;

        List<Future> futures = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(nThread);

        long start = System.currentTimeMillis();

        for (int i = 0; i < nThread; i++) {
            futures.add(
                    executorService.submit(() -> {
                        for (int i1 = 0; i1 < numID; i1++) {
                            long id = sonyflake.nextID();
                            if (map.containsKey(Long.valueOf(id))) {
                                throw new RuntimeException("duplicated id");
                            }
                            map.put(Long.valueOf(id), NULL);
                        }
                    })
            );
        }

        for (Future future : futures) {
            future.get();
        }

        System.out.println("cost: " + (System.currentTimeMillis() - start));

        assert map.size() == nThread * numID;
    }

    @Test
    public void TestSonyflakeFor1SecParallel() throws InterruptedException {
        Map<Long, Object> map = new ConcurrentHashMap<>();
        Object NULL = new Object();

        int nCPU = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(nCPU);

        for (int i = 0; i < nCPU; i++) {
            executorService.execute(() -> {
                while (true) {
                    long id = sonyflake.nextID();
                    if (map.containsKey(Long.valueOf(id))) {
                        throw new RuntimeException("duplicated id");
                    }
                    map.put(Long.valueOf(id), NULL);
                }
            });
        }

        sleep(1000);

        executorService.shutdownNow();

        // max: 2^8 * (1000 / 10)
        System.out.println("QPS: " + map.size());
    }

    @Test
    public void testAfterNowException() {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 1);

            Sonyflake Sonyflake = new Sonyflake(Setting.builder()
                    .startTime(calendar.getTime())
                    .build());

            Sonyflake.nextID();

            assert false;
        } catch (Exception e) {
            assert e instanceof RuntimeException
                    && e.getMessage().equals("Sonyflake not created. StartTime is ahead of the current time");
        }
    }

    @Test
    public void testGetMatchIDException() {
        try {
            Sonyflake Sonyflake = new Sonyflake(Setting.builder()
                    .machineID(() -> {
                        throw new RuntimeException("get matchID empty");
                    })
                    .build());

            Sonyflake.nextID();

            assert false;
        } catch (Exception e) {
            assert e instanceof RuntimeException
                    && e.getMessage().equals("Sonyflake not created. get MachineID error");
        }
    }

    @Test
    public void testDecompose() {
        long id = 392297063510972868L;
        Map<String, Long> parts = SonyflakeUtil.Decompose(id);
        assert Long.valueOf(23382727117L).equals(parts.getOrDefault("time", null));
        // 23382727117 is go Decompose result
    }

    @Test
    public void testGetNextID() {
        Sonyflake Sonyflake = new Sonyflake(Setting.builder().build());
        long id = Sonyflake.nextID();
        System.out.println(id);
        System.out.println(SonyflakeUtil.Decompose(id));
    }
}
