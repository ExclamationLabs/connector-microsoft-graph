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

package com.exclamationlabs.connid.microsoft.graph.model;

import com.exclamationlabs.connid.base.connector.model.IdentityModel;
import com.microsoft.graph.models.SubscribedSku;

public class MicrosoftGraphLicense implements IdentityModel {

  private SubscribedSku graphLicense;

  public MicrosoftGraphLicense(SubscribedSku license) {
    setGraphLicense(license);
  }

  @Override
  public String getIdentityIdValue() {
    return getGraphLicense().id;
  }

  @Override
  public String getIdentityNameValue() {
    return getGraphLicense().skuPartNumber;
  }

  public SubscribedSku getGraphLicense() {
    return graphLicense;
  }

  public void setGraphLicense(SubscribedSku license) {
    this.graphLicense = license;
  }
}
