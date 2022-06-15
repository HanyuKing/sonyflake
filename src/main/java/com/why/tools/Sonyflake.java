package com.why.tools;

import net.jcip.annotations.ThreadSafe;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import static com.why.tools.Constant.*;

/**
 * @Author Hanyu.Wang
 * @Date 2022/6/14 17:22
 * @Description
 *  Sonyflake is a distributed unique ID generator.
 *  doc: https://github.com/sony/Sonyflake
 * @Version 1.0
 **/
@ThreadSafe
public class Sonyflake {
    private Lock mutex;
    private long startTime;
    private long elapsedTime;
    private long sequence;
    private long machineID;

    private Sonyflake() {
        throw new UnsupportedOperationException("please using Sonyflake(Setting setting) constructor method");
    }

    // NewSonyflake returns a new Sonyflake configured with the given Settings.
    // NewSonyflake returns nil in the following cases:
    // - Settings.StartTime is ahead of the current time.
    // - Settings.MachineID returns an error.
    // - Settings.CheckMachineID returns false.
    public Sonyflake(Setting setting) {
        this.mutex = new ReentrantLock();
        this.sequence = Long.parseUnsignedLong(Long.valueOf((1 << BitLenSequence) - 1).toString());

        if (setting.getStartTime() != null && setting.getStartTime().after(new Date())) {
            throw new RuntimeException("Sonyflake not created. StartTime is ahead of the current time");
        }
        if (setting.getStartTime() == null) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(2014, Calendar.SEPTEMBER, 1, 0, 0, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            this.startTime = SonyflakeUtil.toSonyflakeTime(calendar);
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(setting.getStartTime());
            this.startTime = SonyflakeUtil.toSonyflakeTime(calendar);
        }

        try {
            if (setting.getMachineID() == null) {
                this.machineID = SonyflakeUtil.lower16BitPrivateIP();
            } else {
                this.machineID = setting.getMachineID().get();
            }
        } catch (Exception e) {
            throw new RuntimeException("Sonyflake not created. get MachineID error", e);
        }

        if (setting.getCheckMachineID() != null && !setting.getCheckMachineID().apply(this.machineID)) {
            throw new RuntimeException("Sonyflake not created. CheckMachineID returns false");
        }
    }

    // NextID generates a next unique ID.
    // After the Sonyflake time overflows, NextID returns an error.
    public long nextID() {
        final long maskSequence = Integer.toUnsignedLong((1 << BitLenSequence) - 1);

        try {
            this.mutex.lock();

            long current = currentElapsedTime(this.startTime);

            if (this.elapsedTime < current) {
                this.elapsedTime = current;
                this.sequence = 0;
            } else { // sf.elapsedTime >= current
                this.sequence = (this.sequence + 1) & maskSequence;
                if (this.sequence == 0) {
                    this.elapsedTime++;
                    long overTime = this.elapsedTime - current;
                    LockSupport.parkNanos(overTime * 10 * (long) 1e6);
                }
            }

            return toID();
        } finally {
            this.mutex.unlock();
        }
    }

    private long currentElapsedTime(long startTime) {
        return SonyflakeUtil.toSonyflakeTime(Calendar.getInstance()) - startTime;
    }


    private long toID() {
        if (this.elapsedTime >= 1L << BitLenTime) {
            throw new RuntimeException("over the time limit");
        }
        return Long.parseUnsignedLong(Long.valueOf(this.elapsedTime).toString()) << (BitLenSequence + BitLenMachineID) |
                Long.parseUnsignedLong(Long.valueOf(this.sequence).toString()) << BitLenMachineID |
                Long.parseUnsignedLong(Long.valueOf(this.machineID).toString());
    }

}
