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

import com.exclamationlabs.connid.base.connector.driver.DriverInvocator;
import com.exclamationlabs.connid.base.connector.results.ResultsFilter;
import com.exclamationlabs.connid.base.connector.results.ResultsPaginator;
import com.exclamationlabs.connid.microsoft.graph.model.MicrosoftGraphLicense;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.SubscribedSku;
import com.microsoft.graph.requests.SubscribedSkuCollectionPage;
import java.util.*;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

public class MicrosoftGraphLicensesInvocator
    implements DriverInvocator<MicrosoftGraphDriver, MicrosoftGraphLicense> {

  @Override
  public String create(MicrosoftGraphDriver driver, MicrosoftGraphLicense license)
      throws ConnectorException {
    throw new UnsupportedOperationException("Create not supported for MS Graph licenses");
  }

  @Override
  public void update(MicrosoftGraphDriver driver, String id, MicrosoftGraphLicense license)
      throws ConnectorException {
    throw new UnsupportedOperationException("Update not supported for MS Graph licenses");
  }

  @Override
  public void delete(MicrosoftGraphDriver driver, String id) throws ConnectorException {
    throw new UnsupportedOperationException("Delete not supported for MS Graph licenses");
  }

  @Override
  public Set<MicrosoftGraphLicense> getAll(
      MicrosoftGraphDriver driver,
      ResultsFilter resultsFilter,
      ResultsPaginator resultsPaginator,
      Integer integer)
      throws ConnectorException {
    Set<MicrosoftGraphLicense> response = new HashSet<>();
    try {
      final SubscribedSkuCollectionPage page =
          driver.getGraphClient().subscribedSkus().buildRequest().get();
      if (page == null) {
        throw new ConnectorException("Failure retrieving page of users.");
      }

      List<SubscribedSku> licenses = page.getCurrentPage();
      licenses.forEach(it -> response.add(new MicrosoftGraphLicense(it)));
    } catch (GraphServiceException gse) {
      driver.handleGraphServiceException(gse);
    }
    return response;
  }

  @Override
  public MicrosoftGraphLicense getOne(
      MicrosoftGraphDriver driver, String id, Map<String, Object> map) throws ConnectorException {
    try {
      SubscribedSku matchingLicense =
          Objects.requireNonNull(
                  driver.getGraphClient().subscribedSkus().byId(id),
                  String.format("MSGraph license byId produced null result for license %s", id))
              .buildRequest()
              .get();
      return new MicrosoftGraphLicense(matchingLicense);
    } catch (GraphServiceException gse) {
      driver.handleGraphServiceException(gse);
    }
    return null;
  }
}
