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
import static com.exclamationlabs.connid.microsoft.graph.attribute.MicrosoftGraphLicenseAttribute.*;
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.*;

import com.exclamationlabs.connid.base.connector.adapter.BaseAdapter;
import com.exclamationlabs.connid.base.connector.attribute.ConnectorAttribute;
import com.exclamationlabs.connid.base.microsoft.graph.configuration.MicrosoftGraphConfiguration;
import com.exclamationlabs.connid.microsoft.graph.model.MicrosoftGraphLicense;
import com.microsoft.graph.models.SubscribedSku;
import java.util.HashSet;
import java.util.Set;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;

public class MicrosoftGraphLicensesAdapter
    extends BaseAdapter<MicrosoftGraphLicense, MicrosoftGraphConfiguration> {

  @Override
  public ObjectClass getType() {
    return new ObjectClass("license");
  }

  @Override
  public Class<MicrosoftGraphLicense> getIdentityModelClass() {

    return MicrosoftGraphLicense.class;
  }

  @Override
  public Set<ConnectorAttribute> getConnectorAttributes() {
    Set<ConnectorAttribute> result = new HashSet<>();
    result.add(new ConnectorAttribute(APPLIES_TO.name(), STRING, NOT_UPDATEABLE, NOT_CREATABLE));
    result.add(
        new ConnectorAttribute(CAPABILITY_STATUS.name(), STRING, NOT_UPDATEABLE, NOT_CREATABLE));
    result.add(
        new ConnectorAttribute(CONSUMED_UNITS.name(), INTEGER, NOT_UPDATEABLE, NOT_CREATABLE));
    result.add(new ConnectorAttribute(SKU_ID.name(), STRING, NOT_UPDATEABLE, NOT_CREATABLE));
    result.add(
        new ConnectorAttribute(SKU_PART_NUMBER.name(), STRING, NOT_UPDATEABLE, NOT_CREATABLE));

    return result;
  }

  @Override
  protected MicrosoftGraphLicense constructModel(
      Set<Attribute> attributes,
      Set<Attribute> multiValueAdded,
      Set<Attribute> multiValueRemoved,
      boolean creation) {
    return new MicrosoftGraphLicense(new SubscribedSku());
  }

  @Override
  protected Set<Attribute> constructAttributes(MicrosoftGraphLicense license) {
    Set<Attribute> attributes = new HashSet<>();
    attributes.add(AttributeBuilder.build(Uid.NAME, license.getGraphLicense().id));
    attributes.add(
        AttributeBuilder.build(APPLIES_TO.name(), license.getGraphLicense().capabilityStatus));
    if (license.getGraphLicense().skuId != null) {
      attributes.add(
          AttributeBuilder.build(SKU_ID.name(), license.getGraphLicense().skuId.toString()));
    }
    attributes.add(
        AttributeBuilder.build(CONSUMED_UNITS.name(), license.getGraphLicense().consumedUnits));
    attributes.add(
        AttributeBuilder.build(SKU_PART_NUMBER.name(), license.getGraphLicense().skuPartNumber));

    return attributes;
  }
}
