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

package com.exclamationlabs.connid.microsoft.graph.adapter;

import static com.exclamationlabs.connid.base.connector.attribute.ConnectorAttributeDataType.*;
import static com.exclamationlabs.connid.base.connector.attribute.ConnectorAttributeDataType.STRING;
import static com.exclamationlabs.connid.microsoft.graph.attribute.MicrosoftGraphUserAttribute.*;
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.*;

import com.exclamationlabs.connid.base.connector.adapter.AdapterValueTypeConverter;
import com.exclamationlabs.connid.base.connector.adapter.BaseAdapter;
import com.exclamationlabs.connid.base.connector.attribute.ConnectorAttribute;
import com.exclamationlabs.connid.base.connector.logging.Logger;
import com.exclamationlabs.connid.base.connector.util.GuardedStringUtil;
import com.exclamationlabs.connid.base.microsoft.graph.configuration.MicrosoftGraphConfiguration;
import com.exclamationlabs.connid.microsoft.graph.model.MicrosoftGraphUser;
import com.microsoft.graph.models.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.*;

public class MicrosoftGraphUsersAdapter
    extends BaseAdapter<MicrosoftGraphUser, MicrosoftGraphConfiguration> {

  @Override
  public ObjectClass getType() {
    return new ObjectClass("user");
  }

  @Override
  public Class<MicrosoftGraphUser> getIdentityModelClass() {
    return MicrosoftGraphUser.class;
  }

  @Override
  public Set<ConnectorAttribute> getConnectorAttributes() {
    Set<ConnectorAttribute> result = new HashSet<>();
    result.add(
        new ConnectorAttribute(OperationalAttributes.ENABLE_NAME, ACCOUNT_ENABLED.name(), BOOLEAN));
    result.add(new ConnectorAttribute(Uid.NAME, USER_ID.name(), STRING));
    result.add(new ConnectorAttribute(AGE_GROUP.name(), STRING));
    result.add(new ConnectorAttribute(BUSINESS_PHONES.name(), STRING, MULTIVALUED));
    result.add(new ConnectorAttribute(CITY.name(), STRING));
    result.add(new ConnectorAttribute(COMPANY_NAME.name(), STRING));
    result.add(new ConnectorAttribute(COUNTRY.name(), STRING));
    result.add(
        new ConnectorAttribute(CREATED_DATETIME.name(), STRING, NOT_CREATABLE, NOT_UPDATEABLE));
    result.add(new ConnectorAttribute(CREATION_TYPE.name(), STRING));
    result.add(new ConnectorAttribute(Name.NAME, DISPLAY_NAME.name(), STRING));
    result.add(new ConnectorAttribute(EMPLOYEE_HIRE_DATE.name(), STRING));
    result.add(new ConnectorAttribute(EMPLOYEE_ID.name(), STRING));
    result.add(new ConnectorAttribute(COST_CENTER.name(), STRING));
    result.add(new ConnectorAttribute(DIVISION.name(), STRING));
    result.add(new ConnectorAttribute(EMPLOYEE_TYPE.name(), STRING));
    result.add(new ConnectorAttribute(EXTERNAL_USER_STATE.name(), STRING));
    result.add(new ConnectorAttribute(EXTERNAL_USER_STATE_CHANGE_DATETIME.name(), STRING));
    result.add(new ConnectorAttribute(GIVEN_NAME.name(), STRING));
    result.add(new ConnectorAttribute(IM_ADDRESSES.name(), STRING, MULTIVALUED));
    result.add(
        new ConnectorAttribute(
            LAST_PASSWORD_CHANGE_DATETIME.name(), STRING, NOT_CREATABLE, NOT_UPDATEABLE));
    result.add(new ConnectorAttribute(EMAIL.name(), STRING));
    result.add(new ConnectorAttribute(EMAIL_NICKNAME.name(), STRING));
    result.add(new ConnectorAttribute(OFFICE_LOCATION.name(), STRING));
    result.add(new ConnectorAttribute(ON_PREMISES_DISTINGUISHED_NAME.name(), STRING));
    result.add(new ConnectorAttribute(ON_PREMISES_DOMAIN_NAME.name(), STRING));
    result.add(new ConnectorAttribute(ON_PREMISES_IMMUTABLE_ID.name(), STRING));
    result.add(
        new ConnectorAttribute(
            ON_PREMISES_LAST_SYNC_DATETIME.name(), STRING, NOT_CREATABLE, NOT_UPDATEABLE));
    result.add(
        new ConnectorAttribute(
            ON_PREMISES_SAM_ACCOUNT_NAME.name(), STRING, NOT_CREATABLE, NOT_UPDATEABLE));
    result.add(
        new ConnectorAttribute(
            ON_PREMISES_SECURITY_IDENTIFIER.name(), STRING, NOT_CREATABLE, NOT_UPDATEABLE));
    result.add(
        new ConnectorAttribute(
            ON_PREMISES_SYNC_ENABLED.name(), BOOLEAN, NOT_CREATABLE, NOT_UPDATEABLE));
    result.add(new ConnectorAttribute(ON_PREMISES_USER_PRINCIPAL_NAME.name(), STRING));
    result.add(new ConnectorAttribute(OTHER_EMAILS.name(), STRING, MULTIVALUED));
    result.add(new ConnectorAttribute(PASSWORD_POLICIES.name(), STRING));
    result.add(new ConnectorAttribute(POSTAL_CODE.name(), STRING));
    result.add(new ConnectorAttribute(PREFERRED_DATA_LOCATION.name(), STRING));
    result.add(new ConnectorAttribute(PREFERRED_LANGUAGE.name(), STRING));
    result.add(
        new ConnectorAttribute(
            PROXY_ADDRESSES.name(), STRING, MULTIVALUED, NOT_CREATABLE, NOT_UPDATEABLE));
    result.add(new ConnectorAttribute(SECURITY_IDENTIFIER.name(), STRING));
    result.add(new ConnectorAttribute(STATE.name(), STRING));
    result.add(new ConnectorAttribute(STREET_ADDRESS.name(), STRING));
    result.add(new ConnectorAttribute(SURNAME.name(), STRING));
    result.add(new ConnectorAttribute(USAGE_LOCATION.name(), STRING));
    result.add(new ConnectorAttribute(USER_PRINCIPAL_NAME.name(), STRING));
    result.add(new ConnectorAttribute(USER_TYPE.name(), STRING));

    result.add(new ConnectorAttribute(HIRE_DATE.name(), STRING));
    result.add(new ConnectorAttribute(RESPONSIBILITIES.name(), STRING, MULTIVALUED));
    result.add(new ConnectorAttribute(SKILLS.name(), STRING, MULTIVALUED));

    result.add(
        new ConnectorAttribute(
            OperationalAttributes.PASSWORD_NAME,
            PASSWORD.name(),
            GUARDED_STRING,
            NOT_READABLE,
            NOT_RETURNED_BY_DEFAULT));
    result.add(new ConnectorAttribute(FORCE_CHANGE_PASSWORD_NEXT_SIGN_IN.name(), BOOLEAN));
    result.add(new ConnectorAttribute(FORCE_CHANGE_PASSWORD_NEXT_SIGN_IN_WITH_MFA.name(), BOOLEAN));

    result.add(new ConnectorAttribute(ASSIGNED_GROUPS.name(), ASSIGNMENT_IDENTIFIER, MULTIVALUED));
    result.add(
        new ConnectorAttribute(ASSIGNED_LICENSES.name(), ASSIGNMENT_IDENTIFIER, MULTIVALUED));

    return result;
  }

  private void logConstructModel(Set<Attribute> attributes) {
    if (this.configuration.getEnableDebugHttpLogging()) {
      for (Attribute attribute : attributes) {
        try {
          Logger.error(
              this,
              "Construct model for attribute "
                  + attribute.getName()
                  + " value: "
                  + attribute.getValue());
        } catch (Exception e) {

        }
      }
    }
  }

  /**
   * This method does not create the employeeOrgData or passwordProfile objects to avoid the error
   * "Unable to update the specified properties for on-premises mastered Directory Sync objects". If
   * the customer creates accounts in AD and synchronizes those accounts with the cloud. If this is
   * the case the customer should not manage these objects through the OP. The affected Attributes
   * are: COST_CENTER DIVISION __PASSWORD__ FORCE_CHANGE_PASSWORD_NEXT_SIGN_IN
   * FORCE_CHANGE_PASSWORD_NEXT_SIGN_IN_WITH_MFA
   *
   * @param attributes Name/Value Attribute information received from Midpoint.
   * @param multiValueAdded For updateDelta, this may contain multi-valued attributes that were just
   *     added via Midpoint. Null for create, Empty set if none applicable found for UpdateDelta
   * @param multiValueRemoved For updateDelta, this may contain multi-valued attributes that were
   *     just removed via Midpoint. Null for create, Empty set if none applicable found for
   *     UpdateDelta
   * @param creation True if this invocation applies to new object creation, false if object update
   *     is applicable.
   * @return
   */
  @Override
  protected MicrosoftGraphUser constructModel(
      Set<Attribute> attributes,
      Set<Attribute> multiValueAdded,
      Set<Attribute> multiValueRemoved,
      boolean creation) {
    logConstructModel(attributes);
    MicrosoftGraphUser user = new MicrosoftGraphUser(new User());
    /*MG: comment creating these new objects
    user.getGraphUser().employeeOrgData = new EmployeeOrgData();
    user.getGraphUser().passwordProfile = new PasswordProfile();
    */
    user.getGraphUser().id = AdapterValueTypeConverter.getIdentityIdAttributeValue(attributes);
    user.getGraphUser().displayName =
        AdapterValueTypeConverter.getIdentityNameAttributeValue(attributes);
    user.getGraphUser().givenName =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, GIVEN_NAME);
    user.getGraphUser().surname =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, SURNAME);
    user.getGraphUser().mail =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, EMAIL);

    user.getGraphUser().preferredLanguage =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, PREFERRED_LANGUAGE);
    user.getGraphUser().userPrincipalName =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, USER_PRINCIPAL_NAME);

    // MG: do not create password profile unless values exist

    var tempPasswordProfile = new PasswordProfile();
    tempPasswordProfile.password =
        GuardedStringUtil.read(
            AdapterValueTypeConverter.getSingleAttributeValue(
                GuardedString.class, attributes, __PASSWORD__));
    tempPasswordProfile.forceChangePasswordNextSignIn =
        AdapterValueTypeConverter.getSingleAttributeValue(
            Boolean.class, attributes, FORCE_CHANGE_PASSWORD_NEXT_SIGN_IN);

    tempPasswordProfile.forceChangePasswordNextSignInWithMfa =
        AdapterValueTypeConverter.getSingleAttributeValue(
            Boolean.class, attributes, FORCE_CHANGE_PASSWORD_NEXT_SIGN_IN_WITH_MFA);
    if (tempPasswordProfile.forceChangePasswordNextSignInWithMfa != null
        || tempPasswordProfile.forceChangePasswordNextSignIn != null
        || tempPasswordProfile.password != null) {
      if (creation && tempPasswordProfile.forceChangePasswordNextSignIn == null) {
        tempPasswordProfile.forceChangePasswordNextSignIn =
            configuration.getForceChangePasswordOnCreate();
      }
      user.getGraphUser().passwordProfile = tempPasswordProfile;
    }
    user.getGraphUser().accountEnabled =
        AdapterValueTypeConverter.getSingleAttributeValue(Boolean.class, attributes, __ENABLE__);
    user.getGraphUser().mailNickname =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, EMAIL_NICKNAME);

    user.getGraphUser().ageGroup =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, AGE_GROUP);
    user.getGraphUser().businessPhones =
        AdapterValueTypeConverter.getMultipleAttributeValue(
            List.class, attributes, BUSINESS_PHONES);
    user.getGraphUser().city =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, CITY);
    user.getGraphUser().companyName =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, COMPANY_NAME);
    user.getGraphUser().country =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, COUNTRY);
    user.getGraphUser().createdDateTime =
        parseDateTime(
            AdapterValueTypeConverter.getSingleAttributeValue(
                String.class, attributes, CREATED_DATETIME));
    user.getGraphUser().creationType =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, CREATION_TYPE);

    user.getGraphUser().employeeHireDate =
        parseDateTime(
            AdapterValueTypeConverter.getSingleAttributeValue(
                String.class, attributes, EMPLOYEE_HIRE_DATE));
    user.getGraphUser().employeeId =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, EMPLOYEE_ID);

    // MG: do not create Employee Org data unless it exists
    var tempEmployeeOrgData = new EmployeeOrgData();
    tempEmployeeOrgData.costCenter =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, COST_CENTER);
    tempEmployeeOrgData.division =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, DIVISION);
    if (tempEmployeeOrgData.costCenter != null || tempEmployeeOrgData.division != null) {
      user.getGraphUser().employeeOrgData = tempEmployeeOrgData;
    }

    user.getGraphUser().employeeType =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, EMPLOYEE_TYPE);
    user.getGraphUser().externalUserState =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, EXTERNAL_USER_STATE);
    user.getGraphUser().externalUserStateChangeDateTime =
        parseDateTime(
            AdapterValueTypeConverter.getSingleAttributeValue(
                String.class, attributes, EXTERNAL_USER_STATE_CHANGE_DATETIME));
    user.getGraphUser().imAddresses =
        AdapterValueTypeConverter.getMultipleAttributeValue(List.class, attributes, IM_ADDRESSES);
    user.getGraphUser().lastPasswordChangeDateTime =
        parseDateTime(
            AdapterValueTypeConverter.getSingleAttributeValue(
                String.class, attributes, LAST_PASSWORD_CHANGE_DATETIME));
    user.getGraphUser().officeLocation =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, OFFICE_LOCATION);
    user.getGraphUser().onPremisesDistinguishedName =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, ON_PREMISES_DISTINGUISHED_NAME);
    user.getGraphUser().onPremisesDomainName =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, ON_PREMISES_DOMAIN_NAME);
    user.getGraphUser().onPremisesImmutableId =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, ON_PREMISES_IMMUTABLE_ID);
    user.getGraphUser().onPremisesLastSyncDateTime =
        parseDateTime(
            AdapterValueTypeConverter.getSingleAttributeValue(
                String.class, attributes, ON_PREMISES_LAST_SYNC_DATETIME));
    user.getGraphUser().onPremisesSamAccountName =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, ON_PREMISES_SAM_ACCOUNT_NAME);
    user.getGraphUser().onPremisesSecurityIdentifier =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, ON_PREMISES_SECURITY_IDENTIFIER);
    user.getGraphUser().onPremisesSyncEnabled =
        AdapterValueTypeConverter.getSingleAttributeValue(
            Boolean.class, attributes, ON_PREMISES_SYNC_ENABLED);
    user.getGraphUser().onPremisesUserPrincipalName =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, ON_PREMISES_USER_PRINCIPAL_NAME);
    user.getGraphUser().otherMails =
        AdapterValueTypeConverter.getMultipleAttributeValue(List.class, attributes, OTHER_EMAILS);
    user.getGraphUser().passwordPolicies =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, PASSWORD_POLICIES);
    user.getGraphUser().postalCode =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, POSTAL_CODE);
    user.getGraphUser().preferredDataLocation =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, PREFERRED_DATA_LOCATION);
    user.getGraphUser().preferredLanguage =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, PREFERRED_LANGUAGE);
    user.getGraphUser().proxyAddresses =
        AdapterValueTypeConverter.getMultipleAttributeValue(
            List.class, attributes, PROXY_ADDRESSES);
    user.getGraphUser().securityIdentifier =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, SECURITY_IDENTIFIER);
    user.getGraphUser().state =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, STATE);
    user.getGraphUser().streetAddress =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, STREET_ADDRESS);
    user.getGraphUser().usageLocation =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, USAGE_LOCATION);
    user.getGraphUser().userType =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, USER_TYPE);
    user.getGraphUser().hireDate =
        parseDateTime(
            AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, HIRE_DATE));
    user.getGraphUser().responsibilities =
        AdapterValueTypeConverter.getMultipleAttributeValue(
            List.class, attributes, RESPONSIBILITIES);
    user.getGraphUser().skills =
        AdapterValueTypeConverter.getMultipleAttributeValue(List.class, attributes, SKILLS);

    // Prepare group/license associations
    if (creation) {
      List<String> groupIds =
          AdapterValueTypeConverter.getMultipleAttributeValue(
              List.class, attributes, ASSIGNED_GROUPS);
      if (groupIds != null) {
        user.getGroupIdsToAdd().addAll(groupIds);
      }
      List<String> licenseIds =
          AdapterValueTypeConverter.getMultipleAttributeValue(
              List.class, attributes, ASSIGNED_LICENSES);
      if (licenseIds != null) {
        user.getLicenseIdsToAdd().addAll(licenseIds);
      }
    } else {
      List<String> groupIds =
          AdapterValueTypeConverter.getMultipleAttributeValue(
              List.class, multiValueAdded, ASSIGNED_GROUPS);
      if (groupIds != null) {
        user.getGroupIdsToAdd().addAll(groupIds);
      }
      List<String> groupIdsToRemove =
          AdapterValueTypeConverter.getMultipleAttributeValue(
              List.class, multiValueRemoved, ASSIGNED_GROUPS);
      if (groupIdsToRemove != null) {
        user.getGroupIdsToRemove().addAll(groupIdsToRemove);
      }

      List<String> licenseIds =
          AdapterValueTypeConverter.getMultipleAttributeValue(
              List.class, multiValueAdded, ASSIGNED_LICENSES);
      if (licenseIds != null) {
        user.getLicenseIdsToAdd().addAll(licenseIds);
      }
      List<String> licenseIdsToRemove =
          AdapterValueTypeConverter.getMultipleAttributeValue(
              List.class, multiValueRemoved, ASSIGNED_LICENSES);
      if (licenseIdsToRemove != null) {
        user.getLicenseIdsToRemove().addAll(licenseIdsToRemove);
      }
    }

    return user;
  }

  @Override
  protected Set<Attribute> constructAttributes(MicrosoftGraphUser user) {
    Set<Attribute> attributes = new HashSet<>();
    attributes.add(
        AttributeBuilder.build(
            OperationalAttributes.ENABLE_NAME, user.getGraphUser().accountEnabled));
    attributes.add(AttributeBuilder.build(GIVEN_NAME.name(), user.getGraphUser().givenName));
    attributes.add(AttributeBuilder.build(SURNAME.name(), user.getGraphUser().surname));
    attributes.add(AttributeBuilder.build(EMAIL.name(), user.getGraphUser().mail));
    attributes.add(AttributeBuilder.build(EMAIL_NICKNAME.name(), user.getGraphUser().mailNickname));

    attributes.add(
        AttributeBuilder.build(PREFERRED_LANGUAGE.name(), user.getGraphUser().preferredLanguage));
    attributes.add(
        AttributeBuilder.build(USER_PRINCIPAL_NAME.name(), user.getGraphUser().userPrincipalName));

    if (user.getGraphUser().passwordProfile != null) {
      if (user.getGraphUser().passwordProfile.password != null) {
        attributes.add(
            AttributeBuilder.build(
                OperationalAttributes.PASSWORD_NAME,
                new GuardedString(user.getGraphUser().passwordProfile.password.toCharArray())));
      }
      attributes.add(
          AttributeBuilder.build(
              FORCE_CHANGE_PASSWORD_NEXT_SIGN_IN.name(),
              user.getGraphUser().passwordProfile.forceChangePasswordNextSignIn));
      attributes.add(
          AttributeBuilder.build(
              FORCE_CHANGE_PASSWORD_NEXT_SIGN_IN_WITH_MFA.name(),
              user.getGraphUser().passwordProfile.forceChangePasswordNextSignInWithMfa));
    }

    attributes.add(
        AttributeBuilder.build(BUSINESS_PHONES.name(), user.getGraphUser().businessPhones));

    attributes.add(AttributeBuilder.build(AGE_GROUP.name(), user.getGraphUser().ageGroup));
    attributes.add(AttributeBuilder.build(CITY.name(), user.getGraphUser().city));
    attributes.add(AttributeBuilder.build(COMPANY_NAME.name(), user.getGraphUser().companyName));
    attributes.add(AttributeBuilder.build(COUNTRY.name(), user.getGraphUser().country));
    if (user.getGraphUser().createdDateTime != null) {
      attributes.add(
          AttributeBuilder.build(
              CREATED_DATETIME.name(), user.getGraphUser().createdDateTime.toString()));
    }
    attributes.add(AttributeBuilder.build(CREATION_TYPE.name(), user.getGraphUser().creationType));
    if (user.getGraphUser().employeeHireDate != null) {
      attributes.add(
          AttributeBuilder.build(
              EMPLOYEE_HIRE_DATE.name(), user.getGraphUser().employeeHireDate.toString()));
    }
    attributes.add(AttributeBuilder.build(EMPLOYEE_ID.name(), user.getGraphUser().employeeId));
    if (user.getGraphUser().employeeOrgData != null) {
      attributes.add(
          AttributeBuilder.build(
              COST_CENTER.name(), user.getGraphUser().employeeOrgData.costCenter));
      attributes.add(
          AttributeBuilder.build(DIVISION.name(), user.getGraphUser().employeeOrgData.division));
    }
    attributes.add(AttributeBuilder.build(EMPLOYEE_TYPE.name(), user.getGraphUser().employeeType));
    attributes.add(
        AttributeBuilder.build(EXTERNAL_USER_STATE.name(), user.getGraphUser().externalUserState));

    if (user.getGraphUser().externalUserStateChangeDateTime != null) {
      attributes.add(
          AttributeBuilder.build(
              EXTERNAL_USER_STATE_CHANGE_DATETIME.name(),
              user.getGraphUser().externalUserStateChangeDateTime.toString()));
    }
    attributes.add(AttributeBuilder.build(IM_ADDRESSES.name(), user.getGraphUser().imAddresses));

    if (user.getGraphUser().lastPasswordChangeDateTime != null) {
      attributes.add(
          AttributeBuilder.build(
              LAST_PASSWORD_CHANGE_DATETIME.name(),
              user.getGraphUser().lastPasswordChangeDateTime.toString()));
    }
    attributes.add(
        AttributeBuilder.build(OFFICE_LOCATION.name(), user.getGraphUser().officeLocation));
    attributes.add(
        AttributeBuilder.build(
            ON_PREMISES_DISTINGUISHED_NAME.name(),
            user.getGraphUser().onPremisesDistinguishedName));
    attributes.add(
        AttributeBuilder.build(
            ON_PREMISES_DOMAIN_NAME.name(), user.getGraphUser().onPremisesDomainName));
    attributes.add(
        AttributeBuilder.build(
            ON_PREMISES_IMMUTABLE_ID.name(), user.getGraphUser().onPremisesImmutableId));
    if (user.getGraphUser().onPremisesLastSyncDateTime != null) {
      attributes.add(
          AttributeBuilder.build(
              ON_PREMISES_LAST_SYNC_DATETIME.name(),
              user.getGraphUser().onPremisesLastSyncDateTime.toString()));
    }
    attributes.add(
        AttributeBuilder.build(
            ON_PREMISES_SAM_ACCOUNT_NAME.name(), user.getGraphUser().onPremisesSamAccountName));
    attributes.add(
        AttributeBuilder.build(
            ON_PREMISES_SECURITY_IDENTIFIER.name(),
            user.getGraphUser().onPremisesSecurityIdentifier));
    attributes.add(
        AttributeBuilder.build(
            ON_PREMISES_SYNC_ENABLED.name(), user.getGraphUser().onPremisesSyncEnabled));
    attributes.add(
        AttributeBuilder.build(
            ON_PREMISES_USER_PRINCIPAL_NAME.name(),
            user.getGraphUser().onPremisesUserPrincipalName));

    attributes.add(AttributeBuilder.build(OTHER_EMAILS.name(), user.getGraphUser().otherMails));
    attributes.add(
        AttributeBuilder.build(PASSWORD_POLICIES.name(), user.getGraphUser().passwordPolicies));
    attributes.add(AttributeBuilder.build(POSTAL_CODE.name(), user.getGraphUser().postalCode));
    attributes.add(
        AttributeBuilder.build(
            PREFERRED_DATA_LOCATION.name(), user.getGraphUser().preferredDataLocation));
    attributes.add(
        AttributeBuilder.build(PREFERRED_LANGUAGE.name(), user.getGraphUser().preferredLanguage));
    attributes.add(
        AttributeBuilder.build(PROXY_ADDRESSES.name(), user.getGraphUser().proxyAddresses));
    attributes.add(
        AttributeBuilder.build(SECURITY_IDENTIFIER.name(), user.getGraphUser().securityIdentifier));
    attributes.add(AttributeBuilder.build(STATE.name(), user.getGraphUser().state));
    attributes.add(
        AttributeBuilder.build(STREET_ADDRESS.name(), user.getGraphUser().streetAddress));
    attributes.add(
        AttributeBuilder.build(USAGE_LOCATION.name(), user.getGraphUser().usageLocation));
    attributes.add(AttributeBuilder.build(USER_TYPE.name(), user.getGraphUser().userType));
    if (user.getGraphUser().hireDate != null) {
      attributes.add(
          AttributeBuilder.build(HIRE_DATE.name(), user.getGraphUser().hireDate.toString()));
    }
    attributes.add(
        AttributeBuilder.build(RESPONSIBILITIES.name(), user.getGraphUser().responsibilities));
    attributes.add(AttributeBuilder.build(SKILLS.name(), user.getGraphUser().skills));
    attributes.add(
        AttributeBuilder.build(USAGE_LOCATION.name(), user.getGraphUser().usageLocation));

    // read assigned licenses
    if (user.getGraphUser().licenseAssignmentStates != null
        && (!user.getGraphUser().licenseAssignmentStates.isEmpty())) {
      List<String> licenses = new ArrayList<>();
      for (LicenseAssignmentState assignmentState : user.getGraphUser().licenseAssignmentStates) {
        if (assignmentState.assignedByGroup == null && assignmentState.skuId != null) {
          licenses.add(configuration.getTenantId() + "_" + assignmentState.skuId.toString());
        }
      }
      attributes.add(AttributeBuilder.build(ASSIGNED_LICENSES.name(), licenses));
    }

    // read assigned groups
    attributes.add(AttributeBuilder.build(ASSIGNED_GROUPS.name(), user.getMemberOf()));

    return attributes;
  }

  private static OffsetDateTime parseDateTime(String input) {
    return (input == null) ? null : OffsetDateTime.parse(input);
  }
}
