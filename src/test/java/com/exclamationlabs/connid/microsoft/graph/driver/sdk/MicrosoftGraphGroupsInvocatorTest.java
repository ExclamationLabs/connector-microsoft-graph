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
import com.exclamationlabs.connid.microsoft.graph.model.MicrosoftGraphGroup;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.requests.GroupCollectionPage;
import com.microsoft.graph.requests.GroupCollectionRequest;
import com.microsoft.graph.requests.GroupCollectionRequestBuilder;
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
class MicrosoftGraphGroupsInvocatorTest {

  private static final String KNOWN_ID = "6a1b2c3d-4e5f-4a6b-8c9d-0e1f2a3b4c5d";

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  MicrosoftGraphDriver driver;

  @Mock GroupCollectionRequest request;

  @Mock GroupCollectionRequest filteredRequest;

  MicrosoftGraphGroupsInvocator invocator;

  @BeforeEach
  void setUp() {
    invocator = new MicrosoftGraphGroupsInvocator();
    when(driver.getGraphClient().groups().buildRequest().select(anyString())).thenReturn(request);
    when(request.top(anyInt())).thenReturn(request);
    when(request.filter(anyString())).thenReturn(filteredRequest);
    when(driver.getConfiguration().getEnableDebugHttpLogging()).thenReturn(false);
  }

  private static Group group(String id, String displayName) {
    Group group = new Group();
    group.id = id;
    group.displayName = displayName;
    return group;
  }

  private static GroupCollectionPage page(List<Group> contents, GroupCollectionPage nextPage) {
    GroupCollectionPage page = mock(GroupCollectionPage.class);
    when(page.getCurrentPage()).thenReturn(contents);
    if (nextPage == null) {
      when(page.getNextPage()).thenReturn(null);
    } else {
      GroupCollectionRequestBuilder builder = mock(GroupCollectionRequestBuilder.class);
      GroupCollectionRequest nextRequest = mock(GroupCollectionRequest.class);
      when(page.getNextPage()).thenReturn(builder);
      when(builder.buildRequest()).thenReturn(nextRequest);
      when(nextRequest.get()).thenReturn(nextPage);
    }
    return page;
  }

  private static Set<String> idsOf(Set<MicrosoftGraphGroup> groups) {
    return groups.stream().map(MicrosoftGraphGroup::getIdentityIdValue).collect(Collectors.toSet());
  }

  // --- getAll: filtered branch must actually collect results (FIN-14541) ---

  @Test
  void getAll_filtered_returnsMatchingGroups() {
    GroupCollectionPage only = page(Collections.singletonList(group(KNOWN_ID, "Ops")), null);
    when(filteredRequest.get()).thenReturn(only);

    Set<MicrosoftGraphGroup> result =
        invocator.getAll(
            driver, new ResultsFilter("DISPLAY_NAME", "Ops"), new ResultsPaginator(), null);

    assertEquals(Collections.singleton(KNOWN_ID), idsOf(result));
  }

  @Test
  void getAll_filtered_appliesFilterExpression() {
    GroupCollectionPage empty = page(Collections.emptyList(), null);
    when(filteredRequest.get()).thenReturn(empty);

    invocator.getAll(driver, new ResultsFilter("EMAIL", "ops@x.com"), new ResultsPaginator(), null);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(request).filter(captor.capture());
    assertEquals("mail eq 'ops@x.com'", captor.getValue());
  }

  @Test
  void getAll_filtered_pagesBeyondFirstPage() {
    GroupCollectionPage second = page(Collections.singletonList(group("id-2", "B")), null);
    GroupCollectionPage first = page(Collections.singletonList(group("id-1", "A")), second);
    when(filteredRequest.get()).thenReturn(first);

    Set<MicrosoftGraphGroup> result =
        invocator.getAll(
            driver, new ResultsFilter("DISPLAY_NAME", "A"), new ResultsPaginator(), null);

    assertEquals(new HashSet<>(Arrays.asList("id-1", "id-2")), idsOf(result));
  }

  @Test
  void getAll_unfiltered_pagesBeyondFirstPage() {
    GroupCollectionPage second = page(Collections.singletonList(group("id-2", "B")), null);
    GroupCollectionPage first = page(Collections.singletonList(group("id-1", "A")), second);
    when(request.get()).thenReturn(first);

    Set<MicrosoftGraphGroup> result =
        invocator.getAll(driver, new ResultsFilter(), new ResultsPaginator(), null);

    assertEquals(new HashSet<>(Arrays.asList("id-1", "id-2")), idsOf(result));
    verify(request, never()).filter(anyString());
  }

