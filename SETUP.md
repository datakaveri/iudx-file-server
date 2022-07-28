SETUP GUIDE
----

This document contains the installation and configuration processes
of the external modules of each Verticle in IUDX FIle Server.

<p align="center">
<img src="./docs/file_server_overview.png">
</p>

The File Server connects with various external dependencies namely
- `ELK` stack : used to capture and query files of temporal and spatial data
- `PostgreSQL` : used to query data related to Token Invalidation.
- `RabbitMQ` : used to receive token invalidation info
- `ImmuDB` : used to store auditing information

The File Server also connects with various DX dependencies namely
- Authorization Server : used to download the certificate for token decoding
- Catalogue Server : used to download the list of resources, access policies and query types supported on a resource.

## Setting up ELK for IUDX File Server
- Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/elk) to setup ELK stack.

>**Note** : Access to HTTP APIs for search functionality should be configured with TLS and RBAC privileges.

In order to connect to the appropriate Elasticsearch database, required information such as databaseIP,databasePort etc. should be updated in the DatabaseVerticle module available in [config-example.json](configs/config-example.json).

**DatabaseVerticle**
```
{
    "id": "iudx.file.server.database.elasticdb.DatabaseVerticle",
    "verticleInstances": <number-of-verticle-instances>,
    "databaseIP": "localhost",
    "databasePort": <port-number>,
    "databaseUser": <username-for-es>,
    "databasePassword": <password-for-es>,
}
```
----
## Setting up PostgreSQL for IUDX File Server
- Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/postgres) to setup PostgreSQL.

>**Note** : PostgreSQL database should be configured with a RBAC user having CRUD privileges

In order to connect to the appropriate Postgres database, required information such as databaseIP,databasePort etc. should be updated in the PostgresVerticle module available in [config-example.json](configs/config-example.json).

**PostgresVerticle**
```
{
    "id": "iudx.file.server.database.postgres.PostgresVerticle",
    "verticleInstances": <number-of-verticle-instances>,
    "databaseIp": "localhost",
    "databasePort": <port-number>,
    "databaseName": <database-name>,
    "databaseUserName": <username-for-psql>,
    "databasePassword": <password-for-psql>,
    "poolSize": <pool-size>
}
```

#### Schemas for PostgreSQL tables in IUDX File Server
1. Token Invalidation Table Schema
```
CREATE TABLE IF NOT EXISTS revoked_tokens
(
   _id uuid NOT NULL,
   expiry timestamp with time zone NOT NULL,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL,
   CONSTRAINT revoke_tokens_pk PRIMARY KEY (_id)
);
```
----
## Setting up RabbitMQ for IUDX File Server
- Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/databroker) to setup RMQ.


In order to connect to the appropriate RabbitMQ instance, required information such as dataBrokerIP,dataBrokerPort etc. should be updated in the DataBrokerVerticle module available in [config-example.json](configs/config-example.json).

**DataBrokerVerticle**
```
{
    "id": "iudx.file.server.databroker.DataBrokerVerticle",
    "verticleInstances": <number-of-verticle-instances>,
    "dataBrokerIP": "localhost",
    "dataBrokerPort": <port-number>,
    "prodVhost":<production-vHost>, 
    "internalVhost": <internal-vHost>,
    "externalVhost":<external-vHost>,
    "dataBrokerUserName": <username-for-rmq>,
    "dataBrokerPassword": <password-for-rmq>,
    "dataBrokerManagementPort": <management-port-number>,
    "connectionTimeout": <time-in-milliseconds>,
    "requestedHeartbeat": <time-in-seconds>,
    "handshakeTimeout": <time-in-milliseconds>,
    "requestedChannelMax": <num-of-max-channels>,
    "networkRecoveryInterval": <time-in-milliseconds>,
    "automaticRecoveryEnabled": <true | false>,
}
```

----

