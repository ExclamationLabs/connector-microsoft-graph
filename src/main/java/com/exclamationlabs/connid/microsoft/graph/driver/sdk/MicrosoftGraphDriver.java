/*
    Copyright 2020 Exclamation Labs
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package com.exclamationlabs.connid.microsoft.graph.driver.sdk;

import com.exclamationlabs.connid.base.connector.authenticator.Authenticator;
import com.exclamationlabs.connid.base.connector.driver.BaseDriver;
import com.exclamationlabs.connid.base.connector.logging.Logger;
import com.exclamationlabs.connid.base.connector.model.IdentityModel;
import com.exclamationlabs.connid.base.microsoft.graph.configuration.MicrosoftGraphConfiguration;
import com.exclamationlabs.connid.microsoft.graph.authenticator.MicrosoftGraphAuthenticator;
import com.exclamationlabs.connid.microsoft.graph.model.MicrosoftGraphGroup;
import com.exclamationlabs.connid.microsoft.graph.model.MicrosoftGraphLicense;
import com.exclamationlabs.connid.microsoft.graph.model.MicrosoftGraphUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.Admin;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorSecurityException;

public class MicrosoftGraphDriver extends BaseDriver<MicrosoftGraphConfiguration> {

  static final int SDK_FETCH_COUNT = 50;
  private MicrosoftGraphConfiguration configuration;
  private GraphServiceClient<Request> graphClient;
public MicrosoftGraphConfiguration getConfiguration(){
  return configuration;
}
  public MicrosoftGraphDriver() {
    super();
    addInvocator(MicrosoftGraphUser.class, new MicrosoftGraphUsersInvocator());
    addInvocator(MicrosoftGraphGroup.class, new MicrosoftGraphGroupsInvocator());
    addInvocator(MicrosoftGraphLicense.class, new MicrosoftGraphLicensesInvocator());
  }

  @Override
  public void initialize(
      MicrosoftGraphConfiguration configuration,
      Authenticator<MicrosoftGraphConfiguration> authenticator)
      throws ConnectorException {
    this.configuration = configuration;
    authenticator.authenticate(configuration);
    graphClient = ((MicrosoftGraphAuthenticator) authenticator).getAuthenticatedClient();
  }

  @Override
  public void test() throws ConnectorException {
    try {
      Admin admin = graphClient.admin().buildRequest().get();
      if (admin == null) {
        throw new ConnectorException("Failure retrieving admin info for connector test.");
      }

    } catch (ClientException ce) {
      throw new ConnectorSecurityException(
          "Client/Security error occurred for Microsoft Graph", ce);
    } catch (Exception e) {
      throw new ConnectorException("Test (admin info detection) for Microsoft Graph failed.", e);
    }
  }

  @Override
  public void close() {}

  public GraphServiceClient<Request> getGraphClient() {
    return graphClient;
  }

  @Override
  public IdentityModel getOneByName(
      Class<? extends IdentityModel> identityModelClass, String nameValue)
      throws ConnectorException {
    return getInvocator(identityModelClass).getOneByName(this, nameValue);
  }

  void handleGraphServiceException(GraphServiceException exception) throws ConnectorException {
    switch (exception.getResponseCode()) {
      case HttpStatus.SC_BAD_REQUEST:
        if (exception.getError() != null && exception.getError().error != null) {
          String whatIsIt = exception.getError().error.message;
          if (StringUtils.containsIgnoreCase(
              exception.getError().error.message, "property netId is invalid")) {
            throw new AlreadyExistsException(
                "User with this account identification already exists", exception);
          }

          final String ERROR_MESSAGE =
              "Invalid Request: "
                  + exception.getError().error.code
                  + ": "
                  + exception.getError().error.message;
          if (exception.toString() != null
              && configuration.getEnableDebugHttpLogging() ) {
            throw new ConnectorException("Invalid Request: "+exception.toString(), exception);
          } else {
            throw new ConnectorException(ERROR_MESSAGE, exception);
          }

        } else {
          if (exception.toString() != null
              && configuration.getEnableDebugHttpLogging() ) {
            throw new ConnectorException("Invalid Request to MS Graph Service"+exception.toString(), exception);
          } else {
            throw new ConnectorException("Invalid Request to MS Graph Service", exception);
          }

        }

      case HttpStatus.SC_FORBIDDEN:
        if (exception.getError() != null && exception.getError().error != null) {
          final String ERROR_MESSAGE =
              "MS Graph Request forbidden: "
                  + exception.getError().error.code
                  + ": "
                  + exception.getError().error.message;
          if (exception.toString() != null
              && configuration.getEnableDebugHttpLogging() ) {
            throw new ConnectorException("MS Graph Request forbidden: "+exception.toString(), exception);
          } else {
            throw new ConnectorException(ERROR_MESSAGE, exception);
          }
        } else {
          if (exception.toString() != null
              && configuration.getEnableDebugHttpLogging() ) {
            throw new ConnectorException("Unauthorized Request made to MS Graph Service: "+exception.toString(), exception);
          } else {
            throw new ConnectorException("Unauthorized Request made to MS Graph Service", exception);
          }

        }

      case HttpStatus.SC_NOT_FOUND:
        break;

      default:
        if (exception.toString() != null
            && configuration.getEnableDebugHttpLogging() ) {
          throw new ConnectorException("Unexpected GraphServiceException: "+ exception.toString(), exception);
        } else {
          throw new ConnectorException("Unexpected GraphServiceException", exception);
        }
    }
  }
  public void logTransactionPayload(Class clazz,String method, Object data){
  if(this.configuration.getEnableDebugHttpLogging()){
    ObjectMapper mapper = new ObjectMapper();
    try {
      Logger.info(clazz,"Debug payload, Method: " + method + " data: "+ mapper.writeValueAsString(data));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  }
}
