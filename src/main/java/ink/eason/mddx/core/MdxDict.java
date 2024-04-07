package ink.eason.mddx.core;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class MdxDict {

    private final File dict;
    private final HeaderSect headerSect;
    private final ItemsSect itemsSect;
    private final ItemsDetailSect itemsDetailSect;

    public MdxDict(File dict) {
        this.dict = dict;
        try {
            ByteBuffer dataBuffer = ByteBuffer.wrap(Files.readAllBytes(dict.toPath()));
            this.headerSect = new HeaderSect(dataBuffer);
            this.itemsSect = new ItemsSect(dataBuffer);
            this.itemsDetailSect = new ItemsDetailSect(dataBuffer);
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
            String detail = itemsDetailBlock.loadAndFindItemDetail(dict, itemDetailOffset);
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

    public static void main(String[] args) {
        MdxDict mdxDict = new MdxDict(new File("/Users/hyc/Documents/Dictionary/LDOCE5++ V 2-15.mdd"));
        System.out.println(mdxDict.search("LM5style.css"));
    }


}
