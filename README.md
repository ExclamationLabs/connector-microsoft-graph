# Microsoft Graph Connector

Open source Identity Management connector for [Microsoft Graph](https://developer.microsoft.com/en-us/graph)

Leverages the [Connector Base Framework](https://github.com/ExclamationLabs/connector-base)

Developed and tested in [Midpoint](https://evolveum.com/midpoint/), but also could be utilized in any [ConnId](https://connid.tirasa.net/) framework.

## Introductory Notes

- This software is Copyright 2020 Exclamation Labs.  Licensed under the Apache License, Version 2.0.

- Limitations:

  - Connector does not support creating, updating, or deleting licenses due to API/Microsoft restrictions.
  - Licenses can be assigned and removed from a User; Groups can be assigned and removed from a User; BUT
     at this time, Licenses CANNOT be assigned and removed from a Group.  This is a special condition as governed by Graph
  called Group-Based Licensing
    (https://learn.microsoft.com/en-us/azure/active-directory/enterprise-users/licensing-groups-resolve-problems)
  It might be possible to add this support in the future with the properly configured Azure Tenant
    test environment ... but if outside of this connector Groups are created that do have license(s) assigned, you
  will be able to see which license id's were assigned in a read-only manner.

- API/SDK only supports pagination via token(cookie).  Midpoint is unidirectional with 
pagination values and
 doesn't consume the paged result cookie and feed it into subsequent requests.

- API may not be able update/delete users and groups that originated outside of the connector.  
This could require adding permissions in Azure or MS Graph to give the client credentials used applicable
to this connector more privileges. 

- Graph also has concepts of nested Groups/transitive members, and a user being
an Owner of a group as opposed to a member.  As of this time,
this isn't supported - but
could be potentially added in the future.

- For whatever reason, the API allows groups with the same name to be created.  Beware as this could introduce confusion in IAM.

## Further Info

- [REST API/SDK documentation](https://learn.microsoft.com/en-us/graph/api/overview?view=graph-rest-1.0)
- [MS Graph SDK Java source](https://github.com/microsoftgraph/msgraph-sdk-java/)
- [MS Graph API Documentation](https://learn.microsoft.com/en-us/graph/overview)

## Getting started

This is an IAM connector for Microsoft Graph API, used to connect to Microsoft Cloud Services.

This connector leverages the Microsoft Graph SDK available at [Microsoft Graph SDK](https://github.com/microsoftgraph/msgraph-sdk-java/)

NOTE: This connector is a full replacement for Evolveum's [Microsoft Azure (Graph API) Connector](https://docs.evolveum.com/connectors/connectors/com.evolveum.polygon.connector.msgraphapi.MSGraphConnector/).
This connector will no longer work, because MS Azure AD Graph API has reached end-of-life, 
currently deprecated and is to be retired June 30. 2023.

## Configuration properties

- See src/test/resources/__bcon__development__exclamation_labs__microsoft_graph.properties for an example

- security.authenticator.oauth2ClientCredentials.tokenUrl - Set this to `unused` or any other string.  The SDK is aware of its own token url location
- security.authenticator.oauth2ClientCredentials.scope - Normally set to `https://graph.microsoft.com/.default`
- security.authenticator.oauth2ClientCredentials.clientId - Set to the `Application ID` of your application belonging to your Azure AD tenant.
- security.authenticator.oauth2ClientCredentials.clientSecret - Set to Client Secret value generated for your application belonging to your Azure AD tenant.
- custom.tenantId - Set to the Tenant Id of your Microsoft Azure AD Tenant
- results.pagination - Should be `false` since pagination as not supported as discussed above.
