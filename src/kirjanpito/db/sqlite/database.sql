CREATE TABLE account (
	id integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	number varchar(10) NOT NULL,
	name varchar(100) NOT NULL,
	type integer NOT NULL,
	vat_code integer NOT NULL,
	vat_percentage numeric(10, 2) NOT NULL,
	vat_account1_id integer,
	vat_account2_id integer,
	flags integer NOT NULL,
	FOREIGN KEY (vat_account1_id) REFERENCES account (id),
	FOREIGN KEY (vat_account2_id) REFERENCES account (id)
);

CREATE TABLE coa_heading (
	id integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	number varchar(10) NOT NULL,
	text varchar(100) NOT NULL,
	level integer NOT NULL
);

CREATE TABLE period (
	id integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	start_date date NOT NULL,
	end_date date NOT NULL,
	locked bool NOT NULL
);

CREATE TABLE document (
	id integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	number integer NOT NULL,
	period_id integer NOT NULL,
	date date NOT NULL,
	FOREIGN KEY (period_id) REFERENCES period (id)
);

CREATE TABLE entry (
	id integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	document_id integer NOT NULL,
	account_id integer NOT NULL,
	debit bool NOT NULL,
	amount numeric(10, 2) NOT NULL,
	description varchar(100) NOT NULL,
	row_number integer NOT NULL,
	flags integer NOT NULL,
	FOREIGN KEY (document_id) REFERENCES document (id),
	FOREIGN KEY (account_id) REFERENCES account (id)
);

CREATE TABLE document_type (
	id integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	number integer NOT NULL,
	name varchar(100) NOT NULL,
	number_start integer NOT NULL,
	number_end integer NOT NULL
);

CREATE TABLE settings (
	version integer NOT NULL,
	name varchar(100) NOT NULL,
	business_id varchar(50) NOT NULL,
	current_period_id integer NOT NULL,
	document_type_id integer,
	properties text NOT NULL,
	PRIMARY KEY (version),
	FOREIGN KEY (current_period_id) REFERENCES period (id),
	FOREIGN KEY (document_type_id) REFERENCES document_type (id)
);

CREATE TABLE report_structure (
	id varchar(50) NOT NULL,
	data text NOT NULL
);

CREATE TABLE entry_template (
	id integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	number integer NOT NULL,
	name varchar(100) NOT NULL,
	account_id integer NOT NULL,
	debit bool NOT NULL,
	amount numeric(10, 2) NOT NULL,
	description varchar(100) NOT NULL,
	row_number integer NOT NULL,
	FOREIGN KEY (account_id) REFERENCES account (id)
);

CREATE INDEX document_number_idx ON document (
	period_id, number
);
