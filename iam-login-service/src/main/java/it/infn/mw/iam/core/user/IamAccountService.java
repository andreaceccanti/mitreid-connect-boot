/**
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2016-2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.infn.mw.iam.core.user;

import java.util.Date;
import java.util.List;

import it.infn.mw.iam.persistence.model.IamAccount;

/**
 * This service provides basic functionality used to manage IAM accounts
 */
public interface IamAccountService {

  /**
   * Creates a new {@link IamAccount}, after some checks.
   * 
   * @param account the account to be created
   * @return the created {@link IamAccount}
   */
  IamAccount createAccount(IamAccount account);


  /**
   * Deletes a {@link IamAccount}.
   * 
   * @param account the account to be deleted
   * 
   * @return the deleted {@link IamAccount}
   */
  IamAccount deleteAccount(IamAccount account);

  /**
   * Deletes provisioned accounts whose last login time is before than the timestamp passed as
   * argument
   * 
   * @param timestamp the timestamp
   * @return the possibly empty {@link List} of {@link IamAccount} that have been removed
   */
  List<IamAccount> deleteInactiveProvisionedUsersSinceTime(Date timestamp);
}
