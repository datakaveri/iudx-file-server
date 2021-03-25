# iudx-file-server


## Get Started

### Prerequisite - Make configuration
1. Clone this repo and change directory:
   ```sh 
   git clone https://github.com/datakaveri/iudx-file-server.git && cd iudx-file-server
   ```
2. Make a config file based on the template in `example-configs/config-dev.json` for non-clustered vertx and  `example-configs/config-depl.json` for clustered vertx.
   - Generate a certificate using Lets Encrypt or other methods
   - Make a Java Keystore File and mention its path and password in the appropriate sections
   - Modify the database url and associated credentials in the appropriate sections. Don't modify the field names`upload_dir` and `temp_dir`, leave it as it is from the example-configs.
   - Populate secrets directory with following structure in the present directory:
      ```sh
      secrets/
      ├── all-verticles-configs/ (directory)
      │   ├── config-depl.json (needed for clustered vertx all verticles  in one container)
      │   ├── config-dev.json (needed for non-clustered vertx all verticles in one container/maven based setup)
      │  
      ├── keystore.jks
      └── one-verticle-configs/ (directory, needed for clustered vertx in multi-container)
      ``` 
3. Populate `.file-server-api.env` environment file based on template in `example-configs/example-evironment-file(.file-server-api.env)` in the present directory
#### Note
1. DO NOT ADD actual config with credentials to `example-configs/` directory (even in your local git clone!). 
2. If you would like to add your own config with different name than config-dev.json and config-depl.json, place in the `secrets/all-verticles-configs/` and follow the note sections of docker based and maven based setup.
3. Update all appropriate configs in `example-configs/` ONLY when there is addition of new config parameter options.
### Docker based
1. Install docker and docker-compose (one time setup)
2. Create following docker volumes (one time setup) using the commands:
	```sh
	# Creates fs-upload-volume
	docker volume create fs-upload-volume
	
	# Create fs-temp-volume
	docker volume create fs-temp-volume
	```
3. Build the images 
   ```sh
    ./docker/build.sh
    ```
4. There are following two ways of setting/deploying the file server using docker-compose:
   1. Non-Clustered setup with all verticles running in a single container: 
      - This needs no hazelcast, zookeeper, the deployment can be done on non-swarm too and suitable for development environment.
      - This makes use of iudx/fs-dev:latest image and config-dev.json present at `secrets/all-verticles-configs/config-dev.json`
         ```sh 
         # Command to bring up the non-clustered file-server container
         docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
         # Command to bring down the non-clustered file-server container
         docker-compose -f docker-compose.yml -f docker-compose.dev.yml down
         ```
   2. Clustered setup with all verticles running in a single container: 
      - This needs following things:
         - Docker swarm and overlay network having a name 'overlay-net'. Refer [here](https://github.com/datakaveri/iudx-deployment/tree/master/docs/swarm-setup.md)
         - Zookeeper running in 'overlay-net' named overlay network. Refer [here](https://github.com/datakaveri/iudx-deployment/tree/master/single-node/zookeeper)
      - This makes use of iudx/fs-depl:latest image and config-depl.json present at `secrets/all-verticles-configs/config-depl.json`
         ```sh 
         # Command to bring up the clustered one file-server container
         docker-compose -f docker-compose.yml -f docker-compose.depl.yml up -d
         # Command to bring down the clustered one file-server container
         docker-compose -f docker-compose.yml -f docker-compose.depl.yml down
         ```
#### Note   
1. If you want to try out or do temporary things, such as 
   - use different config file than the standard two
   - Binding the ports of clustered file-server container to host, etc.<br>
   Please use [this](readme/multiple-compose-files.md) technique of overriding/merging compose files i.e. using non-git versioned docker-compose.temp.yml file and do not modify the git-versioned files.
2. Modify the git versioned compose files ONLY when the configuration is needed by all (or its for CI - can preferably name it as docker-compose.ci.yml) and commit and push to the repo.


### Maven based
1. Install jdk 11 and maven
2. Use the maven exec plugin based starter to start the server 
   ```sh 
   mvn clean compile exec:java@file-server
   ```
#### Note
1. Privileged access maybe required to bring up the http server at port 80. 
2. Maven based setup by default uses `secrets/all-verticles-configs/config-dev.json` and is non-clustered setup of verticles. Also it cannot take values from `.file-server.env` file and so the default values apply.
3. If you want to use a different named config called `config-x.json`, need to place it at `secrets/all-verticles-configs/config-x.json` and use following command to bring it up:
   ```sh
   mvn clean compile  exec:java@file-server -Dconfig-dev.file=config-x.json
   ```
