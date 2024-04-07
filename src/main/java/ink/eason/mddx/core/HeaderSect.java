package ink.eason.mddx.core;

import ink.eason.mddx.utils.BF;
import ink.eason.mddx.utils.Codec;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Getter
public class HeaderSect {

    private static final Logger logger = LoggerFactory.getLogger(HeaderSect.class);

    private String text;
    private Charset encoding;

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

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(this.text)));
            Element element = doc.getDocumentElement();
            String encoding = element.getAttribute("Encoding");
            this.encoding = encoding.isEmpty() ? StandardCharsets.UTF_16LE : Charset.forName(encoding);
        } catch (Exception e) {
            logger.error("Failed to get Encoding from: {}", this.text, e);
            this.encoding = StandardCharsets.UTF_16LE;
        }
    }

}
