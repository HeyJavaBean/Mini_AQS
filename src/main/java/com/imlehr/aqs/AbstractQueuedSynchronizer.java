package com.imlehr.aqs;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Lehr
 * @create: 2020-09-29
 * https://tech.meituan.com/2019/12/05/aqs-theory-and-apply.html
 */
public abstract class AbstractQueuedSynchronizer {

    private AtomicInteger state = new AtomicInteger(0);

    /**
     * 实际上是exclusiveOnwerThread是在更高一层的抽象里的
     * AbstractOwnableSynchronizer
     */
    private Thread exclusiveOwnerThread;

    protected final void setExclusiveOwnerThread(Thread thread) {
        this.exclusiveOwnerThread = thread;
    }

    protected final Thread getExclusiveOwnerThread() {
        return this.exclusiveOwnerThread;
    }


    private AtomicReference<Node> head = new AtomicReference<>(null);

    private AtomicReference<Node> tail = new AtomicReference<>(null);

    protected final int getState() {
        return state.get();
    }

    protected final void setState(int newState) {
        this.state.set(newState);
    }

    protected final boolean compareAndSetState(int expect, int update) {
        return state.compareAndSet(expect, update);
    }


    /**
     * 整个一套的获取流程
     * 尝试获取->获取不到之后的失败处理：添加节点排队，在队列里尝试获取
     *
     * @param arg
     */
    public void acquire(Integer arg) {

        //tryAcquire是试图获取锁，获取到了直接结束流程
        //如果没有获取到，则先把这个节点放入到等待队列里去，如果没有等待队列就去创建
        //然后试图执行acuireQueued获取锁？？
        if (!tryAcquire(arg)) {
            acquireQueued(addWaiter(), arg);
        }
    }


