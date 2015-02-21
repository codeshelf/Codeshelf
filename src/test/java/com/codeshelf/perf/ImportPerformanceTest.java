package com.codeshelf.perf;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.JvmProperties;

public class ImportPerformanceTest {
	
	static {
		JvmProperties.load("server");
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(ImportPerformanceTest.class);
	private static NumberFormat formatter = new DecimalFormat("#0.00");     

    public void postFile(String urlString, InputStream is) throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        
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
	
	public static void main(String[] args) {
		long start = System.currentTimeMillis();

		ImportPerformanceTest test = new ImportPerformanceTest();
		try {
			ClassLoader classloader = Thread.currentThread().getContextClassLoader();
			// TODO: get facility ID from API
			String facilityId = "df993559-ea88-4a98-808d-0a90c8cc92cd";
			
			LOGGER.info("Started performance import test started...");
			
			String baseUrl = "http://localhost:"+Integer.getInteger("api.port")+"/";
			// post aisle file
			String url = baseUrl+"api/import/site/"+facilityId;			
			InputStream is = classloader.getResourceAsStream("perftest/aisles.csv");
			test.postFile(url, is);

			// post location mappings
			url = baseUrl+"api/import/locations/"+facilityId;			
			is = classloader.getResourceAsStream("perftest/location-mapping.csv");
			test.postFile(url, is);

			// post inventory
			url = baseUrl+"api/import/inventory/"+facilityId;	
			is = classloader.getResourceAsStream("perftest/inventory.csv");
			test.postFile(url, is);

			// post first order file
			url = baseUrl+"api/import/orders/"+facilityId;			
			is = classloader.getResourceAsStream("perftest/orders-1.csv");
			test.postFile(url, is);

			// post second order file
			url = baseUrl+"api/import/orders/"+facilityId;			
			is = classloader.getResourceAsStream("perftest/orders-2.csv");
			test.postFile(url, is);

			// post third order file
			url = baseUrl+"api/import/orders/"+facilityId;			
			is = classloader.getResourceAsStream("perftest/orders-3.csv");
			test.postFile(url, is);
			
		} catch (Exception e) {
			LOGGER.error("Exception while running performance test", e);
		}
		long end = System.currentTimeMillis();
		double duration = ((double)(end - start))/1000;
		LOGGER.info("Import performance test completed in "+formatter.format(duration)+"s");
		System.exit(0);
	}
}

