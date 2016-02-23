package com.codeshelf.model.dao;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.sun.jersey.api.representation.Form;

import lombok.Getter;

public class PageQuery {
	private static final Logger					LOGGER					= LoggerFactory.getLogger(PageQuery.class);

	@Getter
	private MultivaluedMap<String, String> sourceQueryParameters;
	@Getter
	private ImmutableList<Criterion> filters;
	@Getter
	private ImmutableList<Order> orderBys;
	@Getter
	private int limit;
	
	private int start;

	public PageQuery(MultivaluedMap<String, String> srcQueryParams, ImmutableList<Criterion> filters, ImmutableList<Order> orderBys) {
		this.sourceQueryParameters = firstNonNull(srcQueryParams, new Form());
		this.filters = filters;
		this.orderBys = orderBys;
		int limit = Integer.parseInt(firstNonNull(sourceQueryParameters.getFirst("limit"),"15"));
		this.limit = limit;
		int start = Integer.parseInt(firstNonNull(sourceQueryParameters.getFirst("start"),"0"));
		this.start = start;
	}
	
	private PageQuery(PageQuery pageQuery, int start) {
		this(pageQuery.getSourceQueryParameters(), pageQuery.getFilters(), pageQuery.getOrderBys());
		this.sourceQueryParameters.putSingle("start", String.valueOf(start));
		this.start = start;
	}

	public Criteria toFilterCriteria(Criteria criteria) {
		for (Criterion expression : getFilters()) {
			criteria.add(expression);
		}		
		return criteria;
	}

	public Criteria toLimitedCriteria(Criteria criteria) {
		for (Order order : getOrderBys()) {
			criteria.addOrder(order);
		}
		
		criteria.setFirstResult(start);
		criteria.setMaxResults(limit);
		return criteria;
	}
	
	public Optional<String>  getNextQueryToken(long total) {
		int newStart = start + limit;
		if (newStart < total) {
			PageQuery nextQuery = new PageQuery(this, newStart);
			
			UriBuilder builder = UriBuilder.fromUri("/");
	        for (Map.Entry<String, List<String>> e : nextQuery.sourceQueryParameters.entrySet()) {
	            for (String value : e.getValue())
	            	builder.queryParam(e.getKey(), value);
	        }
			String queryString = builder.build(new Object[0]).getQuery();
			try {
				return Optional.of(URLEncoder.encode(builder.build(new Object[0]).getQuery(), "UTF-8"));
			} catch (UnsupportedEncodingException | IllegalArgumentException | UriBuilderException e1) {
				LOGGER.error("Should have been able to encode query string {}", queryString, e1);
				return Optional.of(queryString);
			}
		}
		else {
			return Optional.absent();
		}
	}

}