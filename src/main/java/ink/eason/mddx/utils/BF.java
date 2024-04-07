package ink.eason.mddx.utils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BF {

    public static String readUtf8(ByteBuffer bf) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte b;
        while ((b = bf.get()) != 0x00) {
            baos.write(b);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    public static ByteBuffer slice(ByteBuffer bf, int length) {
        return bf.slice(bf.position(), length);
    }
    public static ByteBuffer sliceAndMove(ByteBuffer bf, int length) {
        ByteBuffer ans = bf.slice(bf.position(), length);
        bf.position(bf.position() + length);
        return ans;
    }

    public static void autoReset(ByteBuffer bf, Consumer<ByteBuffer> ops) {
        int pos = bf.position();
        ops.accept(bf);
        bf.position(pos);
    }

    public static void autoReset(ByteBuffer bf, Runnable ops) {
        int pos = bf.position();
        ops.run();
        bf.position(pos);
    }

    public static <R> R autoReset(ByteBuffer bf, Supplier<R> ops) {
        int pos = bf.position();
        R ans = ops.get();
        bf.position(pos);
        return ans;
    }

}
