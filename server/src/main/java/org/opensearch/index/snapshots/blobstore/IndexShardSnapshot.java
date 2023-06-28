/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.snapshots.blobstore;

import org.opensearch.index.snapshots.IndexShardSnapshotStatus;

import java.io.IOException;

/**
 * Remote Store based Shard snapshot metadata
 *
 * @opensearch.internal
 */
public interface IndexShardSnapshot {

    /**
     *
     * @return IndexShardSnapshotStatus
     */
    IndexShardSnapshotStatus getIndexShardSnapshotStatus();

}
