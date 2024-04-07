package ink.eason.mddx.utils;

import lombok.Getter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.zip.Adler32;
import java.util.zip.InflaterOutputStream;

import static ink.eason.mddx.utils.BF.autoReset;
import static ink.eason.mddx.utils.BF.sliceAndMove;

public class Codec {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static long adler32(ByteBuffer content) {
        Adler32 adler32 = new Adler32();
        adler32.update(content);
        return adler32.getValue();
    }

    public static int swapEndian(int i) {
        return (i & 0xff) << 24 | (i & 0xff00) << 8 | (i & 0xff0000) >> 8 | (i >> 24) & 0xff;
    }

    @Getter
    public static enum Compression {
        NONE(0x00_00_00_00), // 0
        LZO(0x01_00_00_00), // 16777216
        ZLIB(0x02_00_00_00) // 33554432
        ;
        private final int id;

        Compression(int id) {
            this.id = id;
        }

        public static Compression of(int id) {
            if (id == NONE.id) return NONE;
            if (id == LZO.id) return LZO;
            if (id == ZLIB.id) return ZLIB;
            throw new IllegalArgumentException("Unsupported compression: " + id);
        }
    }

    public static class Decompress {
        private boolean encrypted;
        private boolean checkSum;
        private ByteBuffer data;
        private long decompressedSize;

        public static Decompress of(ByteBuffer data) {
            Decompress cmd = new Decompress();
            cmd.data = data;
            cmd.encrypted = false;
            cmd.checkSum = true;
            return cmd;
        }

        public Decompress checksum(boolean checksum) {
            this.checkSum = checksum;
            return this;
        }

        public Decompress encrypted(boolean encrypted) {
            this.encrypted = encrypted;
            return this;
        }

        public Decompress checkDecompressedSize(long decompressedSize) {
            this.decompressedSize = decompressedSize;
            return this;
        }

        public ByteBuffer exec() {
            Compression compression = Compression.of(data.getInt());
            int expectedChecksum = data.getInt();

            if (this.encrypted) {
                byte[] secretKey = secretKey(expectedChecksum);
                decrypt(this.data, secretKey);
            }

            if (this.checkSum) {
                // todo support multiple compression
                Integer actualChecksum = autoReset(data, () -> {
                    data.position(data.capacity() - 4);
                    return sliceAndMove(data, 4).getInt();
                });

                if (expectedChecksum != actualChecksum) {
                    throw new IllegalStateException("Checksum mismatch, expected : [%s] actual : [%s]".formatted(expectedChecksum, actualChecksum));
                }
            }

            ByteBuffer decompressed = decompress(compression, data, data.remaining());

            if (decompressedSize > 0) {
                if (decompressedSize != decompressed.capacity()) {
                    throw new IllegalStateException("Decompressed mismatch, expected : [%s] actual : [%s]".formatted(decompressedSize, decompressed.capacity()));
                }
            }

            return decompressed;
        }

    }

    private static void decrypt(ByteBuffer content, byte[] secretKey) {
        int pos = content.position();

        byte previous = 0x36;
        int len = content.remaining();
        byte[] ans = new byte[len];
        for (int i = 0; i < len; i++) {
            byte val = content.get();

            ans[i] = swapNibble(val);
            ans[i] ^= (byte) i;
            ans[i] ^= secretKey[i % secretKey.length];
            ans[i] ^= previous;

            previous = val;
        }


        content.position(pos);
        content.put(ans);
        content.position(pos);
    }

    private static byte swapNibble(byte x) {
        return (byte) ((x & 0x0F) << 4 | (x & 0xF0) >> 4);
    }

    private static byte[] secretKey(int checksum) {
        ByteBuffer rawSecretKey = ByteBuffer.allocate(8);
        rawSecretKey.putInt(checksum).putInt(0x95_36_00_00);

        try {
            MessageDigest digest = MessageDigest.getInstance("RIPEMD128");
            return digest.digest(rawSecretKey.array());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }

    private static ByteBuffer decompress(Compression compression, ByteBuffer dataBuffer, int length) {

        return switch (compression) {
            case NONE -> sliceAndMove(dataBuffer, length);
            case LZO ->
                    throw new IllegalArgumentException("Unsupported compression: " + Integer.toHexString(compression.getId()));
            case ZLIB -> zlibDecompress(dataBuffer, length);
            default ->
                    throw new IllegalArgumentException("Unsupported compression: " + Integer.toHexString(compression.getId()));
        };
    }

    private static ByteBuffer zlibDecompress(ByteBuffer content, int length) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             InflaterOutputStream inf = new InflaterOutputStream(out)) {

            for (int i = 0; i < length; i++) {
                inf.write(content.get());
            }

            return ByteBuffer.wrap(out.toByteArray());
        } catch (Exception ex) {

            throw new RuntimeException(ex);
        }
    }


}
