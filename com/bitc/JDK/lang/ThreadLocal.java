package com.bitc.JDK.lang;
/**
 * 名词翻译：
 * fields ：字段
 * thread-local variable ： 线程本地变量
 * linear-probe hash maps ：线性探测hash map
 * Expunge ： 清除，抹去
 * <p>
 * 未翻译(国内翻译未统一，提供参考翻译):
 * entry：实体、条目
 * hash maps : 哈希映射
 * stale entries ：陈旧的条目、过期的实体
 * <p>
 * 部分不重要：未翻译，保留英文注解
 */

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 这个类用来提供线程本地变量[thread-local variables]。
 * 这些变量与正常变量的不同之处在于：访问一个ThreadLocal中的变量（通过其get或set方法），
 * 每个线程都有自己独立初始化的变量副本。ThreadLocal实例通常是那些希望将状态
 * 与线程相关联起来的私有静态字段[private static fields]（例如，用户ID或事务ID）。
 *
 * 比如下面这个类就是用来为每一个线程设计都设计一个唯一的本地线程标识符。
 * 线程的id在第一次调用ThreadId.get()方法的时候进行分配（assigned）
 *
 * import java.util.concurrent.atomic.AtomicInteger;
 *
 * public class ThreadId {
 *      // 用来存储下一个线程ID的原子整数
 *      private static final AtomicInteger nextId = new AtomicInteger(0);
 *
 *      // 包含每个线程ID的线程本地变量
 *      private static final ThreadLocal<Integer> threadId =
 *          new ThreadLocal<Integer>() {
 *          @Override
 *          protected Integer initialValue() {
 *              return nextId.getAndIncrement();
 *          }
 *      };
 *
 *      // 在需要时分配并返回当前线程的唯一ID
 *      public static int get() {
 *          return threadId.get();
 *      }
 * }
 *
 * ThreadLocal的大体流程：
 * 使用时可以重写initialValue()方法，当线程进行get调用时，获取当前线程对象的threadLocals
 * 这个成员变量（类型为ThreadLocal.ThreadLocalMap，内部是一个继承了WeakReference
 * <ThreadLocal<?>>的Entry，可以存当前ThreadLocal引用和对应的值），获取之后进行一个是否为null的判断，
 * Thread类中默认为null，写入或者更新通过initialValue()确定的值。
 * 当线程需要取出线程本地变量的时候，还是从Thread实例对象中取出之前赋好值的threadLocals，在从ThreadLocalMap.Entry
 * 中取出实际对应的值。因此ThreadLocal实际上是不存值的。
 *
 * 只要线程处于活跃状态（alive）并且ThreadLocal实例可以访问，那么每个线程都会
 * 持有着一个线程本地变量副本的隐式副本。线程消失之后，线程本地实例的所有相关副本
 * 都会进行垃圾回收（除非存在对这些副本的其他引用）
 * 注：因此虽然ThreadLocal它的底层存储结构(ThreadLocalMap)是弱引用(Entry extends WeakReference<java.ThreadLocal<?>>)
 * ，但是并不是这个弱引用的值一被置为null就会被回收，因为存活着的那个线程对它还有一个强引用
 * (ThreadLocal.ThreadLocalMap threadLocals = Entry(ThreadLocal<?> k, null);)
 */
public class ThreadLocal<T> {
    /**
     * ThreadLocals的实现依赖于附加在每个线程上的线性探测哈希map（Thread.threadLocals和
     * inheritableThreadLocals  注：inheritableThreadLocals在InheritableThreadLocal这个类中会详细说明）。
     * ThreadLocal对象充当键，通过threadLocalHashCode搜索。这是一个自定义哈希代码（仅在ThreadLocalMaps中有用），
     * 它消除（eliminates）了在同一个线程中使用连续构造的ThreadLocals等常见情况下的冲突（collisions），同时
     * 在不太常见的情况下保持良好行为。
     */
    private final int threadLocalHashCode = nextHashCode();

    /**
     * 下一个hash码，原子更新，由0开始
     */
    private static AtomicInteger nextHashCode =
            new AtomicInteger();

    /**
     * The difference between successively generated hash codes - turns
     * implicit sequential thread-local IDs into near-optimally spread
     * multiplicative hash values for power-of-two-sized tables.
     *
     * ？？
     * 用于显示连续生成的哈希码之间的差异 - 将隐式顺序线程本地ID转换为近似最优扩展乘法哈希值，用于两个幂大小的表。
     */
    private static final int HASH_INCREMENT = 0x61c88647;

