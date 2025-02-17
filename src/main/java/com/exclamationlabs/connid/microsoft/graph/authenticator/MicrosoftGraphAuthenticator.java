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

package com.exclamationlabs.connid.microsoft.graph.authenticator;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.exclamationlabs.connid.base.connector.authenticator.Authenticator;
import com.exclamationlabs.connid.base.connector.configuration.TrustStoreConfiguration;
import com.exclamationlabs.connid.base.connector.util.GuardedStringUtil;
import com.exclamationlabs.connid.base.microsoft.graph.configuration.MicrosoftGraphConfiguration;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.http.CoreHttpProvider;
import com.microsoft.graph.logger.LoggerLevel;
import com.microsoft.graph.requests.GraphServiceClient;
import java.util.Collections;
import okhttp3.Request;
import org.identityconnectors.framework.common.exceptions.ConnectorSecurityException;

public class MicrosoftGraphAuthenticator implements Authenticator<MicrosoftGraphConfiguration> {

  private GraphServiceClient<Request> authenticatedClient;

  @Override
  public String authenticate(MicrosoftGraphConfiguration configuration)
      throws ConnectorSecurityException {
    TrustStoreConfiguration.clearJdkProperties();
    final ClientSecretCredential clientSecretCredential =
        new ClientSecretCredentialBuilder()
            .clientId(configuration.getClientId())
            .clientSecret(GuardedStringUtil.read(configuration.getClientSecret()))
            .tenantId(configuration.getTenantId())
            .build();

    final TokenCredentialAuthProvider tokenCredentialAuthProvider =
        new TokenCredentialAuthProvider(
            Collections.singletonList(configuration.getScope()), clientSecretCredential);

    authenticatedClient =
        GraphServiceClient.builder()
            .authenticationProvider(tokenCredentialAuthProvider)
            .buildClient();
    if (configuration.getEnableDebugHttpLogging()) {
      authenticatedClient.getLogger().setLoggingLevel(LoggerLevel.DEBUG);
      try {
        ((CoreHttpProvider) authenticatedClient.getHttpProvider())
            .getLogger()
            .setLoggingLevel(LoggerLevel.DEBUG);
      } catch (Exception e) {
      }
    }

    return "authenticated";
  }

  public GraphServiceClient<Request> getAuthenticatedClient() {
    return authenticatedClient;
  }
}
