package com.imlehr.aqs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * @author Lehr
 * @create: 2020-09-19
 */
public interface Lock {

    void lock();

    void unlock();

    boolean tryLock();

    boolean tryLock(long var1, TimeUnit var3) throws InterruptedException;


    Condition newCondition();


}
