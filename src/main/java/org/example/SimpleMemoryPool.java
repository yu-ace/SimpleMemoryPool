package org.example;

import java.nio.ByteBuffer;

public class SimpleMemoryPool {
    private final ByteBuffer buffer;
    private int position = 0;

    public SimpleMemoryPool(int capacity) {
        buffer = ByteBuffer.allocate(capacity);
    }

    // 分配内存，返回开始位置
    public synchronized int allocate(int size) {
        if (position + size > buffer.capacity()) {
            position = 0; // 如果到达末尾，重新从头开始
        }
        int currentPosition = position;
        position = position + size;
        return currentPosition;
    }

    // 写入数据
    public void write(int position, byte[] data) {
        buffer.position(position);
        buffer.put(data);
    }

    // 读取数据
    public byte[] read(int position, int length) {
        buffer.position(position);
        byte[] data = new byte[length];
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
