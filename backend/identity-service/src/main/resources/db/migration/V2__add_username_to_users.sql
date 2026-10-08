ALTER TABLE users
ADD username VARCHAR2(50) NOT NULL;
ALTER TABLE users
ADD CONSTRAINT uk_users_username UNIQUE (username);