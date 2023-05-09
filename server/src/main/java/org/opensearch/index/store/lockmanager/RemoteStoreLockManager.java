/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.store.lockmanager;

import java.io.IOException;

/**
 * An Interface that defines Index Level Remote Store MD Lock Manager.
 * @opensearch.internal
 */
public interface RemoteStoreLockManager {
    /**
     *
     * @param lockInfo lock info instance for which we need to acquire lock.
     * @throws IOException throws exception if lock is not present.
     */
    public void acquire(LockInfo lockInfo) throws IOException;

    /**
     *
     * @param lockInfo lock info instance for which lock need to be removed.
     * @throws IOException throws exception if lock is not present for given resourceId.
     */
    void release(LockInfo lockInfo) throws IOException;

    /**
     *
     * @param lockInfo lock info instance for which we need to check if lock is acquired.
     * @return whether a lock is acquired on the given lock info.
     * @throws IOException throws exception if file is not present in remote store.
     */
    Boolean isAcquired(LockInfo lockInfo) throws IOException;

    /**
     *
     * @param originalLockInfo lock info instance for original lock.
     * @param clonedLockInfo lock info instance for which lock needs to be cloned.
     * @throws IOException throws IOException if originalResource itself do not have any lock.
     */
    void cloneLock(LockInfo originalLockInfo, LockInfo clonedLockInfo) throws IOException;
}
