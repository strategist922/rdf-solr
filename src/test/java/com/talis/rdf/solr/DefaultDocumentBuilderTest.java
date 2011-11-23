/*
 *    Copyright 2011 Talis Systems Ltd
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.talis.rdf.solr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.vocabulary.RDF;

public class DefaultDocumentBuilderTest {

	private static final String SUBJECT_URI = "http://example.com/subject1";
	private static final String GRAPH_URI = "http://example.com/graph1";
	private static final String PREDICATE_BASE = "http://example.com/schema/predicate/";
	private static final String OBJECT_BASE = "SomeLiteralValue";
	private static final String DOCUMENT_KEY = GRAPH_URI + " " + SUBJECT_URI;
	
	private DefaultDocumentBuilder quadsToDoc;
	
	@Before
	public void setUp() {
		quadsToDoc = new DefaultDocumentBuilder();
	}
	
	@Test
	public void getDocumentAddsDefaultFields() {
		SolrInputDocument doc = quadsToDoc.getDocument(DOCUMENT_KEY, new ArrayList<Quad>());
		assertNotNull(doc);
		assertEquals(GRAPH_URI + " " + SUBJECT_URI, doc.getField(FieldNames.DOCUMENT_KEY).getValue());
		assertEquals(SUBJECT_URI, doc.getField(FieldNames.SUBJECT_URI).getValue());
		assertEquals(GRAPH_URI, doc.getField(FieldNames.GRAPH_URI).getValue());
		assertNotNull(doc.getField(FieldNames.INDEX_DATE).getValue());
		assertEquals(4, doc.size());
	}

	@Test
	public void getDocumentAddsLiteralFieldsForDistinctPredicates() {
		ArrayList<Quad> quads = new ArrayList<Quad>();
		int NUMBER_OF_QUADS = 10;
		for (int i = 0; i < NUMBER_OF_QUADS ; i++) {
			quads.add(new Quad(
				Node.createURI(GRAPH_URI), 
				Node.createURI(SUBJECT_URI), 
				Node.createURI(PREDICATE_BASE + i), 
				Node.createLiteral(OBJECT_BASE + i)));
		}
		
		SolrInputDocument doc = quadsToDoc.getDocument(DOCUMENT_KEY, quads);
		assertNotNull(doc);
		for (int i = 0; i < NUMBER_OF_QUADS ; i++) {
			assertEquals(OBJECT_BASE + i, doc.getField(PREDICATE_BASE + i).getValue());
		}
	}
	
	@Test
	public void getDocumentAddsLiteralFieldsForMultipleValuesOfSamePredicate() {

		ArrayList<Quad> quads = new ArrayList<Quad>();
		int NUMBER_OF_QUADS = 10;
		for (int i = 0; i < NUMBER_OF_QUADS ; i++) {
			quads.add(new Quad(
				Node.createURI(GRAPH_URI), 
				Node.createURI(SUBJECT_URI), 
				Node.createURI(PREDICATE_BASE), 
				Node.createLiteral(OBJECT_BASE + i)));
		}
		
		SolrInputDocument doc = quadsToDoc.getDocument(DOCUMENT_KEY, quads);
		assertNotNull(doc);
		
		SolrInputField field = doc.getField(PREDICATE_BASE);
		assertEquals(NUMBER_OF_QUADS, field.getValueCount());
		Collection<Object> values = field.getValues();
		for (int i = 0; i < NUMBER_OF_QUADS ; i++) {
			String expected = OBJECT_BASE + i;
			assertTrue(values.contains(expected));
		}
		
	}

	@Test (expected=IllegalArgumentException.class)
	public void keyMustContainGraphAndSubjectUriComponents() {
		quadsToDoc.getDocument("THIS_IS_NOT_A_VALID_KEY", new ArrayList<Quad>());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void keyMustContainOnlyGraphAndSubjectUriComponents() {
		quadsToDoc.getDocument("THIS IS ALSO NOT A VALID KEY", new ArrayList<Quad>());
	}

	@Test (expected=IllegalArgumentException.class)
	public void quadsMustNotBeNull() {
		quadsToDoc.getDocument(DOCUMENT_KEY, null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void inconsistentGraphUriThrowsException() {
		ArrayList<Quad> quads = new ArrayList<Quad>();
		quads.add(new Quad(
			Node.createURI(GRAPH_URI), 
			Node.createURI(SUBJECT_URI), 
			Node.createURI(PREDICATE_BASE), 
			Node.createLiteral(OBJECT_BASE)));
		String otherKey = "http://someothergraph.com" + " " +  SUBJECT_URI;
		quadsToDoc.getDocument(otherKey, quads);
	}

	@Test (expected=IllegalArgumentException.class)
	public void inconsistentSubjectUriThrowsException() {
		ArrayList<Quad> quads = new ArrayList<Quad>();
		quads.add(new Quad(
			Node.createURI(GRAPH_URI), 
			Node.createURI(SUBJECT_URI), 
			Node.createURI(PREDICATE_BASE), 
			Node.createLiteral(OBJECT_BASE)));
		String otherKey = GRAPH_URI + " " + "http://someothersubject.com"; 
		quadsToDoc.getDocument(otherKey, quads);
	}
	
	@Test
	public void statisticsFieldsForPropertiesAreAddedToDocument(){
		String firstPredicate = PREDICATE_BASE + "first";
		String secondPredicate = PREDICATE_BASE + "second";
		ArrayList<Quad> quads = new ArrayList<Quad>();
		quads.add(new Quad(
			Node.createURI(GRAPH_URI), 
			Node.createURI(SUBJECT_URI), 
			Node.createURI(firstPredicate), 
			Node.createURI("http://example.com/resources/0")));
		quads.add(new Quad(
				Node.createURI(GRAPH_URI), 
				Node.createURI(SUBJECT_URI), 
				Node.createURI(secondPredicate), 
				Node.createURI("http://example.com/resources/1")));
		
		SolrInputDocument doc = quadsToDoc.getDocument(DOCUMENT_KEY, quads);
		
		SolrInputField propertyField = doc.getField(FieldNames.PROPERTY);
		assertTrue(propertyField.getValues().contains(firstPredicate));
		assertTrue(propertyField.getValues().contains(secondPredicate));
		assertEquals(2, propertyField.getValues().size());

		SolrInputField namespaceField = doc.getField(FieldNames.PROPERTY_NS);
		assertTrue(namespaceField.getValues().contains(PREDICATE_BASE));
		assertEquals(2, namespaceField.getValues().size());
	}
	
	@Test
	public void addNamespaceFieldsForNonXmlCompliantPredicateUris(){
		String firstPredicate = PREDICATE_BASE + "0";
		String secondPredicate = PREDICATE_BASE + "1";
		ArrayList<Quad> quads = new ArrayList<Quad>();
		quads.add(new Quad(
			Node.createURI(GRAPH_URI), 
			Node.createURI(SUBJECT_URI), 
			Node.createURI(firstPredicate), 
			Node.createURI("http://example.com/resources/0")));
		quads.add(new Quad(
				Node.createURI(GRAPH_URI), 
				Node.createURI(SUBJECT_URI), 
				Node.createURI(secondPredicate), 
				Node.createURI("http://example.com/resources/1")));
		
		SolrInputDocument doc = quadsToDoc.getDocument(DOCUMENT_KEY, quads);
		
		SolrInputField propertyField = doc.getField(FieldNames.PROPERTY);
		assertTrue(propertyField.getValues().contains(firstPredicate));
		assertTrue(propertyField.getValues().contains(secondPredicate));
		assertEquals(2, propertyField.getValues().size());

		SolrInputField namespaceField = doc.getField(FieldNames.PROPERTY_NS);
		assertTrue(namespaceField.getValues().contains(firstPredicate));
		assertTrue(namespaceField.getValues().contains(secondPredicate));
		assertEquals(2, namespaceField.getValues().size());
	}

	@Test
	public void statisticsFieldsForClassesAreAddedToDocument(){
		String firstClass = PREDICATE_BASE + "first";
		String secondClass = PREDICATE_BASE + "second";
		ArrayList<Quad> quads = new ArrayList<Quad>();
		quads.add(new Quad(
			Node.createURI(GRAPH_URI), 
			Node.createURI(SUBJECT_URI), 
			Node.createURI(RDF.type.getURI()), 
			Node.createURI(firstClass)));
		quads.add(new Quad(
				Node.createURI(GRAPH_URI), 
				Node.createURI(SUBJECT_URI), 
				Node.createURI(RDF.type.getURI()), 
				Node.createURI(secondClass)));
		
		SolrInputDocument doc = quadsToDoc.getDocument(DOCUMENT_KEY, quads);
		
		SolrInputField propertyField = doc.getField(FieldNames.CLASS);
		assertTrue(propertyField.getValues().contains(firstClass));
		assertTrue(propertyField.getValues().contains(secondClass));
		assertEquals(2, propertyField.getValues().size());

		SolrInputField namespaceField = doc.getField(FieldNames.CLASS_NS);
		assertTrue(namespaceField.getValues().contains(PREDICATE_BASE));
		assertEquals(2, namespaceField.getValues().size());
	}
	
	@Test
	public void addNamespaceFieldsForNonXmlCompliantClassUris(){
		String firstClass = PREDICATE_BASE + "0";
		String secondClass = PREDICATE_BASE + "1";
		ArrayList<Quad> quads = new ArrayList<Quad>();
		quads.add(new Quad(
			Node.createURI(GRAPH_URI), 
			Node.createURI(SUBJECT_URI), 
			Node.createURI(RDF.type.getURI()), 
			Node.createURI(firstClass)));
		quads.add(new Quad(
				Node.createURI(GRAPH_URI), 
				Node.createURI(SUBJECT_URI), 
				Node.createURI(RDF.type.getURI()), 
				Node.createURI(secondClass)));
		
		SolrInputDocument doc = quadsToDoc.getDocument(DOCUMENT_KEY, quads);
		
		SolrInputField propertyField = doc.getField(FieldNames.CLASS);
		assertTrue(propertyField.getValues().contains(firstClass));
		assertTrue(propertyField.getValues().contains(secondClass));
		assertEquals(2, propertyField.getValues().size());

		SolrInputField namespaceField = doc.getField(FieldNames.CLASS_NS);
		assertTrue(namespaceField.getValues().contains(firstClass));
		assertTrue(namespaceField.getValues().contains(secondClass));
		assertEquals(2, namespaceField.getValues().size());
	}

	@Test
	public void literalValuesAreIndexedByPredicateUri() {
		String predicateURI = PREDICATE_BASE + "first";
		ArrayList<Quad> quads = new ArrayList<Quad>();
		quads.add(new Quad(
			Node.createURI(GRAPH_URI), 
			Node.createURI(SUBJECT_URI), 
			Node.createURI(predicateURI), 
			Node.createURI("http://example.com/resource")));
		quads.add(new Quad(
				Node.createURI(GRAPH_URI), 
				Node.createURI(SUBJECT_URI), 
				Node.createURI(predicateURI), 
				Node.createLiteral("Aloha")));
		SolrInputDocument doc = quadsToDoc.getDocument(DOCUMENT_KEY, quads);
		SolrInputField field = doc.getField(predicateURI);
		assertEquals("Aloha", field.getFirstValue());
		assertEquals(1, field.getValues().size());
	}

	
}
