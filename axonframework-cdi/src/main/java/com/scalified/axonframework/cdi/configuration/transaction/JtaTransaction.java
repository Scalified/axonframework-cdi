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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import static java.util.Objects.nonNull;

/**
 * JTA {@link Transaction} implementation
 *
 * @author shell
 * @since 2019-04-12
 */
class JtaTransaction implements Transaction {

	/**
	 * {@link UserTransaction} JNDI locator
	 */
	private static final String USER_TRANSACTION_JNDI = "java:comp/UserTransaction";

	/**
	 * {@link UserTransaction} JNDI locator specific for JBOSS
	 */
	private static final String JBOSS_USER_TRANSACTION_JNDI = "java:jboss/UserTransaction";

	/**
	 * {@link TransactionSynchronizationRegistry} JNDI locator
	 */
	private static final String TRANSACTION_SYNCHRONIZATION_REGISTRY_JNDI
			= "java:comp/TransactionSynchronizationRegistry";

	/**
	 * {@link UserTransaction} instance
	 */
	private UserTransaction userTransaction;

	/**
	 * {@link TransactionSynchronizationRegistry} instance
	 */
	private TransactionSynchronizationRegistry registry;

	/**
	 * Checks whether this transaction is owned
	 */
	private boolean owned = true;

	/**
	 * {@link JtaTransaction} constructor
	 */
	JtaTransaction() {
		initialize();
		begin();
	}

	/**
	 * Initializes this transaction
	 */
	private void initialize() {
		userTransaction = lookupUserTransaction();

		if (nonNull(userTransaction)) {
			try {
				final int status = userTransaction.getStatus();
				if (status == Status.STATUS_ROLLEDBACK || status == Status.STATUS_MARKED_ROLLBACK) {
					userTransaction.rollback();
				}
				if (userTransaction.getStatus() != Status.STATUS_NO_TRANSACTION) {
					owned = false;
				}
			} catch (SystemException e) {
				owned = false;
			}
		} else {
			registry = lookupTransactionSynchronizationRegistry();
		}
	}

	/**
	 * Begins this transaction
	 */
	private void begin() {
		if (nonNull(userTransaction)) {
			try {
				if (owned) {
					userTransaction.begin();
				}
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * Commits this transaction
	 */
	@Override
	public void commit() {
		if (nonNull(userTransaction)) {
			try {
				if (owned) {
					if (userTransaction.getStatus() == Status.STATUS_ACTIVE) {
						userTransaction.commit();
					}
				}
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * Rollbacks this transaction
	 */
	@Override
	public void rollback() {
		if (nonNull(userTransaction)) {
			try {
				if (userTransaction.getStatus() == Status.STATUS_ACTIVE) {
					if (owned) {
						userTransaction.rollback();
					} else {
						userTransaction.setRollbackOnly();
					}
				}
			} catch (Exception ignored) {
			}
		} else {
			if (nonNull(registry)) {
				if (registry.getTransactionStatus() == Status.STATUS_ACTIVE) {
					registry.setRollbackOnly();
				}
			}
		}
	}

	/**
	 * Performs {@link UserTransaction} lookup in the JNDI context
	 *
	 * @return {@link UserTransaction} if found, {@code null} otherwise
	 */
	private static UserTransaction lookupUserTransaction() {
		try {
			return InitialContext.doLookup(USER_TRANSACTION_JNDI);
		} catch (NamingException ex) {
			try {
				return InitialContext.doLookup(JBOSS_USER_TRANSACTION_JNDI);
			} catch (NamingException ignored) {
			}
		}
		return null;
	}

	/**
	 * Performs {@link TransactionSynchronizationRegistry} lookup in the JNDI context
	 *
	 * @return {@link TransactionSynchronizationRegistry} if found, {@code null} otherwise
	 */
	private static TransactionSynchronizationRegistry lookupTransactionSynchronizationRegistry() {
		try {
			return InitialContext.doLookup(TRANSACTION_SYNCHRONIZATION_REGISTRY_JNDI);
		} catch (NamingException ignored) {
		}
		return null;
	}

}
