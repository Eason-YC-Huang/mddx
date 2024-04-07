package ink.eason.mddx.core;

import ink.eason.mddx.utils.BF;
import ink.eason.mddx.utils.Codec;
import ink.eason.mddx.utils.Pair;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@Getter
@EqualsAndHashCode
@ToString
public class ItemsDetailSect {

    private final long numOfItemDetailBlocks;
    private final long numOfItemDetails;
    private final long itemDetailBlocksMetaLen;
    private final long itemDetailBlocksLen;
    private final List<ItemsDetailBlock> itemsDetailBlockList;

    public ItemsDetailSect(ByteBuffer dataBuffer) {
        this.numOfItemDetailBlocks = dataBuffer.getLong();
        this.numOfItemDetails = dataBuffer.getLong();
        this.itemDetailBlocksMetaLen = dataBuffer.getLong();
        this.itemDetailBlocksLen = dataBuffer.getLong();

        {
            ByteBuffer buffer = BF.sliceAndMove(dataBuffer, (int) itemDetailBlocksMetaLen);
            this.itemsDetailBlockList = new ArrayList<>();

            long fileOffset = dataBuffer.position();
            long offset = 0;

            while (buffer.hasRemaining()) {
                ItemsDetailBlock itemsDetailBlock = new ItemsDetailBlock();
                itemsDetailBlock.fileOffset = fileOffset;
                itemsDetailBlock.itemBlockLen = buffer.getLong();
                itemsDetailBlock.decompressedLen = buffer.getLong();
                itemsDetailBlock.itemDetailStartOffset = offset;
                itemsDetailBlock.itemDetailEndOffset = offset + itemsDetailBlock.decompressedLen;
                this.itemsDetailBlockList.add(itemsDetailBlock);

                fileOffset += itemsDetailBlock.itemBlockLen;
                offset += itemsDetailBlock.decompressedLen;
            }
        }

    }

    public ItemsDetailBlock findItemsDetailBlock(long detailOffset) throws Exception {
        for (ItemsDetailBlock block : itemsDetailBlockList) {
            if (block.itemDetailStartOffset <= detailOffset && block.itemDetailEndOffset > detailOffset) {
                return block;
            }
        }
        return null;
    }

    @Data
    public static class ItemsDetailBlock {
        private long fileOffset;
        private long itemBlockLen;
        private long decompressedLen;
        private long itemDetailStartOffset;
        private long itemDetailEndOffset;

        public String loadAndFindItemDetail(File file, long detailOffset) throws Exception {

            try (RandomAccessFile f = new RandomAccessFile(file, "r");
                 FileChannel channel = f.getChannel()) {
                ByteBuffer content = ByteBuffer.allocate((int) itemBlockLen);
                channel.position(fileOffset);
                int bytesRead = channel.read(content);
                if (bytesRead != -1) {
                    content.flip();
                    content = Codec.Decompress.of(content)
                            .encrypted(false)
                            .checksum(false)
                            .checkDecompressedSize(decompressedLen)
                            .exec();

                    long idx = detailOffset - itemDetailStartOffset;
                    content.position((int) idx);

                    return BF.readUtf8(content);
                }
            }

            return null;
        }

    }

}
