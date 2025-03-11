package at.peckventure.world;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class MobRegionFile {
    public static final int SECTOR_SIZE = 512;
    public static final int MOB_CELLS_PER_REGION = RegionFile.CHUNKS_PER_REGION;
    public static final int HEADER_SIZE = MOB_CELLS_PER_REGION * MOB_CELLS_PER_REGION * 4;
    private final RandomAccessFile file;
    private final FileChannel channel;
    private final int[] offsets = new int[MOB_CELLS_PER_REGION * MOB_CELLS_PER_REGION];

    public MobRegionFile(java.io.File filePath) throws IOException {
        file = new RandomAccessFile(filePath, "rw");
        channel = file.getChannel();
        if (file.length() < HEADER_SIZE) {
            file.setLength(HEADER_SIZE);
            ByteBuffer emptyHeader = ByteBuffer.allocate(HEADER_SIZE);
            channel.write(emptyHeader, 0);
        }
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
        channel.read(header, 0);
        header.rewind();
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = header.getInt();
        }
    }

    public void writeMobs(int localX, int localY, byte[] mobData) throws IOException {
        int index = localX + localY * MOB_CELLS_PER_REGION;
        byte[] compressed = compress(mobData);
        int length = compressed.length + 1;
        int sectorsNeeded = (length + 4 + SECTOR_SIZE - 1) / SECTOR_SIZE;
        long fileLength = file.length();
        int sectorNumber = (int)(fileLength / SECTOR_SIZE);
        if (fileLength % SECTOR_SIZE != 0) {
            sectorNumber++;
        }
        ByteBuffer buffer = ByteBuffer.allocate(sectorsNeeded * SECTOR_SIZE);
        buffer.putInt(length);
        buffer.put((byte)2);
        buffer.put(compressed);
        buffer.position(buffer.capacity());
        buffer.rewind();
        channel.write(buffer, (long) sectorNumber * SECTOR_SIZE);
        int headerEntry = (sectorNumber << 8) | sectorsNeeded;
        offsets[index] = headerEntry;
        ByteBuffer entryBuffer = ByteBuffer.allocate(4);
        entryBuffer.putInt(headerEntry);
        entryBuffer.rewind();
        channel.write(entryBuffer, index * 4L);
    }

    public byte[] readMobs(int localX, int localY) throws IOException {
        refreshHeader();
        int index = localX + localY * MOB_CELLS_PER_REGION;
        int headerEntry = offsets[index];
        if (headerEntry == 0) {
            return null;
        }
        int sectorNumber = headerEntry >> 8;
        int sectors = headerEntry & 0xFF;
        int byteCount = sectors * SECTOR_SIZE;
        ByteBuffer buffer = ByteBuffer.allocate(byteCount);
        channel.read(buffer, (long) sectorNumber * SECTOR_SIZE);
        buffer.rewind();
        int length = buffer.getInt();
        byte version = buffer.get();
        byte[] compressed = new byte[length - 1];
        buffer.get(compressed);
        if (version == 2) {
            return decompress(compressed);
        } else {
            throw new IOException("Unsupported mob block version: " + version);
        }
    }

    private byte[] compress(byte[] data) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buf);
            baos.write(buf, 0, count);
        }
        deflater.end();
        return baos.toByteArray();
    }

    private byte[] decompress(byte[] data) throws IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(buf);
                baos.write(buf, 0, count);
            }
        } catch (Exception e) {
            throw new IOException("Mob decompression failed", e);
        }
        inflater.end();
        return baos.toByteArray();
    }

    public void refreshHeader() throws IOException {
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
        channel.read(header, 0);
        header.rewind();
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = header.getInt();
        }
    }

    public void clearMobs(int localX, int localY) throws IOException {
        int index = localX + localY * MOB_CELLS_PER_REGION;
        offsets[index] = 0;
        ByteBuffer entryBuffer = ByteBuffer.allocate(4);
        entryBuffer.putInt(0);
        entryBuffer.rewind();
        channel.write(entryBuffer, index * 4L);
    }

    public void close() throws IOException {
        channel.close();
        file.close();
    }
}
