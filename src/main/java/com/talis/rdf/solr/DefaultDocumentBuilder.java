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
import static org.apache.commons.lang.Validate.isTrue;
import static org.apache.commons.lang.Validate.notNull;

import java.util.Date;
import java.util.HashSet;

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
	public SolrInputDocument getDocument(String subject, Iterable<Quad> quads) {
		LOG.debug("Creating SolrInputDocument for subject {}", subject);
		notNull(subject, "Subject URI cannot be null.");
		notNull(quads, "Quads cannot be null.");
		
		SolrInputDocument doc = new SolrInputDocument();
		doc.setField(INDEX_DATE,DateTools.dateToString(new Date(), DateTools.Resolution.SECOND));
		doc.setField(DOCUMENT_KEY,subject);
		doc.setField(SUBJECT_URI, subject);
		
		HashSet<String> graphUris = new HashSet<String>();
		
		for (Quad quad : quads) {
			LOG.debug("Processing quad {}", quad);
			
			graphUris.add(quad.getGraph().getURI());
			
			isTrue(quad.getSubject().getURI().equals(subject), "Subject URI not consistent with key");
			
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
		for (String graphUri : graphUris) {
			doc.addField(GRAPH_URI, graphUri);
		}
		return doc;
	}

}
