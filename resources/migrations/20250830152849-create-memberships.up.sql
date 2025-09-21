CREATE TYPE InvoiceStatus AS ENUM ('created', 'unpaid', 'paid', 'expired', 'error');
--;;
CREATE TABLE invoices (
  id SERIAL PRIMARY KEY,
  label TEXT NOT NULL,
  amount_msat NUMERIC(19,0) NOT NULL,
  amount_received_msat NUMERIC(19,0),
  status InvoiceStatus default 'created',
  description TEXT,
  bolt_11 TEXT,
  payment_hash TEXT,
  payment_secret TEXT,
  payment_preimage TEXT,
  created_index BIGINT,
  created_at timestamp default current_timestamp,
  expires_at timestamp,
  paid_at timestamp
);
--;;
CREATE TABLE galt_membership_payments (
  user_id UUID NOT NULL,
  invoice_id INTEGER NOT NULL,
  PRIMARY KEY (user_id, invoice_id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);
