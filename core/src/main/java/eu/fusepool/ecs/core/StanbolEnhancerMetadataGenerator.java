/*
 * Copyright 2013 reto.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.fusepool.ecs.core;

import java.io.IOException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.security.TcAccessController;
import org.apache.clerezza.rdf.core.access.security.TcPermission;
import org.apache.clerezza.rdf.cris.PathVirtualProperty;
import org.apache.clerezza.rdf.cris.PropertyHolder;
import org.apache.clerezza.rdf.cris.VirtualProperty;
import org.apache.clerezza.rdf.metadata.MetaDataGenerator;
import org.apache.clerezza.rdf.ontologies.DC;
import org.apache.clerezza.rdf.ontologies.DCTERMS;
import org.apache.clerezza.rdf.ontologies.DISCOBITS;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.clerezza.rdf.ontologies.SIOC;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;
import org.apache.stanbol.enhancer.servicesapi.EnhancementException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.ByteArraySource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.osgi.service.component.ComponentContext;
import org.slf4j.LoggerFactory;

@Component
@Service(MetaDataGenerator.class)
public class StanbolEnhancerMetadataGenerator implements MetaDataGenerator {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(StanbolEnhancerMetadataGenerator.class);
    /**
     * This service allows accessing and creating persistent triple collections
     */
    @Reference
    private TcManager tcManager;
    
    @Reference
    private EnhancementJobManager enhancementJobManager;
    
    @Reference
    private ChainManager chainManager;
    
    @Reference
    private ContentItemFactory contentItemFactory;
    
    /**
     * This service allows to get entities from configures sites
     */
    @Reference
    private SiteManager siteManager;
   
    /**
     * The graph in which the enancer generated enhanceents are stored
     */
    private UriRef ENHANCEMENTS_GRAPH = new UriRef("urn:x-localhost:/ecs-collected-enhancements.graph");
    
    protected void activate(ComponentContext context) {
        log.info("Enhanced Content Store being activated");
        try {            
            tcManager.createMGraph(ENHANCEMENTS_GRAPH);
            //now make sure everybody can read from the graph
            //or more precisly, anybody who can read the content-graph
            TcAccessController tca = new TcAccessController(tcManager);
            tca.setRequiredReadPermissions(ENHANCEMENTS_GRAPH,
                    Collections.singleton((Permission) new TcPermission(
                    "urn:x-localinstance:/content.graph", "read")));
        } catch (EntityAlreadyExistsException ex) {
            log.debug("The graph for the request log already exists");
        }

    }
    
    private MGraph getEnhancementGraph() {
        return tcManager.getMGraph(ENHANCEMENTS_GRAPH);
    }
    
    public void generate(GraphNode node, byte[] data, MediaType mediaType) {
        System.out.println("generating metadata");
        try {
            final ContentSource contentSource = new ByteArraySource(
                    data, mediaType.toString());
            final ContentItem contentItem = contentItemFactory.createContentItem(
                    (UriRef) node.getNode(), contentSource);
            final String chainName = "default";
            final Chain chain = chainManager.getChain(chainName);
            if (chain == null) {
                throw new RuntimeException("No chain by that name: " + chainName);
            }
            enhancementJobManager.enhanceContent(contentItem, chain);
            Blob textBlob =
                    ContentItemHelper.getBlob(contentItem,
                    Collections.singleton("text/plain")).getValue();
            String content = ContentItemHelper.getText(textBlob);
            node.addPropertyValue(SIOC.content, content);
            addSubjects(node, contentItem.getMetadata());
            getEnhancementGraph().addAll(contentItem.getMetadata());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (EnhancementException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void addSubjects(GraphNode node, TripleCollection metadata) {
        final GraphNode enhancementType 
                = new GraphNode(TechnicalClasses.ENHANCER_ENHANCEMENT, metadata);
        final Set<UriRef> entities = new HashSet<UriRef>();
        final Iterator<GraphNode> enhancements = enhancementType.getSubjectNodes(RDF.type);
        while (enhancements.hasNext()) {
            final GraphNode enhhancement = enhancements.next();
            final Iterator<Resource> referencedEntities = enhhancement.getObjects(Properties.ENHANCER_ENTITY_REFERENCE);
            while (referencedEntities.hasNext()) {
                final UriRef entity = (UriRef) referencedEntities.next();
                node.addProperty(DC.subject, entity);
                entities.add(entity);
            }
        }
        for (UriRef uriRef : entities) {
            //We don't get the entity description directly from metadat
            //as the context there would include
            addResourceDescription(uriRef, (MGraph) node.getGraph());
        }
    }
    
        /**
     * Add the description of a serviceUri to the specified MGraph using SiteManager.
     * The description includes the metadata provided by the SiteManager.
     * 
     */
    private void addResourceDescription(UriRef iri, MGraph mGraph) {
        final Entity entity = siteManager.getEntity(iri.getUnicodeString());
        if (entity != null) {
            final RdfValueFactory valueFactory = new RdfValueFactory(mGraph);
            final Representation representation = entity.getRepresentation();
            if (representation != null) {
                valueFactory.toRdfRepresentation(representation);
            }
        }
    }

}
