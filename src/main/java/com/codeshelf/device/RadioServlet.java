package com.codeshelf.device;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.util.PcapRecord;
import com.codeshelf.util.PcapRingBuffer;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RadioServlet extends HttpServlet {
	private static final long	serialVersionUID	= 8642709590957174287L;
	private static final String CONTENT_TYPE_TEXT = "text/html";
	private static final String CONTENT_TYPE_BINARY = "application/octet-stream";
    private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final String NO_CACHE = "must-revalidate,no-cache,no-store";
    
    private CsDeviceManager deviceManager;
    public RadioServlet(CsDeviceManager deviceManager) {
    	this.deviceManager = deviceManager;
    }
    
    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader(CACHE_CONTROL_HEADER, NO_CACHE);

        String fmt=req.getParameter("format");
        if(fmt==null) 		fmt="html";
        //String consumeStr=req.getParameter("consume");
        //boolean consume = (consumeStr==null || consumeStr.equals("1"));
        
    	if(fmt.equals("html")) {
    		// mixed html and plain text output, consume packets as they are output
            resp.setContentType(CONTENT_TYPE_TEXT);
            PrintWriter writer = resp.getWriter();
            try {
            	prettyPrintAll(writer);
            } catch (Exception e) {
            	e.printStackTrace(writer);
            } finally {
                writer.close();
            }
    	} else if(fmt.equals("json")) {
    		// json output, consume packets as they are output
            resp.setContentType(CONTENT_TYPE_TEXT);
            PrintWriter writer = resp.getWriter();
            try {
            	jsonAll(writer);
            } catch (Exception e) {
            	e.printStackTrace(writer);
            } finally {
                writer.close();
            }
    	} else if(fmt.equals("pcap")) {
    		// mixed html and plain text output. take snapshot of ring buffer without consuming packets
            resp.setContentType(CONTENT_TYPE_BINARY);
            resp.setHeader("Content-Disposition", "attachment; filename=\"sitecontroller.pcap\"");
            
            final ServletOutputStream writer = resp.getOutputStream();
            try {
            	generatePcapFile(writer);
            } catch (Exception e) {
            	//writer.println("ERROR: "+e.getMessage());
            } finally {
                writer.close();
            }
    	}
    }
    
    private void generatePcapFile(ServletOutputStream out) {
    	PcapRingBuffer ring = this.deviceManager.getPcapBuffer();    	
    	try {
			out.write(ring.generatePcapHeader());
			out.write(ring.bytes());
		} catch (IOException e1) {
		}
	}
    
    private void jsonAll(PrintWriter out) {
		PcapRingBuffer ring = this.deviceManager.getPcapBuffer();
//    	PcapRecord[] records = null;
//		String jsonString = null;
		ObjectMapper mapper = new ObjectMapper();
    	try {
			/*
			this would grab all records without consuming - disabled pending performance analysis
			records = ring.records();
			jsonString = mapper.writeValueAsString(records);
			*/
    		PcapRecord record;
    		out.println("[");
    		while((record = ring.get())!=null) {
    			out.print(mapper.writeValueAsString(record));
    			out.println(",");
    		}
    		out.println("]");
    		
		} catch (IOException e) {
			out.print("** Ring retrieval error");
//		} catch (JsonProcessingException e) {
//			out.print("** Json Encoding Error");
//		}
//		if(jsonString != null) {
//			out.print(jsonString);
		}
    }

	private void prettyPrintAll(PrintWriter out) {
    	out.println("<html><head><title>radio traffic</title></head><body><h4>recent radio traffic</h4><pre>");
    	PcapRingBuffer ring = this.deviceManager.getPcapBuffer();
		SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
//    	PcapRecord[] records;
		try {
			/*
			this would grab all records without consuming - disabled pending performance analysis
			records = ring.records();
			if(records != null) { 
				SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
			
				for(PcapRecord record : records) {
		    		out.println(record.asText(timestampFormat));
		    	}
			}
			*/
    		PcapRecord record;
    		while((record = ring.get())!=null) {
    			out.println(String.format("%s %9s->%9s %s", 
					timestampFormat.format(new Date(record.getMicroseconds() / 1000)),
					describeNetworkAddress(record.getSourceAddress()),
					describeNetworkAddress(record.getDestinationAddress()),
					record.toString() 
					));
    		}
		} catch (IOException e) {
			e.printStackTrace(out);
		}
    	out.println("</pre><hr></body></html>");
    }

	private String describeNetworkAddress(byte netAddress) {
		if(netAddress == 0) {
			return "gateway";
		} //else
		if(netAddress == -1) {
			return "broadcast";
		} //else
		NetGuid guid = this.deviceManager.getRadioController().getNetGuidFromNetAddress(netAddress);
		if(guid != null) {
			return guid.getHexStringNoPrefix();
		} //else
		return "unknown";
	}
}
