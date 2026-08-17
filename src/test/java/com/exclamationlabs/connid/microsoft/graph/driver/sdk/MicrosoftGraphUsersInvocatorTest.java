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
import com.exclamationlabs.connid.microsoft.graph.model.MicrosoftGraphUser;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GroupCollectionPage;
import com.microsoft.graph.requests.GroupCollectionRequest;
import com.microsoft.graph.requests.GroupCollectionRequestBuilder;
import com.microsoft.graph.requests.UserCollectionPage;
import com.microsoft.graph.requests.UserCollectionRequest;
import com.microsoft.graph.requests.UserCollectionRequestBuilder;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MicrosoftGraphUsersInvocatorTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  MicrosoftGraphDriver driver;

  @Mock UserCollectionRequest request;

  @Mock UserCollectionRequest filteredRequest;

  MicrosoftGraphUsersInvocator invocator;

  @BeforeEach
  void setUp() {
    invocator = new MicrosoftGraphUsersInvocator();
    when(driver.getGraphClient().users().buildRequest().select(anyString())).thenReturn(request);
    when(request.top(anyInt())).thenReturn(request);
    when(request.filter(anyString())).thenReturn(filteredRequest);
    when(driver.getConfiguration().getEnableDebugHttpLogging()).thenReturn(false);
  }

  private static User user(String id) {
    User user = new User();
    user.id = id;
    return user;
  }

  private static UserCollectionPage page(List<User> contents, UserCollectionPage nextPage) {
    UserCollectionPage page = mock(UserCollectionPage.class);
    when(page.getCurrentPage()).thenReturn(contents);
    if (nextPage == null) {
      when(page.getNextPage()).thenReturn(null);
    } else {
      UserCollectionRequestBuilder builder = mock(UserCollectionRequestBuilder.class);
      UserCollectionRequest nextRequest = mock(UserCollectionRequest.class);
      when(page.getNextPage()).thenReturn(builder);
      when(builder.buildRequest()).thenReturn(nextRequest);
      when(nextRequest.get()).thenReturn(nextPage);
    }
    return page;
  }

  private static Set<String> idsOf(Set<MicrosoftGraphUser> users) {
    return users.stream().map(MicrosoftGraphUser::getIdentityIdValue).collect(Collectors.toSet());
  }

  @Test
  void getAll_filtered_returnsMatchingUsers() {
    UserCollectionPage only = page(Collections.singletonList(user("id-1")), null);
    when(filteredRequest.get()).thenReturn(only);

    Set<MicrosoftGraphUser> result =
        invocator.getAll(
            driver, new ResultsFilter("DISPLAY_NAME", "Ann"), new ResultsPaginator(), null);

    assertEquals(Collections.singleton("id-1"), idsOf(result));
  }

  /** A filtered search used to return only the first page, silently capping results at 100. */
  @Test
  void getAll_filtered_pagesBeyondFirstPage() {
    UserCollectionPage second = page(Collections.singletonList(user("id-2")), null);
    UserCollectionPage first = page(Collections.singletonList(user("id-1")), second);
    when(filteredRequest.get()).thenReturn(first);

    Set<MicrosoftGraphUser> result =
        invocator.getAll(
            driver, new ResultsFilter("USER_TYPE", "Member"), new ResultsPaginator(), null);

    assertEquals(new HashSet<>(Arrays.asList("id-1", "id-2")), idsOf(result));
  }

  @Test
  void getAll_filtered_appliesFilterExpression() {
    UserCollectionPage empty = page(Collections.emptyList(), null);
    when(filteredRequest.get()).thenReturn(empty);

    invocator.getAll(driver, new ResultsFilter("EMAIL", "ann@x.com"), new ResultsPaginator(), null);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(request).filter(captor.capture());
    assertEquals("mail eq 'ann@x.com'", captor.getValue());
  }

  /**
   * handleGraphServiceException swallows a 404, so getAll used to return whatever it had collected
   * so far - which Midpoint cannot distinguish from the remaining users having been deleted.
   */
  @Test
  void getAll_throwsWhenAPageFetchFails_ratherThanReturningPartialResults() {
    UserCollectionPage first = page(Collections.singletonList(user("id-1")), null);
    UserCollectionRequestBuilder builder = mock(UserCollectionRequestBuilder.class);
    UserCollectionRequest nextRequest = mock(UserCollectionRequest.class);
    GraphServiceException notFound = mock(GraphServiceException.class);
    when(first.getNextPage()).thenReturn(builder);
    when(builder.buildRequest()).thenReturn(nextRequest);
    when(nextRequest.get()).thenThrow(notFound);
    when(request.get()).thenReturn(first);

    assertThrows(
        ConnectorException.class,
        () -> invocator.getAll(driver, new ResultsFilter(), new ResultsPaginator(), null));
  }

  @Test
  void getAll_throwsWhenANextPageComesBackNull() {
    UserCollectionPage first = page(Collections.singletonList(user("id-1")), null);
    UserCollectionRequestBuilder builder = mock(UserCollectionRequestBuilder.class);
    UserCollectionRequest nextRequest = mock(UserCollectionRequest.class);
    when(first.getNextPage()).thenReturn(builder);
    when(builder.buildRequest()).thenReturn(nextRequest);
    when(nextRequest.get()).thenReturn(null);
    when(request.get()).thenReturn(first);

    assertThrows(
        ConnectorException.class,
        () -> invocator.getAll(driver, new ResultsFilter(), new ResultsPaginator(), null));
  }

  /**
   * getAll follows every nextLink itself, so it must declare the paginator finished.
   * BaseAdapter.executeBatchImport loops until this flag is set and would otherwise re-request the
   * whole tenant forever.
   */
  @Test
  void getAll_marksThePaginatorComplete_soBatchImportTerminates() {
    UserCollectionPage only = page(Collections.singletonList(user("id-1")), null);
    when(request.get()).thenReturn(only);
    ResultsPaginator paginator = new ResultsPaginator(50, 1);
    assertFalse(paginator.getNoMoreResults());

    invocator.getAll(driver, new ResultsFilter(), paginator, null);

    assertTrue(paginator.getNoMoreResults());
  }

  /** An unfiltered read only selects summaryFields, so detail attributes must not be reported. */
  @Test
  void getAll_unfiltered_marksResultsAsShallow() {
    UserCollectionPage only = page(Collections.singletonList(user("id-1")), null);
    when(request.get()).thenReturn(only);

    Set<MicrosoftGraphUser> result =
        invocator.getAll(driver, new ResultsFilter(), new ResultsPaginator(), null);

    assertFalse(result.iterator().next().isDetailLoaded());
  }

  /** A filtered read selects detailFields, so those attributes are genuinely present. */
  @Test
  void getAll_filtered_marksResultsAsDetailLoaded() {
    UserCollectionPage only = page(Collections.singletonList(user("id-1")), null);
    when(filteredRequest.get()).thenReturn(only);

    Set<MicrosoftGraphUser> result =
        invocator.getAll(
            driver, new ResultsFilter("DISPLAY_NAME", "Ann"), new ResultsPaginator(), null);

    assertTrue(result.iterator().next().isDetailLoaded());
    assertFalse(result.iterator().next().isFilterIncompatibleLoaded());
  }

  /** Graph rejects a SharePoint-backed field in $select alongside a $filter. */
  @Test
  void getAll_filtered_doesNotSelectFilterIncompatibleFields() {
    UserCollectionPage only = page(Collections.singletonList(user("id-1")), null);
    when(filteredRequest.get()).thenReturn(only);

    invocator.getAll(
        driver, new ResultsFilter("DISPLAY_NAME", "Ann"), new ResultsPaginator(), null);

    ArgumentCaptor<String> select = ArgumentCaptor.forClass(String.class);
    verify(driver.getGraphClient().users().buildRequest(), atLeastOnce()).select(select.capture());
    List<String> fields = Arrays.asList(select.getValue().split(","));
    assertTrue(fields.contains("city"));
    assertFalse(fields.contains("skills"));
    assertFalse(fields.contains("responsibilities"));
    assertFalse(fields.contains("hireDate"));
  }

  /**
   * getOne selects detailFields and separately fetches memberOf, so its result may report the
   * detail-only attributes and the group assignments.
   */
  @Test
  void getOne_marksTheResultAsDetailLoadedAndPopulatesMemberOf() {
    User found = user("id-1");
    GroupCollectionPage memberPage = mock(GroupCollectionPage.class);
    GroupCollectionRequestBuilder memberBuilder = mock(GroupCollectionRequestBuilder.class);
    GroupCollectionRequest memberRequest = mock(GroupCollectionRequest.class);
    when(memberPage.getCurrentPage()).thenReturn(Collections.emptyList());
    when(memberPage.getNextPage()).thenReturn(null);
    when(memberBuilder.buildRequest()).thenReturn(memberRequest);
    when(memberRequest.get()).thenReturn(memberPage);
    when(driver.getGraphClient().users().byId("id-1").buildRequest().select(anyString()).get())
        .thenReturn(found);
    when(driver.getGraphClient().users().byId("id-1").memberOfAsGroup()).thenReturn(memberBuilder);

    MicrosoftGraphUser result = invocator.getOne(driver, "id-1", Collections.emptyMap());

    assertNotNull(result);
    assertTrue(result.isDetailLoaded());
    assertTrue(result.isFilterIncompatibleLoaded());
    assertNotNull(result.getMemberOf());
  }

  @Test
  void getAll_unfiltered_pagesBeyondFirstPage() {
    UserCollectionPage second = page(Collections.singletonList(user("id-2")), null);
    UserCollectionPage first = page(Collections.singletonList(user("id-1")), second);
    when(request.get()).thenReturn(first);

    Set<MicrosoftGraphUser> result =
        invocator.getAll(driver, new ResultsFilter(), new ResultsPaginator(), null);

    assertEquals(new HashSet<>(Arrays.asList("id-1", "id-2")), idsOf(result));
    verify(request, never()).filter(anyString());
  }
}
