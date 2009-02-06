/* Copyright 2008 Google Inc.
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

package com.google.feedserver.client;

import com.google.feedserver.testing.FeedServerClientTestUtil;
import com.google.feedserver.util.FeedServerClientException;
import com.google.gdata.client.GoogleService;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * Tests for the {@link TypelessFeedServerClient} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class TypelessFeedServerClientTest extends TestCase {
  
  private String TEST_FEED_URL = "http://sample.com/feed";
  private String TEST_ENTRY_URL = TEST_FEED_URL + "/vehicle0";
  
  private FeedServerClientTestUtil testUtil;
  private GoogleService mockService;
  private TypelessFeedServerClient feedServerClient;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    testUtil = new FeedServerClientTestUtil();
    mockService = EasyMock.createMock(GoogleService.class);
    feedServerClient = new TypelessFeedServerClient(mockService);
  }
  
  @Override
  protected void tearDown() throws Exception {
    testUtil = null;
    mockService = null;
    feedServerClient = null;
    super.tearDown();
  }

  public void testGetEntry() throws Exception {
    // Setup
    URL testUrl = new URL(TEST_ENTRY_URL);
    EasyMock.expect(mockService.getEntry(testUrl, FeedServerEntry.class)).andReturn(
        testUtil.getVehicleEntry());
    EasyMock.replay(mockService);
    
    // Perform Test
    Map<String, Object> fetchedMap = feedServerClient.getEntry(testUrl);
    
    // Check results
    assertTrue("Maps are not equal", testUtil.isEqual(testUtil.getSampleVehicleMap(), fetchedMap));
    EasyMock.verify(mockService);
  }
  
  public void testGetEntryInvalidUrl() throws Exception {
    // Setup
    URL badUrl = new URL("http://badUrl");
    String errorMsg = "invalid URL";
    EasyMock.expect(mockService.getEntry(badUrl, FeedServerEntry.class))
        .andThrow(new IOException(errorMsg));
    EasyMock.replay(mockService);
    
    // Perform Test
    try {
      @SuppressWarnings("unused")
      Map<String, Object> fetchedMap = feedServerClient.getEntry(badUrl);
    } catch (FeedServerClientException e) {
      assertTrue(e.getCause().getMessage().equals(errorMsg));
      EasyMock.verify(mockService);
      return;
    }
    fail("Did not get FeedServerClientException");
  }
  
  public void testDeleteEntry() throws Exception {
    // Setup
    mockService.delete(new URL(TEST_ENTRY_URL));
    EasyMock.expectLastCall();
    EasyMock.replay(mockService);
    
    // Perform Test
    feedServerClient.deleteEntry(new URL(TEST_ENTRY_URL));
    
    // Verify
    EasyMock.verify(mockService);
  }
  
  public void testInsertEntry() throws Exception {
    // Setup
    mockService.insert(EasyMock.eq(new URL(TEST_FEED_URL)), FeedServerClientTestUtil.eqEntry(
        testUtil.getVehicleEntry()));
    EasyMock.expectLastCall().andReturn(testUtil.getVehicleEntry());
    EasyMock.replay(mockService);
    
    // Perform Test
    feedServerClient.insertEntry((new URL(TEST_FEED_URL)), testUtil.getSampleVehicleMap());
    
    // Verify
    EasyMock.verify(mockService);
  }
  
  public void testUpdateEntry() throws Exception {
    // Setup
    mockService.update(EasyMock.eq(new URL(TEST_ENTRY_URL)),
    	FeedServerClientTestUtil.eqEntry(testUtil.getVehicleEntry()));
    EasyMock.expectLastCall().andReturn(testUtil.getVehicleEntry());
    EasyMock.replay(mockService);
    
    // Perform Test
    feedServerClient.updateEntry(new URL(TEST_ENTRY_URL), testUtil.getSampleVehicleMap());
    
    // Verify
    EasyMock.verify(mockService);
  }
  
 public void testFillMapFromXml() throws Exception {
   Map<String, Object> map = feedServerClient.getMapFromXml(
       FeedServerClientTestUtil.ENTRY_XML);
   assertTrue(testUtil.isEqual(testUtil.getSampleVehicleMap(), map));
 }
}