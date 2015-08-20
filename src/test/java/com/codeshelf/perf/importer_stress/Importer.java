package com.codeshelf.perf.importer_stress;
import java.io.File;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.codeshelf.application.JvmProperties;

/** 
 * @author Ilya
 * This class makes a single call to the 4 importers: Aisles, Locations, Inventory, and Orders
 * It is not expected to fail, and acts as a sanity test before "StresserTest" is ran 
 */
public class Importer {
	static {
		JvmProperties.load("server");
	}
	
	private static final String SERVER = "http://localhost:" + Integer.getInteger("api.port") + "/";
	private static String cookies = null;
	private static String facilityId = null;
	
	public static void main(String[] args) throws Exception{
		System.out.println("Working Directory = " + System.getProperty("user.dir"));
		Importer importer = new Importer();
		importer.auth();
		importer.getFacility();
		importer.postAisles();
		importer.postLocations();
		importer.postInventory();
		importer.postOrders();
		System.out.println("Done");
	}
	
	protected int postAisles() throws Exception {
		String url = SERVER + "api/facilities/"+ facilityId + "/import/site/";
		String filename = "facility-setup-script/aisles.csv";
		return post(url, filename);
	}
	
	protected int postLocations() throws Exception {
		String url = SERVER + "api/facilities/"+ facilityId + "/import/locations/";
		String filename = "facility-setup-script/locations.csv";
		return post(url, filename);
	}
	
	protected int postInventory() throws Exception {
		String url = SERVER + "api/facilities/"+ facilityId + "/import/inventory/";
		String filename = "facility-setup-script/inventory.csv";
		return post(url, filename);
	}
	
	protected int postOrders() throws Exception {
		String url = SERVER + "api/facilities/"+ facilityId + "/import/orders/";
		String filename = "facility-setup-script/orders_10.csv";
		return post(url, filename);
	}

	protected static void init() throws Exception {
		Importer importer = new Importer();
		importer.auth();
		importer.getFacility();
	}
		
	protected void auth() throws Exception{
		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(SERVER + "auth/");
		ArrayList<NameValuePair> postParameters = new ArrayList<>();
		postParameters.add(new BasicNameValuePair("u", "simulate@example.com"));
		postParameters.add(new BasicNameValuePair("p", "testme"));
		post.setEntity(new UrlEncodedFormEntity(postParameters));

		System.out.println("POST authentication");
		HttpResponse response = client.execute(post);
		System.out.println(response.getStatusLine());
		Header cookieHeader = response.getFirstHeader("Set-Cookie");
		cookies = cookieHeader.getValue();
		System.out.println("Cookies = " + cookies + "\n");
	}

	protected void getFacility() throws Exception {
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet(SERVER + "api/facilities/");
		get.addHeader("Cookie", cookies);
		
		System.out.println("Get Facilities");
		HttpResponse response = client.execute(get);
		System.out.println(response.getStatusLine());
		HttpEntity resEntity = response.getEntity();
		String responseString = EntityUtils.toString(resEntity, "UTF-8");
		System.out.println(responseString + "\n");
		
		JSONArray facilityList = new JSONArray(responseString);
		JSONObject facility = facilityList.getJSONObject(0);
		facilityId = facility.getString("persistentId");
	}
	
	private int post(String url, String filename) throws Exception{
		HttpClient client = HttpClientBuilder.create().build();
		HttpPost httppost = new HttpPost(url);
		httppost.addHeader("Cookie", cookies);

		FileBody file = new FileBody(new File(filename));
		
		MultipartEntityBuilder builder = MultipartEntityBuilder.create(); 
		builder.addPart("file", file);
		httppost.setEntity(builder.build());

		System.out.println("POST " + url);
		HttpResponse response = client.execute(httppost);
		System.out.println(response.getStatusLine() + "\n");
		return response.getStatusLine().getStatusCode();
	}
}
