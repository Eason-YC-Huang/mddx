package ink.eason.mddx;

import ink.eason.mddx.core.MdxDict;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/search")
public class Controller {

    private List<MdxDict> dictList = new ArrayList<>();

    public Controller() {
        loadDictionaries();
    }

    private void loadDictionaries() {
        File file = new File("/Users/hyc/Documents/Dictionary/LDOCE5++ V 2-15.mdx");
        MdxDict mdxDict = new MdxDict(file);
        this.dictList.add(mdxDict);
    }

    @GetMapping("/{word}")
    public ResponseEntity<String> search(@PathVariable String word) {
        for (MdxDict dict : dictList) {
            String ans = dict.search(word);
            if (ans!=null) return ResponseEntity.ok(ans);
        }
        return ResponseEntity.ok("");
    }

}
