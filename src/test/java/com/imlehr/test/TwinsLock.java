package com.imlehr.test;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * @author Lehr
 * @create: 2020-09-28
 */
public class TwinsLock{

    private final Sync sync = new Sync(2);


    public void lock()
    {
        sync.acquireShared(1);
    }

    public void unlock()
    {
        sync.releaseShared(1);
    }


    private static final class Sync extends AbstractQueuedSynchronizer{

        Sync(int count)
        {
            setState(count);
        }

        @Override
        public int tryAcquireShared(int reduceCount)
        {
            for(;;)
            {
                int current = getState();
                int newCount = current - reduceCount;
                if(newCount<0||compareAndSetState(current,newCount))
                {
                    return newCount;
                }
            }
        }

        @Override
        public boolean tryReleaseShared(int returnCount)
        {
            for(;;)
            {
                int current = getState();
                int newCount = current + returnCount;
                if(compareAndSetState(current,newCount))
                {
                    return true;
                }
            }


        }



    }


}
