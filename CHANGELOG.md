# connector-microsoft-graph

## Change Log
+ **1.1.1** - Fix Group attribute return types (2023/09/28)
+ **1.1.0**
  + ***Improvements:***
    + Native password support
    + Native activation support
    + Configuration to force password reset on Create
    + Only directly assigned licenses are returned
  + ***Bug Fixes:***
    + Activation no longer changed on modify
    + Add mailNickname Get value
+ **1.0.6** - Assure uniqueness of group name FIN-10270 (2023/07/17)
+ **1.0.5** - Fix email_enabled Boolean Type FIN-10270 (2023/07/17)
+ **1.0.4** - Remove duplicate UID and Name setting (already handled by base) FIN-10270 (2023/07/17)
+ **1.0.3** - Update base connector version FIN-10270 (2023/05/12)
+ **1.0.2** - Add native name for Uid and Name for objects (2023/03/03)
+ **1.0.1** - Fix issue where NOT_RETURNABLE_BY_DEFAULT needs to be specified (2023/02/28)
+ **1.0** - Initial development FIN-10270 (2023/02/27)
