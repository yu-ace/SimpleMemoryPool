package org.example;


import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class SimpleMemoryPoolJDK21 {

    private final MemorySegment segment;  // 内存段实例
    private final Arena arena;           // 内存作用域控制器
    private long position = 0;             // 当前分配位置

    public SimpleMemoryPoolJDK21(long capacity) {
        this.arena = Arena.ofConfined();  // 创建受控内存域
        this.segment = arena.allocate(capacity);
    }

    public synchronized long allocate(long size) {
        if (position + size > segment.byteSize()) {
            position = 0; // 内存回卷策略
        }
        long current = position;
        position += size;
        return current;
    }

    public void write(long offset, byte[] data) {
        MemorySegment source = MemorySegment.ofArray(data);
        segment.asSlice(offset, data.length)
                .copyFrom(source); // 类型安全的数据拷贝
    }

    public byte[] read(long offset, int length) {
        byte[] buffer = new byte[length];
        MemorySegment.copy(segment, offset,
                MemorySegment.ofArray(buffer), 0, length);
        return buffer;
    }

    public void close() {
        arena.close(); // 显式释放所有内存
    }

    public static void main(String[] args) {
        SimpleMemoryPoolJDK21 pool = new SimpleMemoryPoolJDK21(1024);

        byte[] bytes = "Hello World".getBytes();
        long pos1 = pool.allocate(bytes.length);
        pool.write(pos1, bytes);

        byte[] data = pool.read(pos1, bytes.length);
        System.out.println(new String(data));

        pool.close();
    }

}
