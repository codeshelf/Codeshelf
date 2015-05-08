package com.codeshelf.model.domain;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "import_receipt")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class DataImportReceipt extends DomainObjectTreeABC<Facility> {

	public static class DataImportReceiptDao extends GenericDaoABC<DataImportReceipt> implements ITypedDao<DataImportReceipt> {
		public final Class<DataImportReceipt> getDaoClass() {
			return DataImportReceipt.class;
		}
	}	
	
	// The owning facility.
	@ManyToOne(optional = false, fetch=FetchType.LAZY)
	@Getter
	@Setter
	private Facility parent;
	
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private long received;
	
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private long started;
	
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private long completed;
	
	@Getter
	@Setter
	@JsonProperty
	@Column(name = "orders_processed")
	int ordersProcessed = 0;

	@Getter
	@Setter
	@JsonProperty
	@Column(name = "lines_processed")
	int linesProcessed = 0;
	
	@Column(nullable = false, length=20)
	@Enumerated(EnumType.STRING)
	@Getter @Setter
	DataImportType type = DataImportType.Orders;
	
	@Column(nullable = true, length=20)
	@Enumerated(EnumType.STRING)
	@Getter @Setter
	DataImportStatus status;
	
	@Override
	public String getDefaultDomainIdPrefix() {
		return "IMPORT-";
	}
	
	@SuppressWarnings("unchecked")
	public final ITypedDao<DataImportReceipt> getDao() {
		return staticGetDao();
	}
	
	public static ITypedDao<DataImportReceipt> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(DataImportReceipt.class);
	}

	@Override
	public Facility getFacility() {
		return this.parent;
	}

}
