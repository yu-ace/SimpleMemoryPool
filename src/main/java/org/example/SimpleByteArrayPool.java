package org.example;

public class SimpleByteArrayPool {
    private final byte[] buffer; // 存储内存的字节数组
    private int position = 0; // 当前分配位置

    public SimpleByteArrayPool(int capacity) {
        buffer = new byte[capacity]; // 初始化字节数组
    }

    // 分配内存，返回开始位置
    public synchronized int allocate(int size) {
        if (position + size > buffer.length) {
            position = 0; // 如果到达末尾，重新从头开始
        }
        int currentPosition = position;
        position += size; // 更新位置
        return currentPosition; // 返回当前分配的位置
    }

    // 写入数据
    public void write(int position, byte[] data) {
        System.arraycopy(data, 0, buffer, position, data.length); // 将数据写入指定位置
    }

    // 读取数据
    public byte[] read(int position, int length) {
        byte[] data = new byte[length];
        System.arraycopy(buffer, position, data, 0, length); // 从指定位置读取数据
        return data;
    }

    // 获取总容量
    public int capacity() {
        return buffer.length; // 返回总容量
    }

    public static void main(String[] args) {
        SimpleByteArrayPool pool = new SimpleByteArrayPool(1024); // 1KB池

        byte[] bytes = "Hello World".getBytes();
        // 分配内存并写入数据
        int pos1 = pool.allocate(bytes.length);
        pool.write(pos1, bytes);

        // 读取数据
        byte[] data = pool.read(pos1, bytes.length);
        System.out.println(new String(data)); // 输出: Hello World
    }
}