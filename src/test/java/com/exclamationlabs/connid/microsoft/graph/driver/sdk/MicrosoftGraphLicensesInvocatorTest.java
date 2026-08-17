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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.exclamationlabs.connid.base.connector.results.ResultsFilter;
import com.exclamationlabs.connid.base.connector.results.ResultsPaginator;
import com.exclamationlabs.connid.microsoft.graph.model.MicrosoftGraphLicense;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.SubscribedSku;
import com.microsoft.graph.requests.SubscribedSkuCollectionPage;
import com.microsoft.graph.requests.SubscribedSkuCollectionRequest;
import com.microsoft.graph.requests.SubscribedSkuCollectionRequestBuilder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MicrosoftGraphLicensesInvocatorTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  MicrosoftGraphDriver driver;

  @Mock SubscribedSkuCollectionRequest request;

  MicrosoftGraphLicensesInvocator invocator;

  @BeforeEach
  void setUp() {
    invocator = new MicrosoftGraphLicensesInvocator();
    when(driver.getGraphClient().subscribedSkus().buildRequest()).thenReturn(request);
    when(driver.getConfiguration().getEnableDebugHttpLogging()).thenReturn(false);
  }

  private static SubscribedSku sku(String id) {
    SubscribedSku sku = new SubscribedSku();
    sku.id = id;
    return sku;
  }

  private static SubscribedSkuCollectionPage page(
      List<SubscribedSku> contents, SubscribedSkuCollectionPage nextPage) {
    SubscribedSkuCollectionPage page = mock(SubscribedSkuCollectionPage.class);
    when(page.getCurrentPage()).thenReturn(contents);
    if (nextPage == null) {
      when(page.getNextPage()).thenReturn(null);
    } else {
      SubscribedSkuCollectionRequestBuilder builder =
          mock(SubscribedSkuCollectionRequestBuilder.class);
      SubscribedSkuCollectionRequest nextRequest = mock(SubscribedSkuCollectionRequest.class);
      when(page.getNextPage()).thenReturn(builder);
      when(builder.buildRequest()).thenReturn(nextRequest);
      when(nextRequest.get()).thenReturn(nextPage);
    }
    return page;
  }

  private void givenFirstPage(SubscribedSkuCollectionPage first) {
    when(request.get()).thenReturn(first);
  }

  private static Set<String> idsOf(Set<MicrosoftGraphLicense> licenses) {
    return licenses.stream()
        .map(MicrosoftGraphLicense::getIdentityIdValue)
        .collect(Collectors.toSet());
  }

  @Test
  void getAll_returnsLicensesFromFirstPage() {
    givenFirstPage(page(Collections.singletonList(sku("sku-1")), null));

    Set<MicrosoftGraphLicense> result =
        invocator.getAll(driver, new ResultsFilter(), new ResultsPaginator(), null);

    assertEquals(Collections.singleton("sku-1"), idsOf(result));
  }

  /**
   * subscribedSkus is not expected to paginate, but a truncated license list would read to Midpoint
   * as deleted entitlements, so the nextLink is followed if one is ever returned.
   */
  @Test
  void getAll_pagesBeyondFirstPage() {
    SubscribedSkuCollectionPage second = page(Collections.singletonList(sku("sku-2")), null);
    givenFirstPage(page(Collections.singletonList(sku("sku-1")), second));

    Set<MicrosoftGraphLicense> result =
        invocator.getAll(driver, new ResultsFilter(), new ResultsPaginator(), null);

    assertEquals(new HashSet<>(Arrays.asList("sku-1", "sku-2")), idsOf(result));
  }

  /**
   * getAll follows every nextLink itself, so it must declare the paginator finished.
   * BaseAdapter.executeBatchImport loops until this flag is set and would otherwise re-request
   * every SKU forever.
   */
  @Test
  void getAll_marksThePaginatorComplete_soBatchImportTerminates() {
    givenFirstPage(page(Collections.singletonList(sku("sku-1")), null));
    ResultsPaginator paginator = new ResultsPaginator(50, 1);
    assertFalse(paginator.getNoMoreResults());

    invocator.getAll(driver, new ResultsFilter(), paginator, null);

    assertTrue(paginator.getNoMoreResults());
  }

  /**
   * handleGraphServiceException swallows a 404, so getAll used to return whatever it had collected
   * so far - which Midpoint reads as the remaining licenses having been deleted.
   */
  @Test
  void getAll_throwsWhenAPageFetchFails_ratherThanReturningPartialResults() {
    SubscribedSkuCollectionPage first = page(Collections.singletonList(sku("sku-1")), null);
    SubscribedSkuCollectionRequestBuilder builder =
        mock(SubscribedSkuCollectionRequestBuilder.class);
    SubscribedSkuCollectionRequest nextRequest = mock(SubscribedSkuCollectionRequest.class);
    GraphServiceException notFound = mock(GraphServiceException.class);
    when(first.getNextPage()).thenReturn(builder);
    when(builder.buildRequest()).thenReturn(nextRequest);
    when(nextRequest.get()).thenThrow(notFound);
    givenFirstPage(first);

    assertThrows(
        ConnectorException.class,
        () -> invocator.getAll(driver, new ResultsFilter(), new ResultsPaginator(), null));
  }

  @Test
  void getAll_throwsWhenANextPageComesBackNull() {
    SubscribedSkuCollectionPage first = page(Collections.singletonList(sku("sku-1")), null);
    SubscribedSkuCollectionRequestBuilder builder =
        mock(SubscribedSkuCollectionRequestBuilder.class);
    SubscribedSkuCollectionRequest nextRequest = mock(SubscribedSkuCollectionRequest.class);
    when(first.getNextPage()).thenReturn(builder);
    when(builder.buildRequest()).thenReturn(nextRequest);
    when(nextRequest.get()).thenReturn(null);
    givenFirstPage(first);

    assertThrows(
        ConnectorException.class,
        () -> invocator.getAll(driver, new ResultsFilter(), new ResultsPaginator(), null));
  }
}
