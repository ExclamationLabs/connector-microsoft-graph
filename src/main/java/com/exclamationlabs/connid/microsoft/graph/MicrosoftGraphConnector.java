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

package com.exclamationlabs.connid.microsoft.graph;

import com.exclamationlabs.connid.base.connector.BaseFullAccessConnector;
import com.exclamationlabs.connid.base.microsoft.graph.configuration.MicrosoftGraphConfiguration;
import com.exclamationlabs.connid.microsoft.graph.adapter.MicrosoftGraphGroupsAdapter;
import com.exclamationlabs.connid.microsoft.graph.adapter.MicrosoftGraphLicensesAdapter;
import com.exclamationlabs.connid.microsoft.graph.adapter.MicrosoftGraphUsersAdapter;
import com.exclamationlabs.connid.microsoft.graph.attribute.MicrosoftGraphUserAttribute;
import com.exclamationlabs.connid.microsoft.graph.authenticator.MicrosoftGraphAuthenticator;
import com.exclamationlabs.connid.microsoft.graph.driver.sdk.MicrosoftGraphDriver;
import java.util.Arrays;
import java.util.HashSet;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.spi.ConnectorClass;

@ConnectorClass(
    displayNameKey = "microsoft.graph.connector.display",
    configurationClass = MicrosoftGraphConfiguration.class)
public class MicrosoftGraphConnector extends BaseFullAccessConnector<MicrosoftGraphConfiguration> {

  public MicrosoftGraphConnector() {
    super(MicrosoftGraphConfiguration.class);
    setAuthenticator(new MicrosoftGraphAuthenticator());
    setDriver(new MicrosoftGraphDriver());
    setAdapters(
        new MicrosoftGraphUsersAdapter(),
        new MicrosoftGraphGroupsAdapter(),
        new MicrosoftGraphLicensesAdapter());
    setEnhancedFiltering(true);
    setFilterAttributes(
        new HashSet<>(
            Arrays.asList(
                Name.NAME,
                MicrosoftGraphUserAttribute.DISPLAY_NAME.name(),
                MicrosoftGraphUserAttribute.EMAIL.name(),
                MicrosoftGraphUserAttribute.USER_PRINCIPAL_NAME.name(),
                MicrosoftGraphUserAttribute.USER_TYPE.name(),
                MicrosoftGraphUserAttribute.GIVEN_NAME.name(),
                MicrosoftGraphUserAttribute.SURNAME.name())));
  }
}
