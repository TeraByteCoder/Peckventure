package at.peckventure.world;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class RegionFile
{
    public static final int SECTOR_SIZE = 4096;
    public static final int CHUNKS_PER_REGION = 32; // 32×32 Chunks
    public static final int HEADER_SIZE = CHUNKS_PER_REGION * CHUNKS_PER_REGION * 4; // 4 Byte pro Chunk

    private final RandomAccessFile file;
    private final FileChannel channel;
    private final int[] offsets = new int[CHUNKS_PER_REGION * CHUNKS_PER_REGION];

    public RegionFile(File filePath) throws IOException
    {
        file = new RandomAccessFile(filePath, "rw");
        channel = file.getChannel();
        // Initialisiere den Header, falls die Datei zu klein ist
        if (file.length() < HEADER_SIZE)
        {
            file.setLength(HEADER_SIZE);
            ByteBuffer emptyHeader = ByteBuffer.allocate(HEADER_SIZE);
            channel.write(emptyHeader, 0);
        }
        // Lese den Header ein
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
        channel.read(header, 0);
        header.rewind();
        for (int i = 0; i < offsets.length; i++)
        {
            offsets[i] = header.getInt();
        }
    }

    // Lokale Koordinaten (0 bis CHUNKS_PER_REGION-1)
    public void writeChunk(int localX, int localY, byte[] data) throws IOException
    {
        int index = localX + localY * CHUNKS_PER_REGION;
        byte[] compressed = compress(data);
        int length = compressed.length + 1; // +1 für die Versionsangabe
        int sectorsNeeded = (length + 4 + SECTOR_SIZE - 1) / SECTOR_SIZE; // 4 Byte für die Länge

        // Für eine einfache Implementierung: immer ans Dateiende anhängen
        long fileLength = file.length();
        int sectorNumber = (int) (fileLength / SECTOR_SIZE);
        if (fileLength % SECTOR_SIZE != 0)
        {
            sectorNumber++;
        }

        ByteBuffer buffer = ByteBuffer.allocate(sectorsNeeded * SECTOR_SIZE);
        buffer.putInt(length);
        buffer.put((byte) 2); // Version 2 = zlib-Kompression
        buffer.put(compressed);
        // Auffüllen (Padding) mit Nullen
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

    public byte[] readChunk(int localX, int localY) throws IOException
    {
        // Aktualisiere zuerst den Header, um sicherzustellen, dass wir die aktuellen Offsets haben
        refreshHeader();

        int index = localX + localY * CHUNKS_PER_REGION;
        int headerEntry = offsets[index];
        if (headerEntry == 0)
        {
            return null; // Chunk nicht vorhanden
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
        if (version == 2)
        {
            return decompress(compressed);
        } else
        {
            throw new IOException("Unsupported chunk version: " + version);
        }
    }


    // Helfermethoden für Kompression/Dekompression
    private byte[] compress(byte[] data) throws IOException
    {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        while (!deflater.finished())
        {
            int count = deflater.deflate(buf);
            baos.write(buf, 0, count);
        }
        deflater.end();
        return baos.toByteArray();
    }

    private byte[] decompress(byte[] data) throws IOException
    {
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        try
        {
            while (!inflater.finished())
            {
                int count = inflater.inflate(buf);
                baos.write(buf, 0, count);
            }
        } catch (Exception e)
        {
            throw new IOException("Decompression failed", e);
        }
        inflater.end();
        return baos.toByteArray();
    }

    public void close() throws IOException
    {
        channel.close();
        file.close();
    }

    public void refreshHeader() throws IOException
    {
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
        channel.read(header, 0);
        header.rewind();
        for (int i = 0; i < offsets.length; i++)
        {
            offsets[i] = header.getInt();
        }
    }

}
