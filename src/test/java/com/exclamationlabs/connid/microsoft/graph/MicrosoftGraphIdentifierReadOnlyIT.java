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

import static com.exclamationlabs.connid.microsoft.graph.attribute.MicrosoftGraphUserAttribute.*;
import static org.junit.jupiter.api.Assertions.*;

import com.exclamationlabs.connid.base.connector.configuration.ConfigurationNameBuilder;
import com.exclamationlabs.connid.base.connector.configuration.ConfigurationReader;
import com.exclamationlabs.connid.base.connector.test.ApiIntegrationTest;
import com.exclamationlabs.connid.base.microsoft.graph.configuration.MicrosoftGraphConfiguration;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.junit.jupiter.api.*;

/**
 * Read-only verification of the 3.0.0 identifier change ({@code user.__NAME__} moved from {@code
 * displayName} to {@code userPrincipalName}).
 *
 * <p>Every test here is a schema inspection or a GET/search. Nothing is created, updated, or
 * deleted, so this is safe to run against a live tenant with real accounts.
 *
 * <p>The subject user is discovered from a tenant listing at runtime rather than hardcoded, so no
 * per-tenant constants need to be filled in.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MicrosoftGraphIdentifierReadOnlyIT
    extends ApiIntegrationTest<MicrosoftGraphConfiguration, MicrosoftGraphConnector> {

  /** A real user discovered in test 020, reused as the subject of the later read tests. */
  private static ConnectorObject subject;

  private static String subjectUid;
  private static String subjectUpn;
  private static String subjectDisplayName;

  @Override
  protected MicrosoftGraphConfiguration getConfiguration() {
    return new MicrosoftGraphConfiguration(
        new ConfigurationNameBuilder().withConnector(() -> "MICROSOFT_GRAPH").build());
  }

  @Override
  protected Class<MicrosoftGraphConnector> getConnectorClass() {
    return MicrosoftGraphConnector.class;
  }

  @Override
  protected void readConfiguration(MicrosoftGraphConfiguration configuration) {
    ConfigurationReader.setupTestConfiguration(configuration);
  }

  @Override
  protected APIConfiguration apiConfig(MicrosoftGraphConfiguration configurationObject) {
    APIConfiguration config = super.apiConfig(configurationObject);
    // Disable the framework-side handlers so we observe exactly what the connector emits,
    // not a normalized/filtered view of it.
    config.getResultsHandlerConfiguration().setEnableFilteredResultsHandler(false);
    config.getResultsHandlerConfiguration().setEnableNormalizingResultsHandler(false);
    config.getResultsHandlerConfiguration().setEnableAttributesToGetSearchResultsHandler(false);
    config.getResultsHandlerConfiguration().setFilteredResultsHandlerInValidationMode(false);
    return config;
  }

  @BeforeEach
  public void setup() {
    super.setup();
  }

  // ---------------------------------------------------------------------------
  // Connectivity + schema
  // ---------------------------------------------------------------------------

  @Test
  @Order(10)
  public void test010Connectivity() {
    getConnectorFacade().test();
  }

  @Test
  @Order(11)
  public void test011UserSchemaNameIsUserPrincipalName() {
    Schema schema = getConnectorFacade().schema();
    assertNotNull(schema);

    ObjectClassInfo userInfo = objectClass(schema, "user");

    AttributeInfo nameInfo =
        userInfo.getAttributeInfo().stream()
            .filter(ai -> Name.NAME.equals(ai.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("__NAME__ missing from user schema"));

    // This is the core of the change: midPoint derives namingAttribute, secondaryIdentifier
    // and displayNameAttribute from the native name behind __NAME__.
    assertEquals(
        USER_PRINCIPAL_NAME.name(),
        nameInfo.getNativeName(),
        "__NAME__ must map to USER_PRINCIPAL_NAME, not DISPLAY_NAME");

    // DISPLAY_NAME must still be present, but as an ordinary attribute.
    AttributeInfo displayNameInfo =
        userInfo.getAttributeInfo().stream()
            .filter(ai -> DISPLAY_NAME.name().equals(ai.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("DISPLAY_NAME missing from user schema"));
    assertTrue(displayNameInfo.isCreateable(), "DISPLAY_NAME must remain creatable");
    assertTrue(displayNameInfo.isUpdateable(), "DISPLAY_NAME must remain updateable");

    // USER_PRINCIPAL_NAME must NOT also appear as a separate plain attribute — a duplicate
    // declaration alongside __NAME__ is what would make midPoint see two attributes for one
    // Graph field.
    assertFalse(
        userInfo.getAttributeInfo().stream()
            .anyMatch(ai -> USER_PRINCIPAL_NAME.name().equals(ai.getName())),
        "USER_PRINCIPAL_NAME must be declared only as __NAME__, not duplicated as a plain attribute");
  }

  @Test
  @Order(12)
  public void test012GroupSchemaNameStillDisplayName() {
    // The group objectClass is intentionally unchanged by 3.0.0 — guard against collateral drift.
    ObjectClassInfo groupInfo = objectClass(getConnectorFacade().schema(), "group");

    AttributeInfo nameInfo =
        groupInfo.getAttributeInfo().stream()
            .filter(ai -> Name.NAME.equals(ai.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("__NAME__ missing from group schema"));
    assertEquals(
        com.exclamationlabs.connid.microsoft.graph.attribute.MicrosoftGraphGroupAttribute
            .DISPLAY_NAME.name(),
        nameInfo.getNativeName(),
        "group __NAME__ must still map to DISPLAY_NAME");
  }

  // ---------------------------------------------------------------------------
  // Read path — what the connector actually emits for real accounts
  // ---------------------------------------------------------------------------

  @Test
  @Order(20)
  public void test020ListUsersEmitsUpnAsName() {
    results = new ArrayList<>();
    getConnectorFacade()
        .search(new ObjectClass("user"), null, handler, new OperationOptionsBuilder().build());
    assertFalse(results.isEmpty(), "tenant returned no users");

    // Every returned user must have a non-blank __NAME__ that looks like a UPN. If the read path
    // did not select userPrincipalName, __NAME__ would be null here and the schema would be a lie.
    for (ConnectorObject user : results) {
      String uid = single(user, Uid.NAME);
      String name = single(user, Name.NAME);
      assertTrue(StringUtils.isNotBlank(uid), "user has blank __UID__");
      assertTrue(StringUtils.isNotBlank(name), "user " + uid + " has blank __NAME__");
      assertTrue(
          name.contains("@"),
          "user " + uid + " __NAME__ is '" + name + "', which is not a UPN — expected UPN form");

      // __NAME__ must agree with the explicitly-emitted UPN-bearing value, and must NOT be the
      // displayName.
      String displayName = single(user, DISPLAY_NAME.name());
      if (StringUtils.isNotBlank(displayName) && !displayName.contains("@")) {
        assertNotEquals(
            displayName, name, "user " + uid + " __NAME__ is still the displayName");
      }
    }

    // Pick a subject for the remaining tests: prefer one whose displayName differs from its UPN,
    // so the assertions below can actually distinguish the two fields.
    subject =
        results.stream()
            .filter(u -> StringUtils.isNotBlank(single(u, Name.NAME)))
            .filter(u -> StringUtils.isNotBlank(single(u, DISPLAY_NAME.name())))
            .filter(u -> !single(u, DISPLAY_NAME.name()).equals(single(u, Name.NAME)))
            .findFirst()
            .orElse(results.get(0));

    subjectUid = single(subject, Uid.NAME);
    subjectUpn = single(subject, Name.NAME);
    subjectDisplayName = single(subject, DISPLAY_NAME.name());

    System.out.printf(
        "[read-only IT] subject uid=%s __NAME__=%s DISPLAY_NAME=%s%n",
        subjectUid, subjectUpn, subjectDisplayName);
  }

  @Test
  @Order(21)
  public void test021DisplayNameEmittedAsPlainAttribute() {
    assumeSubject();
    // DISPLAY_NAME became a plain attribute in constructAttributes(); confirm it is actually
    // populated on read rather than silently dropped.
    assertTrue(
        StringUtils.isNotBlank(subjectDisplayName),
        "DISPLAY_NAME was not emitted for user " + subjectUid);
  }

  @Test
  @Order(30)
  public void test030GetByUidEmitsUpnAsName() {
    assumeSubject();
    // Detail read (getObject) travels a different Graph call than the list above — verify it too.
    ConnectorObject user =
        getConnectorFacade()
            .getObject(
                new ObjectClass("user"), new Uid(subjectUid), new OperationOptionsBuilder().build());
    assertNotNull(user, "getObject returned null for known uid " + subjectUid);
    assertEquals(subjectUpn, single(user, Name.NAME), "detail read __NAME__ disagrees with list");
    assertEquals(
        subjectDisplayName,
        single(user, DISPLAY_NAME.name()),
        "detail read DISPLAY_NAME disagrees with list");
  }

  // ---------------------------------------------------------------------------
  // Filter path — the invocator's filterAttributeToFieldMap changes
  // ---------------------------------------------------------------------------

  @Test
  @Order(40)
  public void test040FilterByNameResolvesUpn() {
    assumeSubject();
    // Name.NAME now maps to the Graph field userPrincipalName. Searching by the subject's UPN
    // must return exactly that user.
    //
    // NOTE: this exercises MicrosoftGraphUsersInvocator.getAll()'s *filtered* branch, which
    // selects detailFields. detailFields contains the SharePoint-backed profile fields "skills"
    // and "responsibilities", which Graph refuses to combine with any $filter on /users:
    //     -1, Microsoft.SharePoint.Client.InvalidClientQueryException
    // That defect predates this change (present since the initial 2023 commit) and is unrelated
    // to the identifier mapping — the unfiltered branch selects summaryFields, which excludes
    // those two fields, which is why plain listing has always worked.
    // See test049 for the isolation proof. Remove this guard once detailFields is fixed.
    requireFilteredReadsWork();

    List<ConnectorObject> found = searchUsersBy(Name.NAME, subjectUpn);
    assertEquals(1, found.size(), "__NAME__ filter on UPN '" + subjectUpn + "' did not return 1");
    assertEquals(subjectUid, single(found.get(0), Uid.NAME));
  }

  @Test
  @Order(41)
  public void test041FilterByNameDoesNotMatchDisplayName() {
    assumeSubject();
    requireFilteredReadsWork();
    org.junit.jupiter.api.Assumptions.assumeTrue(
        StringUtils.isNotBlank(subjectDisplayName) && !subjectDisplayName.equals(subjectUpn),
        "subject displayName equals its UPN; cannot distinguish the two fields");

    // The regression guard: before 3.0.0 this filter hit displayName and would have matched.
    // It must now query userPrincipalName and therefore find nothing.
    assertTrue(
        searchUsersBy(Name.NAME, subjectDisplayName).isEmpty(),
        "__NAME__ filter matched the displayName '"
            + subjectDisplayName
            + "' — it is still querying displayName");
  }

  @Test
  @Order(42)
  public void test042FilterByUserPrincipalNameResolvesUpn() {
    assumeSubject();
    requireFilteredReadsWork();
    // USER_PRINCIPAL_NAME previously (and incorrectly) mapped to the Graph field "mail".
    // It must now map to userPrincipalName.
    List<ConnectorObject> found = searchUsersBy(USER_PRINCIPAL_NAME.name(), subjectUpn);
    assertEquals(
        1, found.size(), "USER_PRINCIPAL_NAME filter on '" + subjectUpn + "' did not return 1");
    assertEquals(subjectUid, single(found.get(0), Uid.NAME));
  }

  @Test
  @Order(43)
  public void test043FilterByDisplayNameStillResolvesDisplayName() {
    assumeSubject();
    requireFilteredReadsWork();
    org.junit.jupiter.api.Assumptions.assumeTrue(
        StringUtils.isNotBlank(subjectDisplayName), "subject has no displayName");

    // DISPLAY_NAME remains a filterable plain attribute against the Graph displayName field.
    // displayName is not unique, so assert "at least one, and ours is among them".
    List<ConnectorObject> found = searchUsersBy(DISPLAY_NAME.name(), subjectDisplayName);
    assertFalse(
        found.isEmpty(), "DISPLAY_NAME filter on '" + subjectDisplayName + "' returned nothing");
    assertTrue(
        found.stream().anyMatch(u -> subjectUid.equals(single(u, Uid.NAME))),
        "DISPLAY_NAME filter did not include the subject user");
  }

  @Test
  @Order(44)
  public void test044FilterByNameUnknownUpnReturnsNothing() {
    requireFilteredReadsWork();
    // A well-formed but nonexistent UPN must come back empty rather than erroring.
    assertTrue(
        searchUsersBy(Name.NAME, "no-such-user-" + UUID.randomUUID() + "@example.com").isEmpty(),
        "__NAME__ filter on a nonexistent UPN returned results");
  }

  // ---------------------------------------------------------------------------
  // Regression guard for the SharePoint filtered-read fix
  // ---------------------------------------------------------------------------

  /**
   * Guards the fix for the SharePoint {@code $select}/{@code $filter} conflict.
   *
   * <p>{@code detailFields} includes {@code skills}, {@code responsibilities} and {@code hireDate},
   * which are served by the SharePoint/Delve profile store rather than the Entra directory. Graph
   * rejects any {@code /users} request that selects one of them alongside a {@code $filter}:
   *
   * <pre>-1, Microsoft.SharePoint.Client.InvalidClientQueryException</pre>
   *
   * <p>Filtered searches therefore select {@code filterableDetailFields} instead. This test asserts
   * the outcome that matters — filtering works at all, on both an identifier and a plain attribute —
   * rather than reaching into the field lists.
   */
  @Test
  @Order(49)
  public void test049FilteredReadsSurviveSharePointFieldConflict() {
    assumeSubject();

    // Filtering on the identifier and on an ordinary attribute must both succeed. Before the fix
    // each of these threw InvalidClientQueryException.
    assertEquals(
        1,
        searchUsersBy(Name.NAME, subjectUpn).size(),
        "filtering by __NAME__ failed or returned the wrong count");
    assertFalse(
        searchUsersBy(USER_TYPE.name(), "Member").isEmpty(),
        "filtering by USER_TYPE returned nothing");

    // The excluded fields are only dropped from FILTERED reads. A detail read by UID still selects
    // the full detailFields, so anything the connector can expose is still reachable.
    ConnectorObject byUid =
        getConnectorFacade()
            .getObject(
                new ObjectClass("user"), new Uid(subjectUid), new OperationOptionsBuilder().build());
    assertNotNull(byUid, "detail read by UID failed");
    assertEquals(
        subjectUpn,
        single(byUid, Name.NAME),
        "detail read still selects detailFields and must agree on __NAME__");
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /**
   * True when the connector's filtered read path is usable against this tenant. Probes the filtered
   * branch of {@code getAll()} with a plain, always-present attribute so the result reflects the
   * path itself rather than anything specific to the identifier change.
   */
  private boolean filteredReadsWork() {
    try {
      searchUsersBy(USER_TYPE.name(), "Member");
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  /**
   * Fails the test if filtered reads are broken.
   *
   * <p>These tests previously skipped here, because {@code detailFields} selected the
   * SharePoint-backed fields {@code skills}, {@code responsibilities} and {@code hireDate}, which
   * Graph refuses to combine with any {@code $filter} on {@code /users}. That is fixed —
   * {@code MicrosoftGraphUsersInvocator} now selects {@code filterableDetailFields} when filtering.
   * This is a hard assertion rather than an assumption so a regression fails loudly instead of
   * silently skipping the filter coverage.
   */
  private void requireFilteredReadsWork() {
    assertTrue(
        filteredReadsWork(),
        "connector filtered reads are broken — check that MicrosoftGraphUsersInvocator still uses"
            + " filterableDetailFields (not detailFields) for filtered searches");
  }

  private List<ConnectorObject> searchUsersBy(String attributeName, String value) {
    results = new ArrayList<>();
    Attribute filter = new AttributeBuilder().setName(attributeName).addValue(value).build();
    getConnectorFacade()
        .search(
            new ObjectClass("user"),
            new EqualsFilter(filter),
            handler,
            new OperationOptionsBuilder().build());
    return new ArrayList<>(results);
  }

  private static ObjectClassInfo objectClass(Schema schema, String type) {
    return schema.getObjectClassInfo().stream()
        .filter(oc -> type.equals(oc.getType()))
        .findFirst()
        .orElseThrow(() -> new AssertionError(type + " objectClass missing from schema"));
  }

  private static String single(ConnectorObject object, String attributeName) {
    Attribute attribute = object.getAttributeByName(attributeName);
    if (attribute == null || attribute.getValue() == null || attribute.getValue().isEmpty()) {
      return null;
    }
    Object value = attribute.getValue().get(0);
    return value == null ? null : value.toString();
  }

  private static void assumeSubject() {
    org.junit.jupiter.api.Assumptions.assumeTrue(
        subject != null, "no subject user discovered in test020; run the full class in order");
  }
}
