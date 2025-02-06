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
import static com.exclamationlabs.connid.microsoft.graph.attribute.MicrosoftGraphGroupAttribute.*;
import static com.exclamationlabs.connid.microsoft.graph.attribute.MicrosoftGraphUserAttribute.ASSIGNED_LICENSES;
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.*;

import com.exclamationlabs.connid.base.connector.adapter.AdapterValueTypeConverter;
import com.exclamationlabs.connid.base.connector.adapter.BaseAdapter;
import com.exclamationlabs.connid.base.connector.attribute.ConnectorAttribute;
import com.exclamationlabs.connid.base.microsoft.graph.configuration.MicrosoftGraphConfiguration;
import com.exclamationlabs.connid.microsoft.graph.model.MicrosoftGraphGroup;
import com.microsoft.graph.models.AssignedLicense;
import com.microsoft.graph.models.Group;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.BooleanUtils;
import org.identityconnectors.framework.common.objects.*;

public class MicrosoftGraphGroupsAdapter
    extends BaseAdapter<MicrosoftGraphGroup, MicrosoftGraphConfiguration> {

  @Override
  public ObjectClass getType() {
    return new ObjectClass("group");
  }

  @Override
  public Class<MicrosoftGraphGroup> getIdentityModelClass() {

    return MicrosoftGraphGroup.class;
  }

  @Override
  public Set<ConnectorAttribute> getConnectorAttributes() {
    Set<ConnectorAttribute> result = new HashSet<>();
    result.add(new ConnectorAttribute(Uid.NAME, GROUP_ID.name(), STRING));
    result.add(new ConnectorAttribute(CLASSIFICATION.name(), STRING));
    result.add(new ConnectorAttribute(CREATED_DATETIME.name(), STRING));
    result.add(new ConnectorAttribute(DESCRIPTION.name(), STRING));
    result.add(new ConnectorAttribute(Name.NAME, DISPLAY_NAME.name(), STRING));
    result.add(
        new ConnectorAttribute(EXPIRATION_DATETIME.name(), STRING, NOT_UPDATEABLE, NOT_CREATABLE));
    result.add(new ConnectorAttribute(GROUP_TYPES.name(), STRING, MULTIVALUED));
    result.add(new ConnectorAttribute(IS_ASSIGNABLE_TO_ROLE.name(), BOOLEAN));
    result.add(
        new ConnectorAttribute(
            LICENSE_PROCESSING_STATE.name(), STRING, NOT_UPDATEABLE, NOT_CREATABLE));
    result.add(new ConnectorAttribute(EMAIL.name(), STRING, NOT_UPDATEABLE, NOT_CREATABLE));
    result.add(new ConnectorAttribute(EMAIL_ENABLED.name(), BOOLEAN));
    result.add(new ConnectorAttribute(EMAIL_NICKNAME.name(), STRING));
    result.add(new ConnectorAttribute(MEMBERSHIP_RULE.name(), STRING));
    result.add(new ConnectorAttribute(MEMBERSHIP_RULE_PROCESSING_STATE.name(), STRING));

    result.add(new ConnectorAttribute(ON_PREMISES_NET_BIOS_NAME.name(), STRING));
    result.add(
        new ConnectorAttribute(
            ON_PREMISES_LAST_SYNC_DATETIME.name(), STRING, NOT_UPDATEABLE, NOT_CREATABLE));
    result.add(
        new ConnectorAttribute(
            ON_PREMISES_SAM_ACCOUNT_NAME.name(), STRING, NOT_UPDATEABLE, NOT_CREATABLE));
    result.add(
        new ConnectorAttribute(
            ON_PREMISES_SECURITY_IDENTIFIER.name(), STRING, NOT_UPDATEABLE, NOT_CREATABLE));
    result.add(
        new ConnectorAttribute(
            ON_PREMISES_SYNC_ENABLED.name(), BOOLEAN, NOT_UPDATEABLE, NOT_CREATABLE));
    result.add(new ConnectorAttribute(ON_PREMISES_DOMAIN_NAME.name(), STRING));

    result.add(new ConnectorAttribute(PREFERRED_DATA_LOCATION.name(), STRING));
    result.add(new ConnectorAttribute(PREFERRED_LANGUAGE.name(), STRING));
    result.add(
        new ConnectorAttribute(
            PROXY_ADDRESSES.name(), STRING, MULTIVALUED, NOT_UPDATEABLE, NOT_CREATABLE));
    result.add(
        new ConnectorAttribute(RENEWED_DATETIME.name(), STRING, NOT_UPDATEABLE, NOT_CREATABLE));
    result.add(new ConnectorAttribute(SECURITY_ENABLED.name(), BOOLEAN));
    result.add(new ConnectorAttribute(SECURITY_IDENTIFIER.name(), STRING));
    result.add(new ConnectorAttribute(THEME.name(), STRING));
    result.add(new ConnectorAttribute(VISIBILITY.name(), STRING));

    result.add(new ConnectorAttribute(CREATED_ON_BEHALF_OF.name(), STRING));

    result.add(new ConnectorAttribute(IS_DYNAMIC.name(), BOOLEAN));
    result.add(new ConnectorAttribute(IS_MS_365.name(), BOOLEAN));
    result.add(new ConnectorAttribute(IS_MS_TEAM.name(), BOOLEAN));
    result.add(new ConnectorAttribute(IS_SECURITY_GROUP.name(), BOOLEAN));
    result.add(new ConnectorAttribute(IS_MAIL_ENABLED_SECURITY_GROUP.name(), BOOLEAN));
    result.add(new ConnectorAttribute(IS_DISTRIBUTION_GROUP.name(), BOOLEAN));

    result.add(
        new ConnectorAttribute(ASSIGNED_LICENSES.name(), ASSIGNMENT_IDENTIFIER, MULTIVALUED));

    return result;
  }

  @Override
  protected MicrosoftGraphGroup constructModel(
      Set<Attribute> attributes,
      Set<Attribute> multiValueAdded,
      Set<Attribute> multiValueRemoved,
      boolean creation) {
    MicrosoftGraphGroup group = new MicrosoftGraphGroup(new Group());
    group.getGraphGroup().id = AdapterValueTypeConverter.getIdentityIdAttributeValue(attributes);
    group.getGraphGroup().classification =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, CLASSIFICATION);
    group.getGraphGroup().description =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, DESCRIPTION);
    group.getGraphGroup().displayName =
        AdapterValueTypeConverter.getIdentityNameAttributeValue(attributes);
    group.getGraphGroup().groupTypes =
        AdapterValueTypeConverter.getMultipleAttributeValue(List.class, attributes, GROUP_TYPES);
    group.getGraphGroup().isAssignableToRole =
        AdapterValueTypeConverter.getSingleAttributeValue(
            Boolean.class, attributes, IS_ASSIGNABLE_TO_ROLE);
    group.getGraphGroup().mail =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, EMAIL);
    group.getGraphGroup().mailEnabled =
        AdapterValueTypeConverter.getSingleAttributeValue(Boolean.class, attributes, EMAIL_ENABLED);
    group.getGraphGroup().mailNickname =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, EMAIL_NICKNAME);
    group.getGraphGroup().membershipRule =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, MEMBERSHIP_RULE);
    group.getGraphGroup().membershipRuleProcessingState =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, MEMBERSHIP_RULE_PROCESSING_STATE);

    group.getGraphGroup().onPremisesDomainName =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, ON_PREMISES_DOMAIN_NAME);
    group.getGraphGroup().onPremisesLastSyncDateTime =
        parseDateTime(
            AdapterValueTypeConverter.getSingleAttributeValue(
                String.class, attributes, ON_PREMISES_LAST_SYNC_DATETIME));
    group.getGraphGroup().onPremisesNetBiosName =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, ON_PREMISES_NET_BIOS_NAME);

    group.getGraphGroup().preferredDataLocation =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, PREFERRED_DATA_LOCATION);
    group.getGraphGroup().preferredLanguage =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, PREFERRED_LANGUAGE);
    group.getGraphGroup().securityEnabled =
        AdapterValueTypeConverter.getSingleAttributeValue(
            Boolean.class, attributes, SECURITY_ENABLED);
    group.getGraphGroup().securityIdentifier =
        AdapterValueTypeConverter.getSingleAttributeValue(
            String.class, attributes, SECURITY_IDENTIFIER);
    group.getGraphGroup().theme =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, THEME);
    group.getGraphGroup().visibility =
        AdapterValueTypeConverter.getSingleAttributeValue(String.class, attributes, VISIBILITY);

    return group;
  }

  @Override
  protected Set<Attribute> constructAttributes(MicrosoftGraphGroup group) {
    Set<Attribute> attributes = new HashSet<>();
    attributes.add(
        AttributeBuilder.build(CLASSIFICATION.name(), group.getGraphGroup().classification));
    if (group.getGraphGroup().createdDateTime != null) {
      attributes.add(
          AttributeBuilder.build(
              CREATED_DATETIME.name(), group.getGraphGroup().createdDateTime.toString()));
    }
    if (group.getGraphGroup().expirationDateTime != null) {
      attributes.add(
          AttributeBuilder.build(
              EXPIRATION_DATETIME.name(), group.getGraphGroup().expirationDateTime.toString()));
    }
    attributes.add(AttributeBuilder.build(GROUP_TYPES.name(), group.getGraphGroup().groupTypes));
    attributes.add(
        AttributeBuilder.build(
            IS_ASSIGNABLE_TO_ROLE.name(), group.getGraphGroup().isAssignableToRole));
    if (group.getGraphGroup().licenseProcessingState != null) {
      attributes.add(
          AttributeBuilder.build(
              LICENSE_PROCESSING_STATE.name(), group.getGraphGroup().licenseProcessingState.state));
    }
    attributes.add(AttributeBuilder.build(EMAIL.name(), group.getGraphGroup().mail));
    attributes.add(AttributeBuilder.build(EMAIL_ENABLED.name(), group.getGraphGroup().mailEnabled));
    attributes.add(
        AttributeBuilder.build(EMAIL_NICKNAME.name(), group.getGraphGroup().mailNickname));
    attributes.add(
        AttributeBuilder.build(MEMBERSHIP_RULE.name(), group.getGraphGroup().membershipRule));
    attributes.add(
        AttributeBuilder.build(
            MEMBERSHIP_RULE_PROCESSING_STATE.name(),
            group.getGraphGroup().membershipRuleProcessingState));

    attributes.add(
        AttributeBuilder.build(
            ON_PREMISES_DOMAIN_NAME.name(), group.getGraphGroup().onPremisesDomainName));
    if (group.getGraphGroup().onPremisesLastSyncDateTime != null) {
      attributes.add(
          AttributeBuilder.build(
              ON_PREMISES_LAST_SYNC_DATETIME.name(),
              group.getGraphGroup().onPremisesLastSyncDateTime.toString()));
    }
    attributes.add(
        AttributeBuilder.build(
            ON_PREMISES_NET_BIOS_NAME.name(), group.getGraphGroup().onPremisesNetBiosName));
    attributes.add(
        AttributeBuilder.build(
            ON_PREMISES_SAM_ACCOUNT_NAME.name(), group.getGraphGroup().onPremisesSamAccountName));
    attributes.add(
        AttributeBuilder.build(
            ON_PREMISES_SECURITY_IDENTIFIER.name(),
            group.getGraphGroup().onPremisesSecurityIdentifier));
    attributes.add(
        AttributeBuilder.build(
            ON_PREMISES_SYNC_ENABLED.name(), group.getGraphGroup().onPremisesSyncEnabled));

    attributes.add(
        AttributeBuilder.build(
            PREFERRED_DATA_LOCATION.name(), group.getGraphGroup().preferredDataLocation));
    attributes.add(
        AttributeBuilder.build(PREFERRED_LANGUAGE.name(), group.getGraphGroup().preferredLanguage));
    attributes.add(
        AttributeBuilder.build(PROXY_ADDRESSES.name(), group.getGraphGroup().proxyAddresses));
    if (group.getGraphGroup().renewedDateTime != null) {
      attributes.add(
          AttributeBuilder.build(
              RENEWED_DATETIME.name(), group.getGraphGroup().renewedDateTime.toString()));
    }
    attributes.add(
        AttributeBuilder.build(SECURITY_ENABLED.name(), group.getGraphGroup().securityEnabled));
    attributes.add(
        AttributeBuilder.build(
            SECURITY_IDENTIFIER.name(), group.getGraphGroup().securityIdentifier));
    attributes.add(AttributeBuilder.build(THEME.name(), group.getGraphGroup().theme));
    attributes.add(AttributeBuilder.build(VISIBILITY.name(), group.getGraphGroup().visibility));
    if (group.getGraphGroup().createdOnBehalfOf != null) {
      attributes.add(
          AttributeBuilder.build(
              CREATED_ON_BEHALF_OF.name(), group.getGraphGroup().createdOnBehalfOf.id));
    }

    // read assigned licenses - read-only
    if (group.getGraphGroup().assignedLicenses != null
        && (!group.getGraphGroup().assignedLicenses.isEmpty())) {
      List<String> licenses = new ArrayList<>();
      for (AssignedLicense current : group.getGraphGroup().assignedLicenses) {
        if (current != null && current.skuId != null) {
          licenses.add(current.skuId.toString());
        }
      }
      attributes.add(AttributeBuilder.build(ASSIGNED_LICENSES.name(), licenses));
    }

    // Additional group identification flags
    boolean is365Group =
        group.getGraphGroup().groupTypes != null
            && !group.getGraphGroup().groupTypes.isEmpty()
            && group.getGraphGroup().groupTypes.stream()
                .anyMatch(s -> s.equalsIgnoreCase("Unified"))
            && group.getGraphGroup().groupTypes.stream()
                .noneMatch(s -> s.equalsIgnoreCase("DynamicMembership"))
            && BooleanUtils.isTrue(group.getGraphGroup().mailEnabled);
    attributes.add(AttributeBuilder.build(IS_MS_365.name(), is365Group));
    boolean isDynamicMembershipGroup =
        group.getGraphGroup().groupTypes != null
            && !group.getGraphGroup().groupTypes.isEmpty()
            && group.getGraphGroup().groupTypes.stream()
                .anyMatch(s -> s.equalsIgnoreCase("DynamicMembership"))
            && BooleanUtils.isTrue(group.getGraphGroup().mailEnabled);
    attributes.add(AttributeBuilder.build(IS_DYNAMIC.name(), isDynamicMembershipGroup));
    boolean isSecurity =
        (group.getGraphGroup().groupTypes == null || group.getGraphGroup().groupTypes.isEmpty())
            && BooleanUtils.isFalse(group.getGraphGroup().mailEnabled)
            && BooleanUtils.isTrue(group.getGraphGroup().securityEnabled);
    attributes.add(AttributeBuilder.build(IS_SECURITY_GROUP.name(), isSecurity));
    boolean isMailSecurityGroup =
        (group.getGraphGroup().groupTypes == null || group.getGraphGroup().groupTypes.isEmpty())
            && BooleanUtils.isTrue(group.getGraphGroup().mailEnabled)
            && BooleanUtils.isTrue(group.getGraphGroup().securityEnabled);
    attributes.add(
        AttributeBuilder.build(IS_MAIL_ENABLED_SECURITY_GROUP.name(), isMailSecurityGroup));
    boolean isDistributionGroup =
        (group.getGraphGroup().groupTypes == null || group.getGraphGroup().groupTypes.isEmpty())
            && BooleanUtils.isTrue(group.getGraphGroup().mailEnabled)
            && BooleanUtils.isFalse(group.getGraphGroup().securityEnabled);
    attributes.add(AttributeBuilder.build(IS_DISTRIBUTION_GROUP.name(), isDistributionGroup));

    attributes.add(AttributeBuilder.build(IS_MS_TEAM.name(), group.getMsTeam()));

    return attributes;
  }

  private static OffsetDateTime parseDateTime(String input) {
    return (input == null) ? null : OffsetDateTime.parse(input);
  }
}