    /**
     * Returns the next hash code.
     */
    private static int nextHashCode() {
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }

    /**
     * 返回此thread-local变量的当前线程的“初始值”。
     * 在线程使用get方法访问变量的第一个值时，将调用此方法。如果该线程之前调用了set方法，那么在
     * 这种情况下initialValue方法将不会为该线程调用。
     * 通常，每个线程最多调用一次此方法，但如果后面调用了remove方法，然后调用get方法，则可以再次调用此方法。
     * 此实现只返回null;
     *
     * 如果程序员希望线程本地变量具有除null之外的初始值，则ThreadLocal必须被子类化，并且覆盖此方法。
     *
     * 通常使用匿名内部类。
     * 返回此线程本地的初始值，默认为null
     */
    protected T initialValue() {
        return null;
    }

    /**
     * 创建一个线程局部变量。通过在Supplier上调用get方法确定变量的初始值。
     */
    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedThreadLocal<>(supplier);
    }

    /**
     * 创建一个线程局部变量
     */
    public ThreadLocal() {
    }

    /**
     * 返回此线程本地变量的当前线程副本中的值。
     * 如果变量没有当前线程的值，则首先通过调用initialValue方法将其初始化为返回值。
     */
    public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked") T result = (T) e.value;
                return result;
            }
        }
        return setInitialValue();
    }

    /**
     * set()的另一种实现形式，用来初始化值。如果用户重写了set()方法，那么就使用set()方法
     */
    private T setInitialValue() {
        // value默认为null
        T value = initialValue();
        Thread t = Thread.currentThread();
        // 从线程Thread中获取ThreadLocal.ThreadLocalMap这个属性，默认为null的
        ThreadLocalMap map = getMap(t);
        // 如果得到的ThreadLocalMap不为null，说明已经由ThreadLocal对其进行过赋值，
        // 直接将当前对象的引用和初始化值添加进去
        if (map != null)
            map.set(this, value);
            // 如果为null，说明未进行赋值，则创建一个与ThreadLocal相关联的map。
        else
            // createMap(t, value) -> t.threadLocals = new ThreadLocalMap(this, firstValue);
            createMap(t, value);
        return value;
    }

    /**
     * 将此线程本地变量的当前线程副本设置为指定值。和setInitialValue()主要区别就是初始化值的不同
     * 大多数子类都不需要覆盖此方法，仅依靠initialValue方法来设置线程本地的值。
     */
    public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
    }

    /**
     * Removes the current thread's value for this thread-local
     * variable.  If this thread-local variable is subsequently
     * {@linkplain #get read} by the current thread, its value will be
     * reinitialized by invoking its {@link #initialValue} method,
     * unless its value is {@linkplain #set set} by the current thread
     * in the interim.  This may result in multiple invocations of the
     * {@code initialValue} method in the current thread.
     *
     * @since 1.5
     */
    public void remove() {
        ThreadLocalMap m = getMap(Thread.currentThread());
        if (m != null)
            m.remove(this);
    }

    /**
     * 获取与ThreadLocal关联的地图。在InheritableThreadLocal中重写。
     *
     * t.threadLocals这个值时Thread类中的一个类属性->ThreadLocal.ThreadLocalMap threadLocals = null
     * 表示与此线程有关的 ThreadLocal值，这个map由ThreadLocal类维护。
     */
    ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    }

    /**
     * Create the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     *
     * @param t          the current thread
     * @param firstValue value for the initial entry of the map
     */
    void createMap(Thread t, T firstValue) {
        t.threadLocals = new ThreadLocalMap(this, firstValue);
    }

    /**
     * Factory method to create map of inherited thread locals.
     * Designed to be called only from Thread constructor.
     *
     * @param parentMap the map associated with parent thread
     * @return a map containing the parent's inheritable bindings
     */
    static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
        return new ThreadLocalMap(parentMap);
    }

    /**
     * Method childValue is visibly defined in subclass
     * InheritableThreadLocal, but is internally defined here for the
     * sake of providing createInheritedMap factory method without
     * needing to subclass the map class in InheritableThreadLocal.
     * This technique is preferable to the alternative of embedding
     * instanceof tests in methods.
     */
    T childValue(T parentValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * ThreadLocal的扩展，从指定的Supplier获取其初始值，目的是实现java8的函数编程（Lambda）
     */
    static final class SuppliedThreadLocal<T> extends ThreadLocal<T> {

        private final Supplier<? extends T> supplier;

        SuppliedThreadLocal(Supplier<? extends T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        @Override
        protected T initialValue() {
            return supplier.get();
        }
    }

    /**
     * ThreadLocalMap是一个自定义的hash map，仅适用于维护线程本地值。在ThreadLocal类之外不会暴露任何操作。
     * 这个类的访问权限为包访问，即仅能够在该类的包下访问，因此允许在类Thread中进行成员变量的字段声明。
     * 为了帮助处理非常大且存活时间长的用法，哈希表条目使用WeakReferences作为键。
     * 但是，由于不使用引用队列，因此只有当表开始空间不足时才能保证过时条目被删除。
     */
    static class ThreadLocalMap {

        /**
         * 此hash map中的实体扩展了WeakReference，使用其ThreadLocal对象作为键。
         * 需要注意的是，null键（即entry.get（）== null）表示不再引用该键，因此
         * 可以从表中删除这个entry，这些entry在下面的代码中称为“stale entries”。
         */
        static class Entry extends WeakReference<java.lang.ThreadLocal<?>> {
            /**
             * 对当前ThreadLocal的值
             */
            Object value;

            Entry(java.lang.ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }

        /**
         * 初始容量 - 必须是2的幂。
         */
        private static final int INITIAL_CAPACITY = 16;

        /**
         * 该表根据需要调整大小，但是table.length必须始终是2的N次幂。
         */
        private ThreadLocalMap.Entry[] table;

        /**
         * table中entry的数量
         */
        private int size = 0;

        /**
         * 要调整大小的下一个阈值
         */
        private int threshold; // Default to 0

        /**
         * 设置扩容的阈值，为len的2/3
         */
        private void setThreshold(int len) {
            threshold = len * 2 / 3;
        }

        /**
         * entry数组下一个索引
         */
        private static int nextIndex(int i, int len) {
            return ((i + 1 < len) ? i + 1 : 0);
        }

        /**
         * entry数组上一个索引
         */
        private static int prevIndex(int i, int len) {
            return ((i - 1 >= 0) ? i - 1 : len - 1);
        }

        /**
         * 构造一个包括（firstKey，firstValue）的新map。ThreadLocalMaps是懒加载模式，
         * 只有在第一次放入entry的时候才进行创建
         */
        ThreadLocalMap(java.lang.ThreadLocal<?> firstKey, Object firstValue) {
            table = new ThreadLocalMap.Entry[INITIAL_CAPACITY];
            // 由于INITIAL_CAPACITY是2的N次幂（即2的多少次方），在-1之后与hashcod值进行相与操作就是一个求余数的
            // 改进版本（HashMap,ConcurrentHashMap也是这么干的）
            int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
            table[i] = new ThreadLocalMap.Entry(firstKey, firstValue);
            size = 1;
            setThreshold(INITIAL_CAPACITY);
        }

        /**
         * Construct a new map including all Inheritable ThreadLocals
         * from given parent map. Called only by createInheritedMap.
         *
         * @param parentMap the map associated with parent thread.
         */
        private ThreadLocalMap(ThreadLocalMap parentMap) {
            ThreadLocalMap.Entry[] parentTable = parentMap.table;
            int len = parentTable.length;
            setThreshold(len);
            table = new ThreadLocalMap.Entry[len];

            for (int j = 0; j < len; j++) {
                ThreadLocalMap.Entry e = parentTable[j];
                if (e != null) {
                    @SuppressWarnings("unchecked")
                    java.lang.ThreadLocal<Object> key = (java.lang.ThreadLocal<Object>) e.get();
                    if (key != null) {
                        Object value = key.childValue(e.value);
                        ThreadLocalMap.Entry c = new ThreadLocalMap.Entry(key, value);
                        int h = key.threadLocalHashCode & (len - 1);
                        while (table[h] != null)
                            h = nextIndex(h, len);
                        table[h] = c;
                        size++;
                    }
                }
            }
        }

        /**
         * 通过key获取entry。这个方法只处理能够直接命中的现有的key，否则将调用getEntryAfterMiss()进行处理
         * 希望通过这个方法最大限度地提高直接命中的性能
         */
        private ThreadLocalMap.Entry getEntry(java.lang.ThreadLocal<?> key) {
            int i = key.threadLocalHashCode & (table.length - 1);
            ThreadLocalMap.Entry e = table[i];
            if (e != null && e.get() == key)
                return e;
            else
                return getEntryAfterMiss(key, i, e);
        }

        /**
         *在对应的table数组下标所在位置没有找到对应的entry对象时使用的方法
         */
        private ThreadLocalMap.Entry getEntryAfterMiss(java.lang.ThreadLocal<?> key, int i, ThreadLocalMap.Entry e) {
            ThreadLocalMap.Entry[] tab = table;
            int len = tab.length;

            while (e != null) {
                java.lang.ThreadLocal<?> k = e.get();
                if (k == key)
                    return e;
                if (k == null)
                    expungeStaleEntry(i);
                else
                    i = nextIndex(i, len);
                e = tab[i];
            }
            return null;
        }

        /**
         * Set the value associated with key.
         *
         * @param key   the thread local object
         * @param value the value to be set
         */
        private void set(java.lang.ThreadLocal<?> key, Object value) {

            // We don't use a fast path as with get() because it is at
            // least as common to use set() to create new entries as
            // it is to replace existing ones, in which case, a fast
            // path would fail more often than not.

            ThreadLocalMap.Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len - 1);
            // 这儿！线性探测hash map的体现，每次的nextIndex其实就是在寻找下一个索引存储的位置
            for (ThreadLocalMap.Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                ThreadLocal<?> k = e.get();

                if (k == key) {
                    e.value = value;
                    return;
                }

                if (k == null) {
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }

            tab[i] = new ThreadLocalMap.Entry(key, value);
            int sz = ++size;
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                rehash();
        }

        /**
         * 这儿很关键，将referent和value都被设置为null，使引用的内存地址不可达，
         * 让垃圾回收器能够正常回收，避免内存泄漏
         */
        private void remove(java.lang.ThreadLocal<?> key) {
            ThreadLocalMap.Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len - 1);
            for (ThreadLocalMap.Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                if (e.get() == key) {
                    // 清除entry的reference（引用）
                    e.clear();
                    // 将entry中的 key value 均设置为null
                    expungeStaleEntry(i);
                    return;
                }
            }
        }

        /**
         * Replace a stale entry encountered during a set operation
         * with an entry for the specified key.  The value passed in
         * the value parameter is stored in the entry, whether or not
         * an entry already exists for the specified key.
         * <p>
         * As a side effect, this method expunges all stale entries in the
         * "run" containing the stale entry.  (A run is a sequence of entries
         * between two null slots.)
         *
         * @param key       the key
         * @param value     the value to be associated with key
         * @param staleSlot index of the part1 stale entry encountered while
         *                  searching for key.
         */
        private void replaceStaleEntry(java.lang.ThreadLocal<?> key, Object value,
                                       int staleSlot) {
            ThreadLocalMap.Entry[] tab = table;
            int len = tab.length;
            ThreadLocalMap.Entry e;

            // Back up to check for prior stale entry in current run.
            // We clean out whole runs at a time to avoid continual
            // incremental rehashing due to garbage collector freeing
            // up refs in bunches (i.e., whenever the collector runs).
            int slotToExpunge = staleSlot;
            for (int i = prevIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = prevIndex(i, len))
                if (e.get() == null)
                    slotToExpunge = i;

            // Find either the key or trailing null slot of run, whichever
            // occurs part1
            for (int i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                java.lang.ThreadLocal<?> k = e.get();

                // If we find key, then we need to swap it
                // with the stale entry to maintain hash table order.
                // The newly stale slot, or any other stale slot
                // encountered above it, can then be sent to expungeStaleEntry
                // to remove or rehash all of the other entries in run.
                if (k == key) {
                    e.value = value;

                    tab[i] = tab[staleSlot];
                    tab[staleSlot] = e;

                    // Start expunge at preceding stale entry if it exists
                    if (slotToExpunge == staleSlot)
                        slotToExpunge = i;
                    cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
                    return;
                }

                // If we didn't find stale entry on backward scan, the
                // part1 stale entry seen while scanning for key is the
                // part1 still present in the run.
                if (k == null && slotToExpunge == staleSlot)
                    slotToExpunge = i;
            }

            // If key not found, put new entry in stale slot
            tab[staleSlot].value = null;
            tab[staleSlot] = new ThreadLocalMap.Entry(key, value);

            // If there are any other stale entries in run, expunge them
            if (slotToExpunge != staleSlot)
                cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
        }

        /**
         * Expunge a stale entry by rehashing any possibly colliding entries
         * lying between staleSlot and the next null slot.  This also expunges
         * any other stale entries encountered before the trailing null.  See
         * Knuth, Section 6.4
         *
         * @param staleSlot index of slot known to have null key
         * @return the index of the next null slot after staleSlot
         * (all between staleSlot and this slot will have been checked
         * for expunging).
         */
        private int expungeStaleEntry(int staleSlot) {
            ThreadLocalMap.Entry[] tab = table;
            int len = tab.length;

            // expunge entry at staleSlot
            tab[staleSlot].value = null;
            tab[staleSlot] = null;
            size--;

            // Rehash until we encounter null
            ThreadLocalMap.Entry e;
            int i;
            for (i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                java.lang.ThreadLocal<?> k = e.get();
                if (k == null) {
                    e.value = null;
                    tab[i] = null;
                    size--;
                } else {
                    int h = k.threadLocalHashCode & (len - 1);
                    if (h != i) {
                        tab[i] = null;

                        // Unlike Knuth 6.4 Algorithm R, we must scan until
                        // null because multiple entries could have been stale.
                        while (tab[h] != null)
                            h = nextIndex(h, len);
                        tab[h] = e;
                    }
                }
            }
            return i;
        }

        /**
         * Heuristically scan some cells looking for stale entries.
         * This is invoked when either a new element is added, or
         * another stale one has been expunged. It performs a
         * logarithmic number of scans, as a balance between no
         * scanning (fast but retains garbage) and a number of scans
         * proportional to number of elements, that would find all
         * garbage but would cause some insertions to take O(n) time.
         *
         * @param i a position known NOT to hold a stale entry. The
         *          scan starts at the element after i.
         * @param n scan control: {@code log2(n)} cells are scanned,
         *          unless a stale entry is found, in which case
         *          {@code log2(table.length)-1} additional cells are scanned.
         *          When called from insertions, this parameter is the number
         *          of elements, but when from replaceStaleEntry, it is the
         *          table length. (Note: all this could be changed to be either
         *          more or less aggressive by weighting n instead of just
         *          using straight log n. But this version is simple, fast, and
         *          seems to work well.)
         * @return true if any stale entries have been removed.
         */
        private boolean cleanSomeSlots(int i, int n) {
            boolean removed = false;
            ThreadLocalMap.Entry[] tab = table;
            int len = tab.length;
            do {
                i = nextIndex(i, len);
                ThreadLocalMap.Entry e = tab[i];
                if (e != null && e.get() == null) {
                    n = len;
                    removed = true;
                    i = expungeStaleEntry(i);
                }
            } while ((n >>>= 1) != 0);
            return removed;
        }

        /**
         * 清除表中过期实体（stale entry）或者重新调整表格大小（这两件事情都可以做，也可以只做一件）。
         *
         */
        private void rehash() {
            expungeStaleEntries();

            // 使用较低的阈值避免滞后（什么时候会发生滞后？）
            if (size >= threshold - threshold / 4)
                resize();
        }

        /**
         * 双倍扩容
         */
        private void resize() {
            ThreadLocalMap.Entry[] oldTab = table;
            int oldLen = oldTab.length;
            int newLen = oldLen * 2;
            ThreadLocalMap.Entry[] newTab = new ThreadLocalMap.Entry[newLen];
            int count = 0;

            for (int j = 0; j < oldLen; ++j) {
                ThreadLocalMap.Entry e = oldTab[j];
                if (e != null) {
                    java.lang.ThreadLocal<?> k = e.get();
                    if (k == null) {
                        e.value = null; // Help the GC
                    } else {
                        int h = k.threadLocalHashCode & (newLen - 1);
                        while (newTab[h] != null)
                            h = nextIndex(h, newLen);
                        newTab[h] = e;
                        count++;
                    }
                }
            }

            setThreshold(newLen);
            size = count;
            table = newTab;
        }

        /**
         * 清除在table中的所有过期实体（stale entry）
         */
        private void expungeStaleEntries() {
            ThreadLocalMap.Entry[] tab = table;
            int len = tab.length;
            for (int j = 0; j < len; j++) {
                ThreadLocalMap.Entry e = tab[j];
                if (e != null && e.get() == null)
                    expungeStaleEntry(j);
            }
        }
    }
}
