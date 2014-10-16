package com.gadgetworks.codeshelf.device;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gadgetworks.codeshelf.util.PcapRecord;
import com.gadgetworks.codeshelf.util.PcapRingBuffer;

public class RadioServlet extends HttpServlet {
	private static final long	serialVersionUID	= 8642709590957174287L;
	private static final String CONTENT_TYPE = "text/html";
    private static final String CACHE_CONTROL = "Cache-Control";
    private static final String NO_CACHE = "must-revalidate,no-cache,no-store";

    private ICsDeviceManager deviceManager;
    public RadioServlet(ICsDeviceManager deviceManager) {
    	this.deviceManager = deviceManager;
    }
    
    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader(CACHE_CONTROL, NO_CACHE);
        resp.setContentType(CONTENT_TYPE);
        final PrintWriter writer = resp.getWriter();
        try {
        	prettyPrintAll(writer);
        } catch (Exception e) {
        	writer.println("ERROR");
        } finally {
            writer.close();
        }
    }
    
    private void prettyPrintAll(PrintWriter out) throws IOException {
    	out.println("<html><head><title>radio traffic</title></head><body><h4>recent radio traffic</h4>");
    	PcapRecord record;
    	PcapRingBuffer ring = this.deviceManager.getPcapBuffer();
    	while((record = ring.get()) != null) {
    		out.println(record.toString());
    		out.println("<br/>");
    	}
    	out.println("</body></html>");
    }
}
