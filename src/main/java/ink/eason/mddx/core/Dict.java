package ink.eason.mddx.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Dict {

    private static final Logger logger = LoggerFactory.getLogger(Dict.class);
    private final List<Mddx> dictList = new ArrayList<>();

    public Dict(List<String> mdxList, List<String> mddList) {
        logger.info("Start loading dictionaries");
        mdxList.stream().map(file -> new Mddx(new File(file), false)).forEach(dictList::add);
        logger.info("Finished loading mdx files");
        mddList.stream().map(file -> new Mddx(new File(file), true)).forEach(dictList::add);
        logger.info("Finished loading mdd files");
    }

    public String search(String key) {
        if (key.endsWith("css") || key.endsWith("js") || key.endsWith("mp3")) {
            key = "\\" + key;
        }
        for (Mddx dict : dictList) {
            String ans = dict.search(key);
            if (ans != null) return ans;
        }
        return null;
    }


    public static void main(String[] args) {

        Dict dict = new Dict(Collections.singletonList("/Users/hyc/Documents/Dictionary/LDOCE5++ V 2-15.mdx"),
                Collections.singletonList("/Users/hyc/Documents/Dictionary/LDOCE5++ V 2-15.mdd"));

        System.out.println(dict.search("key"));
        System.out.println(dict.search("LM5style.css"));

    }


}
