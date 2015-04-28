package com.codeshelf.manager;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.hibernate.annotations.NaturalId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@Table(name = "security_question")
@EqualsAndHashCode(callSuper = false, of = { "id", "question" })
public class SecurityQuestion {
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(SecurityQuestion.class);
 
	@Id
	@Column(nullable = false, name = "id")
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Getter
	@Setter
	//@JsonProperty
	int							id;

	/* Timestamped entity */
	@Getter
	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable = false, name = "created")
	//@JsonProperty
	Date						created;
	//
	@Getter
	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable = false, name = "last_modified")
	//@JsonProperty
	Date						lastModified;
	//
	@PrePersist
	protected void onCreate() { this.created = this.lastModified = new Date(); }
	@PreUpdate
	protected void onUpdate() { this.lastModified = new Date(); }
	/* Timestamped entity */

	@Column(nullable = false, length = 32, name = "code")
	@NonNull
	@Getter
	@Setter
	@JsonProperty
	@NaturalId
	private String				code;

	@Column(nullable = false, length = 255, name = "question")
	@Getter
	@Setter
	@NonNull
	@JsonProperty
	String						question;
	
	@Column(nullable = false, name = "active")
	@Getter
	@Setter
	@JsonIgnore
	boolean						active	= true;
	
	@Override
	public String toString() {
		return code;
	}
	
	
}