## Setting up ImmuDB for IUDX File Server
- Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/immudb) to setup ImmuDB.
- Refer [this](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/immudb/docker/immudb-config-generator/immudb-config-generator.py) to create table/user.
- In order to connect to the appropriate ImmuDB database, required information such as auditingDatabaseIP,auditingDatabasePort etc. should be updated in the AuditingVerticle module available in [config-example.json](configs/config-example.json).

**AuditingVerticle**
```
{
    "id": "iudx.file.server.auditing.AuditingVerticle",
    "verticleInstances": <number-of-verticle-instances>,
    "auditingDatabaseIP": "localhost",
    "auditingDatabasePort": <port-number>,
    "auditingDatabaseName": <database-name>,
    "auditingDatabaseUserName": <username-for-immudb>,
    "auditingDatabasePassword": <password-for-immudb>,
    "auditingDatabaseTableName": <table-name-for-immudb>
    "auditingPoolSize": <pool-size>
}
```

**Auditing Table Schema**
```
CREATE TABLE IF NOT EXISTS rsaudit (
    id VARCHAR[128] NOT NULL, 
    api VARCHAR[128], 
    userid VARCHAR[128],
    epochtime INTEGER,
    resourceid VARCHAR[200],
    isotime VARCHAR[128],
    providerid VARCHAR[128],
    size INTEGER,
    PRIMARY KEY id
);

```


## Connecting with DX Catalogue Server

In order to connect to the DX catalogue server, required information such as catServerHost,catServerPort etc. should be updated in the AuthenticationVerticle and FileServerVerticle modules availabe in [config-example.json](configs/config-example.json).

**AuthenticationVerticle**
```
{
    "id": "iudx.file.server.authenticator.AuthenticationVerticle",
    "verticleInstances": <number-of-verticle-instances,
    "audience": <resource-server-host>,
    "catalogueHost": <catalogue-server-host>,
    "cataloguePort": <catalogue-server-port>,
    "file-keystore": <path/to/keystore-file.jks>,
    "file-keystorePassword": <password-for-file-keystore>,
    "rs-keystore": <path/to/keystore-rs.jks>,
    "rs-keystorePassword": <password-for-rs-keystore>,
    "authHost": <auth-server-host>,
    "authPort": <auth-server-port>,
    "jwtIgnoreExpiry": <true | false>
}
```

**FileServerVerticle**
```
{
    "id": "iudx.file.server.apiserver.FileServerVerticle",
    "verticleInstances": <number-of-verticle-instances>,
    "ssl": true,
    "port": <port-to-listen>,
    "automaticRecoveryEnabled": true,
    "catalogueHost": <catalogue-server-host>,
    "cataloguePort": <catalogue-server-port>,
    "file-keystore": <path/to/keystore-file.jks>,
    "file-keystorePassword": <password-for-file-keystore>,
    "tmp_dir": <path/to/temp-dir/>,
    "upload_dir": <path/to/upload-dir/>,
    "allowedContentType": {
        "text/plain": "txt",
        "text/csv": "csv",
        "application/pdf": "pdf",
        "video/mp4": "mp4",
        "application/zip": "zip",
        "application/x-7z-compressed": "7z",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": "xlsx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document": "docx"
    }
}
```

## Connecting with DX Authorization Server

In order to connect to the DX authorization server, required information such as authHost, authPort should be updated in the AuthenticationVerticle module availabe in [config-example.json](configs/config-example.json).

**AuthenticationVerticle**
```
{
    "id": "iudx.file.server.authenticator.AuthenticationVerticle",
    "verticleInstances": <number-of-verticle-instances,
    "audience": <resource-server-host>,
    "catalogueHost": <catalogue-server-host>,
    "cataloguePort": <catalogue-server-port>,
    "file-keystore": <path/to/keystore-file.jks>,
    "file-keystorePassword": <password-for-file-keystore>,
    "rs-keystore": <path/to/keystore-rs.jks>,
    "rs-keystorePassword": <password-for-rs-keystore>,
    "authHost": <auth-server-host>,
    "authPort": <auth-server-port>,
    "jwtIgnoreExpiry": <true | false>
}
```