    /**
     * 尝试去获取，拿得到就返回true，拿不到就返回false，至于你后续阻塞怎么处理又是另外一回事了
     *
     * @param arg
     * @return
     */
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }


    /**
     * 在一段时间内尝试获取
     *
     * @param arg
     * @param nanosTimeout
     * @return
     * @throws InterruptedException
     */
    public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        //todo 尝试拿锁，公平就公平，不公平就不公平
        //如果try不到，就超时try
        return this.tryAcquire(arg) || this.doAcquireNanos(arg, nanosTimeout);
    }

    //todo 未完待续
    private boolean doAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (nanosTimeout <= 0L) {
            return false;
        } else {
            //设置一个等待时间
            long deadline = System.nanoTime() + nanosTimeout;
            //创建一个节点并插入到队列里去
            Node node = this.addWaiter();

            for (; ; ) {
                //检查他前一个节点
                Node p = node.prev;
                //如果他前一个节点是空头结点，那么他就可以走tryAcquire的流程了
                if (p == this.head.get() && this.tryAcquire(arg)) {
                    //如果获取成功了，那么接下来的部分应该是线程安全了的
                    //当前node变成头节点
                    node.thread = null;
                    head.set(node);
                    //把前面那个空头节点的关联取消了使得其会被gc
                    p.next = null;
                    return true;
                }

                //检查剩余时间
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L) {
                    //如果时间到了，那么就不用尝试了直接失败
                    this.cancelAcquire(node);
                    return false;
                }

                //park一段时间 醒了之后再去查看
                if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout > 1000L) {
                    LockSupport.parkNanos(this, nanosTimeout);
                }
            }
        }
    }

    /**
     * 把一个排队的节点ban出去取消掉
     *
     * @param node
     */
    private void cancelAcquire(Node node) {
        //todo 未完待续
//
//        if (node != null) {
//            //线程清空？为啥
//            node.thread = null;
//
//            Node pred;
//            //从这个地方向前找  如果前面节点也是取消状态的，那么这里的大概就是一整条取消，因为估计是考虑多次try并发
//            for(pred = node.prev; pred.waitStatus.get() > 0; node.prev = pred = pred.prev) {
//
//            }
//
//            Node predNext = pred.next;
//
//            node.waitStatus.set(1);
//
//            if (node == tail.get() && compareAndSetTail(node, pred)) {
//                pred.compareAndSetNext(predNext, (Node)null);
//            } else {
//                int ws;
//                if (pred != this.head && ((ws = pred.waitStatus) == -1 || ws <= 0 && pred.compareAndSetWaitStatus(ws, -1)) && pred.thread != null) {
//                    java.util.concurrent.locks.AbstractQueuedSynchronizer.Node next = node.next;
//                    if (next != null && next.waitStatus <= 0) {
//                        pred.compareAndSetNext(predNext, next);
//                    }
//                } else {
//                    this.unparkSuccessor(node);
//                }
//
//                node.next = node;
//            }

    }


    /**
     * 创建一个节点并插入到队列里去
     *
     * @return
     */
    private Node addWaiter() {
        //创建一个节点记录当前线程的情况
        Node node = new Node();
        //代表之前的尾节点
        Node oldTail;
        //进行插入 and 特定位置进行轮询
        //接下来这段会是并发操作
        do {
            while (true) {
                //指向当前的尾节点
                oldTail = tail.get();
                //如果尾节点不是空的（有任务在执行了需要排队的情况）
                if (oldTail != null) {
                    //设置好两个节点的前后关系，我的理解是这样的
                    //目前node是你新加的节点，oldTail是你现在取得到的最后的节点
                    //node的上一个就是默认的这个最后的，挂好
                    node.prev = oldTail;
                    break;
                }
                //如果尾节点是空的，则代表需要初始化这个等待队列
                //如果头节点是空的，则cas去设置他
                if (head.compareAndSet(null, new Node())) {
                    //首位节点都指向新节点，感觉这个类似一个哨兵节点
                    tail.set(head.get());
                }
            }
            //把尾节点标记为新的这个尾部
            //如果现在尾节点还是之前取得的oldTail的值，那么我们就把尾节点替换为node节点
        } while (!tail.compareAndSet(oldTail, node));
        //现在node是最后一个节点了，oldTail不是最后的了，所以下一个就是node这个了
        //所以这一行代码才是放到队列里去
        oldTail.next = node;
        return node;
    }


    private void acquireQueued(Node node, Integer acquires) {
        for (; ; ) {
            //获取前一个节点(源码还考虑了前一个是空的情况，我懒得了，省略了)
            Node p = node.prev;
            //如果前一个节点是头节点，说明没有人排队了，则继续试图去tryAcquire试图获取锁
            if (p == this.head.get() && this.tryAcquire(acquires)) {
                //如果获取成功了，那么接下来的部分应该是线程安全了的
                //当前node变成头节点
                node.thread = null;
                head.set(node);
                //把前面那个空头节点的关联取消了使得其会被gc
                p.next = null;
                return;
            }

            //如果前一个节点不是头结点，则可能需要sleep然后去被挂起，按理说是首部节点两次cas然后才wait的，我这里就直接sleep了得了
            if (shouldParkAfterFailedAcquire(p, node)) {
                LockSupport.park();
                //被唤醒之后继续执行，直到离开，我没考虑interrupt的情况
            }
        }
    }

    private boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus.get();
        if (ws == -1) {
            return true;
        } else {
            //可以自旋一次，旋不起就算了？
            pred.waitStatus.compareAndSet(ws, -1);
            return false;
        }
    }


    public boolean release(Integer arg) {
        //尝试释放锁
        if (tryRelease(arg)) {
            //如果成功了，唤醒后继线程
            Node h = head.get();
            if (h != null) {
                unparkSuccessor(h);
            }
            return true;
        } else {
            return false;
        }
    }

    private void unparkSuccessor(Node node) {

        //拿到真正的第一个等待的节点
        Node s = node.next;
        //这里压缩了很多内容
        if (s != null) {
            LockSupport.unpark(s.thread);
        }
    }


    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }


    /**
     * 直接暴力释放
     */
    final int fullyRelease(Node node) {

        int savedState = state.get();

        if (release(savedState)) {
            return savedState;
        } else {
            throw new IllegalMonitorStateException();
        }
    }


    /**
     * 一个检查前面是否有人排队的方法
     * TODO 这段代码还没有读通顺
     *
     * @return
     */
    public final boolean hasQueuedPredecessors() {
        Node h;
        if ((h = this.head.get()) != null) {
            Node s;
            if ((s = h.next) == null || s.waitStatus.get() > 0) {
                s = null;

                for (Node p = this.tail.get(); p != h && p != null; p = p.prev) {
                    if (p.waitStatus.get() <= 0) {
                        s = p;
                    }
                }
            }
            if (s != null && s.thread != Thread.currentThread()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @author Lehr
     * @create: 2020-09-19
     */
    static final class Node {


        public volatile Node prev;
        public volatile Node next;

        //由于超时或中断，节点已被取消
        static final int CANCELLED = 1;
        //表示下一个节点是通过park堵塞的，需要通过unpark唤醒
        static final int SIGNAL = -1;
        //表示线程在等待条件变量（先获取锁，加入到条件等待队列，然后释放锁，等待条件变量满足条件；只有重新获取锁之后才能返回）
        static final int CONDITION = -2;
        //表示后续结点会传播唤醒的操作，共享模式下起作用
        static final int PROPAGATE = -3;

        public AtomicInteger waitStatus = new AtomicInteger(0);


        //其实我觉得nextWaiter和next一起用也可以啊？反正我看源码分开写了我也分开写了
        //豁然开朗：原来这个是在IsOnSyncQueue的那个方法做快速检测做的
        Node nextWaiter;

        public Thread thread;

        public Node() {
            thread = Thread.currentThread();
        }

        Node(Thread thread, int waitStatus) { // Used by Condition
            this.waitStatus.set(waitStatus);
            this.thread = thread;
        }

    }


    public class ConditionObject implements Condition {

        /**
         * 用来维持队列的
         */
        private Node firstWaiter;
        /**
         * Last node of condition queue.
         */
        private Node lastWaiter;


        /**
         * Moves the longest-waiting thread, if one exists, from the
         * wait queue for this condition to the wait queue for the
         * owning lock.
         * 唤醒在Condition里等待最长的节点也就是首节点
         * 然后把这个节点在唤醒之前移动到同步队列
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *                                      returns {@code false}
         */
        @Override
        public final void signal() {
            //检查当前线程是否获取到了锁，而await是在fullyRelease里处理的
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            //找到第一个等待的
            Node first = firstWaiter;
            //如果不是空的，激活！
            if (first != null) {
                doSignal(first);
            }

        }

        //todo https://juejin.im/post/6844903602419400718#heading-3
        private void doSignal(Node first) {
            //这个地方按理说应该也不会有并发问题？
            do {
                //如果没人了，清空队列  头元素变成下一个！
                if ((firstWaiter = first.nextWaiter) == null) {
                    lastWaiter = null;
                }

                //移出condition队列，则去掉nextWaiter的这个引用标记！！！
                first.nextWaiter = null;

            } while (!transferForSignal(first) &&
                    (first = firstWaiter) != null);
        }


        final boolean transferForSignal(Node node) {
            /*
             * If cannot change waitStatus, the node has been cancelled.
             */
            if (!node.waitStatus.compareAndSet(Node.CONDITION, 0)) {
                return false;
            }


            /*
             * Splice onto queue and try to set waitStatus of predecessor to
             * indicate that thread is (probably) waiting. If cancelled or
             * attempt to set waitStatus fails, wake up to resync (in which
             * case the waitStatus can be transiently and harmlessly wrong).
             */
            Node p = enq(node);

            int ws = p.waitStatus.get();

            if (ws > 0 || !p.waitStatus.compareAndSet(ws, Node.SIGNAL)) {
                LockSupport.unpark(node.thread);
            }

            return true;
        }

        private Node enq(final Node node) {
            for (; ; ) {
                Node t = tail.get();
                if (t == null) { // Must initialize
                    if (head.compareAndSet(head.get(), new Node())) {
                        tail = head;
                    }
                } else {
                    node.prev = t;
                    if (tail.compareAndSet(t, node)) {
                        t.next = node;
                        return t;
                    }
                }
            }
        }


        public boolean isHeldExclusively() {
            return exclusiveOwnerThread == Thread.currentThread();
        }


        /**
         * 大概意思也就是，这个应该是同步队列里虚拟的首个位置的那个
         * 我们要把他放入到Conditon的阻塞队列里来，然后同步队列里唤醒下一个Node
         * 这个过程是线程安全的，因为是必须先Lock.lock获得了锁才能执行的
         * 不然会报错的
         *
         * @throws InterruptedException
         */
        @Override
        public final void await() throws InterruptedException {

            //现在我们已经把这个node放到了condition队列的尾部了
            Node node = addConditionWaiter();

            //获取到之前加锁的state，释放锁，这个时候会唤醒同步队列的下一个节点
            int savedState = fullyRelease(node);

            //检查这个节点是否被移到了同步队列里，这才是出口
            while (!isOnSyncQueue(node)) {
                //似乎这个blocker就只是用来排错的...
                LockSupport.park(this);
            }

            //已经到了同步队列，于是乎就去尝试获取锁，之前aqs这套操作
            acquireQueued(node, savedState);
        }

        /**
         * 检查这个节点是否被移到了同步队列里
         */
        final boolean isOnSyncQueue(Node node) {
            //如果还在condition队列里，爆炸
            if (node.waitStatus.get() == Node.CONDITION || node.prev == null) {
                return false;
            }

            // If has successor, it must be on queue
            if (node.next != null) {
                return true;
            }

            /*
             * node.prev can be non-null, but not yet on queue because
             * the CAS to place it on queue can fail. So we have to
             * traverse from tail to make sure it actually made it.  It
             * will always be near the tail in calls to this method, and
             * unless the CAS failed (which is unlikely), it will be
             * there, so we hardly ever traverse much.
             */
            return findNodeFromTail(node);
        }

        /**
         * Returns true if node is on sync queue by searching backwards from tail.
         * Called only when needed by isOnSyncQueue.
         *
         * @return true if present
         * 从同步队列的队尾开始遍历寻找
         */
        private boolean findNodeFromTail(Node node) {
            Node t = tail.get();
            for (; ; ) {
                if (t == node) {
                    return true;
                }

                if (t == null) {
                    return false;
                }

                t = t.prev;
            }
        }


        /**
         * 因为同步队列里的首个节点实际上并不存在，所以这里我们直接new一个节点表示当前的线程
         * 然后加入到conditon的队列里就好了
         * 这里就简单设计了，不考虑中断和cancelled的情况
         * 比方说cancelled机制，设置为取消之后就是后面有人排队然后把他给清理了，这个我就没写了
         * <p>
         * 和同步队列不太一样的地方就是，他没有头结点，而同步队列是有个空头节点
         */
        private Node addConditionWaiter() {
            Node t = lastWaiter;

            //创建一个新节点，状态代表是在条件变量里被阻塞了的
            Node node = new Node(Thread.currentThread(), Node.CONDITION);

            //如果没有人在这里排队
            if (t == null) {
                //那么第一个节点就是他了（不用考虑线程安全，因为有Lock保护）
                firstWaiter = node;
            } else {
                //要不然，他就是最后一个
                t.nextWaiter = node;
            }

            //记录最后一个节点指针变换
            lastWaiter = node;
            //done!
            return node;
        }


        @Override
        public void awaitUninterruptibly() {

        }

        @Override
        public long awaitNanos(long l) throws InterruptedException {
            return 0;
        }

        @Override
        public boolean await(long l, TimeUnit timeUnit) throws InterruptedException {
            return false;
        }

        @Override
        public boolean awaitUntil(Date date) throws InterruptedException {
            return false;
        }

        @Override
        public void signalAll() {

        }
    }


}
