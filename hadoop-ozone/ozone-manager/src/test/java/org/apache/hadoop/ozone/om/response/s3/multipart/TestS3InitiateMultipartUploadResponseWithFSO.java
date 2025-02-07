/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.hadoop.ozone.om.response.s3.multipart;

import org.apache.hadoop.ozone.om.helpers.BucketLayout;
import org.apache.hadoop.ozone.om.helpers.OmDirectoryInfo;
import org.apache.hadoop.ozone.om.helpers.OmKeyInfo;
import org.apache.hadoop.ozone.om.helpers.OmMultipartKeyInfo;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Class tests S3 Initiate MPU response.
 */
public class TestS3InitiateMultipartUploadResponseWithFSO
    extends TestS3InitiateMultipartUploadResponse {

  public BucketLayout getBucketLayout() {
    return BucketLayout.FILE_SYSTEM_OPTIMIZED;
  }

  @Test
  public void testAddDBToBatch() throws Exception {
    String volumeName = UUID.randomUUID().toString();
    String bucketName = UUID.randomUUID().toString();
    String prefix = "a/b/c/d/";
    String fileName = UUID.randomUUID().toString();
    String keyName = prefix + fileName;

    String multipartUploadID = UUID.randomUUID().toString();

    addVolumeToDB(volumeName);
    addBucketToDB(volumeName, bucketName);

    long parentID = 1027; // assume objectID of dir path "a/b/c/d" is 1027
    List<OmDirectoryInfo> parentDirInfos = new ArrayList<>();

    final long volumeId = omMetadataManager.getVolumeId(volumeName);
    final long bucketId = omMetadataManager.getBucketId(volumeName,
        bucketName);

    S3InitiateMultipartUploadResponse s3InitiateMultipartUploadResponseFSO =
        createS3InitiateMPUResponseFSO(volumeName, bucketName, parentID,
            keyName, multipartUploadID, parentDirInfos, volumeId, bucketId);

    s3InitiateMultipartUploadResponseFSO.addToDBBatch(omMetadataManager,
        batchOperation);

    // Do manual commit and see whether addToBatch is successful or not.
    omMetadataManager.getStore().commitBatchOperation(batchOperation);

    String multipartKey = omMetadataManager
        .getMultipartKey(volumeName, bucketName, keyName, multipartUploadID);



    String multipartOpenKey = omMetadataManager
        .getMultipartKey(volumeId, bucketId, parentID,
                fileName, multipartUploadID);

    OmKeyInfo omKeyInfo = omMetadataManager.getOpenKeyTable(getBucketLayout())
        .get(multipartOpenKey);
    Assert.assertNotNull("Failed to find the fileInfo", omKeyInfo);
    Assert.assertNotNull("Key Location is null!",
        omKeyInfo.getLatestVersionLocations());
    Assert.assertTrue("isMultipartKey is false!",
        omKeyInfo.getLatestVersionLocations().isMultipartKey());
    Assert.assertEquals("FileName mismatches!", fileName,
            omKeyInfo.getKeyName());
    Assert.assertEquals("ParentId mismatches!", parentID,
            omKeyInfo.getParentObjectID());

    OmMultipartKeyInfo omMultipartKeyInfo = omMetadataManager
            .getMultipartInfoTable().get(multipartKey);
    Assert.assertNotNull("Failed to find the multipartFileInfo",
            omMultipartKeyInfo);
    Assert.assertEquals("ParentId mismatches!", parentID,
            omMultipartKeyInfo.getParentID());

    Assert.assertEquals("Upload Id mismatches!", multipartUploadID,
            omMultipartKeyInfo.getUploadID());
  }
}
