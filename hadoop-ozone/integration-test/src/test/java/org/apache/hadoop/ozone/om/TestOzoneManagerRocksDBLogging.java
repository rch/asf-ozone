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

package org.apache.hadoop.ozone.om;

import java.util.concurrent.TimeoutException;

import org.apache.hadoop.hdds.conf.OzoneConfiguration;
import org.apache.hadoop.hdds.utils.db.DBStoreBuilder;
import org.apache.hadoop.hdds.utils.db.RocksDBConfiguration;
import org.apache.hadoop.ozone.MiniOzoneCluster;
import org.apache.ozone.test.GenericTestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.apache.ozone.test.JUnit5AwareTimeout;

/**
 * Test RocksDB logging for Ozone Manager.
 */
public class TestOzoneManagerRocksDBLogging {
  private MiniOzoneCluster cluster = null;
  private OzoneConfiguration conf;
  private RocksDBConfiguration dbConf;

  @Rule
  public TestRule timeout = new JUnit5AwareTimeout(Timeout.seconds(100));

  private static GenericTestUtils.LogCapturer logCapturer =
      GenericTestUtils.LogCapturer.captureLogs(DBStoreBuilder.ROCKS_DB_LOGGER);

  @Before
  public void init() throws Exception {
    conf = new OzoneConfiguration();
    dbConf = conf.getObject(RocksDBConfiguration.class);
    enableRocksDbLogging(false);
    cluster =  MiniOzoneCluster.newBuilder(conf)
        .withoutDatanodes().build();
    cluster.waitForClusterToBeReady();
  }

  /**
   * Shutdown MiniDFSCluster.
   */
  @After
  public void shutdown() {
    if (cluster != null) {
      cluster.shutdown();
    }
  }

  @Test
  public void testOMRocksDBLoggingEnabled() throws Exception {
    try {
      waitForRocksDbLog();
      Assert.fail("Unexpected RocksDB log: " + logCapturer.getOutput());
    } catch (TimeoutException ex) {
      Assert.assertTrue(ex.getMessage().contains("Timed out"));
    }

    enableRocksDbLogging(true);
    cluster.restartOzoneManager();

    waitForRocksDbLog();
  }

  private void enableRocksDbLogging(boolean b) {
    dbConf.setRocksdbLoggingEnabled(b);
    conf.setFromObject(dbConf);
  }

  private static void waitForRocksDbLog()
      throws TimeoutException, InterruptedException {
    GenericTestUtils.waitFor(() -> logCapturer.getOutput()
            .contains("db_impl.cc"),
        1000, 30000);
  }

}
