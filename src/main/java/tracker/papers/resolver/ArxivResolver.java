package tracker.papers.resolver;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** Looks up arXiv links (https://arxiv.org/abs/...) through the arXiv API, which answers in Atom XML. */
@Component
public class ArxivResolver implements PaperResolver {

    private static final String ABS_PREFIX = "https://arxiv.org/abs/";
    private static final String ATOM = "http://www.w3.org/2005/Atom";
    private static final String ARXIV = "http://arxiv.org/schemas/atom";

    private final RestClient http;

    public ArxivResolver(@Value("${papers.arxiv-url:https://export.arxiv.org}") String baseUrl) {
        this.http = ResolverHttp.client(baseUrl);
    }

    @Override
    public boolean supports(String normalizedUrl) {
        return normalizedUrl.startsWith(ABS_PREFIX);
    }

    @Override
    public Optional<ResolvedMetadata> resolve(String normalizedUrl) {
        String id = normalizedUrl.substring(ABS_PREFIX.length());
        String body = http.get().uri("/api/query?id_list={id}", id).retrieve().body(String.class);

        Element entry = firstEntry(parse(body));
        // an unknown id gives an empty feed; a malformed id gives an entry whose id points at /api/errors
        if (entry == null || text(entry, ATOM, "id").contains("/api/errors")) {
            return Optional.empty();
        }
        String doi = text(entry, ARXIV, "doi").trim();
        return Optional.of(new ResolvedMetadata(
                text(entry, ATOM, "title").replaceAll("\\s+", " ").trim(),
                authors(entry),
                LocalDate.parse(text(entry, ATOM, "published").substring(0, 10)),
                doi.isEmpty() ? null : doi));
    }

    private static Document parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | SAXException e) {
            throw new IllegalStateException("Could not read the arXiv response", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Element firstEntry(Document feed) {
        NodeList entries = feed.getElementsByTagNameNS(ATOM, "entry");
        return entries.getLength() == 0 ? null : (Element) entries.item(0);
    }

    private static List<String> authors(Element entry) {
        List<String> names = new ArrayList<>();
        NodeList authors = entry.getElementsByTagNameNS(ATOM, "author");
        for (int i = 0; i < authors.getLength(); i++) {
            names.add(text((Element) authors.item(i), ATOM, "name").trim());
        }
        return names;
    }

    private static String text(Element parent, String namespace, String name) {
        NodeList nodes = parent.getElementsByTagNameNS(namespace, name);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent();
    }
}
