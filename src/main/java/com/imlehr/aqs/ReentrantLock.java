package com.imlehr.aqs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * @author Lehr
 * @create: 2020-09-19
 */
public class ReentrantLock implements Lock{

    private final Sync sync;

    public ReentrantLock() {
        this.sync = new ReentrantLock.NonfairSync();
    }

    public ReentrantLock(boolean fair) {
        this.sync = (ReentrantLock.Sync)(fair ? new ReentrantLock.FairSync() : new NonfairSync());
    }

    @Override
    public void lock() {
        sync.acquire(1);
    }

    /**
     * tryLock就是似乎无论你sync是公平还是不公平，都是直接走非公平？？？？
     * @return
     */
    @Override
    public boolean tryLock() {
        return this.sync.nonfairTryAcquire(1);
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        return this.sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    @Override
    public void unlock() {
        sync.release(1);
    }

    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }


    static final class FairSync extends ReentrantLock.Sync {

        /**
         * 这个是个公平的实现
         * @param acquires
         * @return
         */
        @Override
        protected final boolean tryAcquire(int acquires) {
            Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (!hasQueuedPredecessors() && compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            } else if (current == this.getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) {
                    throw new Error("Maximum lock count exceeded");
                }

                this.setState(nextc);
                return true;
            }

            return false;
        }
    }

    static final class NonfairSync extends ReentrantLock.Sync {

        /**
         * 默认走不公平的
         * @param acquires
         * @return
         */
        @Override
        protected final boolean tryAcquire(int acquires) {
            return this.nonfairTryAcquire(acquires);
        }
    }


    abstract static class Sync extends AbstractQueuedSynchronizer {

        /**
         * 给提供一个不公平的方法
         * @param acquires
         * @return
         */
        final boolean nonfairTryAcquire(int acquires) {
            Thread current = Thread.currentThread();
            int c = getState();
            //如果没有人入锁，就直接进入，有点类似直接偏向锁的味道
            if (c == 0) {
                if (compareAndSetState(0, acquires)) {
                    //然后现在已经线程安全了
                    //设置当前持锁队线程
                    setExclusiveOwnerThread(current);
                    return true;
                }
                //如果是拿到了锁的这个线程第二次重入，则重新计数
                //这个时候就不会有多线程竞争了，只有他自己在重入，所以不需要用cas
            } else if (current == getExclusiveOwnerThread()) {
                //统计新的重入数量
                int nextc = c + acquires;
                //溢出的情况处理
                if (nextc < 0) {
                    throw new Error("Maximum lock count exceeded");
                }
                //CAS重新设置数量
                setState(nextc);
                return true;
            }
            //拿锁失败，准备去排队咯  如果是tryLock这里就直接返回false了
            return false;
        }

        /**
         * 原本是有@ReservedStackAccess注解，用于保护被注解的方法，通过添加一些额外的空间，防止在多线程运行的时候出现栈溢出
         * 但是这个只有核心类的地方的类加载器才会给你用，所以这里就不加了
         * @param releases
         * @return
         */
        @Override
        protected final boolean tryRelease(int releases) {
            //按理说放锁的时候也应该是线程安全的
            //检查释放后的state数量
            int c = getState() - releases;
            //如果是不该释放锁的线程干的，则报错
            if (Thread.currentThread() != getExclusiveOwnerThread()) {
                throw new IllegalMonitorStateException();
            } else {
                boolean free = false;
                if (c == 0) {
                    free = true;
                    //自由了！
                    setExclusiveOwnerThread(null);
                }
                setState(c);
                return free;
            }
        }

        public Condition newCondition() {
            //源码这里传入了个this搞不懂是什么操作....
            return new ConditionObject();
        }


    }


}