  /**
   * getAll follows every nextLink itself, so it must declare the paginator finished.
   * BaseAdapter.executeBatchImport loops until this flag is set and would otherwise re-request the
   * whole tenant forever.
   */
  @Test
  void getAll_marksThePaginatorComplete_soBatchImportTerminates() {
    GroupCollectionPage only = page(Collections.singletonList(group(KNOWN_ID, "Ops")), null);
    when(request.get()).thenReturn(only);
    ResultsPaginator paginator = new ResultsPaginator(50, 1);
    assertFalse(paginator.getNoMoreResults());

    invocator.getAll(driver, new ResultsFilter(), paginator, null);

    assertTrue(paginator.getNoMoreResults());
  }

  @Test
  void getAll_leavesPaginatorIncompleteWhenTheReadFails() {
    when(request.get()).thenReturn(null);
    ResultsPaginator paginator = new ResultsPaginator(50, 1);

    assertThrows(
        ConnectorException.class,
        () -> invocator.getAll(driver, new ResultsFilter(), paginator, null));
    assertFalse(paginator.getNoMoreResults());
  }

  // --- a partial read must fail loudly, never masquerade as the full set ---

  /**
   * handleGraphServiceException swallows a 404, so getAll used to return whatever it had collected
   * so far. Midpoint cannot tell that from "the remaining groups were deleted" and unlinks them.
   */
  @Test
  void getAll_throwsWhenAPageFetchFails_ratherThanReturningPartialResults() {
    GroupCollectionPage first = page(Collections.singletonList(group("id-1", "A")), null);
    GroupCollectionRequestBuilder builder = mock(GroupCollectionRequestBuilder.class);
    GroupCollectionRequest nextRequest = mock(GroupCollectionRequest.class);
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
    GroupCollectionPage first = page(Collections.singletonList(group("id-1", "A")), null);
    GroupCollectionRequestBuilder builder = mock(GroupCollectionRequestBuilder.class);
    GroupCollectionRequest nextRequest = mock(GroupCollectionRequest.class);
    when(first.getNextPage()).thenReturn(builder);
    when(builder.buildRequest()).thenReturn(nextRequest);
    when(nextRequest.get()).thenReturn(null);
    when(request.get()).thenReturn(first);

    assertThrows(
        ConnectorException.class,
        () -> invocator.getAll(driver, new ResultsFilter(), new ResultsPaginator(), null));
  }

  @Test
  void getAll_throwsWhenTheFirstPageComesBackNull() {
    when(request.get()).thenReturn(null);

    assertThrows(
        ConnectorException.class,
        () -> invocator.getAll(driver, new ResultsFilter(), new ResultsPaginator(), null));
  }

  /** getOne selects detailFields, so its result may report the detail-only attributes. */
  @Test
  void getOne_marksTheResultAsDetailLoaded() {
    Group found = group(KNOWN_ID, "Ops");
    when(driver.getGraphClient().groups().byId(KNOWN_ID).buildRequest().select(anyString()).get())
        .thenReturn(found);

    MicrosoftGraphGroup result = invocator.getOne(driver, KNOWN_ID, Collections.emptyMap());

    assertNotNull(result);
    assertTrue(result.isDetailLoaded());
  }

  /** getAll only selects summaryFields, so its results must not claim to carry detail. */
  @Test
  void getAll_marksResultsAsShallow() {
    GroupCollectionPage only = page(Collections.singletonList(group(KNOWN_ID, "Ops")), null);
    when(request.get()).thenReturn(only);

    Set<MicrosoftGraphGroup> result =
        invocator.getAll(driver, new ResultsFilter(), new ResultsPaginator(), null);

    assertFalse(result.iterator().next().isDetailLoaded());
  }

  /**
   * Deliberate asymmetry with getAll: a 404 on a single-object read means that object is genuinely
   * absent, which Midpoint needs to hear as null. Only a failed collection read is ambiguous enough
   * to warrant throwing.
   */
  @Test
  void getOne_returnsNullWhenTheGroupIsNotFound() {
    GraphServiceException notFound = mock(GraphServiceException.class);
    when(driver.getGraphClient().groups().byId(KNOWN_ID).buildRequest().select(anyString()).get())
        .thenThrow(notFound);

    assertNull(invocator.getOne(driver, KNOWN_ID, Collections.emptyMap()));
  }

