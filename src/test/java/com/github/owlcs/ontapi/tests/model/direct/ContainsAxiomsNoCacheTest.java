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

package com.github.owlcs.ontapi.tests.model.direct;

import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.config.CacheSettings;
import com.github.owlcs.ontapi.config.OntConfig;
import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.tests.ModelData;
import com.github.owlcs.ontapi.tests.model.ContainsAxiomsTest;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.SKOS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.model.*;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * To test {@link com.github.owlcs.ontapi.internal.DirectObjectMapImpl} for {@link org.semanticweb.owlapi.model.OWLAxiom}.
 * Created by @ssz on 22.09.2020.
 */
public class ContainsAxiomsNoCacheTest extends ContainsAxiomsTest {

    public static Stream<Arguments> data() {
        return Stream.of(
                of(ModelData.PIZZA, OWLDeclarationAxiom.class,
                        (f, d) -> f.getOWLDeclarationAxiom(f.getOWLObjectProperty(d.getNS() + "hasCountryOfOrigin")))
                , of(ModelData.PIZZA, OWLSubClassOfAxiom.class, (f, d) -> {
                    OWLClass sub = f.getOWLClass(d.getNS() + "PizzaTopping");
                    OWLClass sup = f.getOWLClass(d.getNS() + "Food");
                    return f.getOWLSubClassOfAxiom(sub, sup);
                })
                , of(ModelData.TRAVEL, OWLDifferentIndividualsAxiom.class, (f, d) -> {
                    OWLNamedIndividual i1 = f.getOWLNamedIndividual(d.getNS() + "ThreeStarRating");
                    OWLNamedIndividual i2 = f.getOWLNamedIndividual(d.getNS() + "TwoStarRating");
                    return f.getOWLDifferentIndividualsAxiom(i1, i2);
                })
                , of(ModelData.PIZZA, OWLObjectPropertyRangeAxiom.class, (f, d) -> {
                    OWLObjectProperty property = f.getOWLObjectProperty(d.getNS() + "hasIngredient");
                    OWLClass clazz = f.getOWLClass(d.getNS() + "Food");
                    return f.getOWLObjectPropertyRangeAxiom(property, clazz);
                })
                , of(ModelData.FAMILY, OWLDataPropertyRangeAxiom.class, (f, d) -> {
                    OWLDataProperty p = f.getOWLDataProperty(d.getNS() + "hasMarriageYear");
                    OWLDatatype r = f.getIntegerOWLDatatype();
                    return f.getOWLDataPropertyRangeAxiom(p, r);
                })
                , of(ModelData.PIZZA, OWLObjectPropertyDomainAxiom.class, (f, d) -> {
                    OWLObjectProperty property = f.getOWLObjectProperty(d.getNS() + "hasIngredient");
                    OWLClass clazz = f.getOWLClass(d.getNS() + "Food");
                    return f.getOWLObjectPropertyDomainAxiom(property, clazz);
                })
                , of(ModelData.FAMILY, OWLDataPropertyDomainAxiom.class, (f, d) -> {
                    OWLDataProperty p = f.getOWLDataProperty(d.getNS() + "hasMarriageYear");
                    OWLClass c = f.getOWLClass(d.getNS() + "Marriage");
                    return f.getOWLDataPropertyDomainAxiom(p, c);
                })
                , of(ModelData.PIZZA, OWLSubObjectPropertyOfAxiom.class, (f, d) -> {
                    OWLObjectProperty sub = f.getOWLObjectProperty(d.getNS() + "isBaseOf");
                    OWLObjectProperty sup = f.getOWLObjectProperty(d.getNS() + "isIngredientOf");
                    return f.getOWLSubObjectPropertyOfAxiom(sub, sup);
                })
                , of(ModelData.FAMILY, OWLSubDataPropertyOfAxiom.class, (f, d) -> {
                    OWLDataProperty sub = f.getOWLDataProperty(d.getNS() + "formerlyKnownAs");
                    OWLDataProperty sup = f.getOWLDataProperty(d.getNS() + "knownAs");
                    return f.getOWLSubDataPropertyOfAxiom(sub, sup);
                })
                , of(ModelData.NCBITAXON_CUT, OWLSubAnnotationPropertyOfAxiom.class, (f, d) -> {
                    OWLAnnotationProperty sub = f.getOWLAnnotationProperty(SKOS.editorialNote.getURI());
                    OWLAnnotationProperty sup = f.getOWLAnnotationProperty(SKOS.note.getURI());
                    return f.getOWLSubAnnotationPropertyOfAxiom(sub, sup);
                })
        );
    }

    private static <X> Arguments of(ModelData data, Class<X> type, BiFunction<OWLDataFactory, ModelData, X> get) {
        return Arguments.of(data, type, (Function<OWLDataFactory, X>) f -> get.apply(f, data));
    }

    @Override
    protected OWLOntologyManager newManager() {
        OntologyManager m = OntManagers.createManager();
        OntConfig conf = m.getOntologyConfigurator().setModelCacheLevel(CacheSettings.CACHE_CONTENT, false);
        Assertions.assertFalse(conf.useContentCache());
        return m;
    }

    @ParameterizedTest
    @EnumSource(value = ModelData.class, names = {"PIZZA", "FAMILY", "CAMERA", "TRAVEL"})
    public void testContainsAxioms(ModelData data) {
        super.testContainsAxioms(data);
    }

    @ParameterizedTest(name = "[{index}] ::: type={1}, model={0}")
    @MethodSource("data")
    public <X extends OWLAxiom> void testAxiomTranslator(ModelData data, Class<X> type, Function<OWLDataFactory, X> get) {
        OWLOntologyManager m = newManager();
        OWLDataFactory df = m.getOWLDataFactory();
        AxiomTranslator<X> tr = AxiomParserProvider.get(type);
        Assertions.assertNotNull(tr);

        X key = get.apply(df);

        OntModel ont = ((Ontology) data.fetch(m)).asGraphModel();
        ONTObjectFactory f = AxiomTranslator.getObjectFactory(ont);
        InternalConfig c = AxiomTranslator.getConfig(ont);
        Optional<ONTObject<X>> ax = tr.findONTObject(key, ont, f, c);
        Assertions.assertTrue(ax.isPresent());
        Assertions.assertTrue(tr.containsONTObject(key, ont, f, c));

        ont.remove(ModelFactory.createModelForGraph(ax.get().toGraph()));

        Assertions.assertFalse(tr.containsONTObject(key, ont, f, c));
        Assertions.assertFalse(tr.findONTObject(key, ont, f, c).isPresent());
    }
}
