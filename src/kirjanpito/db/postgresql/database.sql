CREATE SEQUENCE account_id_seq;

CREATE TABLE account (
	id int4 NOT NULL,
	number varchar(10) NOT NULL,
	name varchar(100) NOT NULL,
	type int2 NOT NULL,
	vat_code int2 NOT NULL,
	vat_percentage numeric(10, 2) NOT NULL,
	vat_account1_id int4,
	vat_account2_id int4,
	flags int4 NOT NULL,
	PRIMARY KEY (id),
	FOREIGN KEY (vat_account1_id) REFERENCES account (id),
	FOREIGN KEY (vat_account2_id) REFERENCES account (id)
);

CREATE SEQUENCE coa_heading_id_seq;

CREATE TABLE coa_heading (
	id int4 NOT NULL,
	number varchar(10) NOT NULL,
	text varchar(100) NOT NULL,
	level int2 NOT NULL,
	PRIMARY KEY (id)
);

CREATE SEQUENCE period_id_seq;

CREATE TABLE period (
	id int4 NOT NULL,
	start_date date NOT NULL,
	end_date date NOT NULL,
	locked bool NOT NULL,
	PRIMARY KEY (id)
);

CREATE SEQUENCE document_id_seq;

CREATE TABLE document (
	id int4 NOT NULL,
	number int4 NOT NULL,
	period_id int4 NOT NULL,
	date date NOT NULL,
	PRIMARY KEY (id),
	FOREIGN KEY (period_id) REFERENCES period (id)
);

CREATE SEQUENCE entry_id_seq;

CREATE TABLE entry (
	id int4 NOT NULL,
	document_id int4 NOT NULL,
	account_id int4 NOT NULL,
	debit bool NOT NULL,
	amount numeric(10, 2) NOT NULL,
	description varchar(100) NOT NULL,
	row_number int4 NOT NULL,
	flags int4 NOT NULL,
	PRIMARY KEY (id),
	FOREIGN KEY (document_id) REFERENCES document (id),
	FOREIGN KEY (account_id) REFERENCES account (id)
);

CREATE TABLE document_type (
	id int4 NOT NULL,
	number int4 NOT NULL,
	name varchar(100) NOT NULL,
	number_start int4 NOT NULL,
	number_end int4 NOT NULL,
	PRIMARY KEY (id)
);

CREATE SEQUENCE document_type_id_seq;

CREATE TABLE settings (
	version int4 NOT NULL,
	name varchar(100) NOT NULL,
	business_id varchar(50) NOT NULL,
	current_period_id int4 NOT NULL,
	document_type_id int4,
	properties text NOT NULL,
	PRIMARY KEY (version),
	FOREIGN KEY (current_period_id) REFERENCES period (id),
	FOREIGN KEY (document_type_id) REFERENCES document_type (id)
);

CREATE TABLE report_structure (
	id varchar(50) NOT NULL,
	data text NOT NULL,
	PRIMARY KEY (id)
);

CREATE TABLE entry_template (
	id int4 NOT NULL,
	number int4 NOT NULL,
	name varchar(100) NOT NULL,
	account_id int4 NOT NULL,
	debit bool NOT NULL,
	amount numeric(10, 2) NOT NULL,
	description varchar(100) NOT NULL,
	row_number int4 NOT NULL,
	PRIMARY KEY (id),
	FOREIGN KEY (account_id) REFERENCES account (id)
);

CREATE SEQUENCE entry_template_id_seq;

CREATE INDEX document_number_idx ON document (
	period_id, number
);
