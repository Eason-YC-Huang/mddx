package ink.eason.mddx.core;

import ink.eason.mddx.utils.BF;
import ink.eason.mddx.utils.Codec;
import ink.eason.mddx.utils.Pair;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static ink.eason.mddx.utils.BF.sliceAndMove;

@Getter
@ToString
@EqualsAndHashCode
public class ItemsSect {

    private final long numOfItemBlocks;
    private final long numOfItems;
    private final long itemBlocksIndexDecompressedLen;
    private final long itemBlocksIndexLen;
    private final long itemBlockLen;
    // checksum for above 40 bytes, for version 2 only
    private final long checksum;

    private final List<ItemsBlock> itemsBlocks;

    public ItemsSect(ByteBuffer dataBuffer) {

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
                itemsBlock.firstItem = StandardCharsets.UTF_8.decode(sliceAndMove(content, firstValLen)).toString();
                content.get();//skip null
                short lastValLen = content.getShort();
                itemsBlock.lastItem = StandardCharsets.UTF_8.decode(sliceAndMove(content, lastValLen)).toString();
                content.get();//skip null
                itemsBlock.itemBlockLen = content.getLong();
                itemsBlock.decompressedLen = content.getLong();
                itemsBlocks.add(itemsBlock);
            }
        }

        {
            for (int i = 0, itemsBlocksSize = itemsBlocks.size(); i < itemsBlocksSize; i++) {
                ItemsBlock itemsBlock = itemsBlocks.get(i);
                itemsBlock.offset = dataBuffer.position();
                ByteBuffer content = sliceAndMove(dataBuffer, (int) itemsBlock.itemBlockLen);
                content = Codec.Decompress.of(content)
                        .checkDecompressedSize(itemsBlock.decompressedLen)
                        .checksum(true)
                        .exec();
                Map<String, Long> mapping = new LinkedHashMap<>();
                while (content.hasRemaining()) {
                    long itemDetailOffset = content.getLong();
                    String item = BF.readUtf8(content);
                    // why we have duplicated items here?
                    mapping.putIfAbsent(item, itemDetailOffset);
                }
                itemsBlock.mapping = mapping;
            }
        }

    }

    public Long findItemDetailOffset(String item) {
        for (ItemsBlock block : itemsBlocks) {
            if (block.firstItem.compareTo(item) <= 0 && block.lastItem.compareTo(item) >= 0) {
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
