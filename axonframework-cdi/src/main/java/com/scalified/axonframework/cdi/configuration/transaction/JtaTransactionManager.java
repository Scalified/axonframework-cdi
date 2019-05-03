/*
 * Copyright 2019 Scalified
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

package com.scalified.axonframework.cdi.configuration.transaction;

import org.axonframework.common.transaction.Transaction;
import org.axonframework.common.transaction.TransactionManager;

/**
 * JTA {@link TransactionManager} implementation
 *
 * @author shell
 * @since 2019-04-12
 */
public class JtaTransactionManager implements TransactionManager {

	/**
	 * Starts a transaction. The return value is the started transaction that
	 * can be committed or rolled back
	 *
	 * @return The object representing the transaction
	 */
	@Override
	public Transaction startTransaction() {
		return new JtaTransaction();
	}

}
