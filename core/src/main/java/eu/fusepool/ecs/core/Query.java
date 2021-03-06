/*
 * Copyright 2013 Reto.
 *
 * Licensed under the Apache License; Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing; software
 * distributed under the License is distributed on an "AS IS" BASIS;
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND; either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.fusepool.ecs.core;

import java.util.Collection;
import org.apache.clerezza.rdf.core.UriRef;

/**
 *
 * @author Reto
 */
public class Query {

    final UriRef contentStoreUri;
    final UriRef contentStoreViewUri;
    final Collection<UriRef> subjects;
    final Collection<UriRef> types;
    final Collection<String> searchs;
    final Integer items;
    final Integer offset;
    final Integer maxFacets;
    final boolean withContent;

    public Query(UriRef contentStoreUri, 
            UriRef contentStoreViewUri, 
            Collection<UriRef> subjects, 
            Collection<UriRef> types, 
            Collection<String> searchs, 
            Integer items, Integer offset, Integer maxFacets, 
            boolean withContent) {
        this.contentStoreUri = contentStoreUri;
        this.contentStoreViewUri = contentStoreViewUri;
        this.subjects = subjects;
        this.types = types;
        this.searchs = searchs;
        this.items = items;
        this.offset = offset;
        this.maxFacets = maxFacets;
        this.withContent = withContent;
    }

    /**
     * This is typically the same as contentStoreViewUri but without any 
     * query parameter.
     * 
     * @return 
     */
    public UriRef getContentStoreUri() {
        return contentStoreUri;
    }

    /**
     * This is typically the requested URI but might contain some additional query
     * parameters for values the client didn't specify.
     * 
     * @return the contentStoreViewUri
     */
    public UriRef getContentStoreViewUri() {
        return contentStoreViewUri;
    }

    public Collection<UriRef> getSubjects() {
        return subjects;
    }

    public Collection<UriRef> getTypes() {
        return types;
    }

    public Collection<String> getSearchs() {
        return searchs;
    }

    public Integer getItems() {
        return items;
    }

    public Integer getOffset() {
        return offset;
    }

    public Integer getMaxFacets() {
        return maxFacets;
    }

    public boolean isWithContent() {
        return withContent;
    }
    
    
}
