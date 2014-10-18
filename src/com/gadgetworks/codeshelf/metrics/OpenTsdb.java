package com.gadgetworks.codeshelf.metrics;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

/**
 * OpenTSDB 2.0 jersey based REST client
 */
public class OpenTsdb {

	@SuppressWarnings("unused")
	private static final Logger	LOGGER = LoggerFactory.getLogger(OpenTsdb.class);

    public static final int DEFAULT_BATCH_SIZE_LIMIT = 1;
    public static final int CONN_TIMEOUT_DEFAULT_MS = 5000;
    public static final int READ_TIMEOUT_DEFAULT_MS = 5000;

    /**
     * Initiate a client Builder with the provided base opentsdb server url.
     *
     * @param baseUrl
     * @return
     */
    public static Builder forService(String baseUrl) {
        return new Builder(baseUrl);
    }

    /**
     * create a client by providing the underlying WebResource
     *
     * @param apiResource
     */
    public static OpenTsdb create(WebResource apiResource) {
        return new OpenTsdb(apiResource);
    }

    private final WebResource apiResource;
    private int batchSizeLimit = DEFAULT_BATCH_SIZE_LIMIT;

    public static class Builder {
        private Integer connectionTimeout = CONN_TIMEOUT_DEFAULT_MS;
        private Integer readTimeout = READ_TIMEOUT_DEFAULT_MS;
        private String baseUrl;

        public Builder(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Builder withConnectTimeout(Integer connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder withReadTimeout(Integer readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public OpenTsdb create() {
            return new OpenTsdb(baseUrl, connectionTimeout, readTimeout);
        }
    }

    private OpenTsdb(WebResource apiResource) {
        this.apiResource = apiResource;
    }

    private OpenTsdb(String baseURL, Integer connectionTimeout, Integer readTimeout) {

        final ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

        final Client client = Client.create(clientConfig);
        client.setConnectTimeout(connectionTimeout);
        client.setReadTimeout(readTimeout);

        this.apiResource = client.resource(baseURL);
    }

    public void setBatchSizeLimit(int batchSizeLimit) {
        this.batchSizeLimit = batchSizeLimit;
    }

    /**
     * Send a metric to opentsdb
     *
     * @param metric
     * @throws ReportingException 
     */
    public void send(OpenTsdbMetric metric) throws ReportingException {
        send(Collections.singleton(metric));
    }

    /**
     * send a set of metrics to opentsdb
     *
     * @param metrics
     */
    public void send(Set<OpenTsdbMetric> metrics) throws ReportingException {
        // we set the patch size because of existing issue in opentsdb where large batch of metrics failed
        // see at https://groups.google.com/forum/#!topic/opentsdb/U-0ak_v8qu0
        // we recommend batch size of 5 - 10 will be safer
        // alternatively you can enable chunked request
        if (batchSizeLimit > 0 && metrics.size() > batchSizeLimit) {
            final Set<OpenTsdbMetric> smallMetrics = new HashSet<OpenTsdbMetric>();
            for (final OpenTsdbMetric metric: metrics) {
            	// skip undefined metrics
            	/*
            	if (metric.getValue()==null) continue;
            	if (metric.getValue().getClass().isArray()) {
            		Object[] v = (Object[]) metric.getValue();
            		if (v.length==0) continue;
            	}            	
            	*/
            	// exclude deadlock metric
            	if (metric.getMetric().equals("jvm-thread.deadlocks")) continue;
            	// add metric to list and send if batch size reached
                smallMetrics.add(metric);
                if (smallMetrics.size() >= batchSizeLimit) {
                    sendHelper(smallMetrics);
                    smallMetrics.clear();
                }
            }
            sendHelper(smallMetrics);
        } else {
            sendHelper(metrics);
        }
    }

    private void sendHelper(Set<OpenTsdbMetric> metrics) throws ReportingException {
        /*
         * might want to bind to a specific version of the API.
         * according to: http://opentsdb.net/docs/build/html/api_http/index.html#api-versioning
         * "if you do not supply an explicit version, ... the latest version will be used."
         * circle back on this if it's a problem.
         */
        if (!metrics.isEmpty()) {
        	String jsonString="";
            try {
            	// create json message using jackson to avoid jaxb generics issue
    			ObjectMapper mapper = new ObjectMapper();
    			jsonString = mapper.writeValueAsString(metrics);
    			// System.out.println(jsonString);
                apiResource.path("/api/put")
                        .type(MediaType.APPLICATION_JSON)
                        //.entity(metrics)
                        .entity(jsonString)
                        .post();
            } catch (Exception ex) {
            	LOGGER.error("Failed to post metrics: "+jsonString);
            	throw new ReportingException(ex.getMessage());
            }
        }
    }

}