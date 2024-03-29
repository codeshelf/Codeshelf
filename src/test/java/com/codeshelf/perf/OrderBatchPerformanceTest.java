package com.codeshelf.perf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
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
import com.google.common.collect.ImmutableList;

public class OrderBatchPerformanceTest {

	final private static String	BASE_URL				= "http://localhost:8181/";
	final private static String	USERNAME				= "simulate@example.com";
	final private static String	PASSWORD				= "testme";
	// final private static String	FACILITY	= "7d16e862-32db-4270-8eda-aff14629aceb";
	final private static String	FACILITY				= "d678cc4d-3251-4be1-bfec-199393705bba";
	// Set this to something like "24 CHE1 CHE2 CHE3'
	// To look up active orders and distribute 24 to each of the 3 CHE.
	final private static List<BasicNameValuePair> POST_PARAMETER_TEMPLATE = ImmutableList.of(
		new BasicNameValuePair("ordersPerChe", "24"),
		new BasicNameValuePair("ches", "CHE1 CHE2 CHE3"));
	
	static {
		JvmProperties.load("server");
	}

	private static final Logger	LOGGER					= LoggerFactory.getLogger(OrderBatchPerformanceTest.class);
	private static NumberFormat	formatter				= new DecimalFormat("#0.00");

	CloseableHttpClient			client;
	HttpClientContext			context;

	public OrderBatchPerformanceTest() {
		CookieStore cookieStore = new BasicCookieStore();
		context = HttpClientContext.create();
		context.setCookieStore(cookieStore);
		RequestConfig config = RequestConfig.custom().setCookieSpec(CookieSpecs.BEST_MATCH).build();
		client = HttpClients.custom().setDefaultRequestConfig(config).setDefaultCookieStore(cookieStore).build();
	}

	private boolean authenticate() throws Exception {
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
			LOGGER.info("Auth complete: " + status);
			if (status == 200) {
				return true;
			}
		}
		LOGGER.error("Authentication failed.");
		return false;
	}

	private void postFile(String urlString, String fileToPost) throws Exception {
		HttpResponse response = doPost(urlString, fileToPost);

		if (response != null) {
			int status = response.getStatusLine().getStatusCode();
			if (status == 403 || status == 401) {
				if (!authenticate()) {
					LOGGER.error("Failed to re-authenticate");
					System.exit(0);
				}
				// retry
				response = doPost(urlString, fileToPost);
				if (response == null || response.getStatusLine().getStatusCode() != 200) {
					LOGGER.error("Retry failed");
					System.exit(0);
				}
			} else if (status == 404) {
				LOGGER.error("Post failed:  Valid facility id?");
				System.exit(0);
			}
			LOGGER.info("Post complete: " + status);
		}
	}

	private HttpResponse doPost(String urlString, String fileToPost) throws ClientProtocolException, IOException {
		FileInputStream is = new FileInputStream(fileToPost);
		HttpPost postRequest = new HttpPost(urlString);
		InputStreamBody isb = new InputStreamBody(is, ContentType.TEXT_PLAIN);
		HttpEntity reqEntity = MultipartEntityBuilder.create().addPart("file", isb).build();
		postRequest.setEntity(reqEntity);

		//Send request
		HttpResponse response = client.execute(postRequest, context);
		HttpEntity entity = response.getEntity();
		EntityUtils.consume(entity);

		return response;
	}

	private void postScriptTemplate() throws ClientProtocolException, IOException {
		// Paul, can you hook this up?
		LOGGER.info("Examining orders and creating script commands for optional use");
		// This will fire the SCRIPTCREATETEMPLATE string to the REST API. Transformed string is logged and returned.
		String url = BASE_URL + "api/facilities/" + FACILITY + "/test/setupManyCartsWithOrders";

		HttpPost postRequest = new HttpPost(url);
		postRequest.setEntity(new UrlEncodedFormEntity(POST_PARAMETER_TEMPLATE));
		client.execute(postRequest);

		// move the function as you wish. Currently, it would call through to WorkService.setupManyCartsWithOrders() with SCRIPTCREATETEMPLATE as the string parameter.

		//Send request
		HttpResponse response = client.execute(postRequest, context);
		HttpEntity entity = response.getEntity();
		EntityUtils.consume(entity);
	}

	public static void main(String[] args) {

		LOGGER.info("Directory name argument is: " + args[0]);
		List<String> fileNames = new ArrayList<String>();
		String orderInputDirectory = args[0];
		File[] files = new File(orderInputDirectory).listFiles();

		if (files == null) {
			LOGGER.info("No files found in " + args[0]);
			System.exit(0);
		}

		for (File file : files) {
			if (file.isFile()) {
				// PFSWeb files are coming as .txt. They definitely are not .csv files; they need three extension points to convert to our format.
				if (file.getName().endsWith(".csv") || file.getName().endsWith(".txt")) {
					fileNames.add(file.getName());
				}
			}
		}
		LOGGER.info(fileNames.size() + " files found...");
		long start = System.currentTimeMillis();

		OrderBatchPerformanceTest test = new OrderBatchPerformanceTest();

		// authenticate
		boolean authenticated = false;
		try {
			authenticated = test.authenticate();
		} catch (Exception ex) {
			LOGGER.error("Failed to authenticate", ex);
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
				String orderInputFile = orderInputDirectory + File.separator + fileName;
				br = new BufferedReader(new FileReader(orderInputFile));
				LOGGER.info("Processing order file " + orderInputFile);
				// post first order file
				String url = BASE_URL + "api/facilities/" + FACILITY + "/import/orders/";

				test.postFile(url, orderInputFile);
			} catch (Exception e) {
				LOGGER.error("Exception while running performance test", e);
				break;
			} finally {
				try {
					if (br != null) {
						br.close();
					}
				} catch (IOException e) {
					LOGGER.error("Failed "
							+ ""
							+ ""
							+ "to close stream", e);
				}
			}
			numProcessed++;
			LOGGER.info(numProcessed + " files processed in " + (System.currentTimeMillis() - startTime) / 60000 + " mins");
		}
		long end = System.currentTimeMillis();
		double duration = ((double) (end - start)) / 1000;
		LOGGER.info("Import performance test completed in " + formatter.format(duration) + "s");

		if (!POST_PARAMETER_TEMPLATE.isEmpty()) {
			try {
				test.postScriptTemplate();
			} catch (Exception e) {
				LOGGER.error("Exception while posting script template for transformation", e);
			}
		}

		System.exit(0);
	}
}
