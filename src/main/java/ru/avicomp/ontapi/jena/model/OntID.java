/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.jena.model;

import ru.avicomp.ontapi.OntApiException;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Interface encapsulating an Ontology Identifier.
 * Each {@link OntGraphModel OWL2 Obtology} must have one and only one {@link OntID Ontology ID} inside.
 * <p>
 * Please note: the methods of this interface do not affect the hierarchical structure of the graph
 * to which this resource is attached, they only affect the structure of the graph itself.
 * In other words, calling the methods {@link #removeImport(String)} does not remove the sub-graph
 * from the main {@link ru.avicomp.ontapi.jena.UnionGraph Union Graph}.
 * Similar, calling the method {@link #addImport(String)} simply adds the corresponding triple to the base graph.
 * <p>
 * Created by szuev on 09.11.2016.
 *
 * @see <a href='https://www.w3.org/TR/owl-syntax/#Ontology_IRI_and_Version_IRI'>3.1 Ontology IRI and Version IRI</a>
 */
public interface OntID extends OntObject {

    /**
     * Returns an IRI from the right side of {@code this owl:versionIRI IRI} statement.
     *
     * @return String IRI or {@code null}
     */
    String getVersionIRI();

    /**
     * Assigns a new version IRI to this Ontology ID object.
     * A {@code null} argument means that current version IRI should be deleted.
     *
     * @param uri String, can be {@code null}.
     * @return this ID to allow cascading calls
     */
    OntID setVersionIRI(String uri);

    /**
     * Adds the triple {@code this owl:import uri} to this resource.
     *
     * @param uri String, not null
     * @return this id-object to allow cascading calls
     * @throws OntApiException if input is wrong
     */
    OntID addImport(String uri) throws OntApiException;

    /**
     * Removes the triple {@code this owl:import uri} from this resource.
     *
     * @param uri String, not null
     * @return this id-object to allow cascading calls
     */
    OntID removeImport(String uri);

    /**
     * Lists all {@code owl:import}s.
     *
     * @return Stream of Strings
     */
    Stream<String> imports();

    /**
     * Indicates whether the given {@link OntID Ontology ID} is equal to this one in OWL2 terms.
     * This means that the IDs must have the same IRI + version IRI pairs.
     * If the method returns {@code true}, then two ontologies can not be coexist in the same scope.
     *
     * @param other {@link OntID}
     * @return {@code true} in case the IDs are the same, otherwise {@code false}
     * @since 1.3.0
     */
    default boolean sameAs(OntID other) {
        return equals(other) && Objects.equals(getVersionIRI(), other.getVersionIRI());
    }

}
