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

import com.google.gdata.client.GoogleService;
import com.google.gdata.data.OtherContent;
import com.google.gdata.util.ServiceException;
import com.google.inject.Inject;
import com.google.feedserver.util.FeedServerClientException;
import com.google.feedserver.util.ContentUtil;
import com.google.feedserver.util.XmlUtil;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Implements a Gdata feed client that represents feeds as "typeless" maps of String->String pairs.
 * 
 * "typeless" maps are String -> List<String> representations of a FeedServer entry.  Each
 * key represents an element in the entry XML while the List<String> are the possible values.
 * For non-repeated elements, the list will only ever have one entry.  For "repeated=true" 
 * elements, the list will have N number of elements depending on how many entries exist for
 * that element.
 * 
 * This client is good for consumers of feeds who need quick access to a limited set of data
 * in the feed.  If you are using most of the data, using the "typed" java bean 
 * {@link FeedServerClient} is probably a better choice.
 * 
 * @author r@kuci.org (Ray Colline)
 */
public class TypelessFeedServerClient {
  
  private static final String NAME_ELEMENT = "name";

  // Logging instance
  private static final Logger log = Logger.getLogger(TypelessFeedServerClient.class);
  
  // Dependencies
  private GoogleService service;
  private ContentUtil contentUtil;
  private XmlUtil xmlUtil; 
  
  /**
   * Creates the client using provided dependencies.
   * 
   * @param service the configured Gdata service.
   */
  @Inject
  public TypelessFeedServerClient(GoogleService service, ContentUtil contentUtil, XmlUtil xmlUtil) {
    this.service = service;
    this.contentUtil = contentUtil;
    this.xmlUtil = xmlUtil;
  }
  
  /**
   * Creates the client by creating the dependencies.
   * 
   * @param service the configured Gdata service.
   */
  public TypelessFeedServerClient(GoogleService service) {
    this(service, new ContentUtil(), new XmlUtil());
  }
  
  /**
   * Fetches generic "payload-in-content" entry into a Map.  The returned map is a 
   * "map of objects" where each object is either 1) a String if its not repeatable or 2) an 
   * Object[] if the element is "repeatable".  Using introspection or "instanceof" one can
   * determine how to consume this map.  
   * 
   * @param feedUrl the feed URL which can contain any valid ATOM "query"
   * @return a map of objects representing the "payload-in-content" entry.
   * @throws FeedServerClientException if we cannot contact the feedserver, fetch the URL, or 
   * parse the XML.
   */
  public Map<String, Object> getEntry(URL feedUrl) throws FeedServerClientException {
    try {
      FeedServerEntry entry = service.getEntry(feedUrl, FeedServerEntry.class);
      return getTyplessMapFromEntry(entry);
    } catch (IOException e) {
      throw new FeedServerClientException("Error while fetching " + feedUrl, e);
    } catch (ServiceException e) {
      throw new FeedServerClientException(e);
    }
  }
  
  /**
   * Fetches generic "payload-in-content" entries to a Map for the given query URL.  The returned 
   * map is a "map of objects" where each object is either 1) a String if its not repeatable or 
   * 2) an Object[] if the element is "repeatable".  Using introspection or "instanceof" one can
   * determine how to consume this map.  
   * 
   * @param feedUrl the feed URL which can contain any valid ATOM "query"
   * @return a list of maps representing all the "payload-in-content" entries.
   * @throws FeedServerClientException if we cannot contact the feedserver, fetch the URL, or 
   * parse the XML.
   */
  public List<Map<String, Object>> getEntries(URL feedUrl) throws FeedServerClientException {
    // Retrieve Feed from network
    FeedServerFeed feed;
    try {
      feed = service.getFeed(feedUrl, FeedServerFeed.class);
    } catch (IOException e) {
      throw new FeedServerClientException("Error while fetching " + feedUrl, e);
    } catch (ServiceException e) {
      throw new FeedServerClientException(e);
    }
    
    // Go through all entries and build the map.
    List<Map<String, Object>> feedMap = new ArrayList<Map<String, Object>>();
    for (FeedServerEntry entry : feed.getEntries()) {
      feedMap.add(getTyplessMapFromEntry(entry));
    }
    return feedMap;
  }
  
