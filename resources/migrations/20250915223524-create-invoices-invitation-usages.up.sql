CREATE TABLE invitation_usages (
  id SERIAL PRIMARY KEY,
  invitation_id UUID NOT NULL,
  user_id UUID NOT NULL,
  invoice_id INTEGER REFERENCES invoices(id),
  status TEXT,
  created_at timestamp default current_timestamp,
  FOREIGN KEY (invitation_id) REFERENCES invitations(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
