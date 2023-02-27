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

public enum MicrosoftGraphUserAttribute {
  ACCOUNT_ENABLED, // boolean
  AGE_GROUP,

  ASSIGNED_LICENSES, // identity assignment
  ASSIGNED_GROUPS, // identity assignment
  // LICENSE_ASSIGNMENT_STATES, // complex

  BUSINESS_PHONES, // String, multiple
  CITY,
  COMPANY_NAME,
  COUNTRY,

  CREATED_DATETIME, // convert to string
  CREATION_TYPE,
  DEPARTMENT,
  DISPLAY_NAME,
  EMPLOYEE_HIRE_DATE,
  EMPLOYEE_ID,

  COST_CENTER, // in EmployeeOrgData
  DIVISION, // in EmployeeOrgData

  EMPLOYEE_TYPE,
  EXTERNAL_USER_STATE,
  EXTERNAL_USER_STATE_CHANGE_DATETIME,
  GIVEN_NAME,
  IM_ADDRESSES,
  JOB_TITLE,
  LAST_PASSWORD_CHANGE_DATETIME,
  EMAIL, // Mail in User
  EMAIL_NICKNAME,
  OFFICE_LOCATION,
  ON_PREMISES_DISTINGUISHED_NAME,
  ON_PREMISES_DOMAIN_NAME,
  ON_PREMISES_IMMUTABLE_ID,
  ON_PREMISES_LAST_SYNC_DATETIME,
  ON_PREMISES_SAM_ACCOUNT_NAME,
  ON_PREMISES_SECURITY_IDENTIFIER,
  ON_PREMISES_SYNC_ENABLED, // Boolean
  ON_PREMISES_USER_PRINCIPAL_NAME,
  OTHER_EMAILS, // OtherMails - multiple
  PASSWORD_POLICIES,
  POSTAL_CODE,
  PREFERRED_DATA_LOCATION,
  PREFERRED_LANGUAGE,

  // PROVISIONED_PLANS, // complex
  PROXY_ADDRESSES, // multiple
  SECURITY_IDENTIFIER,
  STATE,
  STREET_ADDRESS,
  SURNAME,
  USAGE_LOCATION, // A two letter country code (ISO standard 3166), MUST be given to assign licenses
  // to a user
  USER_PRINCIPAL_NAME,
  USER_TYPE,

  // MAILBOX_SETTINGS, // complex
  HIRE_DATE,
  RESPONSIBILITIES,
  SKILLS,

  PASSWORD, // complex in PasswordProfile
  FORCE_CHANGE_PASSWORD_NEXT_SIGN_IN, // boolean
  FORCE_CHANGE_PASSWORD_NEXT_SIGN_IN_WITH_MFA // boolean
}
