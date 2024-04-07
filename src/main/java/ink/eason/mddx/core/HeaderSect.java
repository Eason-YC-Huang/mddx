package ink.eason.mddx.core;

import ink.eason.mddx.utils.BF;
import ink.eason.mddx.utils.Codec;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Getter
public class HeaderSect {

    private String text;

    public HeaderSect(ByteBuffer dataBuffer) {
        int len = dataBuffer.getInt();
        ByteBuffer content = BF.sliceAndMove(dataBuffer, len);
        Long actualChecksum = BF.autoReset(content, () -> Codec.adler32(content));
        long expectedChecksum = Codec.swapEndian(dataBuffer.getInt());

        if (actualChecksum != expectedChecksum) {
            throw new IllegalArgumentException("Checksum mismatched: expected:%s , actual:%s".formatted(expectedChecksum, actualChecksum));
        }

        // no sure if all the dict need this special handle, at least my current dictionary need it.
        content.limit(content.capacity() - 2);
        this.text = StandardCharsets.UTF_16LE.decode(content).toString();
    }

}
