<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@page import="java.io.PrintWriter"%><html>
<head>
<%@page import="org.hibernate.SessionFactory"%>
<%@page import="org.hibernate.stat.SecondLevelCacheStatistics"%>
<%@page import="java.util.*"%>
<%@page import="org.hibernate.stat.*"%>
<%@page import="com.codeshelf.platform.persistence.PersistenceService"%>
<style type="text/css">
body {background-color: white}
th {
	background-color:#A7C942;
	color:#FFFFFF;
	font-size:1em;
	padding-bottom:4px;
	padding-top:5px;
	text-align:left;
}
td, th {
	border:1px solid #98BF21;
	font-size:1em;
	padding:3px 7px 2px;
}
table  {
	border-collapse:collapse;
	font-family:"Trebuchet MS",Arial,Helvetica,sans-serif;
}
table, th, td, input, textarea {
	font-size:100%;
}
tr.alt td {
	background-color:#EAF2D3;
	color:#000000;
}
h3 {
	margin: 0px;
	padding: 20px 0px 6px 2px;
}
</style>
</head>
<body bgcolor="#FFFFFF" style="height:800px;">
<%
	SessionFactory sessionFactory = PersistenceService.getInstance().getCurrentTenantSessionFactory();
	Statistics s = sessionFactory.getStatistics();
	String x = s.toString();
	PrintWriter pw = response.getWriter();
	
	Map<String,Long> properties = new LinkedHashMap<String,Long>();
	properties.put("Connections",s.getConnectCount());
	properties.put("Session Opened",s.getSessionOpenCount());
	properties.put("Session Closed",s.getSessionCloseCount());
	properties.put("Transactions",s.getTransactionCount());
	properties.put("Successful Transactions",s.getSuccessfulTransactionCount());
	properties.put("Queries",s.getQueryExecutionCount());
	properties.put("Flushes",s.getFlushCount());
	properties.put("Max Query Time",s.getQueryExecutionMaxTime());	
	//properties.put("Query Hits",s.getQueryCacheHitCount());	
	//properties.put("Query Misses",s.getQueryCacheMissCount());	
	
	// print out properties
	pw.write("<h3>Database Statistics:</h3>");
	pw.write("<table>");
	pw.write("<tr><th>Property</th><th>Value</th></tr>");	
	int c=0;
	for (String name : properties.keySet()) {
		c++;
		if (c%2==1) {
			pw.write("<tr>");
		}
		else {
			pw.write("<tr class='alt'>");			
		}
		pw.write("<td>"+name+"</td>");
		pw.write("<td>"+properties.get(name)+"</td>");
	}
	pw.write("</table>");
	
	// print out cache region stats
	String[] regionNames = sessionFactory.getStatistics().getSecondLevelCacheRegionNames();
	Arrays.sort(regionNames);
	pw.write("<h3>Cache Regions:</h3>");
	pw.write("<table>");
	pw.write("<tr><th>Name</th><th>Hits</th><th>Misses</th><th>Ratio</th><th>Puts</th><th>Elements</th></tr>");	
	c=0;
	for (String regionName : regionNames) {
		c++;
		SecondLevelCacheStatistics statistics = sessionFactory.getStatistics().getSecondLevelCacheStatistics(regionName);
		long ratio = 0;
		if (statistics.getMissCount()+statistics.getHitCount()>0) {
			ratio = statistics.getHitCount()*100/(statistics.getMissCount()+statistics.getHitCount());
		}
		if (c%2==1) {
			pw.write("<tr>");
		}
		else {
			pw.write("<tr class='alt'>");			
		}
		pw.write("<td>"+regionName+"</td>");
		pw.write("<td>"+statistics.getHitCount()+"</td>");
		pw.write("<td>"+statistics.getMissCount()+"</td>");
		pw.write("<td>"+ratio+" %</td>");
		pw.write("<td>"+statistics.getPutCount()+"</td>");
		pw.write("<td>"+statistics.getElementCountInMemory()+"</td>");
		pw.write("</tr>");
	}
	pw.write("</table>");

	// print out entity stats
	String[] entityNames = sessionFactory.getStatistics().getEntityNames();
	Arrays.sort(entityNames);
	pw.write("<h3>Entity Statistics:</h3>");
	pw.write("<table>");
	pw.write("<tr><th>Name</th><th>Insert</th><th>Load</th><th>Fetch</th><th>Update</th><th>Delete</th></tr>");	
	c=0;
	for (String entityName : entityNames) {
		c++;
		EntityStatistics statistics = sessionFactory.getStatistics().getEntityStatistics(entityName);
		if (c%2==1) {
			pw.write("<tr>");
		}
		else {
			pw.write("<tr class='alt'>");			
		}
		pw.write("<td>"+entityName+"</td>");
		pw.write("<td>"+statistics.getInsertCount()+"</td>");
		pw.write("<td>"+statistics.getLoadCount()+"</td>");
		pw.write("<td>"+statistics.getFetchCount()+"</td>");
		pw.write("<td>"+statistics.getUpdateCount()+"</td>");
		pw.write("<td>"+statistics.getDeleteCount()+"</td>");
		pw.write("</tr>");
	}
	pw.write("</table>");
	
	// print out collection stats
	String[] collectionNames = sessionFactory.getStatistics().getCollectionRoleNames();
	Arrays.sort(collectionNames);
	pw.write("<h3>Collection Statistics:</h3>");
	pw.write("<table>");
	pw.write("<tr><th>Name</th><th>Load</th><th>Fetch</th><th>Recreate</th><th>Update</th><th>Remove</th></tr>");	
	c=0;
	for (String collectionName : collectionNames) {
		c++;
		CollectionStatistics statistics = sessionFactory.getStatistics().getCollectionStatistics(collectionName);
		long ratio = 0;
		if (c%2==1) {
			pw.write("<tr>");
		}
		else {
			pw.write("<tr class='alt'>");			
		}
		pw.write("<td>"+collectionName+"</td>");
		pw.write("<td>"+statistics.getLoadCount()+"</td>");
		pw.write("<td>"+statistics.getFetchCount()+"</td>");
		pw.write("<td>"+statistics.getRecreateCount()+"</td>");
		pw.write("<td>"+statistics.getUpdateCount()+"</td>");
		pw.write("<td>"+statistics.getRemoveCount()+"</td>");
		pw.write("</tr>");
	}
	pw.write("</table>");
	
	// print out query stats
	String[] queryNames = sessionFactory.getStatistics().getQueries();
	Arrays.sort(regionNames);
	pw.write("<h3>Query Statistics:</h3>");
	pw.write("<table>");
	pw.write("<tr><th>Name</th><th>Count</th><th>Rows</th><th>Hits</th><th>Misses</th><th>Ratio</th><th>Puts</th><th>Min</th><th>Average</th><th>Max</th></tr>");	
	c=0;
	for (String queryName : queryNames) {
		c++;
		QueryStatistics statistics = sessionFactory.getStatistics().getQueryStatistics(queryName);
		long ratio = 0;
		if (statistics.getCacheMissCount()+statistics.getCacheHitCount()>0) {
			ratio = statistics.getCacheHitCount()*100/(statistics.getCacheMissCount()+statistics.getCacheHitCount());
		}
		if (c%2==1) {
			pw.write("<tr>");
		}
		else {
			pw.write("<tr class='alt'>");			
		}
		pw.write("<td>"+queryName+"</td>");
		pw.write("<td>"+statistics.getExecutionCount()+"</td>");
		pw.write("<td>"+statistics.getExecutionRowCount()+"</td>");
		pw.write("<td>"+statistics.getCacheHitCount()+"</td>");
		pw.write("<td>"+statistics.getCacheMissCount()+"</td>");
		pw.write("<td>"+ratio+" %</td>");
		pw.write("<td>"+statistics.getCachePutCount()+"</td>");
		pw.write("<td>"+statistics.getExecutionMinTime()+"</td>");
		pw.write("<td>"+statistics.getExecutionAvgTime()+"</td>");
		pw.write("<td>"+statistics.getExecutionMaxTime()+"</td>");
		pw.write("</tr>");
	}
	pw.write("</table>");
		
	pw.flush();
%>
<br>
</body>
</html>