  /**
   * Deletes entry specified by supplied URL.  This URL must include the full path.
   * 
   * @param feedUrl the full URL to the entry in this feed.
   * @throws FeedServerClientException if any communication issues occur with the feed or the
   * feed ID is invalid or malformed..
   */
  public void deleteEntry(URL feedUrl) throws FeedServerClientException {
    try {
      service.delete(feedUrl);
    } catch (IOException e) {
      throw new FeedServerClientException("Error while deleting " + feedUrl, e);
    } catch (ServiceException e) {
      throw new FeedServerClientException(e);
    }
  }
  
  /**
   * Deletes specified by "name" in supplied entry map.
   * 
   * @param baseUrl Feed url not including ID.
   * @param entry a valid entry map.
   * @throws FeedServerClientException if any communication issues occur with the feed or the
   * feed ID is invalid or malformed.
   */
  public void deleteEntry(URL baseUrl, Map<String, Object> entry) throws 
      FeedServerClientException {
    
    try {
      String name = (String) entry.get(NAME_ELEMENT);
      URL feedUrl = new URL(baseUrl.toString() + "/" + name);
      log.info("deleting entry in feed " + feedUrl);
      deleteEntry(feedUrl);
    } catch (NullPointerException e) {
      throw new RuntimeException("entry map does not have 'name' key", e);
    } catch (ClassCastException e) {
      throw new RuntimeException("entry map does not have 'name' key as String", e);
    } catch (IndexOutOfBoundsException e) {
      throw new RuntimeException("'name' in entry map is invalid.", e);
    } catch (MalformedURLException e) {
      throw new FeedServerClientException("invalid base URL", e);
    }
  }
  
  /**
   * Deletes each entry in the supplied list of entries.  This makes one request per entry.
   * 
   * @param baseUrl the feed URL not including ID.
   * @param entries a list of valid entries.
   * @throws FeedServerClientException if any communication issues occur with the feed or the
   * feed ID is invalid.
   */
  public void deleteEntries(URL baseUrl, List<Map<String, Object>> entries) throws
      FeedServerClientException {
    for (Map<String, Object> entry : entries) {
      deleteEntry(baseUrl, entry);
    }
  }
  
  /**
   * Updates the entry using the baseUrl plus the ID contained in the entry.
   * 
   * @param baseUrl feed URL without an ID.
   * @param mapEntry a "typeless" map representing a feed entry.
   * @throws FeedServerClientException if any feed communication issues occur or the URL is 
   * malformed.
   */
  public void updateEntry(URL baseUrl, Map<String, Object> mapEntry) throws 
      FeedServerClientException {
    
    try {
      String name = (String) mapEntry.get(NAME_ELEMENT);
      URL feedUrl = new URL(baseUrl.toString() + "/" + name);
      log.info("updating entry to feed " + feedUrl);
      service.update(feedUrl, getEntryFromMap(mapEntry));
    } catch (MalformedURLException e) {
      throw new RuntimeException("Invalid URL", e);
    } catch (IOException e) {
      throw new FeedServerClientException("Error while deleting " + baseUrl, e);
    } catch (ServiceException e) {
      throw new FeedServerClientException(e);
    } catch (NullPointerException e) {
      throw new RuntimeException("Invalid Entry", e); 
    } catch (ClassCastException e) {
      throw new RuntimeException("entry map does not have 'name' key as String", e);
    }
  }
  
  /**
   * Updates the entries using the baseUrl plus the ID contained in each entry.
   * 
   * @param baseUrl feed URL without an ID.
   * @param entries a list of "typeless" maps representing feed entries.
   * @throws FeedServerClientException if any feed communication issues occur or the URL is 
   * malformed.
   */
  public void updateEntries(URL baseUrl, List<Map<String, Object>> entries) 
      throws FeedServerClientException {
    for (Map<String, Object> mapEntry : entries) {
       updateEntry(baseUrl, mapEntry);      
    }
  }

