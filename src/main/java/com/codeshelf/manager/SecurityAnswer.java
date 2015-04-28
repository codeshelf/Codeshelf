package com.codeshelf.manager;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.annotations.NaturalId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "security_answer")
@EqualsAndHashCode(callSuper = false, of = { "id", "user", "question", "hashedAnswer" })
@ToString(of = { "id", "user", "question" }, callSuper = false)
public class SecurityAnswer {
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(SecurityAnswer.class);

	@Id
	@Column(nullable = false, name = "id")
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Getter
	@Setter
	@JsonProperty
	int							id;

	/* Timestamped entity */
	@Getter
	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable = false, name = "created")
	@JsonProperty
	Date						created;
	//
	@Getter
	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable = false, name = "last_modified")
	@JsonProperty
	Date						lastModified;

	/* Timestamped entity */
	@PrePersist protected void onCreate() { this.created = this.lastModified = new Date(); }
	@PreUpdate protected void onUpdate() { this.lastModified = new Date(); }

	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@Getter
	@Setter
	@NonNull
	@NaturalId
	User				user;

	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@Getter
	@Setter
	@NonNull
	@NaturalId
	SecurityQuestion	question;

	@Column(nullable = false, length = 255, name = "hashed_answer")
	@Getter
	@Setter
	@NonNull
	String				hashedAnswer;
}
