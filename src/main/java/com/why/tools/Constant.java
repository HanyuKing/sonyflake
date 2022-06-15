package com.why.tools;

/**
 * @Author Hanyu.Wang
 * @Date 2022/6/14 17:22
 * @Description
 *      These constants are the bit lengths of Sonyflake ID parts.
 * @Version 1.0
 **/
public class Constant {
    public static final int BitLenTime      = 39;                               // bit length of time
    public static final int BitLenSequence  = 8;                                // bit length of sequence number
    public static final int BitLenMachineID = 63 - BitLenTime - BitLenSequence; // bit length of machine id
    public static final long SonyflakeTimeUnit = 10 * 1000 * 1000; // nsec, i.e. 10 msec
}
