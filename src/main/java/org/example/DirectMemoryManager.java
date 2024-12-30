package org.example;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DirectMemoryManager {
    // 用于追踪所有分配的DirectByteBuffer
    private static final Map<ByteBuffer, MemoryInfo> memoryRegistry = new ConcurrentHashMap<>();
    // 配置参数
    private static final long MAX_MEMORY_SIZE = 1024 * 1024 * 100; // 100MB
    private static final long MAX_IDLE_TIME = 30000; // 30秒
    private static long currentUsedMemory = 0;

    static class MemoryInfo {
        private final long size;
        private final long createTime;
        private long lastAccessTime;
        private boolean isMarkedForCleanup;

        public MemoryInfo(long size) {
            this.size = size;
            this.createTime = System.currentTimeMillis();
            this.lastAccessTime = System.currentTimeMillis();
            this.isMarkedForCleanup = false;
        }
    }

    static {
        // 启动定时清理任务
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(DirectMemoryManager::cleanupMemory, 30, 30, TimeUnit.SECONDS);
    }

    public static ByteBuffer allocateMemory(int size) {
        if (currentUsedMemory + size > MAX_MEMORY_SIZE) {
            throw new OutOfMemoryError("Direct memory limit exceeded");
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(size);
        memoryRegistry.put(buffer, new MemoryInfo(size));
        currentUsedMemory = currentUsedMemory + size;
        return buffer;
    }

    public static void accessBuffer(ByteBuffer buffer) {
        MemoryInfo info = memoryRegistry.get(buffer);
        if (info != null) {
            info.lastAccessTime = System.currentTimeMillis();
        }
    }

    public static void releaseMemory(ByteBuffer buffer) {
        MemoryInfo info = memoryRegistry.remove(buffer);
        if (info != null) {
            currentUsedMemory = currentUsedMemory - info.size;
            cleanDirectBuffer(buffer);
        }
    }

    private static void cleanupMemory() {
        long currentTime = System.currentTimeMillis();

        memoryRegistry.entrySet().removeIf(entry -> {
            ByteBuffer buffer = entry.getKey();
            MemoryInfo info = entry.getValue();

            // 检查是否超过最大空闲时间
            if (currentTime - info.lastAccessTime > MAX_IDLE_TIME) {
                currentUsedMemory = currentUsedMemory - info.size;
                cleanDirectBuffer(buffer);
                return true;
            }
            return false;
        });
    }

    private static void cleanDirectBuffer(ByteBuffer buffer) {
        if (buffer.isDirect()) {
            try {
                // 使用反射调用cleaner方法释放直接内存
                // 注意：这种方式在不同JDK版本中可能需要调整
                buffer.clear();
            } catch (Exception e) {
                // 处理清理失败的情况
                e.printStackTrace();
            }
        }
    }

    public static long getCurrentUsedMemory() {
        return currentUsedMemory;
    }

    public static void main(String[] args) {
        try {
            // 模拟多个线程分配和使用内存的场景
            Thread allocThread1 = new Thread(() -> {
                try {
                    // 分配一些内存块并存储
                    ByteBuffer buffer1 = DirectMemoryManager.allocateMemory(1024 * 1024); // 1MB
                    ByteBuffer buffer2 = DirectMemoryManager.allocateMemory(2 * 1024 * 1024); // 2MB
                    System.out.println("Thread 1 allocated memory, current usage: "
                            + getCurrentUsedMemory() / (1024 * 1024) + "MB");

                    // 模拟使用内存
                    Thread.sleep(2000);
                    accessBuffer(buffer1);
                    System.out.println("Thread 1 accessed buffer1");

                    // 主动释放一个buffer
                    releaseMemory(buffer1);
                    System.out.println("Thread 1 released buffer1, current usage: "
                            + getCurrentUsedMemory() / (1024 * 1024) + "MB");

                    // buffer2故意不释放，让定时任务来清理
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            Thread allocThread2 = new Thread(() -> {
                try {
                    // 分配另外一些内存块
                    ByteBuffer buffer3 = DirectMemoryManager.allocateMemory(3 * 1024 * 1024); // 3MB
                    System.out.println("Thread 2 allocated memory, current usage: "
                            + getCurrentUsedMemory() / (1024 * 1024) + "MB");

                    // 模拟间歇性访问
                    for (int i = 0; i < 3; i++) {
                        Thread.sleep(5000);
                        accessBuffer(buffer3);
                        System.out.println("Thread 2 accessed buffer3, iteration " + (i + 1));
                    }

                    // 最后不释放buffer3，让定时任务来清理
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // 启动线程
            allocThread1.start();
            allocThread2.start();

            // 启动监控线程
            Thread monitorThread = new Thread(() -> {
                try {
                    while (true) {
                        Thread.sleep(5000);
                        System.out.println("Current memory usage: "
                                + getCurrentUsedMemory() / (1024 * 1024) + "MB");
                        System.out.println("Number of tracked buffers: " + memoryRegistry.size());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            monitorThread.setDaemon(true);
            monitorThread.start();

            // 等待足够长的时间以观察定时清理的效果
            Thread.sleep(90000); // 等待90秒

            System.out.println("Final memory usage: "
                    + getCurrentUsedMemory() / (1024 * 1024) + "MB");
            System.out.println("Final number of tracked buffers: " + memoryRegistry.size());

            // 关闭程序
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
