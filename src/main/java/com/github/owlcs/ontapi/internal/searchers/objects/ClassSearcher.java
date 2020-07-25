/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.internal.searchers.objects;

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.AxiomTranslator;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.internal.OWLComponentType;
import com.github.owlcs.ontapi.internal.ObjectsSearcher;
import com.github.owlcs.ontapi.internal.searchers.axioms.ByClass;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;

import java.util.Set;

/**
 * An {@link ObjectsSearcher} that retrieves {@link OWLClass OWL-API Class}es.
 * Created by @ssz on 19.04.2020.
 */
public class ClassSearcher extends WithBuiltins<OWLClass> {
    private static final Set<AxiomTranslator<OWLAxiom>> TRANSLATORS = selectTranslators(OWLComponentType.CLASS);

    @Override
    protected Resource getEntityType() {
        return OWL.Class;
    }

    @Override
    protected ExtendedIterator<? extends AxiomTranslator<OWLAxiom>> listTranslators() {
        return Iter.create(TRANSLATORS);
    }

    @Override
    protected ONTObject<OWLClass> createEntity(String uri, OntModel model, ONTObjectFactory factory) {
        return factory.getClass(OntApiException.mustNotBeNull(model.getOntClass(uri)));
    }

    @Override
    protected ONTObject<OWLClass> createEntity(String uri, ModelObjectFactory factory) {
        return factory.getClass(uri);
    }

    @Override
    protected boolean containsEntity(String uri, OntModel m, AxiomsSettings conf) {
        Resource clazz = toResource(m, uri);
        if (containsBuiltin(m, clazz)) {
            if (OWL.Thing.equals(clazz)) {
                if (containsAxiom(Iter.flatMap(listImplicitStatements(m), s -> listRootStatements(m, s)), conf)) {
                    return true;
                }
            }
            return containsInAxiom(clazz, m, conf);
        }
        return containsDeclaration(clazz, m, conf);
    }

    @Override
    protected ExtendedIterator<String> listEntities(OntModel m, AxiomsSettings conf) {
        Set<String> builtins = getBuiltins(m, conf);
        if (!builtins.contains(OWL.Thing.getURI())) {
            if (containsAxiom(Iter.flatMap(listImplicitStatements(m), s -> listRootStatements(m, s)), conf)) {
                builtins.add(OWL.Thing.getURI());
            }
        }
        return listEntities(m, builtins, conf);
    }

    protected ExtendedIterator<OntStatement> listImplicitStatements(OntModel m) {
        return Iter.flatMap(Iter.of(OWL.cardinality, OWL.maxCardinality, OWL.minCardinality), p -> listByPredicate(m, p))
                .filterKeep(this::isCardinalityRestriction);
    }

    protected boolean isCardinalityRestriction(OntStatement s) {
        return ByClass.OBJECT_CARDINALITY_TYPES.stream().anyMatch(t -> s.getSubject().canAs(t));
    }

    @Override
    protected Set<Node> getBuiltins(OntModel m) {
        return getBuiltinsVocabulary(m).getClasses();
    }
}
