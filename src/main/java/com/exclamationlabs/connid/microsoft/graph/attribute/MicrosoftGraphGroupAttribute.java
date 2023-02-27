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

package com.exclamationlabs.connid.microsoft.graph.attribute;

public enum MicrosoftGraphGroupAttribute {
  CLASSIFICATION,
  CREATED_DATETIME,
  DESCRIPTION,
  DISPLAY_NAME,
  EXPIRATION_DATETIME,
  GROUP_TYPES,
  IS_ASSIGNABLE_TO_ROLE,
  LICENSE_PROCESSING_STATE,
  EMAIL,
  EMAIL_ENABLED,
  EMAIL_NICKNAME,
  MEMBERSHIP_RULE,
  MEMBERSHIP_RULE_PROCESSING_STATE,
  ON_PREMISES_DOMAIN_NAME,
  ON_PREMISES_NET_BIOS_NAME,
  ON_PREMISES_LAST_SYNC_DATETIME,
  ON_PREMISES_SAM_ACCOUNT_NAME,
  ON_PREMISES_SECURITY_IDENTIFIER,
  ON_PREMISES_SYNC_ENABLED, // Boolean

  PREFERRED_DATA_LOCATION,
  PREFERRED_LANGUAGE,
  PROXY_ADDRESSES,
  RENEWED_DATETIME,
  SECURITY_ENABLED,
  SECURITY_IDENTIFIER,
  THEME,
  VISIBILITY,

  CREATED_ON_BEHALF_OF,

  ASSIGNED_LICENSES // read-only
}
