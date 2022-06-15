package com.why.tools;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @Author Hanyu.Wang
 * @Date 2022/6/14 17:25
 * @Description
 *  Settings configures Sonyflake:
 *  StartTime is the time since which the Sonyflake time is defined as the elapsed time.
 *  If StartTime is 0, the start time of the Sonyflake is set to "2014-09-01 00:00:00 +0000 UTC".
 *  If StartTime is ahead of the current time, Sonyflake is not created.
 * 
 *  MachineID returns the unique ID of the Sonyflake instance.
 *  If MachineID returns an error, Sonyflake is not created.
 *  If MachineID is nil, default MachineID is used.
 *  Default MachineID returns the lower 16 bits of the private IP address.
 * 
 *  CheckMachineID validates the uniqueness of the machine ID.
 *  If CheckMachineID returns false, Sonyflake is not created.
 *  If CheckMachineID is nil, no validation is done.
 *
 * @Version 1.0
 **/
@Data
@Builder
public class Setting {
    private Date startTime;
    private Supplier<Long> machineID;
    private Function<Long, Boolean> checkMachineID;
}
