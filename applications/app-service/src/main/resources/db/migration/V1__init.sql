CREATE TABLE IF NOT EXISTS status (
  status_id SERIAL PRIMARY KEY,
  code VARCHAR(50) UNIQUE NOT NULL,
  name VARCHAR(100) UNIQUE NOT NULL
);

INSERT INTO status (code, name) VALUES
  ('PEND_REV', 'PENDING_REVIEW'),
  ('MAN_REV', 'MANUAL_REVIEW'),
  ('REJE', 'REJECTED'),
  ('APPRO', 'APPROVED')
ON CONFLICT (code) DO NOTHING;

CREATE TABLE IF NOT EXISTS loan_type (
  id                  BIGSERIAL PRIMARY KEY,
  code                VARCHAR(50)  NOT NULL UNIQUE,
  name                VARCHAR(120) NOT NULL,
  active              BOOLEAN      NOT NULL DEFAULT TRUE,
  minimum_amount      BIGINT       NOT NULL,
  maximum_amount      BIGINT       NOT NULL,
  interest_rate       NUMERIC(5,2) NOT NULL,
  automatic_validation BOOLEAN     NOT NULL DEFAULT FALSE,
  created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);



CREATE TABLE IF NOT EXISTS loan_application (
  id             BIGSERIAL PRIMARY KEY,
  document       VARCHAR(40)   NOT NULL,
  email 		 VARCHAR(160)  NOT NULL,
  user_id	     int           NOT null,
  loan_type      VARCHAR(100)   NOT NULL,          -- entidad: String
  amount         NUMERIC(30,0) NOT NULL CHECK (amount > 0),  -- entidad: BigInteger
  term_months    INT           NOT NULL CHECK (term_months BETWEEN 1 AND 120),
  status         VARCHAR(40)   NOT NULL DEFAULT 'PENDING_REVIEW',
  interest_rate       NUMERIC(5,2) NOT NULL,
  current_loan_monthly_payment          NUMERIC(100),
  total_approved_loans_monthly_payment  NUMERIC(100),
  available_indebtedness                NUMERIC(100),
  observations   varchar(300),
  created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_loan_application_status ON loan_application(status);
CREATE INDEX IF NOT EXISTS idx_loan_application_email ON loan_application(email);
CREATE INDEX IF NOT EXISTS idx_loan_application_document ON loan_application(document);


-- Semilla de tipos de préstamo
INSERT INTO loan_type 
  (code, name, active, minimum_amount, maximum_amount, interest_rate, automatic_validation)
VALUES
  ('PERSONAL',  'Personal',  TRUE,  1000000,  50000000,  1.50,  FALSE),
  ('COMERCIAL', 'Comercial', TRUE,  5000000, 200000000,  2.00,  FALSE),
  ('MORTGAGE',  'Hipotecario', TRUE, 20000000, 500000000, 3.25,  TRUE)
ON CONFLICT (code) DO NOTHING;

