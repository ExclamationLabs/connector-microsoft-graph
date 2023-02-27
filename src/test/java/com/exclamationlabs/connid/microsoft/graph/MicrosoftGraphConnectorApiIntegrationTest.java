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
import com.exclamationlabs.connid.microsoft.graph.attribute.MicrosoftGraphGroupAttribute;
import com.exclamationlabs.connid.microsoft.graph.attribute.MicrosoftGraphUserAttribute;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MicrosoftGraphConnectorApiIntegrationTest
    extends ApiIntegrationTest<MicrosoftGraphConfiguration, MicrosoftGraphConnector> {

  private static final String LOCATION_SET = "KDS Location";

  private static final String VALID_PASSWORD_VALUE = "D8weoIru#4";

  private static final String KNOWN_USER_ID = "redacted";
  private static final String KNOWN_USERNAME = "redacted";
  private static final String KNOWN_GIVEN_NAME = "redacted";
  private static final String KNOWN_SURNAME = "redacted";
  private static final String KNOWN_USERTYPE = "redacted";

  private static final String KNOWN_EMAIL = "redacted";

  private static final String KNOWN_GROUP_ID = "redacted";
  private static final String KNOWN_GROUP_NAME = "redacted";
  private static final String KNOWN_GROUP_EMAIL = "redacted";

  private static final String KNOWN_LICENSE_ID = "redacted";

  private static final String KNOWN_LICENSE_DETAIL_ID = "redacted";

  private static String generatedUserId;
  private static String generatedGroupId;

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

  @BeforeEach
  public void setup() {
    super.setup();
  }

  @Test
  @Order(100)
  public void test100Test() {
    getConnectorFacade().test();
  }

  @Test
  @Order(110)
  public void test110UserCreateBareMinimumAttributes() {
    final String TEST_EMAIL = "budcoke@exclamationlabs.com";
    Set<Attribute> attributes = new HashSet<>();

    attributes.add(new AttributeBuilder().setName(GIVEN_NAME.name()).addValue("Bud").build());
    attributes.add(new AttributeBuilder().setName(SURNAME.name()).addValue("Coke").build());
    attributes.add(
        new AttributeBuilder().setName(DISPLAY_NAME.name()).addValue("Bud Coke").build());
    attributes.add(new AttributeBuilder().setName(EMAIL.name()).addValue(TEST_EMAIL).build());
    attributes.add(
        new AttributeBuilder().setName(EMAIL_NICKNAME.name()).addValue("BudCoke3").build());
    attributes.add(
        new AttributeBuilder().setName(OFFICE_LOCATION.name()).addValue("Old Location").build());
    attributes.add(
        new AttributeBuilder()
            .setName(PASSWORD.name())
            .addValue(new GuardedString(VALID_PASSWORD_VALUE.toCharArray()))
            .build());
    attributes.add(
        new AttributeBuilder()
            .setName(FORCE_CHANGE_PASSWORD_NEXT_SIGN_IN.name())
            .addValue(true)
            .build());
    attributes.add(
        new AttributeBuilder()
            .setName(FORCE_CHANGE_PASSWORD_NEXT_SIGN_IN_WITH_MFA.name())
            .addValue(false)
            .build());
    attributes.add(
        new AttributeBuilder().setName(PREFERRED_LANGUAGE.name()).addValue("en-US").build());
    attributes.add(
        new AttributeBuilder().setName(USER_PRINCIPAL_NAME.name()).addValue(TEST_EMAIL).build());
    // Usage location needed for license assignment
    attributes.add(new AttributeBuilder().setName(USAGE_LOCATION.name()).addValue("US").build());
    Uid newId =
        getConnectorFacade()
            .create(new ObjectClass("user"), attributes, new OperationOptionsBuilder().build());
    assertNotNull(newId);
    assertNotNull(newId.getUidValue());
    generatedUserId = newId.getUidValue();
  }

  @Test
  @Order(111)
  public void test111UserCreateAlreadyExists() {
    final String TEST_EMAIL = "sidcoke@exclamationlabs.com";
    Set<Attribute> attributes = new HashSet<>();

    attributes.add(new AttributeBuilder().setName(GIVEN_NAME.name()).addValue("Sid").build());
    attributes.add(new AttributeBuilder().setName(SURNAME.name()).addValue("Coke").build());
    attributes.add(
        new AttributeBuilder().setName(DISPLAY_NAME.name()).addValue("Sid Coke").build());
    attributes.add(new AttributeBuilder().setName(EMAIL.name()).addValue(TEST_EMAIL).build());
    attributes.add(
        new AttributeBuilder().setName(EMAIL_NICKNAME.name()).addValue("SidCoke3").build());
    attributes.add(
        new AttributeBuilder().setName(OFFICE_LOCATION.name()).addValue("Old Location").build());
    attributes.add(
        new AttributeBuilder()
            .setName(PASSWORD.name())
            .addValue(new GuardedString(VALID_PASSWORD_VALUE.toCharArray()))
            .build());
    attributes.add(
        new AttributeBuilder()
            .setName(FORCE_CHANGE_PASSWORD_NEXT_SIGN_IN.name())
            .addValue(true)
            .build());
    attributes.add(
        new AttributeBuilder()
            .setName(FORCE_CHANGE_PASSWORD_NEXT_SIGN_IN_WITH_MFA.name())
            .addValue(false)
            .build());
    attributes.add(
        new AttributeBuilder().setName(PREFERRED_LANGUAGE.name()).addValue("en-US").build());
    attributes.add(
        new AttributeBuilder().setName(USER_PRINCIPAL_NAME.name()).addValue(TEST_EMAIL).build());
    // Usage location needed for license assignment
    attributes.add(new AttributeBuilder().setName(USAGE_LOCATION.name()).addValue("US").build());

    assertThrows(
        AlreadyExistsException.class,
        () ->
            getConnectorFacade()
                .create(
                    new ObjectClass("user"), attributes, new OperationOptionsBuilder().build()));
  }

  @Test
  @Order(112)
  public void test112UserCreateWithAssociationsAndCheck() {
    final String TEST_EMAIL = "komaha@exclamationlabs.com";
    Set<Attribute> attributes = new HashSet<>();
    attributes.add(
        new AttributeBuilder()
            .setName(ASSIGNED_LICENSES.name())
            .addValue(KNOWN_LICENSE_ID)
            .build());
    attributes.add(
        new AttributeBuilder().setName(ASSIGNED_GROUPS.name()).addValue(KNOWN_GROUP_ID).build());

    attributes.add(new AttributeBuilder().setName(GIVEN_NAME.name()).addValue("Kevin").build());
    attributes.add(new AttributeBuilder().setName(SURNAME.name()).addValue("Omaha").build());
    attributes.add(
        new AttributeBuilder().setName(DISPLAY_NAME.name()).addValue("Kevin Omaha").build());
    attributes.add(new AttributeBuilder().setName(EMAIL.name()).addValue(TEST_EMAIL).build());
    attributes.add(
        new AttributeBuilder().setName(EMAIL_NICKNAME.name()).addValue("JOmaha").build());
    attributes.add(
        new AttributeBuilder().setName(OFFICE_LOCATION.name()).addValue("Old Location").build());
    // Usage location needed for license assignment
    attributes.add(new AttributeBuilder().setName(USAGE_LOCATION.name()).addValue("US").build());
    attributes.add(
        new AttributeBuilder()
            .setName(PASSWORD.name())
            .addValue(new GuardedString(VALID_PASSWORD_VALUE.toCharArray()))
            .build());
    attributes.add(
        new AttributeBuilder()
            .setName(FORCE_CHANGE_PASSWORD_NEXT_SIGN_IN.name())
            .addValue(true)
            .build());
    attributes.add(
        new AttributeBuilder()
            .setName(FORCE_CHANGE_PASSWORD_NEXT_SIGN_IN_WITH_MFA.name())
            .addValue(false)
            .build());
    attributes.add(
        new AttributeBuilder().setName(PREFERRED_LANGUAGE.name()).addValue("en-US").build());
    attributes.add(
        new AttributeBuilder().setName(USER_PRINCIPAL_NAME.name()).addValue(TEST_EMAIL).build());

    Uid newId =
        getConnectorFacade()
            .create(new ObjectClass("user"), attributes, new OperationOptionsBuilder().build());
    assertNotNull(newId);
    assertNotNull(newId.getUidValue());
    final String generatedUserId2 = newId.getUidValue();

    Attribute idAttribute =
        new AttributeBuilder().setName(Uid.NAME).addValue(generatedUserId2).build();

    results = new ArrayList<>();
    getConnectorFacade()
        .search(
            new ObjectClass("user"),
            new EqualsFilter(idAttribute),
            handler,
            new OperationOptionsBuilder().build());
    assertEquals(1, results.size());
    assertEquals(1, results.get(0).getAttributeByName(ASSIGNED_GROUPS.name()).getValue().size());
    assertEquals(1, results.get(0).getAttributeByName(ASSIGNED_LICENSES.name()).getValue().size());
    assertEquals(
        KNOWN_GROUP_ID,
        results.get(0).getAttributeByName(ASSIGNED_GROUPS.name()).getValue().get(0).toString());
    assertEquals(
        KNOWN_LICENSE_ID,
        results.get(0).getAttributeByName(ASSIGNED_LICENSES.name()).getValue().get(0).toString());

    getConnectorFacade()
        .delete(
            new ObjectClass("user"),
            new Uid(generatedUserId2),
            new OperationOptionsBuilder().build());
  }

  @Test
  @Order(120)
  public void test120UserCreateMissingRequiredElements() {

    Set<Attribute> attributes = new HashSet<>();
    attributes.add(new AttributeBuilder().setName(GIVEN_NAME.name()).addValue("Kevin").build());
    attributes.add(new AttributeBuilder().setName(SURNAME.name()).addValue("Durant").build());
    attributes.add(
        new AttributeBuilder().setName(DISPLAY_NAME.name()).addValue("Kevin Durant").build());

    assertThrows(
        ConnectorException.class,
        () ->
            getConnectorFacade()
                .create(
                    new ObjectClass("user"), attributes, new OperationOptionsBuilder().build()));
  }

  @Test
  @Order(120)
  public void test120UserModify() {
    Set<AttributeDelta> attributes = new HashSet<>();
    attributes.add(
        new AttributeDeltaBuilder()
            .setName(OFFICE_LOCATION.name())
            .addValueToReplace(LOCATION_SET)
            .build());

    Set<AttributeDelta> response =
        getConnectorFacade()
            .updateDelta(
                new ObjectClass("user"),
                new Uid(generatedUserId),
                attributes,
                new OperationOptionsBuilder().build());
    assertNotNull(response);
    assertTrue(response.isEmpty());
  }

  @Test
  @Order(130)
  public void test130UsersGet() {
    results = new ArrayList<>();
    getConnectorFacade()
        .search(new ObjectClass("user"), null, handler, new OperationOptionsBuilder().build());
    assertTrue(results.size() > 1);
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Uid.NAME).getValue().get(0).toString()));
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Name.NAME).getValue().get(0).toString()));
  }

  @Test
  @Order(132)
  public void test132UsersGetNameFiler() {
    results = new ArrayList<>();
    Attribute filterNameAttribute =
        new AttributeBuilder().setName(Name.NAME).addValue(KNOWN_USERNAME).build();
    getConnectorFacade()
        .search(
            new ObjectClass("user"),
            new EqualsFilter(filterNameAttribute),
            handler,
            new OperationOptionsBuilder().build());
    assertEquals(1, results.size());
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Uid.NAME).getValue().get(0).toString()));
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Name.NAME).getValue().get(0).toString()));
  }

  @Test
  @Order(133)
  public void test133UsersGetEmailFiler() {
    results = new ArrayList<>();
    Attribute filterNameAttribute =
        new AttributeBuilder()
            .setName(MicrosoftGraphUserAttribute.EMAIL.name())
            .addValue(KNOWN_EMAIL)
            .build();
    getConnectorFacade()
        .search(
            new ObjectClass("user"),
            new EqualsFilter(filterNameAttribute),
            handler,
            new OperationOptionsBuilder().build());
    assertEquals(1, results.size());
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Uid.NAME).getValue().get(0).toString()));
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Name.NAME).getValue().get(0).toString()));
  }

  @Test
  @Order(134)
  public void test134UsersGetGivenNameFiler() {
    results = new ArrayList<>();
    Attribute filterNameAttribute =
        new AttributeBuilder().setName(GIVEN_NAME.name()).addValue(KNOWN_GIVEN_NAME).build();
    getConnectorFacade()
        .search(
            new ObjectClass("user"),
            new EqualsFilter(filterNameAttribute),
            handler,
            new OperationOptionsBuilder().build());
    assertEquals(1, results.size());
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Uid.NAME).getValue().get(0).toString()));
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Name.NAME).getValue().get(0).toString()));
  }

  @Test
  @Order(135)
  public void test135UsersGetSurnameFiler() {
    results = new ArrayList<>();
    Attribute filterNameAttribute =
        new AttributeBuilder().setName(SURNAME.name()).addValue(KNOWN_SURNAME).build();
    getConnectorFacade()
        .search(
            new ObjectClass("user"),
            new EqualsFilter(filterNameAttribute),
            handler,
            new OperationOptionsBuilder().build());
    assertEquals(1, results.size());
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Uid.NAME).getValue().get(0).toString()));
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Name.NAME).getValue().get(0).toString()));
  }

  @Test
  @Order(136)
  public void test136UsersGetUserTypeFiler() {
    results = new ArrayList<>();
    Attribute filterNameAttribute =
        new AttributeBuilder().setName(USER_TYPE.name()).addValue(KNOWN_USERTYPE).build();
    getConnectorFacade()
        .search(
            new ObjectClass("user"),
            new EqualsFilter(filterNameAttribute),
            handler,
            new OperationOptionsBuilder().build());
    assertTrue(results.size() > 30);
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Uid.NAME).getValue().get(0).toString()));
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Name.NAME).getValue().get(0).toString()));
  }

  @Test
  @Order(140)
  public void test140UserGet() {
    Attribute idAttribute =
        new AttributeBuilder().setName(Uid.NAME).addValue(KNOWN_USER_ID).build();

    results = new ArrayList<>();
    getConnectorFacade()
        .search(
            new ObjectClass("user"),
            new EqualsFilter(idAttribute),
            handler,
            new OperationOptionsBuilder().build());
    assertEquals(1, results.size());
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Uid.NAME).getValue().get(0).toString()));
  }

  @Test
  @Order(145)
  public void test140UserGetCreated() {
    Attribute idAttribute =
        new AttributeBuilder().setName(Uid.NAME).addValue(generatedUserId).build();
    results = new ArrayList<>();
    getConnectorFacade()
        .search(
            new ObjectClass("user"),
            new EqualsFilter(idAttribute),
            handler,
            new OperationOptionsBuilder().build());
    assertEquals(1, results.size());
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Uid.NAME).getValue().get(0).toString()));
  }

  @Test
  @Order(150)
  public void test150UserGetNotFound() {
    Attribute idAttribute =
        new AttributeBuilder().setName(Uid.NAME).addValue(KNOWN_USER_ID + "X").build();

    results = new ArrayList<>();
    getConnectorFacade()
        .search(
            new ObjectClass("user"),
            new EqualsFilter(idAttribute),
            handler,
            new OperationOptionsBuilder().build());
    assertEquals(0, results.size());
  }

  @Test
  @Order(210)
  public void test210GroupCreate() {
    Set<Attribute> attributes = new HashSet<>();
    attributes.add(
        new AttributeBuilder()
            .setName(MicrosoftGraphGroupAttribute.DISPLAY_NAME.name())
            .addValue("Flinstones2")
            .build());
    attributes.add(
        new AttributeBuilder()
            .setName(MicrosoftGraphGroupAttribute.EMAIL_ENABLED.name())
            .addValue(false)
            .build());
    attributes.add(
        new AttributeBuilder()
            .setName(MicrosoftGraphGroupAttribute.EMAIL_NICKNAME.name())
            .addValue("FlinstonesMailGroup2")
            .build());
    attributes.add(
        new AttributeBuilder()
            .setName(MicrosoftGraphGroupAttribute.SECURITY_ENABLED.name())
            .addValue(true)
            .build());

    generatedGroupId =
        getConnectorFacade()
            .create(new ObjectClass("group"), attributes, new OperationOptionsBuilder().build())
            .getUidValue();
  }

  @Test
  @Order(220)
  public void test220GroupModify() {
    Set<AttributeDelta> attributes = new HashSet<>();
    attributes.add(
        new AttributeDeltaBuilder()
            .setName(MicrosoftGraphGroupAttribute.EMAIL_NICKNAME.name())
            .addValueToReplace("FlinstonesMailGroup3")
            .build());

    Set<AttributeDelta> response =
        getConnectorFacade()
            .updateDelta(
                new ObjectClass("group"),
                new Uid(generatedGroupId),
                attributes,
                new OperationOptionsBuilder().build());
    assertNotNull(response);
    assertTrue(response.isEmpty());
  }

  @Test
  @Order(230)
  public void test230GroupsGet() {
    results = new ArrayList<>();
    getConnectorFacade()
        .search(new ObjectClass("group"), null, handler, new OperationOptionsBuilder().build());
    assertTrue(results.size() >= 1);
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Uid.NAME).getValue().get(0).toString()));
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Name.NAME).getValue().get(0).toString()));
  }

  @Test
  @Order(240)
  public void test240GroupGetKnown() {
    results = new ArrayList<>();
    Attribute idAttribute =
        new AttributeBuilder().setName(Uid.NAME).addValue(KNOWN_GROUP_ID).build();

    getConnectorFacade()
        .search(
            new ObjectClass("group"),
            new EqualsFilter(idAttribute),
            handler,
            new OperationOptionsBuilder().build());
    assertEquals(1, results.size());
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Uid.NAME).getValue().get(0).toString()));
  }

  @Test
  @Order(242)
  public void test242GroupGetCreated() {
    results = new ArrayList<>();
    Attribute idAttribute =
        new AttributeBuilder().setName(Uid.NAME).addValue(generatedGroupId).build();
    getConnectorFacade()
        .search(
            new ObjectClass("group"),
            new EqualsFilter(idAttribute),
            handler,
            new OperationOptionsBuilder().build());
    assertEquals(1, results.size());
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Uid.NAME).getValue().get(0).toString()));
  }

  @Test
  @Order(243)
  public void test243GroupsGetNameFiler() {
    results = new ArrayList<>();
    Attribute filterNameAttribute =
        new AttributeBuilder().setName(Name.NAME).addValue(KNOWN_GROUP_NAME).build();
    getConnectorFacade()
        .search(
            new ObjectClass("group"),
            new EqualsFilter(filterNameAttribute),
            handler,
            new OperationOptionsBuilder().build());
    assertEquals(1, results.size());
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Uid.NAME).getValue().get(0).toString()));
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Name.NAME).getValue().get(0).toString()));
  }

  @Test
  @Order(244)
  public void test244GroupsGetEmailFiler() {
    results = new ArrayList<>();
    Attribute filterNameAttribute =
        new AttributeBuilder()
            .setName(MicrosoftGraphGroupAttribute.EMAIL.name())
            .addValue(KNOWN_GROUP_EMAIL)
            .build();
    getConnectorFacade()
        .search(
            new ObjectClass("group"),
            new EqualsFilter(filterNameAttribute),
            handler,
            new OperationOptionsBuilder().build());
    assertEquals(1, results.size());
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Uid.NAME).getValue().get(0).toString()));
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Name.NAME).getValue().get(0).toString()));
  }

  @Test
  @Order(250)
  public void test250AssignCreatedUserToCreatedGroup() {
    Set<AttributeDelta> attributes = new HashSet<>();
    attributes.add(
        new AttributeDeltaBuilder()
            .setName(ASSIGNED_GROUPS.name())
            .addValueToAdd(generatedGroupId)
            .build());

    Set<AttributeDelta> response =
        getConnectorFacade()
            .updateDelta(
                new ObjectClass("user"),
                new Uid(generatedUserId),
                attributes,
                new OperationOptionsBuilder().build());
    assertNotNull(response);
    assertTrue(response.isEmpty());
  }

  @Test
  @Order(252)
  public void test252AssignCreatedUserToLicense() {
    Set<AttributeDelta> attributes = new HashSet<>();
    attributes.add(
        new AttributeDeltaBuilder()
            .setName(ASSIGNED_LICENSES.name())
            .addValueToAdd(KNOWN_LICENSE_ID)
            .build());

    Set<AttributeDelta> response =
        getConnectorFacade()
            .updateDelta(
                new ObjectClass("user"),
                new Uid(generatedUserId),
                attributes,
                new OperationOptionsBuilder().build());
    assertNotNull(response);
    assertTrue(response.isEmpty());
  }

  @Test
  @Order(254)
  public void test254CheckAssignments() {
    Attribute idAttribute =
        new AttributeBuilder().setName(Uid.NAME).addValue(generatedUserId).build();
    results = new ArrayList<>();
    getConnectorFacade()
        .search(
            new ObjectClass("user"),
            new EqualsFilter(idAttribute),
            handler,
            new OperationOptionsBuilder().build());
    assertEquals(1, results.size());
    assertEquals(1, results.get(0).getAttributeByName(ASSIGNED_GROUPS.name()).getValue().size());
    assertEquals(
        generatedGroupId,
        results.get(0).getAttributeByName(ASSIGNED_GROUPS.name()).getValue().get(0).toString());
    assertEquals(1, results.get(0).getAttributeByName(ASSIGNED_LICENSES.name()).getValue().size());
    assertEquals(
        KNOWN_LICENSE_ID,
        results.get(0).getAttributeByName(ASSIGNED_LICENSES.name()).getValue().get(0).toString());
  }

  @Test
  @Order(256)
  public void test256UnAssignCreatedUserToCreatedGroup() {
    Set<AttributeDelta> attributes = new HashSet<>();
    attributes.add(
        new AttributeDeltaBuilder()
            .setName(ASSIGNED_GROUPS.name())
            .addValueToRemove(generatedGroupId)
            .build());

    Set<AttributeDelta> response =
        getConnectorFacade()
            .updateDelta(
                new ObjectClass("user"),
                new Uid(generatedUserId),
                attributes,
                new OperationOptionsBuilder().build());
    assertNotNull(response);
    assertTrue(response.isEmpty());
  }

  @Test
  @Order(258)
  public void test258UnAssignCreatedUserToLicense() {
    Set<AttributeDelta> attributes = new HashSet<>();
    attributes.add(
        new AttributeDeltaBuilder()
            .setName(ASSIGNED_LICENSES.name())
            .addValueToRemove(KNOWN_LICENSE_ID)
            .build());

    Set<AttributeDelta> response =
        getConnectorFacade()
            .updateDelta(
                new ObjectClass("user"),
                new Uid(generatedUserId),
                attributes,
                new OperationOptionsBuilder().build());
    assertNotNull(response);
    assertTrue(response.isEmpty());
  }

  @Test
  @Order(260)
  public void test260CheckUnAssignments() {
    Attribute idAttribute =
        new AttributeBuilder().setName(Uid.NAME).addValue(generatedUserId).build();
    results = new ArrayList<>();
    getConnectorFacade()
        .search(
            new ObjectClass("user"),
            new EqualsFilter(idAttribute),
            handler,
            new OperationOptionsBuilder().build());
    assertEquals(1, results.size());
    assertNull(results.get(0).getAttributeByName(ASSIGNED_GROUPS.name()));
    assertNull(results.get(0).getAttributeByName(ASSIGNED_LICENSES.name()));
  }

  @Test
  @Order(270)
  public void test270AssignKnownUserToUnknownGroupSecurityError() {
    Set<AttributeDelta> attributes = new HashSet<>();
    attributes.add(
        new AttributeDeltaBuilder()
            .setName(ASSIGNED_GROUPS.name())
            .addValueToAdd(UUID.randomUUID().toString())
            .build());

    assertThrows(
        ConnectorException.class,
        () ->
            getConnectorFacade()
                .updateDelta(
                    new ObjectClass("user"),
                    new Uid(KNOWN_USER_ID),
                    attributes,
                    new OperationOptionsBuilder().build()));
  }

  @Test
  @Order(272)
  public void test272AssignCreatedUserToUnknownGroup() {
    Set<AttributeDelta> attributes = new HashSet<>();
    attributes.add(
        new AttributeDeltaBuilder()
            .setName(ASSIGNED_GROUPS.name())
            .addValueToAdd(UUID.randomUUID().toString())
            .build());

    assertThrows(
        ConnectorException.class,
        () ->
            getConnectorFacade()
                .updateDelta(
                    new ObjectClass("user"),
                    new Uid(generatedUserId),
                    attributes,
                    new OperationOptionsBuilder().build()));
  }

  @Test
  @Order(274)
  public void test274AssignCreatedUserToInvalidGroupId() {
    Set<AttributeDelta> attributes = new HashSet<>();
    attributes.add(
        new AttributeDeltaBuilder()
            .setName(ASSIGNED_GROUPS.name())
            .addValueToAdd("12345678")
            .build());

    assertThrows(
        ConnectorException.class,
        () ->
            getConnectorFacade()
                .updateDelta(
                    new ObjectClass("user"),
                    new Uid(generatedUserId),
                    attributes,
                    new OperationOptionsBuilder().build()));
  }

  @Test
  @Order(300)
  public void test300LicensesGet() {
    results = new ArrayList<>();
    getConnectorFacade()
        .search(new ObjectClass("license"), null, handler, new OperationOptionsBuilder().build());
    assertTrue(results.size() >= 1);
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Uid.NAME).getValue().get(0).toString()));
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Name.NAME).getValue().get(0).toString()));
  }

  @Test
  @Order(310)
  public void test310LicenseGetKnown() {
    results = new ArrayList<>();
    Attribute idAttribute =
        new AttributeBuilder().setName(Uid.NAME).addValue(KNOWN_LICENSE_DETAIL_ID).build();

    getConnectorFacade()
        .search(
            new ObjectClass("license"),
            new EqualsFilter(idAttribute),
            handler,
            new OperationOptionsBuilder().build());
    assertEquals(1, results.size());
    assertTrue(
        StringUtils.isNotBlank(
            results.get(0).getAttributeByName(Uid.NAME).getValue().get(0).toString()));
  }

  @Test
  @Order(490)
  public void test490GroupDelete() {
    getConnectorFacade()
        .delete(
            new ObjectClass("group"),
            new Uid(generatedGroupId),
            new OperationOptionsBuilder().build());
  }

  @Test
  @Order(590)
  public void test590UserDelete() {
    getConnectorFacade()
        .delete(
            new ObjectClass("user"),
            new Uid(generatedUserId),
            new OperationOptionsBuilder().build());
  }
}
