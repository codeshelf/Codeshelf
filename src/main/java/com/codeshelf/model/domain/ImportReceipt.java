package com.codeshelf.model.domain;

import java.util.Date;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.codeshelf.model.EdiTransportType;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
//Below constraint is disabled to give PFS time to purge their duplicate receipts. Re-enabling this is DEV-1364
@Table(name = "import_receipt"/*, uniqueConstraints = {@UniqueConstraint(columnNames = {"parent_persistentid", "domainid"})}*/)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class ImportReceipt extends DomainObjectTreeABC<Facility> {

	public static class ImportReceiptDao extends GenericDaoABC<ImportReceipt> implements ITypedDao<ImportReceipt> {
		public final Class<ImportReceipt> getDaoClass() {
			return ImportReceipt.class;
		}
	}	
	
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	@Temporal(TemporalType.TIMESTAMP)
	private Date received;
		
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	@Temporal(TemporalType.TIMESTAMP)
	private Date completed;
	
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
	
	@Getter
	@Setter
	@JsonProperty
	@Column(name = "lines_failed")
	Integer linesFailed = 0;
	
	@Column(nullable = false, length=20)
	@Enumerated(EnumType.STRING)
	@Getter @Setter
	ImportType type = ImportType.Orders;
	
	@Column(nullable = true, length=20)
	@Enumerated(EnumType.STRING)
	@JsonProperty
	@Getter @Setter
	ImportStatus status;
	
	@Column(nullable = true)
	@JsonProperty
	@Getter @Setter
	String username;
	
	@Column(nullable = true)
	@JsonProperty
	@Getter @Setter
	String filename;
	
	@Column(nullable = true, columnDefinition = "TEXT", name = "order_ids")
	@Getter @Setter
	@JsonProperty
	String orderIds;

	@Column(nullable = true, columnDefinition = "TEXT", name = "item_ids")
	@Getter @Setter
	@JsonProperty
	String itemIds;

	@Column(nullable = true, columnDefinition = "TEXT")
	@Getter @Setter
	@JsonProperty
	String gtins;
	
	@Column(nullable = true, name = "transport_type")
	@Enumerated(value = EnumType.STRING)
	@Setter
	@JsonProperty
	private EdiTransportType		transportType;

	
	@Override
	public String getDefaultDomainIdPrefix() {
		return "IMPORT";
	}
	
	@SuppressWarnings("unchecked")
	public final ITypedDao<ImportReceipt> getDao() {
		return staticGetDao();
	}
	
	public static ITypedDao<ImportReceipt> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(ImportReceipt.class);
	}

	@Override
	public Facility getFacility() {
		return getParent();
	}
	
	@Override
	public String toString() {
		return String.format("ImportDataReceipt filename: %s , orders:, %d, lines: %d, seconds: %d", filename, ordersProcessed, linesProcessed, (completed.getTime()-received.getTime())/1000);
	}
	
	public void setOrderIdsList(List<String> list) {
		setOrderIds(listToCsv(list));
	}
	
	public void setItemIdsList(List<String> list) {
		setItemIds(listToCsv(list));
	}

	public void setGtinsList(List<String> list) {
		setGtins(listToCsv(list));
	}

	private String listToCsv(List<String> list) {
		StringBuilder csv = new StringBuilder();
		for (String item : list) {
			if (item != null && !item.isEmpty()) {
				csv.append(item).append(",");
			}
		}
		int csvLen = csv.length();
		//If any items given, cut off the last comma
		return csvLen > 0 ? csv.substring(0, csvLen - 1) : null;
	}

}
