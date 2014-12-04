package rsssucker.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class HttpUtils {

    /** Return final location from http redirects  */
    public static String resolveHttpRedirects(String uri) 
            throws IOException, URISyntaxException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpClientContext context = HttpClientContext.create();
        HttpGet httpget = new HttpGet(uri);
        //httpget.addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:17.0) Gecko/20121202 Firefox/17.0 Iceweasel/17.0.1");        
        CloseableHttpResponse response = httpclient.execute(httpget, context);          
        try {
            HttpHost target = context.getTargetHost();
            List<URI> redirectLocations = context.getRedirectLocations();
            URI location = URIUtils.resolve(httpget.getURI(), target, redirectLocations);
            return location.toString();
        } finally {
            response.close();           
        }                
    }
    
    /** Exctract redirect target from google's redirect url */
    public static String resolveGoogleRedirect(String url) 
            throws URISyntaxException, UnsupportedEncodingException {
        String query = new URI(url).getRawQuery();        
        for (String param : query.split("&")) if (param.contains("=")) {
            String [] parts = param.split("=");
            if (parts.length == 2 && parts[0].equals("url")) {
                return URLDecoder.decode(parts[1], "UTF-8");
            }
        }                
        throw new IllegalArgumentException("google redirect not found for url: " + url);
    }
    
}
