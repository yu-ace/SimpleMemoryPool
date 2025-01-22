package org.example;

import java.nio.ByteBuffer;

public class SimpleMemoryPool {
    private final ByteBuffer buffer;  // 底层存储缓冲区
    private int position = 0;         // 当前分配位置指针

    public SimpleMemoryPool(int capacity) {
        // 分配指定容量的ByteBuffer
        // 这里使用allocate创建堆内存缓冲区
        // 也可以使用allocateDirect创建直接内存缓冲区
        buffer = ByteBuffer.allocate(capacity);
    }

    // 分配内存，返回开始位置
    public synchronized int allocate(int size) {
        // 检查是否需要重置位置
        if (position + size > buffer.capacity()) {
            position = 0; // 如果到达末尾，重新从头开始
        }
        int currentPosition = position;
        position = position + size;
        return currentPosition;
    }

    // 写入数据
    public void write(int position, byte[] data) {
        // 设置缓冲区的位置
        buffer.position(position);
        // 写入数据
        buffer.put(data);
    }

    // 读取数据
    public byte[] read(int position, int length) {
        // 设置缓冲区的位置
        buffer.position(position);
        // 创建结果数组
        byte[] data = new byte[length];
        // 读取数据
        buffer.get(data);
        return data;
    }

    // 获取总容量
    public int capacity() {
        return buffer.capacity();
    }

    public static void main(String[] args) {
        SimpleMemoryPool pool = new SimpleMemoryPool(1024); // 1KB池

        // 分配内存并写入数据
        int pos1 = pool.allocate(10);
        pool.write(pos1, "HelloWorld".getBytes());

        // 读取数据
        byte[] data = pool.read(pos1, 10);
        System.out.println(new String(data));
    }
}
