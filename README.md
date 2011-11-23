A small library for building SolrInputDocuments from RDF Quads. 

Comprised of a single interface SolrDocumentBuilder, with a single getDocument method.
This takes a String key, and a Collection<Quad> and transforms it into a SolrInputDocument.

A default implementation is provided which assumes that the key is a (space delimited)
graph & subject URI pair. The default document builder indexes only literal values with
the field name being the predicate URI. 

It also adds indexes values of RDF type, and Predicate URIs into specifically named fields, 
which may be useful for analysis of vocabulary usage in a dataset.

