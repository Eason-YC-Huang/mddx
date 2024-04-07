package ink.eason.mddx.utils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class BF {


    public static void move(ByteBuffer bf, int step) {
        bf.position(bf.position() + step);
    }

    public static int step(Charset encoding) {
        if (Objects.equals(encoding, StandardCharsets.UTF_8)) {
            return 1;
        }

        if (Objects.equals(encoding, StandardCharsets.UTF_16LE)) {
            return 2;
        }

        throw new UnsupportedOperationException();
    }

    public static String readText(ByteBuffer bf, Charset encoding) {

        if (Objects.equals(encoding, StandardCharsets.UTF_8)) {
            return readUtf8(bf);
        }

        if (Objects.equals(encoding, StandardCharsets.UTF_16LE)) {
            return readUtf16(bf);
        }

        throw new UnsupportedOperationException();
    }

    private static String readUtf8(ByteBuffer bf) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte b;
        while ((b = bf.get()) != 0x00) {
            baos.write(b);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    private static String readUtf16(ByteBuffer buf) {

        Integer textLen = autoReset(buf, () -> {
            int mark = buf.position();
            while (buf.getChar() != '\u0000') {
            }
            return buf.position() - 2 - mark;
        });

        String ans = StandardCharsets.UTF_16LE.decode(sliceAndMove(buf, textLen)).toString();
        buf.getChar();
        return ans;
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

    public static <R> R autoReset(ByteBuffer bf, Function<ByteBuffer, R> ops) {
        int pos = bf.position();
        R ans = ops.apply(bf);
        bf.position(pos);
        return ans;
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