  /**
   * Inserts the entry using the baseUrl provided.
   * 
   * @param baseUrl feed URL without an ID.
   * @param mapEntry a "typeless" map representing a feed entry.
   * @throws FeedServerClientException if any feed communication issues occur or the URL is 
   * malformed.
   */
  public void insertEntry(URL baseUrl, Map<String, Object> mapEntry) throws
      FeedServerClientException {
    
    try {
      String name = (String) mapEntry.get(NAME_ELEMENT);
      log.info("inserting entry to feed " + baseUrl);
      service.insert(baseUrl, getEntryFromMap(mapEntry));
    } catch (MalformedURLException e) {
      throw new RuntimeException("Invalid URL", e);
    } catch (IOException e) {
      throw new FeedServerClientException("Error while deleting " + baseUrl, e);
    } catch (ServiceException e) {
      throw new FeedServerClientException(e);
    } catch (NullPointerException e) {
      throw new RuntimeException("Invalid Entry", e); 
    } catch (ClassCastException e) {
      throw new RuntimeException("entry map does not have 'name' key as String", e);
    }
  }
  
  /**
   * Inserts the entries using the baseUrl provided.
   * 
   * @param baseUrl feed URL without an ID.
   * @param entries a list of "typeless" maps each representing a feed entry.
   * @throws FeedServerClientException if any feed communication issues occur or the URL is 
   * malformed.
   */
  public void insertEntries(URL baseUrl, List<Map<String, Object>> entries) throws
      FeedServerClientException {
    for (Map<String, Object> mapEntry : entries) {
       insertEntry(baseUrl, mapEntry);      
    }
  }
  
  /**
   * Converts raw XML representation of a feed entry into a "typeless" map.
   * 
   * @param xmlText raw XML entry.
   * @returns a "typeless" map representing an entry.
   * @throws FeedServerClientException if the Xml cannot be parsed.
   */
  public Map<String, Object> getTypelessMapFromXml(String xmlText) throws
      FeedServerClientException {
    try {
      Map<String,Object> rawEntryMap = xmlUtil.convertXmlToProperties(xmlText);
      return rawEntryMap;
    } catch (SAXException e) {
      throw new FeedServerClientException(e);
    } catch (IOException e) {
      throw new FeedServerClientException(e);
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Returns a gdata entry object populated with contents generated from a "typeless" map.
   * 
   * @param entryMap the "typeless" map to convert.
   * @return a populated gdata entry object.
   */
  private FeedServerEntry getEntryFromMap(Map<String, Object> entryMap) {
    // XMLutil expects entries in map form of string -> object.  For repeatable elements
    // the object is really an "object[]" but for single elements its a "String".  
    // This loop prepares this very hacky map representation from the passed in more sane
    // typed implementation.
    OtherContent content = contentUtil.createXmlContent(
        xmlUtil.convertPropertiesToXml(entryMap));
    FeedServerEntry entry = new FeedServerEntry(content);
    return entry;
  }
  
  /**
   * Helper function that parses entry into an entry map of with string keys and list of string
   * values.  
   * 
   * @param entry the entry to parse.
   * @return the populated map.
   * @throws FeedServerClientException if the XML parse fails.
   */
  private Map<String, Object> getTyplessMapFromEntry(FeedServerEntry entry) 
      throws FeedServerClientException {
    // Get XML and convert to primitive Object map. 
    OtherContent content = (OtherContent) entry.getContent();  
    log.info("Entry info " + content.getXml().getBlob());
    XmlUtil xmlUtil = new XmlUtil();
    Map<String, Object> rawEntryMap;
    try {
      rawEntryMap = xmlUtil.convertXmlToProperties(content.getXml().getBlob());
      return rawEntryMap;
    } catch (SAXException e) {
      throw new FeedServerClientException(e);
    } catch (IOException e) {
      throw new FeedServerClientException(e);
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }
}
