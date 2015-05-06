package com.codeshelf.perf;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.JvmProperties;

public class ImportPerformanceTest {
	
	static {
		JvmProperties.load("server");
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(ImportPerformanceTest.class);
	private static NumberFormat formatter = new DecimalFormat("#0.00");     

    public void postFile(String authUrl, String urlString, InputStream is) throws Exception {
		CloseableHttpClient client = HttpClients.createDefault();
		
		//Authenticate
		auth(client, authUrl, "simulate@example.com", "testme");
		
        HttpPost postRequest = new HttpPost (urlString) ;
        // create entity            
        InputStreamBody isb = new InputStreamBody(is,ContentType.TEXT_PLAIN);
        // StringBody comment = new StringBody("A binary file of some kind", ContentType.TEXT_PLAIN);
        
        HttpEntity reqEntity = MultipartEntityBuilder
        		.create()
        		.addPart("file", isb)
//				.addPart("comment", comment)
                .build();
        
        postRequest.setEntity(reqEntity);

        //Send request
        HttpResponse response = client.execute(postRequest);
        //Verify response if any
        if (response != null) {
            LOGGER.info("Post complete: "+response.getStatusLine().getStatusCode());
        }
    }	
    
    private void auth(CloseableHttpClient client, String authUrl, String username, String password) throws Exception{
    	HttpPost authRequest = new HttpPost(authUrl);
    	ArrayList<NameValuePair> postParameters = new ArrayList<>();
    	postParameters.add(new BasicNameValuePair("u", username));
    	postParameters.add(new BasicNameValuePair("p", password));
    	authRequest.setEntity(new UrlEncodedFormEntity(postParameters));
    	client.execute(authRequest);
    }
	
	public static void main(String[] args) {
		long start = System.currentTimeMillis();

		ImportPerformanceTest test = new ImportPerformanceTest();
		try {
			ClassLoader classloader = Thread.currentThread().getContextClassLoader();
			// TODO: get facility ID from API
			String facilityId = "7d16e862-32db-4270-8eda-aff14629aceb";
			
			LOGGER.info("Started performance import test started...");
			

			String baseUrl = "http://localhost:"+Integer.getInteger("api.port");
			String authUrl = baseUrl + "/auth/";
			String baseImportUrl = baseUrl + "/api/facilities/" + facilityId + "/import";
						
			// post aisle file
			String url = baseImportUrl + "/site";			
			InputStream is = classloader.getResourceAsStream("perftest/aisles.csv");
			test.postFile(authUrl, url, is);

			// post location mappings
			url = baseImportUrl + "/locations";
			is = classloader.getResourceAsStream("perftest/location-mapping.csv");
			test.postFile(authUrl, url, is);

			// post inventory
			url = baseImportUrl + "/inventory";	
			is = classloader.getResourceAsStream("perftest/inventory.csv");
			test.postFile(authUrl, url, is);

			// post first order file
			url = baseImportUrl + "/orders";			
			is = classloader.getResourceAsStream("perftest/orders-1.csv");
			test.postFile(authUrl, url, is);

			// post second order file
			url = baseImportUrl + "/orders";			
			is = classloader.getResourceAsStream("perftest/orders-2.csv");
			test.postFile(authUrl, url, is);

			// post third order file
			url = baseImportUrl + "/orders";			
			is = classloader.getResourceAsStream("perftest/orders-3.csv");
			test.postFile(authUrl, url, is);
			
		} catch (Exception e) {
			LOGGER.error("Exception while running performance test", e);
		}
		long end = System.currentTimeMillis();
		double duration = ((double)(end - start))/1000;
		LOGGER.info("Import performance test completed in "+formatter.format(duration)+"s");
		System.exit(0);
	}
}

