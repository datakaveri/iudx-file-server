
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

--revoked token table
CREATE TABLE IF NOT EXISTS revoked_tokens
(
   _id uuid NOT NULL,
   expiry timestamp without time zone NOT NULL,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL,
   CONSTRAINT revoke_tokens_pk PRIMARY KEY (_id)
);

---
-- Functions for audit[cerated,updated] on table/column
---

-- modified_at column function
CREATE
OR REPLACE
   FUNCTION update_modified () RETURNS TRIGGER AS '
BEGIN NEW.modified_at = now ();
RETURN NEW;
END;
' language 'plpgsql';

-- created_at column function
CREATE
OR REPLACE
   FUNCTION update_created () RETURNS TRIGGER AS '
BEGIN NEW.created_at = now ();
RETURN NEW;
END;
' language 'plpgsql';

-- revoked_tokens table
CREATE TRIGGER update_rt_created BEFORE INSERT ON revoked_tokens FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_rt_modified BEFORE INSERT
OR UPDATE ON
   revoked_tokens FOR EACH ROW EXECUTE PROCEDURE update_modified ();

 
 
-- insert statements
INSERT INTO revoked_tokens (_id, expiry) VALUES(uuid_generate_v4(),NOW() + INTERVAL '+1 DAY ') 
   ON CONFLICT (_id) 
   DO UPDATE SET expiry = NOW() + INTERVAL '+1 DAY ';
   
INSERT INTO revoked_tokens (_id, expiry) VALUES(uuid_generate_v4(),NOW() + INTERVAL '+1 DAY ') 
   ON CONFLICT (_id) 
   DO UPDATE SET expiry = NOW() + INTERVAL '+2 DAY ';
   
INSERT INTO revoked_tokens (_id, expiry) VALUES(uuid_generate_v4(),NOW() + INTERVAL '+1 DAY ') 
   ON CONFLICT (_id) 
   DO UPDATE SET expiry = NOW() + INTERVAL '+1 DAY ';
   
   
INSERT INTO revoked_tokens (_id, expiry) VALUES('0ed019fe-be38-4903-8f0f-5285d2985780',NOW() + INTERVAL '+1 DAY ') 
   ON CONFLICT (_id) 
   DO UPDATE SET expiry = NOW() + INTERVAL '+1 DAY ';
   
          
          
          
          
          
          
          