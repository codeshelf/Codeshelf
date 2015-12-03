package com.codeshelf.model.domain;

import java.sql.Timestamp;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "facility_metric")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class FacilityMetric extends DomainObjectTreeABC<Facility>{
	public static class FacilityMetricDao extends GenericDaoABC<FacilityMetric> implements ITypedDao<FacilityMetric> {
		public final Class<FacilityMetric> getDaoClass() {
			return FacilityMetric.class;
		}
	}

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp	date;
	
	@Column(nullable = false, name = "tz")
	@Getter
	@Setter
	@JsonProperty
	private String		tz;
	
	@Column(nullable = false, name = "date_local_ui")
	@Getter
	@Setter
	@JsonProperty
	private String		dateLocalUI;

	@Column(nullable = false, name = "orders_picked")
	@Getter @Setter
	@JsonProperty
	private Integer		ordersPicked = 0;
	
	@Column(nullable = false, name = "lines_picked")
	@Getter @Setter
	@JsonProperty
	private Integer		linesPicked = 0;
	
	@Column(nullable = false, name = "lines_picked_each")
	@Getter @Setter
	@JsonProperty
	private Integer		linesPickedEach = 0;
	
	@Column(nullable = false, name = "lines_picked_case")
	@Getter @Setter
	@JsonProperty
	private Integer		linesPickedCase = 0;
	
	@Column(nullable = false, name = "lines_picked_other")
	@Getter @Setter
	@JsonProperty
	private Integer		linesPickedOther = 0;
	
	@Column(nullable = false, name = "count_picked")
	@Getter @Setter
	@JsonProperty
	private Integer		countPicked = 0;

	@Column(nullable = false, name = "count_picked_each")
	@Getter @Setter
	@JsonProperty
	private Integer		countPickedEach = 0;
	@Column(nullable = false, name = "count_picked_case")
	@Getter @Setter
	@JsonProperty
	private Integer		countPickedCase = 0;
	
	@Column(nullable = false, name = "count_picked_other")
	@Getter @Setter
	@JsonProperty
	private Integer		countPickedOther = 0;
	
	@Column(nullable = false, name = "house_keeping")
	@Getter @Setter
	@JsonProperty
	private Integer		houseKeeping = 0;
	
	@Column(nullable = false, name = "put_wall_puts")
	@Getter @Setter
	@JsonProperty
	private Integer		putWallPuts = 0;
	
	@Column(nullable = false, name = "sku_wall_puts")
	@Getter @Setter
	@JsonProperty
	private Integer		skuWallPuts = 0;
	
	@Column(nullable = false, name = "palletizer_puts")
	@Getter @Setter
	@JsonProperty
	private Integer		palletizerPuts = 0;
	
	@Column(nullable = false, name = "replenish_puts")
	@Getter @Setter
	@JsonProperty
	private Integer		replenishPuts = 0;
	
	@Column(nullable = false, name = "skip_scan_events")
	@Getter @Setter
	@JsonProperty
	private Integer		skipScanEvents = 0;
	
	@Column(nullable = false, name = "short_events")
	@Getter @Setter
	@JsonProperty
	private Integer		shortEvents = 0;
	
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp					updated;
	
	@Override
	public String getDefaultDomainIdPrefix() {
		return "Metric";
	}

	@Override
	@SuppressWarnings("unchecked")
	public final ITypedDao<FacilityMetric> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<FacilityMetric> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(FacilityMetric.class);
	}

	@Override
	public Facility getFacility() {
		return getParent();
	}
}
