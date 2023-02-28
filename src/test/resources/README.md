### Making configurable base path
- Base path can be added in postman environment file or in postman.
- `FS.postman_environment.json` has **values** array which have fields named **dxApiBasePath** whose **value** is currently set to `ngsi-ld/v1`, **iudxApiBasePath** with value `iudx/v1`, **dxAuthBasePath** with value `auth/v1`.
- The **value** could be changed according to the deployment and then the collection with the `FS.postman_environment.json` file can be uploaded to Postman
- For the changing **dxApiBasePath**,**iudxApiBasePath**, **dxAuthBasePath** values in postman after importing the collection and environment files, locate `FS Environment` from **Environments** in sidebar of Postman application.
- To know more about Postman environments, refer : [postman environments](https://learning.postman.com/docs/sending-requests/managing-environments/)
- The **CURRENT VALUE** of the variable could be changed