  // --- getOneByName: composite "displayName (id)" Name handling ---

  @Test
  void getOneByName_compositeName_readsGroupById() {
    Group found = group(KNOWN_ID, "Ops");
    when(driver.getGraphClient().groups().byId(KNOWN_ID).buildRequest().select(anyString()).get())
        .thenReturn(found);

    MicrosoftGraphGroup result = invocator.getOneByName(driver, "Ops (" + KNOWN_ID + ")");

    assertNotNull(result);
    assertEquals(KNOWN_ID, result.getIdentityIdValue());
    verify(request, never()).filter(anyString());
  }

  @Test
  void getOneByName_plainDisplayName_fallsBackToFilteredLookup() {
    GroupCollectionPage only = page(Collections.singletonList(group(KNOWN_ID, "Ops")), null);
    when(filteredRequest.get()).thenReturn(only);

    MicrosoftGraphGroup result = invocator.getOneByName(driver, "Ops");

    assertNotNull(result);
    assertEquals(KNOWN_ID, result.getIdentityIdValue());
    verify(request).filter("displayName eq 'Ops'");
  }

  @Test
  void getOneByName_nullName_returnsNull() {
    assertNull(invocator.getOneByName(driver, null));
  }

  @Test
  void extractGroupId_pullsIdFromCompositeName() {
    assertEquals(KNOWN_ID, MicrosoftGraphGroupsInvocator.extractGroupId("Ops (" + KNOWN_ID + ")"));
  }

  @Test
  void extractGroupId_handlesDisplayNameContainingParentheses() {
    assertEquals(
        KNOWN_ID, MicrosoftGraphGroupsInvocator.extractGroupId("Sales (EMEA) (" + KNOWN_ID + ")"));
  }

  @Test
  void extractGroupId_returnsNullForPlainDisplayName() {
    assertNull(MicrosoftGraphGroupsInvocator.extractGroupId("Ops"));
  }

  @Test
  void extractGroupId_returnsNullWhenParentheticalIsNotAnId() {
    assertNull(MicrosoftGraphGroupsInvocator.extractGroupId("Sales (EMEA)"));
  }

  // --- isMsTeam: read from the group payload, no per-group /teams probe ---

  private static Group groupWithProvisioningOptions(String... options) {
    Group group = group(KNOWN_ID, "Ops");
    JsonArray array = new JsonArray();
    for (String option : options) {
      array.add(new JsonPrimitive(option));
    }
    group.additionalDataManager().put("resourceProvisioningOptions", array);
    return group;
  }

  @Test
  void isMsTeam_trueWhenProvisioningOptionsContainTeam() {
    assertTrue(MicrosoftGraphGroupsInvocator.isMsTeam(groupWithProvisioningOptions("Team")));
  }

  @Test
  void isMsTeam_falseWhenProvisioningOptionsOmitTeam() {
    assertFalse(MicrosoftGraphGroupsInvocator.isMsTeam(groupWithProvisioningOptions("Other")));
  }

  @Test
  void isMsTeam_falseWhenProvisioningOptionsEmpty() {
    assertFalse(MicrosoftGraphGroupsInvocator.isMsTeam(groupWithProvisioningOptions()));
  }

  @Test
  void isMsTeam_falseWhenPropertyAbsent() {
    assertFalse(MicrosoftGraphGroupsInvocator.isMsTeam(group(KNOWN_ID, "Ops")));
  }

  @Test
  void isMsTeam_falseForNullGroup() {
    assertFalse(MicrosoftGraphGroupsInvocator.isMsTeam(null));
  }

  @Test
  void getAll_setsMsTeamFlagFromGroupPayload() {
    GroupCollectionPage only =
        page(Collections.singletonList(groupWithProvisioningOptions("Team")), null);
    when(request.get()).thenReturn(only);

    Set<MicrosoftGraphGroup> result =
        invocator.getAll(driver, new ResultsFilter(), new ResultsPaginator(), null);

    assertTrue(result.iterator().next().getMsTeam());
  }

  @Test
  void getAll_neverProbesTeamsEndpoint() {
    GroupCollectionPage only =
        page(Collections.singletonList(groupWithProvisioningOptions("Team")), null);
    when(request.get()).thenReturn(only);

    invocator.getAll(driver, new ResultsFilter(), new ResultsPaginator(), null);

    verify(driver.getGraphClient(), never()).teams();
  }
}
