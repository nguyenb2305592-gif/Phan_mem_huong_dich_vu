package vn.edu.bookstore.legacy.adapter.api;

import java.util.regex.Pattern;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.bookstore.legacy.adapter.soap.LegacyCatalogClient;

/** API REST bọc dịch vụ SOAP kế thừa: /api/v1/legacy/books */
@RestController
@RequestMapping("/api/v1/legacy/books")
public class LegacyBookController {

    private static final Pattern ISBN13 = Pattern.compile("^97[89][0-9]{10}$");
    private final LegacyCatalogClient client;

    public LegacyBookController(LegacyCatalogClient client) {
        this.client = client;
    }

    @GetMapping("/{isbn}")
    public BookView getBook(@PathVariable String isbn) {
        // TODO(TH1-6): kiểm tra isbn khớp ^97[89][0-9]{10}$ (sai → InvalidRequestException),
        //              gọi client.getBook(isbn) rồi BookView.from(...)
    	if (!ISBN13.matcher(isbn).matches()) {
        	throw new InvalidRequestException("isbn phải khớp mẫu ^97[89][0-9]{10}$");
    	}
    	return BookView.from(client.getBook(isbn));
    }

    @GetMapping
    public List<BookView> search(@RequestParam("q") String keyword,
                                 @RequestParam(name = "limit", defaultValue = "10") int limit) {
        // TODO(TH1-6): q ít nhất 2 ký tự, limit 1..50; gọi client.search(...)
	if (keyword.isBlank() || keyword.trim().length() < 2) {
        	throw new InvalidRequestException("q phải có ít nhất 2 ký tự");
    	}
    	if (limit < 1 || limit > 50) {
        	throw new InvalidRequestException("limit phải trong khoảng 1..50");
    	}
    	return client.search(keyword.trim(), limit).stream().map(BookView::from).toList();
    }
}
