package com.why.tools;

import java.net.SocketException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static com.why.tools.Constant.*;

/**
 * @Author Hanyu.Wang
 * @Date 2022/6/15 11:38
 * @Description
 * @Version 1.0
 **/
public class SonyflakeUtil {

    // Decompose returns a set of Sonyflake ID parts.
    public static Map<String, Long> Decompose(long id) {
        final long maskSequence = Long.parseUnsignedLong(Long.valueOf(((1 << BitLenSequence) - 1) << BitLenMachineID).toString());
        final long maskMachineID = Long.parseUnsignedLong(Long.valueOf((1 << BitLenMachineID) - 1).toString());
        long msb = id >> 63;
        long time = id >> (BitLenSequence + BitLenMachineID);
        long sequence = (id & maskSequence) >> BitLenMachineID;
        long machineID = id & maskMachineID;
        Map<String, Long> result = new HashMap<>();
        result.put("id", id);
        result.put("msb", msb);
        result.put("time", time);
        result.put("sequence", sequence);
        result.put("machine-id", machineID);
        return result;
    }

    public static long lower16BitPrivateIP() throws SocketException {
        String ip = NetUtil.privateIPv4();
        String[] ips = ip.split("\\.");
        int ip2 = Integer.valueOf(ips[2]);
        int ip3 = Integer.valueOf(ips[3]);
        return Long.parseUnsignedLong(Long.valueOf(ip2 << 8).toString())
                + Long.parseUnsignedLong(Long.valueOf(ip3).toString());
    }

    public static long toSonyflakeTime(Calendar calendar) {
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        return calendar.getTime().getTime() * (long) 1e6 / SonyflakeTimeUnit;
    }

}
