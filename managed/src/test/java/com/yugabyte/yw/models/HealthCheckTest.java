// Copyright (c) YugaByte, Inc.

package com.yugabyte.yw.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.yugabyte.yw.common.FakeDBApplication;
import com.yugabyte.yw.common.ModelFactory;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

public class HealthCheckTest extends FakeDBApplication {
  private Customer defaultCustomer;

  @Before
  public void setUp() {
    defaultCustomer = ModelFactory.testCustomer();
  }

  private HealthCheck addCheck(UUID universeUUID) {
    return addCheck(universeUUID, "{}");
  }

  private HealthCheck addCheck(UUID universeUUID, String detailsJson) {
    try {
      // The checkTime is created internally and part of the primary key
      Thread.sleep(10);
    } catch (InterruptedException e) {
      // Ignore in test..
    }
    HealthCheck check =
        HealthCheck.addAndPrune(universeUUID, defaultCustomer.getCustomerId(), detailsJson);
    assertNotNull(check);
    return check;
  }

  private void addChecks(UUID universeUUID, int numChecks) {
    for (int i = 0; i < numChecks; ++i) {
      addCheck(universeUUID);
    }
    List<HealthCheck> checks = HealthCheck.getAll(universeUUID);
    assertNotNull(checks);
    // If we were asked to insert over the limit, pruning will happen.
    assertEquals(checks.size(), Math.min(HealthCheck.RECORD_LIMIT, checks.size()));
  }

  @Test
  public void testAddToLimit() {
    UUID universeUUID = UUID.randomUUID();
    addChecks(universeUUID, HealthCheck.RECORD_LIMIT);
  }

  @Test
  public void testPruneEnoughItems() {
    UUID universeUUID = UUID.randomUUID();
    addChecks(universeUUID, 2 * HealthCheck.RECORD_LIMIT);
  }

  @Test
  public void testPruneRightItems() throws java.lang.InterruptedException {
    UUID universeUUID = UUID.randomUUID();
    addChecks(universeUUID, HealthCheck.RECORD_LIMIT);
    Date now = new Date();
    addChecks(universeUUID, HealthCheck.RECORD_LIMIT);
    List<HealthCheck> after = HealthCheck.getAll(universeUUID);
    for (HealthCheck check : after) {
      assertTrue(now.compareTo(check.idKey.checkTime) < 0);
    }
  }

  @Test
  public void testGetLatest() {
    UUID universeUUID = UUID.randomUUID();
    HealthCheck check1 = addCheck(universeUUID);
    HealthCheck check2 = addCheck(universeUUID);
    HealthCheck latest = HealthCheck.getLatest(universeUUID);
    assertNotNull(latest);
    assertEquals(latest.idKey.checkTime, check2.idKey.checkTime);
  }

  @Test
  public void testHasError() {
    UUID universeUUID = UUID.randomUUID();
    HealthCheck noDetails = addCheck(universeUUID);
    assertFalse(noDetails.hasError());

    HealthCheck trueError = addCheck(universeUUID, "{\"" + "has_error" + "\": true}");
    assertTrue(trueError.hasError());

    HealthCheck falseError = addCheck(universeUUID, "{\"" + "has_error" + "\": false}");
    assertFalse(falseError.hasError());
  }

  @Test(expected = RuntimeException.class)
  public void testInvalidDetailsJson() {
    HealthCheck shouldThrow = addCheck(UUID.randomUUID(), "invalid_json");
  }
}
