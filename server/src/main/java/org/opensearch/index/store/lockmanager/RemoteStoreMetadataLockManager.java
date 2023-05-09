/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.store.lockmanager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.opensearch.index.store.RemoteDirectory;

import java.io.IOException;
import java.util.Collection;

/**
 * A Class that implements Remote Store Lock Manager by creating lock files.
 * @opensearch.internal
 */
public class RemoteStoreMetadataLockManager implements RemoteStoreLockManager {
    private static final Logger logger = LogManager.getLogger(RemoteStoreMetadataLockManager.class);
    private final RemoteDirectory lockDirectory;
    public RemoteStoreMetadataLockManager(RemoteDirectory lockDirectory) {
        this.lockDirectory = lockDirectory;
    }

    @Override
    public void acquire(LockInfo lockInfo) throws IOException {
        assert lockInfo instanceof ShardLockInfo : "lockinfo should be instance of ShardLockInfo";
        assert ((ShardLockInfo) lockInfo).getFileToLock() != null : "file to be locked should be provided while " +
            "acquiring lock";
        assert ((ShardLockInfo) lockInfo).getResourceId() != null : "resource Id should be provided while " +
            "acquiring lock";
        assert ((ShardLockInfo) lockInfo).getExpiryTime() != null : "expiry time for lock should be provided while " +
            "acquiring lock";
        try (IndexOutput indexOutput = lockDirectory.createOutput(lockInfo.getLockName(), IOContext.DEFAULT)) {
            lockInfo.writeLockContent(indexOutput);
        }
    }

    @Override
    public void release(LockInfo lockInfo) throws IOException {
        assert lockInfo instanceof ShardLockInfo : "lockinfo should be instance of ShardLockInfo";
        String resourceId = ((ShardLockInfo) lockInfo).getResourceId();
        assert resourceId != null : "resourceId for which we need to release lock should not be null";
        String[] lockFiles = lockDirectory.listAll();
        String lockNameForResource = ShardLockInfo.getLockNameForResource(lockFiles, resourceId);
        lockDirectory.deleteFile(lockNameForResource);
    }

    @Override
    public Boolean isAcquired(LockInfo lockInfo) throws IOException {
        assert lockInfo instanceof ShardLockInfo : "lockInfo should be instance of ShardLockInfo";
        String fileToLock = ((ShardLockInfo) lockInfo).getFileToLock();
        assert !fileToLock.isEmpty() && !fileToLock.isBlank(): "fileToLock should be provided to check " +
            "if lock is acquired";
        Collection<String> lockFiles = lockDirectory.listFilesByPrefix(
            ShardLockInfo.getLockPrefixFromFileToLock(fileToLock));
        for (String lock: lockFiles) {
            logger.info("lock is " + lock);
            if (!ShardLockInfo.isLockExpired(lock)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void cloneLock(LockInfo originalLockInfo, LockInfo clonedLockInfo) throws IOException {
        assert originalLockInfo instanceof ShardLockInfo : "originalLockInfo should be instance of ShardLockInfo";
        assert clonedLockInfo instanceof ShardLockInfo: "clonedLockInfo should be instance of ShardLockInfo";
        String originalResourceId = ((ShardLockInfo) originalLockInfo).getResourceId();
        String clonedResourceId = ((ShardLockInfo) clonedLockInfo).getResourceId();
        assert originalResourceId != null && clonedResourceId != null : "provided resourceIds should not be null";
        String clonedLockExpiryTime = ((ShardLockInfo) clonedLockInfo).getExpiryTime();
        String[] lockFiles = lockDirectory.listAll();
        String lockNameForResource = ShardLockInfo.getLockNameForResource(lockFiles, originalResourceId);
        ShardLockInfo originalLockData = readLockData(lockNameForResource);
        acquire(ShardLockInfo.getLockInfoBuilder().withFileToLock(originalLockData.getFileToLock())
            .withResourceId(clonedResourceId).withExpiryTime(clonedLockExpiryTime).build());
    }

    private ShardLockInfo readLockData(String lockName) throws IOException {
        try (IndexInput indexInput = lockDirectory.openInput(lockName, IOContext.DEFAULT)) {
            return ShardLockInfo.getLockFileInfoFromIndexInput(indexInput);
        }
    }

    /**
     * Acquires lock on the file mentioned in originalLockInfo for acquirer mentioned in clonedLockInfo
     * @param originalLockInfo lock info instance for original lock.
     * @param clonedLockInfo lock info instance for which lock needs to be cloned.
     * @throws IOException throws IOException if originalResource itself do not have any lock.
     */
    @Override
    public void cloneLock(LockInfo originalLockInfo, LockInfo clonedLockInfo) throws IOException {
        assert originalLockInfo instanceof FileLockInfo : "originalLockInfo should be instance of ShardLockInfo";
        assert clonedLockInfo instanceof FileLockInfo : "clonedLockInfo should be instance of ShardLockInfo";
        String originalResourceId = ((FileLockInfo) originalLockInfo).getAcquirerId();
        String clonedResourceId = ((FileLockInfo) clonedLockInfo).getAcquirerId();
        assert originalResourceId != null && clonedResourceId != null : "provided resourceIds should not be null";
        String[] lockFiles = lockDirectory.listAll();
        String lockNameForAcquirer = ((FileLockInfo) originalLockInfo).getLocksForAcquirer(lockFiles).get(0);
        String fileToLockName = FileLockInfo.LockFileUtils.getFileToLockNameFromLock(lockNameForAcquirer);
        acquire(FileLockInfo.getLockInfoBuilder().withFileToLock(fileToLockName).withAcquirerId(clonedResourceId).build());
    }
}
