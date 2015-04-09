package com.codeshelf.perf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.JvmProperties;

public class OrderBatchPerformanceTest {
	
	final private static String BASE_URL = "http://localhost:8181/";
	final private static String USERNAME = "simulate@example.com";
	final private static String PASSWORD = "testme";
	final private static String FACILITY = "9345a902-ac24-4d2d-a049-54f68c2112a2";
	
	static {
		JvmProperties.load("server");
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(OrderBatchPerformanceTest.class);
	private static NumberFormat formatter = new DecimalFormat("#0.00");
	
	CloseableHttpClient client;
	HttpClientContext context;

	public OrderBatchPerformanceTest() {
        CookieStore cookieStore = new BasicCookieStore();
        context = HttpClientContext.create();
        context.setCookieStore(cookieStore);
        RequestConfig config = RequestConfig.custom().setCookieSpec(CookieSpecs.BEST_MATCH).build();       
        client = HttpClients.custom().setDefaultRequestConfig(config).setDefaultCookieStore(cookieStore).build();
	}
	
    public boolean authenticate() throws Exception {
    	String authUrl = BASE_URL + "auth/";
    	
    	List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("u", USERNAME));
        params.add(new BasicNameValuePair("p", PASSWORD));

        HttpPost postRequest = new HttpPost(authUrl);
        postRequest.setEntity(new UrlEncodedFormEntity(params));

        HttpResponse response = client.execute(postRequest);
        HttpEntity entity = response.getEntity();
        System.out.println(context.getCookieStore().getCookies());
        EntityUtils.consume(entity);
        
        // verify response if any
        if (response != null) {
        	int status = response.getStatusLine().getStatusCode();
            LOGGER.info("Auth complete: "+status);
            if (status==200) {
            	return true;
            }
        }
    	LOGGER.error("Authentication failed.");
        return false;
    }
    
    public void postFile(String urlString, InputStream is) throws Exception {
        HttpPost postRequest = new HttpPost(urlString);
        InputStreamBody isb = new InputStreamBody(is,ContentType.TEXT_PLAIN);
        HttpEntity reqEntity = MultipartEntityBuilder
        		.create()
        		.addPart("file", isb)
                .build();        
        postRequest.setEntity(reqEntity);
        
       //  System.out.println(context.getCookieStore().getCookies());

        //Send request
        HttpResponse response = client.execute(postRequest,context);
        HttpEntity entity = response.getEntity();
        EntityUtils.consume(entity);
                 
        if (response != null) {
        	int status = response.getStatusLine().getStatusCode();
        	if (status==403) {
        		if (!authenticate()) {
        			LOGGER.error("Failed to re-authenticate");
        			System.exit(0);
        		}
        		// retry
                response = client.execute(postRequest,context);
                entity = response.getEntity();
                EntityUtils.consume(entity);
                if (response.getStatusLine().getStatusCode()!=200) {
        			LOGGER.error("Retry failed");
        			System.exit(0);     	
                }
        	}
            LOGGER.info("Post complete: "+status);
        }
    }	
	
	public static void main(String[] args) {
		
		List<String> fileNames = new ArrayList<String>();
		String orderInputDirectory = args[0];
		File[] files = new File(orderInputDirectory).listFiles();

		for (File file : files) {
		    if (file.isFile()) {
		    	if (file.getName().endsWith(".csv")) {
		    		fileNames.add(file.getName());
		    	}
		    }
		}
		LOGGER.info(fileNames.size()+" files found...");
		long start = System.currentTimeMillis();
		
		OrderBatchPerformanceTest test = new OrderBatchPerformanceTest();

		// authenticate
		boolean authenticated = false;
		try {
			authenticated = test.authenticate();
		} 
		catch (Exception ex) {
			LOGGER.error("Failed to authenticate",ex);
			System.exit(0);
		}
		if (!authenticated) {
			LOGGER.error("Failed to authenticate");
			System.exit(0);
		}
		
		long startTime = System.currentTimeMillis();
		int numProcessed = 0;
		for (String fileName : fileNames) {
			BufferedReader br = null;
			try {
				String orderInputFile = orderInputDirectory+File.separator+fileName;
				br = new BufferedReader(new FileReader(orderInputFile));
				
				LOGGER.info("Processing order file "+orderInputFile);
				
				// TODO: get facility ID from API
				//String facilityId = "db495d37-8a99-4cc5-9514-a1f91475bd81";			
				//String baseUrl = "http://localhost:"+Integer.getInteger("api.port")+"/";
				
				// post first order file
				String url = BASE_URL +"api/import/orders/"+ FACILITY;			
				FileInputStream is = new FileInputStream(orderInputFile);
				test.postFile(url, is);
			} catch (Exception e) {
				LOGGER.error("Exception while running performance test", e);
				break;
			}
			finally {
				try {
					if (br!=null) {
						br.close();
					}
				} catch (IOException e) {
					LOGGER.error("Failed to close stream", e);
				}
			}
			numProcessed++;
			LOGGER.info(numProcessed+" files processed in "+(System.currentTimeMillis()-startTime)/60000+" mins");
		}
		long end = System.currentTimeMillis();
		double duration = ((double)(end - start))/1000;
		LOGGER.info("Import performance test completed in "+formatter.format(duration)+"s");
		System.exit(0);
	}
}

