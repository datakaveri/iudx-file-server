CREATE TABLE file_server_token
(
    file_token character varying  NOT NULL,
    user_token character varying  NOT NULL,
    validity_date timestamp without time zone NOT NULL,
    server_id character varying  NOT NULL
);

INSERT into file_server_token VALUES('fileServerToken','testing_key',current_timestamp,'example.com');
