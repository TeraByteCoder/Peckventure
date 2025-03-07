package at.peckventure.entities;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;

public class MobFile {
    public static final int SECTOR_SIZE = 4096;
    public static final int MAX_MOBS = 1024;
    public static final int HEADER_SIZE = MAX_MOBS * 4;
    private final RandomAccessFile file;
    private final FileChannel channel;
    private final int[] offsets = new int[MAX_MOBS];

    public MobFile(File filePath) throws IOException {
        file = new RandomAccessFile(filePath, "rw");
        channel = file.getChannel();
        if (file.length() < HEADER_SIZE) {
            file.setLength(HEADER_SIZE);
            ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
            channel.write(header, 0);
        }
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
        channel.read(header, 0);
        header.rewind();
        for (int i = 0; i < MAX_MOBS; i++) {
            offsets[i] = header.getInt();
        }
    }

    public synchronized void writeMob(int slot, byte[] data) throws IOException {
        if (slot < 0 || slot >= MAX_MOBS) throw new IllegalArgumentException("Invalid slot");
        int length = data.length + 1;
        int sectorsNeeded = (length + 4 + SECTOR_SIZE - 1) / SECTOR_SIZE;
        long fileLength = file.length();
        int sectorNumber = (int) (fileLength / SECTOR_SIZE);
        if (fileLength % SECTOR_SIZE != 0) sectorNumber++;
        ByteBuffer buffer = ByteBuffer.allocate(sectorsNeeded * SECTOR_SIZE);
        buffer.putInt(length);
        buffer.put((byte) 1);
        buffer.put(data);
        buffer.position(buffer.capacity());
        buffer.rewind();
        channel.write(buffer, (long) sectorNumber * SECTOR_SIZE);
        int headerEntry = (sectorNumber << 8) | sectorsNeeded;
        offsets[slot] = headerEntry;
        ByteBuffer entryBuffer = ByteBuffer.allocate(4);
        entryBuffer.putInt(headerEntry);
        entryBuffer.rewind();
        channel.write(entryBuffer, slot * 4L);
    }

    public synchronized byte[] readMob(int slot) throws IOException {
        if (slot < 0 || slot >= MAX_MOBS) throw new IllegalArgumentException("Invalid slot");
        int headerEntry = offsets[slot];
        if (headerEntry == 0) return null;
        int sectorNumber = headerEntry >> 8;
        int sectors = headerEntry & 0xFF;
        int byteCount = sectors * SECTOR_SIZE;
        ByteBuffer buffer = ByteBuffer.allocate(byteCount);
        channel.read(buffer, (long) sectorNumber * SECTOR_SIZE);
        buffer.rewind();
        int length = buffer.getInt();
        buffer.get();
        byte[] mobData = new byte[length - 1];
        buffer.get(mobData);
        return mobData;
    }

    public synchronized void saveAllMobs(Collection<byte[]> mobsData) throws IOException {
        file.setLength(HEADER_SIZE);
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
        for (int i = 0; i < MAX_MOBS; i++) {
            offsets[i] = 0;
            header.putInt(0);
        }
        header.rewind();
        channel.write(header, 0);
        int slot = 0;
        for (byte[] data : mobsData) {
            if (slot >= MAX_MOBS) break;
            writeMob(slot, data);
            slot++;
        }
    }

    public synchronized void close() throws IOException {
        channel.close();
        file.close();
    }
}
