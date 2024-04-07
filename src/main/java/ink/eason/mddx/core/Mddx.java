package ink.eason.mddx.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Mddx {
    private static final Logger logger = LoggerFactory.getLogger(Mddx.class);

    private final File dict;
    private final HeaderSect headerSect;
    private final ItemsSect itemsSect;
    private final ItemsDetailSect itemsDetailSect;

    private final boolean isMddFile;

    public Mddx(File dict, boolean isMddFile) {
        this.dict = dict;
        this.isMddFile = isMddFile;
        try {
            ByteBuffer dataBuffer = ByteBuffer.wrap(Files.readAllBytes(dict.toPath()));
            this.headerSect = new HeaderSect(dataBuffer);
            this.itemsSect = new ItemsSect(dataBuffer, this.headerSect.getEncoding(), isMddFile);
            this.itemsDetailSect = new ItemsDetailSect(dataBuffer, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String search(String key) {
        try {
            Long itemDetailOffset = itemsSect.findItemDetailOffset(key);
            if (itemDetailOffset == null) return null;

            ItemsDetailSect.ItemsDetailBlock itemsDetailBlock = itemsDetailSect.findItemsDetailBlock(itemDetailOffset);
            if (itemsDetailBlock == null) return null;

            String detail = itemsDetailBlock.loadAndFindItemDetail(dict, isMddFile, itemDetailOffset);
            if (detail == null) return null;

            detail = detail.trim();
            if (detail.startsWith("@@@LINK=")) {
                return search(detail.substring(8));
            } else {
                return detail;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
