/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.feedserver.util;

import com.google.feedserver.util.XmlUtil;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

/**
 * @author abhinavk@google.com (Abhinav Khandelwal)
 * 
 */
public class XmlUtilTest extends TestCase {


  XmlUtil xmlUtil;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    xmlUtil = new XmlUtil();
  }

  public void testSimpleXml() throws SAXException, IOException, ParserConfigurationException {
    String xmlString = "<entity><a>first</a><b>second</b><c>third</c><z></z></entity>";
    Map<String, Object> props = xmlUtil.convertXmlToProperties(xmlString);
    assertEquals(4, props.size());
    assertEquals("first", props.get("a"));
    assertEquals("second", props.get("b"));
    assertEquals("third", props.get("c"));
    assertEquals(null, props.get("z"));
  }

  public void testXmlWithRepeatedValues() throws SAXException, IOException,
      ParserConfigurationException {
    String xmlString =
        "<entity><a>first</a>" + "<b repeatable=\"true\">second0</b><b>second1</b><b>second2</b>"
            + "<c>third</c></entity>";
    Map<String, Object> props = xmlUtil.convertXmlToProperties(xmlString);
    assertEquals(3, props.size());
    assertEquals("first", props.get("a"));
    assertEquals("third", props.get("c"));
    assertTrue(props.get("b").getClass().isArray());
    Object[] bValues = (Object[]) props.get("b");
    assertEquals(3, bValues.length);
    assertEquals("second0", bValues[0]);
    assertEquals("second1", bValues[1]);
    assertEquals("second2", bValues[2]);
  }

  @SuppressWarnings("unchecked")
  public void testXmlWithNestedElement() throws SAXException, IOException,
      ParserConfigurationException {
    String xmlString =
        "<entity><a>first</a><b>second</b>" + "<d><c>third</c><e>forth</e><z></z></d>"
            + "<z></z></entity>";
    Map<String, Object> props = xmlUtil.convertXmlToProperties(xmlString);
    assertEquals(4, props.size());
    assertEquals("first", props.get("a"));
    assertEquals("second", props.get("b"));
    assertEquals(null, props.get("z"));
    Map<String, Object> dValue = (Map<String, Object>) props.get("d");
    assertEquals(3, dValue.size());
    assertEquals("third", dValue.get("c"));
    assertEquals("forth", dValue.get("e"));
    assertEquals(null, dValue.get("z"));
  }

  @SuppressWarnings("unchecked")
  public void testXmlWithNestedElementWithRepeatedValue() throws SAXException, IOException,
      ParserConfigurationException {
    String xmlString =
        "<entity><a>first</a><b>second</b>" + "<d>" + "<c>third</c>"
            + "<e repeatable=\"true\">forth0</e><e>forth1</e>" + "<z></z>" + "</d>"
            + "<z></z></entity>";
    Map<String, Object> props = xmlUtil.convertXmlToProperties(xmlString);
    assertEquals(4, props.size());
    assertEquals("first", props.get("a"));
    assertEquals("second", props.get("b"));
    assertEquals(null, props.get("z"));
    Map<String, Object> dValue = (Map<String, Object>) props.get("d");
    assertEquals(3, dValue.size());
    assertEquals("third", dValue.get("c"));
    assertEquals(null, dValue.get("z"));
    Object[] eValues = (Object[]) dValue.get("e");
    assertEquals(2, eValues.length);
    assertEquals("forth0", eValues[0]);
    assertEquals("forth1", eValues[1]);
  }

  @SuppressWarnings("unchecked")
  public void testXmlWithNesting2() throws SAXException, IOException, ParserConfigurationException {
    String xmlString =
        "<entity><a>first</a><b>second</b>" + "<d repeatable=\"true\">" + "<b>third</b>"
            + "<e repeatable=\"true\">forth0</e><e>forth1</e>" + "<z></z>" + "</d>"
            + "<z></z></entity>";
    Map<String, Object> props = xmlUtil.convertXmlToProperties(xmlString);
    assertEquals(4, props.size());
    assertEquals("first", props.get("a"));
    assertEquals("second", props.get("b"));
    assertEquals(null, props.get("z"));
    Map<String, Object>[] d = (Map<String, Object>[]) props.get("d");
    assertEquals(1, d.length);
    Map<String, Object> dValue = d[0];
    assertEquals(3, dValue.size());
    assertEquals("third", dValue.get("b"));
    assertEquals(null, dValue.get("z"));
    Object[] eValues = (Object[]) dValue.get("e");
    assertEquals(2, eValues.length);
    assertEquals("forth0", eValues[0]);
    assertEquals("forth1", eValues[1]);
  }

  @SuppressWarnings("unchecked")
  public void testXmlWithNesting3() throws SAXException, IOException, ParserConfigurationException {
    String xmlString =
        "<entity><a>first</a><b>second</b>" + "<d repeatable=\"true\">" + "<d>third</d>"
            + "<e repeatable=\"true\">forth0</e>" + "<e>forth1</e>" + "<z></z>" + "</d>"
            + "<z></z></entity>";
    Map<String, Object> props = xmlUtil.convertXmlToProperties(xmlString);
    assertEquals(4, props.size());
    assertEquals("first", props.get("a"));
    assertEquals("second", props.get("b"));
    assertEquals(null, props.get("z"));
    Map<String, Object>[] d = (Map<String, Object>[]) props.get("d");
    assertEquals(1, d.length);
    Map<String, Object> dValue = d[0];
    assertEquals(3, dValue.size());
    assertEquals("third", dValue.get("d"));
    assertEquals(null, dValue.get("z"));
    Object[] eValues = (Object[]) dValue.get("e");
    assertEquals(2, eValues.length);
    assertEquals("forth0", eValues[0]);
    assertEquals("forth1", eValues[1]);
  }
}