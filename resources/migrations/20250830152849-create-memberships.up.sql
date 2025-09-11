CREATE TABLE payments (
  id SERIAL PRIMARY KEY,
  amount NUMERIC(14,3),
  created_at timestamp default current_timestamp,
  paid_at timestamp
);
--;;
CREATE TABLE galt_membership_payments (
  member_id UUID REFERENCES members(id) NOT NULL,
  payment_id INTEGER REFERENCES payments(id) NOT NULL,
  PRIMARY KEY (member_id, payment_id)
);
