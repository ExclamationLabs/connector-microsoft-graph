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
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.objects.*;
import org.junit.jupiter.api.*;

/**
 * Write-path verification of the 3.0.0 identifier change: creating a user must take {@code __NAME__}
 * as the {@code userPrincipalName} and {@code DISPLAY_NAME} as the friendly name.
 *
 * <p>This complements {@link MicrosoftGraphIdentifierReadOnlyIT}, which cannot cover {@code
 * MicrosoftGraphUsersAdapter.constructModel()} without writing to the tenant.
 *
 * <p><b>Scope of mutation.</b> This test creates exactly ONE user, with a UPN carrying the {@link
 * #THROWAWAY_TAG} marker, and deletes it in an {@link AfterAll} hook that runs even if assertions
 * fail. It never touches any pre-existing account: {@code test100} asserts the target UPN is unused
 * before creating, and the delete is guarded to the UID this test created and to UPNs bearing the
 * marker.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MicrosoftGraphIdentifierWriteIT
    extends ApiIntegrationTest<MicrosoftGraphConfiguration, MicrosoftGraphConnector> {

  /**
   * Marker embedded in the throwaway UPN. The cleanup hook refuses to delete anything whose UPN
   * lacks it, so a bug in this test cannot remove a real account.
   */
  private static final String THROWAWAY_TAG = "conn-write-it";

  /** UPN under the tenant-verified provisioniam.com domain, tagged as disposable. */
  private static final String TEST_UPN = THROWAWAY_TAG + "-dchoiniere@provisioniam.com";

  /**
   * UPN that test170 renames the account to. Keeps the {@link #THROWAWAY_TAG} marker so the cleanup
   * hook still recognises the account after the rename.
   */
  private static final String RENAMED_UPN = THROWAWAY_TAG + "-dchoiniere-renamed@provisioniam.com";

  private static final String TEST_DISPLAY_NAME = "Dan Choiniere (connector write IT)";
  private static final String TEST_GIVEN_NAME = "Dan";
  private static final String TEST_SURNAME = "Choiniere";
  private static final String TEST_MAIL_NICKNAME = THROWAWAY_TAG + "dchoiniere";

  private static final String VALID_PASSWORD_VALUE = "D8weoIru#4";

  // ---------------------------------------------------------------------------
  // Divergent-mail fixture (test160): login identity != mail address.
  //
  // Mimics a common real client shape — short login handle, friendly mail address — which also
  // arises after domain migrations and for B2B guests. This matters because the 3.0.0 change fixed
  // USER_PRINCIPAL_NAME's filter mapping from the Graph field "mail" to "userPrincipalName", and
  // that fix is INVISIBLE in a tenant where mail == UPN for everyone (which is true of all but a
  // handful of accounts here). Divergent values are what make the old and new mappings
  // distinguishable.
  // ---------------------------------------------------------------------------

  /** Login identity (userPrincipalName / __NAME__) — the short handle. */
  private static final String DIVERGENT_UPN = THROWAWAY_TAG + "-dchoiniere3@provisioniam.com";

  /** Mail address (Graph "mail" / EMAIL) — deliberately a DIFFERENT string from the UPN. */
  private static final String DIVERGENT_MAIL =
      THROWAWAY_TAG + "-dan.choiniere@provisioniam.com";

  private static final String DIVERGENT_MAIL_NICKNAME = THROWAWAY_TAG + "danchoiniere";

  /** UID of the account created by test110, used by later tests and by the cleanup hook. */
  private static String createdUid;

  /**
   * The account's current UPN. Starts as {@link #TEST_UPN} and is updated by test170, which renames
   * it — so the delete test must assert against this rather than the original constant.
   */
  private static String currentUpn = TEST_UPN;

  /** Set once the account is confirmed deleted, so the cleanup hook does not double-delete. */
  private static boolean deleted;

  /**
   * Facade captured during the run so the static {@link AfterAll} cleanup hook can reach it —
   * {@code getConnectorFacade()} is an instance method on the base class.
   */
  private static org.identityconnectors.framework.api.ConnectorFacade facadeForCleanup;

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
    config.getResultsHandlerConfiguration().setEnableFilteredResultsHandler(false);
    config.getResultsHandlerConfiguration().setEnableNormalizingResultsHandler(false);
    config.getResultsHandlerConfiguration().setEnableAttributesToGetSearchResultsHandler(false);
    config.getResultsHandlerConfiguration().setFilteredResultsHandlerInValidationMode(false);
    return config;
  }

  @BeforeEach
  public void setup() {
    super.setup();
    facadeForCleanup = getConnectorFacade();
  }

  // ---------------------------------------------------------------------------
  // Pre-flight: prove we are not about to disturb an existing account
  // ---------------------------------------------------------------------------

  @Test
  @Order(100)
  public void test100TargetUpnIsUnused() {
    // Guard against colliding with a real account (e.g. the operator's own dchoiniere@ mailbox).
    // A listing is used rather than a $filter because the connector's filtered read path trips a
    // pre-existing SharePoint/detailFields defect; see MicrosoftGraphIdentifierReadOnlyIT.test040.
    // One listing covers every UPN this class touches. Debris from a previously failed run would
    // otherwise make later tests fail for the wrong reason — e.g. test170's "old UPN no longer
    // resolves" assertion, or test160's create.
    results = new ArrayList<>();
    getConnectorFacade()
        .search(new ObjectClass("user"), null, handler, new OperationOptionsBuilder().build());
    Set<String> existingUpns = new HashSet<>();
    for (ConnectorObject user : results) {
      String upn = single(user, Name.NAME);
      if (upn != null) {
        existingUpns.add(upn.toLowerCase(Locale.ROOT));
      }
    }
    for (String upn :
        Arrays.asList(
            TEST_UPN,
            RENAMED_UPN,
            DIVERGENT_UPN,
            THROWAWAY_TAG + "-dchoiniere2@provisioniam.com")) {
      assertFalse(
          existingUpns.contains(upn.toLowerCase(Locale.ROOT)),
          "throwaway UPN " + upn + " already exists; remove it before running so no real account is touched");
    }
  }

  // ---------------------------------------------------------------------------
  // Create — the constructModel() path
  // ---------------------------------------------------------------------------

  @Test
  @Order(110)
  public void test110CreateUserWithNameAsUpn() {
    Set<Attribute> attributes = new HashSet<>();

    // The crux of the change: __NAME__ carries the UPN, DISPLAY_NAME carries the friendly name.
    // Under 2.0.x these two were the same attribute and this create would have set displayName
    // from __NAME__ and left userPrincipalName null.
    attributes.add(new AttributeBuilder().setName(Name.NAME).addValue(TEST_UPN).build());
    attributes.add(
        new AttributeBuilder().setName(DISPLAY_NAME.name()).addValue(TEST_DISPLAY_NAME).build());

    attributes.add(
        new AttributeBuilder().setName(GIVEN_NAME.name()).addValue(TEST_GIVEN_NAME).build());
    attributes.add(new AttributeBuilder().setName(SURNAME.name()).addValue(TEST_SURNAME).build());
    attributes.add(
        new AttributeBuilder().setName(EMAIL_NICKNAME.name()).addValue(TEST_MAIL_NICKNAME).build());
    // The adapter reads the password from the ConnId operational attribute __PASSWORD__, and the
    // enabled flag from __ENABLE__ — not from the connector-specific PASSWORD/ACCOUNT_ENABLED
    // enum names. Graph rejects a create with a passwordProfile that has no password.
    attributes.add(
        new AttributeBuilder()
            .setName(OperationalAttributes.PASSWORD_NAME)
            .addValue(new GuardedString(VALID_PASSWORD_VALUE.toCharArray()))
            .build());
    attributes.add(
        new AttributeBuilder().setName(OperationalAttributes.ENABLE_NAME).addValue(true).build());
    attributes.add(
        new AttributeBuilder().setName(FORCE_CHANGE_PASSWORD_NEXT_SIGN_IN.name()).addValue(true).build());
    attributes.add(
        new AttributeBuilder()
            .setName(FORCE_CHANGE_PASSWORD_NEXT_SIGN_IN_WITH_MFA.name())
            .addValue(false)
            .build());
    attributes.add(
        new AttributeBuilder().setName(PREFERRED_LANGUAGE.name()).addValue("en-US").build());
    attributes.add(new AttributeBuilder().setName(USAGE_LOCATION.name()).addValue("US").build());

    Uid newUid =
        getConnectorFacade()
            .create(new ObjectClass("user"), attributes, new OperationOptionsBuilder().build());

    assertNotNull(newUid, "create returned a null Uid");
    assertTrue(StringUtils.isNotBlank(newUid.getUidValue()), "create returned a blank Uid");
    createdUid = newUid.getUidValue();

    System.out.printf("[write IT] created uid=%s upn=%s%n", createdUid, TEST_UPN);
  }

  @Test
  @Order(120)
  public void test120CreatedUserHasUpnAsNameAndSeparateDisplayName() {
    assumeCreated();

    // Read the account back through the connector and confirm constructModel() sent both fields
    // to the right Graph properties. This is the assertion that would fail under 2.0.x: __NAME__
    // would come back as the displayName, and userPrincipalName would be null/absent.
    ConnectorObject created =
        getConnectorFacade()
            .getObject(
                new ObjectClass("user"), new Uid(createdUid), new OperationOptionsBuilder().build());
    assertNotNull(created, "could not read back the created user");

    assertEquals(
        TEST_UPN,
        single(created, Name.NAME),
        "__NAME__ must read back as the userPrincipalName that was supplied on create");
    assertEquals(
        TEST_DISPLAY_NAME,
        single(created, DISPLAY_NAME.name()),
        "DISPLAY_NAME must round-trip independently of __NAME__");

    // The two must be genuinely distinct, otherwise this test proves nothing.
    assertNotEquals(
        single(created, Name.NAME),
        single(created, DISPLAY_NAME.name()),
        "__NAME__ and DISPLAY_NAME collapsed to the same value");

    // Ordinary attributes must survive the create unharmed.
    assertEquals(TEST_GIVEN_NAME, single(created, GIVEN_NAME.name()));
    assertEquals(TEST_SURNAME, single(created, SURNAME.name()));
  }

  @Test
  @Order(130)
  public void test130DuplicateUpnRaisesAlreadyExists() {
    assumeCreated();

    // With __NAME__ bound to the tenant-unique UPN, a duplicate create must surface as
    // AlreadyExistsException. Under 2.0.x the uniqueness collision happened on the non-unique
    // displayName instead, which is what produced "Too many iterations" in midPoint.
    //
    // This test originally failed with a generic ConnectorException: MicrosoftGraphDriver only
    // translated the mailNickname collision signature ("property netId is invalid") into
    // AlreadyExistsException, so a userPrincipalName collision fell through untranslated. That
    // mattered specifically because of this change — UPN is now the naming attribute, so a UPN
    // collision is the case midPoint's uniqueness iterator has to recognize in order to retry.
    // handleGraphServiceException() now also maps the userPrincipalName signature.
    Set<Attribute> attributes = new HashSet<>();
    attributes.add(new AttributeBuilder().setName(Name.NAME).addValue(TEST_UPN).build());
    attributes.add(
        new AttributeBuilder()
            .setName(DISPLAY_NAME.name())
            .addValue(TEST_DISPLAY_NAME + " duplicate")
            .build());
    attributes.add(
        new AttributeBuilder().setName(GIVEN_NAME.name()).addValue(TEST_GIVEN_NAME).build());
    attributes.add(new AttributeBuilder().setName(SURNAME.name()).addValue(TEST_SURNAME).build());
    attributes.add(
        new AttributeBuilder()
            .setName(EMAIL_NICKNAME.name())
            .addValue(TEST_MAIL_NICKNAME + "dup")
            .build());
    attributes.add(
        new AttributeBuilder()
            .setName(OperationalAttributes.PASSWORD_NAME)
            .addValue(new GuardedString(VALID_PASSWORD_VALUE.toCharArray()))
            .build());
    attributes.add(
        new AttributeBuilder().setName(OperationalAttributes.ENABLE_NAME).addValue(true).build());
    attributes.add(
        new AttributeBuilder().setName(USAGE_LOCATION.name()).addValue("US").build());

    assertThrows(
        AlreadyExistsException.class,
        () ->
            getConnectorFacade()
                .create(new ObjectClass("user"), attributes, new OperationOptionsBuilder().build()),
        "duplicate UPN must raise AlreadyExistsException");
  }

  @Test
  @Order(140)
  public void test140DuplicateDisplayNameIsAllowed() {
    assumeCreated();

    // The point of the change: displayName is NOT an identifier, so reusing one must succeed.
    // This is the case that previously collided. The second account is created and immediately
    // removed so the tenant is left as it was found.
    final String duplicateUpn = THROWAWAY_TAG + "-dchoiniere2@provisioniam.com";
    String secondUid = null;
    try {
      Set<Attribute> attributes = new HashSet<>();
      // Same DISPLAY_NAME as the first account, different UPN.
      attributes.add(new AttributeBuilder().setName(Name.NAME).addValue(duplicateUpn).build());
      attributes.add(
          new AttributeBuilder().setName(DISPLAY_NAME.name()).addValue(TEST_DISPLAY_NAME).build());
      attributes.add(
          new AttributeBuilder().setName(GIVEN_NAME.name()).addValue(TEST_GIVEN_NAME).build());
      attributes.add(new AttributeBuilder().setName(SURNAME.name()).addValue(TEST_SURNAME).build());
      attributes.add(
          new AttributeBuilder()
              .setName(EMAIL_NICKNAME.name())
              .addValue(TEST_MAIL_NICKNAME + "2")
              .build());
      attributes.add(
          new AttributeBuilder()
              .setName(OperationalAttributes.PASSWORD_NAME)
              .addValue(new GuardedString(VALID_PASSWORD_VALUE.toCharArray()))
              .build());
      attributes.add(
          new AttributeBuilder().setName(OperationalAttributes.ENABLE_NAME).addValue(true).build());
      attributes.add(new AttributeBuilder().setName(USAGE_LOCATION.name()).addValue("US").build());

      Uid secondUidValue =
          getConnectorFacade()
              .create(new ObjectClass("user"), attributes, new OperationOptionsBuilder().build());
      assertNotNull(secondUidValue, "creating a second user with a duplicate displayName failed");
      secondUid = secondUidValue.getUidValue();

      ConnectorObject second =
          getConnectorFacade()
              .getObject(
                  new ObjectClass("user"),
                  new Uid(secondUid),
                  new OperationOptionsBuilder().build());
      assertEquals(duplicateUpn, single(second, Name.NAME), "second account has the wrong __NAME__");
      assertEquals(
          TEST_DISPLAY_NAME,
          single(second, DISPLAY_NAME.name()),
          "second account should share the first account's displayName");
      assertNotEquals(createdUid, secondUid, "expected two distinct accounts");
    } finally {
      if (secondUid != null) {
        // Assert the removal actually happened — a swallowed cleanup failure previously leaked this
        // account into the tenant.
        assertTrue(
            deleteThrowaway(secondUid),
            "failed to clean up the duplicate-displayName account " + secondUid);
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Update — DISPLAY_NAME is now an ordinary, updateable attribute
  // ---------------------------------------------------------------------------

  @Test
  @Order(150)
  public void test150UpdateDisplayNameLeavesUpnUnchanged() {
    assumeCreated();

    final String updatedDisplayName = TEST_DISPLAY_NAME + " renamed";
    Set<AttributeDelta> deltas = new HashSet<>();
    deltas.add(
        new AttributeDeltaBuilder()
            .setName(DISPLAY_NAME.name())
            .addValueToReplace(updatedDisplayName)
            .build());

    Set<AttributeDelta> response =
        getConnectorFacade()
            .updateDelta(
                new ObjectClass("user"),
                new Uid(createdUid),
                deltas,
                new OperationOptionsBuilder().build());
    assertNotNull(response);
    assertTrue(response.isEmpty(), "updateDelta reported unexpected side effects: " + response);

    // Entra serves reads from replicas that lag writes by seconds, so poll rather than trusting a
    // single immediate read.
    ConnectorObject updated =
        awaitUser(
            createdUid,
            u -> updatedDisplayName.equals(single(u, DISPLAY_NAME.name())),
            "DISPLAY_NAME update to '" + updatedDisplayName + "' to become visible");
    assertEquals(
        updatedDisplayName,
        single(updated, DISPLAY_NAME.name()),
        "DISPLAY_NAME update did not take effect");
    assertEquals(
        TEST_UPN,
        single(updated, Name.NAME),
        "renaming DISPLAY_NAME must not disturb __NAME__/userPrincipalName");
  }

  // ---------------------------------------------------------------------------
  // Renaming __NAME__ itself — the path midPoint's uniqueness iterator drives
  // ---------------------------------------------------------------------------

  /**
   * Renames {@code __NAME__} on an existing account, i.e. changes its {@code userPrincipalName}.
   *
   * <p>This is the update path the 3.0.0 change exists to enable: when midPoint hits a naming
   * collision it retries with a modified name, which now means issuing a UPN change rather than a
   * displayName change. {@code constructModel()} is shared between create and update and sources
   * {@code userPrincipalName} from {@code __NAME__} unconditionally, so a {@code __NAME__} delta has
   * to reach Graph as a UPN patch.
   *
   * <p>Also checks the fields a UPN rename can plausibly disturb in Entra — {@code mailNickname} and
   * {@code displayName} — and confirms the account is still addressable by its stable {@code __UID__}
   * afterwards, which is what keeps midPoint shadows from orphaning.
   */
  @Test
  @Order(170)
  public void test170RenameUpnViaNameAttribute() {
    assumeCreated();
    requireFilteredReadsWork();

    // Capture pre-rename state so the assertions can prove what did and did not move.
    ConnectorObject before =
        getConnectorFacade()
            .getObject(
                new ObjectClass("user"), new Uid(createdUid), new OperationOptionsBuilder().build());
    assertNotNull(before, "could not read the account before renaming");
    final String displayNameBefore = single(before, DISPLAY_NAME.name());
    final String mailNicknameBefore = single(before, EMAIL_NICKNAME.name());
    assertEquals(TEST_UPN, single(before, Name.NAME), "unexpected UPN before rename");

    final String renamedUpn = RENAMED_UPN;

    Set<AttributeDelta> deltas = new HashSet<>();
    deltas.add(
        new AttributeDeltaBuilder().setName(Name.NAME).addValueToReplace(renamedUpn).build());

    Set<AttributeDelta> response =
        getConnectorFacade()
            .updateDelta(
                new ObjectClass("user"),
                new Uid(createdUid),
                deltas,
                new OperationOptionsBuilder().build());
    assertNotNull(response);
    assertTrue(response.isEmpty(), "updateDelta reported unexpected side effects: " + response);

    // The rename must be visible on read, addressed by the UNCHANGED uid.
    ConnectorObject after =
        awaitUser(
            createdUid,
            u -> renamedUpn.equalsIgnoreCase(single(u, Name.NAME)),
            "UPN rename to '" + renamedUpn + "' to become visible");
    assertEquals(
        renamedUpn, single(after, Name.NAME), "__NAME__ did not change to the new userPrincipalName");
    assertEquals(
        createdUid,
        single(after, Uid.NAME),
        "__UID__ must be stable across a rename — midPoint shadows depend on it");

    // A UPN rename must not silently drag other fields with it.
    assertEquals(
        displayNameBefore,
        single(after, DISPLAY_NAME.name()),
        "renaming the UPN must not change DISPLAY_NAME");
    assertEquals(
        mailNicknameBefore,
        single(after, EMAIL_NICKNAME.name()),
        "renaming the UPN must not change EMAIL_NICKNAME/mailNickname");

    // The account must be findable by its NEW name and no longer by the old one — this is what
    // midPoint relies on after an iterator retry.
    List<ConnectorObject> byNewName =
        awaitSearch(Name.NAME, renamedUpn, 1, "the renamed UPN to become searchable");
    assertEquals(createdUid, single(byNewName.get(0), Uid.NAME));
    assertTrue(
        searchUsersBy(Name.NAME, TEST_UPN).isEmpty(),
        "the old UPN '" + TEST_UPN + "' still resolves after the rename");

    // Point the shared state at the new UPN so the delete test and cleanup hook stay correct.
    currentUpn = renamedUpn;
  }

  // ---------------------------------------------------------------------------
  // Divergent mail vs UPN — closes the one gap the tenant's data could not cover
  // ---------------------------------------------------------------------------

  /**
   * Verifies the {@code USER_PRINCIPAL_NAME} filter mapping fix using an account whose {@code mail}
   * differs from its {@code userPrincipalName}.
   *
   * <p>3.0.0 changed {@code USER_PRINCIPAL_NAME}'s Graph field from {@code "mail"} to {@code
   * "userPrincipalName"}. In this tenant almost every account has {@code mail == userPrincipalName},
   * so both the old (buggy) and new (correct) mappings return the same row and the fix cannot be
   * observed — which is exactly why the bug survived from 2023. This test manufactures the
   * divergence so the two mappings produce different answers.
   *
   * <p>Creates one extra throwaway account and removes it in a {@code finally} block.
   */
  @Test
  @Order(160)
  public void test160UpnFilterIsNotMailWhenTheyDiffer() {
    // Sanity: the fixture must actually be divergent, or this test proves nothing.
    assertNotEquals(
        DIVERGENT_UPN, DIVERGENT_MAIL, "fixture is not divergent; test would be vacuous");

    String divergentUid = null;
    try {
      Set<Attribute> attributes = new HashSet<>();
      // __NAME__ is the login identity; EMAIL is the (different) mail address.
      attributes.add(new AttributeBuilder().setName(Name.NAME).addValue(DIVERGENT_UPN).build());
      attributes.add(new AttributeBuilder().setName(EMAIL.name()).addValue(DIVERGENT_MAIL).build());
      attributes.add(
          new AttributeBuilder()
              .setName(DISPLAY_NAME.name())
              .addValue(TEST_DISPLAY_NAME + " divergent-mail")
              .build());
      attributes.add(
          new AttributeBuilder().setName(GIVEN_NAME.name()).addValue(TEST_GIVEN_NAME).build());
      attributes.add(new AttributeBuilder().setName(SURNAME.name()).addValue(TEST_SURNAME).build());
      attributes.add(
          new AttributeBuilder()
              .setName(EMAIL_NICKNAME.name())
              .addValue(DIVERGENT_MAIL_NICKNAME)
              .build());
      attributes.add(
          new AttributeBuilder()
              .setName(OperationalAttributes.PASSWORD_NAME)
              .addValue(new GuardedString(VALID_PASSWORD_VALUE.toCharArray()))
              .build());
      attributes.add(
          new AttributeBuilder().setName(OperationalAttributes.ENABLE_NAME).addValue(true).build());
      attributes.add(new AttributeBuilder().setName(USAGE_LOCATION.name()).addValue("US").build());

      Uid newUid =
          getConnectorFacade()
              .create(new ObjectClass("user"), attributes, new OperationOptionsBuilder().build());
      assertNotNull(newUid, "create of the divergent-mail account returned null");
      divergentUid = newUid.getUidValue();

      // Confirm the two fields really landed differently on the resource. If Graph had derived
      // mail from mailNickname and overwritten our value, the assertions below would be vacuous.
      ConnectorObject created =
          awaitUser(
              divergentUid,
              u -> DIVERGENT_MAIL.equalsIgnoreCase(single(u, EMAIL.name())),
              "the divergent mail value to become visible");
      assertEquals(
          DIVERGENT_UPN,
          single(created, Name.NAME),
          "__NAME__ must be the userPrincipalName, not the mail address");
      assertEquals(
          DIVERGENT_MAIL,
          single(created, EMAIL.name()),
          "EMAIL must round-trip as the distinct mail address");
      assertNotEquals(
          single(created, Name.NAME),
          single(created, EMAIL.name()),
          "fixture collapsed: mail and UPN ended up identical on the resource");

      // The actual regression check. Under the old mapping USER_PRINCIPAL_NAME queried Graph's
      // "mail" field, so searching for the UPN would have found nothing (the mail value differs).
      // Under the fix it queries "userPrincipalName" and finds exactly this account.
      requireFilteredReadsWork();

      // Filter indexes lag a create by longer than the by-id read does — a filter on a
      // just-created account returns 0 immediately and 1 after roughly ten seconds — so poll on
      // the filter itself rather than assuming awaitUser() above implies filter visibility.
      List<ConnectorObject> byUpn =
          awaitSearch(
              USER_PRINCIPAL_NAME.name(),
              DIVERGENT_UPN,
              1,
              "USER_PRINCIPAL_NAME filter to find the new account by its UPN");
      assertEquals(
          1,
          byUpn.size(),
          "USER_PRINCIPAL_NAME filter on the UPN found "
              + byUpn.size()
              + " results; the old mapping (to Graph 'mail') would find 0");
      assertEquals(divergentUid, single(byUpn.get(0), Uid.NAME));

      // EMAIL must still resolve via the mail field, so the fix did not break email search. Do
      // this before the negative assertion below: once the mail value is searchable, the account is
      // fully indexed, so a subsequent empty result is meaningful rather than just lag.
      List<ConnectorObject> byMail =
          awaitSearch(
              EMAIL.name(), DIVERGENT_MAIL, 1, "EMAIL filter to find the new account by its mail");
      assertEquals(1, byMail.size(), "EMAIL filter on the mail address did not return 1");
      assertEquals(divergentUid, single(byMail.get(0), Uid.NAME));

      // Conversely, the UPN filter must NOT match the mail address — that would mean it is still
      // querying the mail field. Meaningful only now that the account is known to be indexed.
      assertTrue(
          searchUsersBy(USER_PRINCIPAL_NAME.name(), DIVERGENT_MAIL).isEmpty(),
          "USER_PRINCIPAL_NAME filter matched the mail address '"
              + DIVERGENT_MAIL
              + "' — it is still querying Graph's 'mail' field");
    } finally {
      if (divergentUid != null) {
        assertTrue(
            deleteThrowaway(divergentUid),
            "failed to clean up the divergent-mail account " + divergentUid);
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Delete
  // ---------------------------------------------------------------------------

  @Test
  @Order(190)
  public void test190DeleteCreatedUser() {
    assumeCreated();

    getConnectorFacade()
        .delete(
            new ObjectClass("user"), new Uid(createdUid), new OperationOptionsBuilder().build());
    deleted = true;

    // Confirm it is gone, so the tenant is verifiably back to its prior state. Deletes propagate
    // to read replicas with the same lag as any other write, so poll instead of asserting once.
    // Check currentUpn, not TEST_UPN: test170 renames the account, and the old UPN would be absent
    // regardless of whether the delete worked.
    boolean gone = false;
    for (int attempt = 0; attempt < REPLICATION_ATTEMPTS && !gone; attempt++) {
      if (attempt > 0) {
        pause();
      }
      gone = findByUpnInListing(currentUpn).isEmpty();
    }
    assertTrue(gone, "user still present in the tenant after delete and replication wait");
  }

  /**
   * Safety net: removes the throwaway account even if an assertion above failed before test190 ran,
   * so a failed run cannot leave debris in the tenant.
   */
  @AfterAll
  public static void cleanup() {
    if (createdUid == null || deleted) {
      return;
    }
    System.out.printf("[write IT] cleanup: removing leftover uid=%s%n", createdUid);
    deleteThrowaway(createdUid);
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /**
   * Deletes a user created by this test, refusing to act unless the account's UPN carries {@link
   * #THROWAWAY_TAG}. This makes it impossible for a wrong UID to remove a real account.
   */
  private static boolean deleteThrowaway(String uid) {
    if (facadeForCleanup == null) {
      System.err.printf("[write IT] no facade available to clean up uid=%s%n", uid);
      return false;
    }

    // A just-created account may not be visible yet on the replica that serves the DELETE, which
    // returns Request_ResourceNotFound (404). Retry rather than leaking the account into the
    // tenant. Verified by observation: an immediate delete after create fails this way.
    RuntimeException lastFailure = null;
    for (int attempt = 0; attempt < REPLICATION_ATTEMPTS; attempt++) {
      if (attempt > 0) {
        pause();
      }
      try {
        ConnectorObject target =
            facadeForCleanup.getObject(
                new ObjectClass("user"), new Uid(uid), new OperationOptionsBuilder().build());
        if (target == null) {
          // Not visible yet, or already gone. Keep trying until we can positively confirm.
          continue;
        }

        // Refuse to delete anything that is not one of this test's throwaway accounts, so a wrong
        // UID can never remove a real account.
        String upn = single(target, Name.NAME);
        if (upn == null || !upn.contains(THROWAWAY_TAG)) {
          System.err.printf(
              "[write IT] REFUSING to delete uid=%s (upn=%s): missing '%s' marker%n",
              uid, upn, THROWAWAY_TAG);
          return false;
        }

        facadeForCleanup.delete(
            new ObjectClass("user"), new Uid(uid), new OperationOptionsBuilder().build());
        return true;
      } catch (RuntimeException e) {
        lastFailure = e;
      }
    }

    System.err.printf(
        "[write IT] FAILED to clean up uid=%s after %d attempts — MANUAL REMOVAL REQUIRED: %s%n",
        uid, REPLICATION_ATTEMPTS, lastFailure == null ? "never became visible" : lastFailure.getMessage());
    return false;
  }

  /**
   * Finds a user by UPN by scanning an unfiltered listing. Used instead of an EqualsFilter search
   * because the connector's filtered read path selects detailFields, which includes SharePoint
   * profile fields that Graph rejects alongside any $filter.
   */
  private List<ConnectorObject> findByUpnInListing(String upn) {
    results = new ArrayList<>();
    getConnectorFacade()
        .search(new ObjectClass("user"), null, handler, new OperationOptionsBuilder().build());
    List<ConnectorObject> matches = new ArrayList<>();
    for (ConnectorObject user : results) {
      if (upn.equalsIgnoreCase(single(user, Name.NAME))) {
        matches.add(user);
      }
    }
    return matches;
  }

  private List<ConnectorObject> searchUsersBy(String attributeName, String value) {
    results = new ArrayList<>();
    Attribute filter = new AttributeBuilder().setName(attributeName).addValue(value).build();
    getConnectorFacade()
        .search(
            new ObjectClass("user"),
            new org.identityconnectors.framework.common.objects.filter.EqualsFilter(filter),
            handler,
            new OperationOptionsBuilder().build());
    return new ArrayList<>(results);
  }

  /**
   * True when the connector's filtered read path is usable against this tenant. It is broken by a
   * pre-existing defect unrelated to the identifier change: {@code detailFields} includes the
   * SharePoint-backed profile fields {@code skills} and {@code responsibilities}, which Graph
   * refuses to combine with any {@code $filter} on {@code /users}.
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
   * Fails the test if filtered reads are broken. See {@code
   * MicrosoftGraphIdentifierReadOnlyIT.test049} — a hard assertion, so a regression in the
   * {@code filterableDetailFields} fix fails loudly instead of silently skipping coverage.
   */
  private void requireFilteredReadsWork() {
    assertTrue(
        filteredReadsWork(),
        "connector filtered reads are broken — check that MicrosoftGraphUsersInvocator still uses"
            + " filterableDetailFields (not detailFields) for filtered searches");
  }

  /** Number of read attempts allowed while waiting for an Entra write to reach a read replica. */
  private static final int REPLICATION_ATTEMPTS = 10;

  /** Delay between replication polls. */
  private static final long REPLICATION_PAUSE_MILLIS = 3_000L;

  private static void pause() {
    try {
      Thread.sleep(REPLICATION_PAUSE_MILLIS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("interrupted while waiting for Entra replication", e);
    }
  }

  /**
   * Re-reads a user until {@code condition} holds, absorbing Entra's write-to-read replication lag.
   * Returns the first object that satisfied the condition, or fails the test on timeout.
   */
  private ConnectorObject awaitUser(
      String uid, java.util.function.Predicate<ConnectorObject> condition, String description) {
    ConnectorObject latest = null;
    for (int attempt = 0; attempt < REPLICATION_ATTEMPTS; attempt++) {
      if (attempt > 0) {
        pause();
      }
      latest =
          getConnectorFacade()
              .getObject(new ObjectClass("user"), new Uid(uid), new OperationOptionsBuilder().build());
      if (latest != null && condition.test(latest)) {
        return latest;
      }
    }
    fail(
        "timed out after "
            + (REPLICATION_ATTEMPTS * REPLICATION_PAUSE_MILLIS / 1000)
            + "s waiting for "
            + description);
    return latest;
  }

  /**
   * Repeats a filtered search until it returns {@code expectedCount} results, absorbing the lag
   * between a write and the filter index that serves searches. That lag is longer than the by-id
   * read lag: a filter on a just-created account returns 0 immediately and 1 after roughly ten
   * seconds, so {@link #awaitUser} succeeding does not imply the account is searchable yet.
   */
  private List<ConnectorObject> awaitSearch(
      String attributeName, String value, int expectedCount, String description) {
    List<ConnectorObject> latest = Collections.emptyList();
    for (int attempt = 0; attempt < REPLICATION_ATTEMPTS; attempt++) {
      if (attempt > 0) {
        pause();
      }
      latest = searchUsersBy(attributeName, value);
      if (latest.size() == expectedCount) {
        return latest;
      }
    }
    fail(
        "timed out after "
            + (REPLICATION_ATTEMPTS * REPLICATION_PAUSE_MILLIS / 1000)
            + "s waiting for "
            + description
            + " (last count: "
            + latest.size()
            + ", expected "
            + expectedCount
            + ")");
    return latest;
  }

  private static String single(ConnectorObject object, String attributeName) {
    Attribute attribute = object.getAttributeByName(attributeName);
    if (attribute == null || attribute.getValue() == null || attribute.getValue().isEmpty()) {
      return null;
    }
    Object value = attribute.getValue().get(0);
    return value == null ? null : value.toString();
  }

  private static void assumeCreated() {
    org.junit.jupiter.api.Assumptions.assumeTrue(
        createdUid != null, "no user was created by test110; run the full class in order");
  }
}
