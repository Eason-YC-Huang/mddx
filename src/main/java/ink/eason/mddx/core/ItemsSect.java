package ink.eason.mddx.core;

import ink.eason.mddx.utils.BF;
import ink.eason.mddx.utils.Codec;
import ink.eason.mddx.utils.Pair;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static ink.eason.mddx.utils.BF.readText;
import static ink.eason.mddx.utils.BF.sliceAndMove;

@Getter
@ToString
@EqualsAndHashCode
public class ItemsSect {

    private final Charset encoding;
    private final int step;
    private final boolean isMddFile;

    private final long numOfItemBlocks;
    private final long numOfItems;
    private final long itemBlocksIndexDecompressedLen;
    private final long itemBlocksIndexLen;
    private final long itemBlockLen;
    // checksum for above 40 bytes, for version 2 only
    private final long checksum;

    private final List<ItemsBlock> itemsBlocks;

    public ItemsSect(ByteBuffer dataBuffer, Charset encoding, boolean isMddFile) {

        this.encoding = encoding;
        this.step = BF.step(encoding);
        this.isMddFile = isMddFile;

        {
            ByteBuffer buffer = sliceAndMove(dataBuffer, 40);
            Long actualChecksum = BF.autoReset(buffer, () -> Codec.adler32(buffer));
            this.numOfItemBlocks = buffer.getLong();
            this.numOfItems = buffer.getLong();
            this.itemBlocksIndexDecompressedLen = buffer.getLong();
            this.itemBlocksIndexLen = buffer.getLong();
            this.itemBlockLen = buffer.getLong();

            this.checksum = dataBuffer.getInt();
            if (actualChecksum != checksum) {
                throw new IllegalArgumentException("Checksum mismatched: expected:%s , actual:%s".formatted(checksum, actualChecksum));
            }
        }

        {
            ByteBuffer content = sliceAndMove(dataBuffer, (int) this.itemBlocksIndexLen);
            content = Codec.Decompress.of(content)
                    .checkDecompressedSize(itemBlocksIndexDecompressedLen)
                    .encrypted(true)
                    .checksum(true)
                    .exec();

            this.itemsBlocks = new ArrayList<>();
            for (long i = 0; i < this.numOfItemBlocks; i++) {
                ItemsBlock itemsBlock = new ItemsBlock();
                itemsBlock.totalItems = content.getLong();
                short firstValLen = content.getShort();
                itemsBlock.firstItem = encoding.decode(sliceAndMove(content, firstValLen * step)).toString();
                BF.move(content, step);
                short lastValLen = content.getShort();
                itemsBlock.lastItem = encoding.decode(sliceAndMove(content, lastValLen * step)).toString();
                BF.move(content, step);
                itemsBlock.itemBlockLen = content.getLong();
                itemsBlock.decompressedLen = content.getLong();
                itemsBlocks.add(itemsBlock);
            }
        }

        {
            for (ItemsBlock itemsBlock : itemsBlocks) {
                itemsBlock.offset = dataBuffer.position();
                ByteBuffer content = sliceAndMove(dataBuffer, (int) itemsBlock.itemBlockLen);
                content = Codec.Decompress.of(content)
                        .checkDecompressedSize(itemsBlock.decompressedLen)
                        .checksum(true)
                        .exec();
                Map<String, Long> mapping = new LinkedHashMap<>();
                while (content.hasRemaining()) {
                    long itemDetailOffset = content.getLong();
                    String item = BF.readText(content, encoding);
                    // why we have duplicated items here?
                    mapping.putIfAbsent(item, itemDetailOffset);
                }
                itemsBlock.mapping = mapping;
            }
        }

    }

    public Long findItemDetailOffset(String item) {
        for (ItemsBlock block : itemsBlocks) {
            if (block.firstItem.compareTo(item.toLowerCase()) <= 0
                    && ((isMddFile) ? block.lastItem.compareTo(item.toLowerCase()) > 0 : block.lastItem.compareTo(item.toLowerCase()) >= 0)) {
                return block.findItemDetailOffset(item);
            }
        }
        return null;
    }

    @Data
    public static class ItemsBlock {
        private long offset;
        private long itemBlockLen;
        private long decompressedLen;
        private long totalItems;
        private String firstItem;
        private String lastItem;
        private Map<String, Long> mapping;

        public Long findItemDetailOffset(String item) {
            return mapping.getOrDefault(item, null);
        }

    }

}
