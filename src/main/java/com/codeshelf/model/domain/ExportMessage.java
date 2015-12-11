package com.codeshelf.model.domain;

import java.sql.Timestamp;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.codeshelf.edi.ExportMessageFuture;
import com.codeshelf.edi.ExportMessageFuture.OrderOnCartAddedExportMessage;
import com.codeshelf.edi.ExportMessageFuture.OrderOnCartFinishedExportMessage;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "export_message", uniqueConstraints = {@UniqueConstraint(columnNames = {"parent_persistentid", "domainid"})})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class ExportMessage extends DomainObjectTreeABC<Facility>{
	public static class ExportMessageDao extends GenericDaoABC<ExportMessage> implements ITypedDao<ExportMessage> {
		public final Class<ExportMessage> getDaoClass() {
			return ExportMessage.class;
		}
	}
	
	public enum ExportMessageType {ORDER_ON_CART_ADDED, ORDER_ON_CART_FINISHED}
	//private static final Logger		LOGGER				= LoggerFactory.getLogger(ExportMessage.class);
	
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Boolean				active;
	
	@Column(nullable = false, columnDefinition = "TEXT")
	@Getter @Setter
	@JsonProperty
	String contents;
	
	@Column(nullable = true, name = "type")
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private ExportMessageType	type;
	
	@Column(nullable = true, name = "orderid")
	@Getter @Setter
	@JsonProperty
	private String	orderId;
	
	@Column(nullable = true, name = "cheguid")
	@Getter @Setter
	@JsonProperty
	private String	cheGuid;
	
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp					created;


	@SuppressWarnings("unchecked")
	public final ITypedDao<ExportMessage> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<ExportMessage> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(ExportMessage.class);
	}

	@Override
	public String getDefaultDomainIdPrefix() {
		return "ExMsg";
	}

	@Override
	public Facility getFacility() {
		return getParent();
	}
	
	public ExportMessage() {
		super();
	}
	
	public ExportMessage(Facility facility, ExportMessageFuture message) {
		setParent(facility);
		setContents(message.getContents());
		setActive(true);
		setOrderId(message.getOrderId());
		setCheGuid(message.getCheGuid());
		setDomainId(getDefaultDomainIdPrefix() + "_" + message.getOrderId() + "_" + System.currentTimeMillis());
		if (message instanceof OrderOnCartAddedExportMessage) {
			setType(ExportMessageType.ORDER_ON_CART_ADDED);
		} else if (message instanceof OrderOnCartFinishedExportMessage) {
			setType(ExportMessageType.ORDER_ON_CART_FINISHED);
		}
		setCreated(new Timestamp(System.currentTimeMillis()));
	}

	public ExportMessageFuture toExportMessageFuture(){
		ExportMessageFuture exportMessage = null;
		if (type == ExportMessageType.ORDER_ON_CART_ADDED) {
			exportMessage = new OrderOnCartAddedExportMessage(getOrderId(), getCheGuid(), getContents());
			exportMessage.setPersistentId(getPersistentId());
		} else if (type == ExportMessageType.ORDER_ON_CART_FINISHED) {
			exportMessage = new OrderOnCartFinishedExportMessage(getOrderId(), getCheGuid(), getContents());
			exportMessage.setPersistentId(getPersistentId());
		}
		return exportMessage;
	}
}