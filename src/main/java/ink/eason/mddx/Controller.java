package ink.eason.mddx;

import ink.eason.mddx.core.Dict;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/search")
public class Controller {

    private Dict dict;

    @Value("${files.mdx}")
    private String mdxFiles;

    @Value("${files.mdd}")
    private String mddFiles;

    public Controller() {}

    @PostConstruct
    public void init() {
        dict = new Dict(Arrays.stream(mdxFiles.split(",")).toList(),
                Arrays.stream(mddFiles.split(",")).toList()
        );
    }


    @GetMapping("/{word}")
    public ResponseEntity<String> search(@PathVariable String word) {
        return ResponseEntity.ok(dict.search(word));
    }
}
