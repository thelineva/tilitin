CREATE TABLE account (
	id int auto_increment NOT NULL,
	number varchar(10) NOT NULL,
	name varchar(100) NOT NULL,
	type int NOT NULL,
	vat_code smallint NOT NULL,
	vat_percentage numeric(10, 2) NOT NULL,
	vat_account1_id int,
	vat_account2_id int,
	flags int NOT NULL,
	PRIMARY KEY (id),
	FOREIGN KEY (vat_account1_id) REFERENCES account(id),
	FOREIGN KEY (vat_account2_id) REFERENCES account(id)
) ENGINE=InnoDB;

CREATE TABLE coa_heading (
	id int auto_increment NOT NULL,
	number varchar(10) NOT NULL,
	text varchar(100) NOT NULL,
	level int NOT NULL,
	PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE period (
	id int auto_increment NOT NULL,
	start_date date NOT NULL,
	end_date date NOT NULL,
	locked bool NOT NULL,
	PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE document (
	id int auto_increment NOT NULL,
	number int NOT NULL,
	period_id int NOT NULL,
	date date NOT NULL,
	PRIMARY KEY (id),
	FOREIGN KEY (period_id) REFERENCES period (id)
) ENGINE=InnoDB;

CREATE TABLE entry (
	id int auto_increment NOT NULL,
	document_id int NOT NULL,
	account_id int NOT NULL,
	debit bool NOT NULL,
	amount numeric(10, 2) NOT NULL,
	description varchar(100) NOT NULL,
	row_number int NOT NULL,
	flags int NOT NULL,
	PRIMARY KEY (id),
	FOREIGN KEY (document_id) REFERENCES document (id),
	FOREIGN KEY (account_id) REFERENCES account (id)
) ENGINE=InnoDB;

CREATE TABLE document_type (
	id int auto_increment NOT NULL,
	number int NOT NULL,
	name varchar(100) NOT NULL,
	number_start int NOT NULL,
	number_end int NOT NULL,
	PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE settings (
	version int NOT NULL,
	name varchar(100) NOT NULL,
	business_id varchar(50) NOT NULL,
	current_period_id int NOT NULL,
	document_type_id int,
	properties text NOT NULL,
	PRIMARY KEY (version),
	FOREIGN KEY (current_period_id) REFERENCES period (id),
	FOREIGN KEY (document_type_id) REFERENCES document_type (id)
) ENGINE=InnoDB;

CREATE TABLE report_structure (
	id varchar(50) NOT NULL,
	data text NOT NULL,
	PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE entry_template (
	id int auto_increment NOT NULL,
	number int NOT NULL,
	name varchar(100) NOT NULL,
	account_id int NOT NULL,
	debit bool NOT NULL,
	amount numeric(10, 2) NOT NULL,
	description varchar(100) NOT NULL,
	row_number int NOT NULL,
	PRIMARY KEY (id),
	FOREIGN KEY (account_id) REFERENCES account (id)
) ENGINE=InnoDB;

CREATE INDEX document_number_idx ON document (
	period_id, number
);
