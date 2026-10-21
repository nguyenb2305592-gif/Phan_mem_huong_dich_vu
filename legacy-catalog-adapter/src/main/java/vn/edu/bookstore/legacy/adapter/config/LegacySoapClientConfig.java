package vn.edu.bookstore.legacy.adapter.config;

import java.net.URL;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.edu.bookstore.legacy.adapter.soap.generated.BookCatalogPortType;
import vn.edu.bookstore.legacy.adapter.soap.generated.BookCatalogService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.Handler;
import vn.edu.bookstore.legacy.adapter.soap.SoapLoggingHandler;

@Configuration
public class LegacySoapClientConfig {

    /** Thuộc tính timeout của Metro (Jakarta XML WS RI), đơn vị mili giây. */
    static final String CONNECT_TIMEOUT = "com.sun.xml.ws.connect.timeout";
    static final String REQUEST_TIMEOUT = "com.sun.xml.ws.request.timeout";

    @Bean
    BookCatalogPortType bookCatalogPort(LegacySoapProperties props) {
        URL wsdl = LegacySoapClientConfig.class.getResource("/wsdl/BookCatalogService.wsdl");
        BookCatalogPortType port = new BookCatalogService(wsdl).getBookCatalogPort();
        // TODO(TH1-4): ép kiểu port sang BindingProvider, đặt vào request context:
        //   - ENDPOINT_ADDRESS_PROPERTY = props.endpoint()
        //   - CONNECT_TIMEOUT / REQUEST_TIMEOUT (mili giây) từ props
        // TODO(TH1-5): nếu props.logMessages() thì thêm SoapLoggingHandler vào handler chain

    	Map<String, Object> ctx = ((BindingProvider) port).getRequestContext();
    	ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, props.endpoint());
	ctx.put("com.sun.xml.ws.connect.timeout", (int) props.connectTimeout().toMillis());
	ctx.put("com.sun.xml.ws.request.timeout", (int) props.readTimeout().toMillis());

    	// TH1-5: gắn handler ghi log thông điệp SOAP vào/ra
    	if (props.logMessages()) {
        	@SuppressWarnings("rawtypes")
        	List<Handler> chain = new ArrayList<>(((BindingProvider) port).getBinding().getHandlerChain());
        	chain.add(new SoapLoggingHandler());
        	((BindingProvider) port).getBinding().setHandlerChain(chain);
    	}
        return port;
    }
}
