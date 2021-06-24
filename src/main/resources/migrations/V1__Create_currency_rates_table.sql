CREATE TABLE currency_rates
(
	currency VARCHAR(4) NOT NULL,
	rate DOUBLE PRECISION,
	actual_at TIMESTAMP NOT NULL,

	PRIMARY KEY (actual_at, currency)
);