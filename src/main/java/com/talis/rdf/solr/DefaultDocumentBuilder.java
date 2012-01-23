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

import static com.talis.rdf.solr.FieldNames.*;
import static org.apache.commons.lang.Validate.notNull;
import static org.apache.commons.lang.Validate.isTrue;

import java.util.Collection;
import java.util.Date;

import org.apache.lucene.document.DateTools;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.vocabulary.RDF;

public class DefaultDocumentBuilder implements SolrDocumentBuilder{

	private final static Logger LOG = LoggerFactory.getLogger(DefaultDocumentBuilder.class);
	
	@Override
	public SolrInputDocument getDocument(String key, Collection<Quad> quads) {
//		String[] keyComponents = key.split(" ");
//		isTrue(keyComponents.length == 2, "Invalid document key");
//		String graph = keyComponents[0];
//		String subject = keyComponents[1]; 
		LOG.debug("Creating SolrInputDocument with key {}", key);
		notNull(key, "Key cannot be null.");
		notNull(quads, "Quads cannot be null.");
		
		SolrInputDocument doc = new SolrInputDocument();
		doc.setField(INDEX_DATE,DateTools.dateToString(new Date(), DateTools.Resolution.SECOND));
		doc.setField(DOCUMENT_KEY, key);
		doc.setField(SUBJECT_URI, key);
		
		for (Quad quad : quads) {
			LOG.debug("Processing quad {}", quad);
			
			doc.addField(GRAPH_URI, quad.getGraph().getURI());
			
//			isTrue(quad.getGraph().getURI().equals(graph), "Graph URI not consistent with key");
			isTrue(quad.getSubject().getURI().equals(key), "Subject URI not consistent with key");
			
			if (quad.getPredicate().getURI().equals(RDF.type.getURI())){
				doc.addField(CLASS, quad.getObject().getURI());
				doc.addField(CLASS_NS, quad.getObject().getNameSpace());
			}
			
			doc.addField(PROPERTY, quad.getPredicate().getURI());
			doc.addField(PROPERTY_NS, quad.getPredicate().getNameSpace());
			
			Node object = quad.getObject();
			if ( object.isLiteral() ) {
				doc.addField(quad.getPredicate().getURI(), object.getLiteralValue());
			}
		}
		return doc;
	}

//	public String documentKeyFor(String graphUri, String subjectUri) {
//		return graphUri + " " + subjectUri;
//	}

}
