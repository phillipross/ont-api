/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.tests.jena;

import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.utils.Models;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * To test {@link Models} and {@link OntModels} utilities.
 * <p>
 * Created by @szuev on 25.04.2018.
 */
public class ModelUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelUtilsTest.class);

    @Test
    public void testDeleteResources() {
        OntModel m = OntModelFactory.createModel()
                .read(ModelUtilsTest.class.getResourceAsStream("/ontapi/recursive-graph.ttl"), null, "ttl");
        String ns = m.getID().getURI() + "#";
        OntObject d = m.createDisjointClasses(
                m.createOntClass(ns + "CL1"),
                m.createOntClass(ns + "CL2"),
                m.createObjectUnionOf(Arrays.asList(
                        m.createOntClass(ns + "CL4"),
                        m.createOntClass(ns + "CL5"),
                        m.createOntClass(ns + "CL6"))),
                m.createOntClass(ns + "CL3"));

        ReadWriteUtils.print(m);
        Assert.assertEquals(40, m.localStatements().count());

        Resource r = m.statements(null, RDFS.subClassOf, null)
                .map(Statement::getObject)
                .filter(RDFNode::isAnon)
                .map(RDFNode::asResource)
                .filter(s -> s.hasProperty(OWL.someValuesFrom))
                .findFirst().orElseThrow(IllegalStateException::new);

        LOGGER.debug("Delete {}", r);
        Models.deleteAll(r);
        LOGGER.debug("Delete {}", d);
        Models.deleteAll(d);
        List<OntClass> classes = m.classes()
                .filter(s -> s.getLocalName().contains("CL"))
                .collect(Collectors.toList());
        classes.forEach(c -> {
            LOGGER.debug("Delete {}", c);
            Models.deleteAll(c);
        });

        LOGGER.debug("---------------");
        ReadWriteUtils.print(m);
        Assert.assertEquals(10, m.statements().count());
    }

    @Test
    public void testListLangValues() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntID id = m.getID()
                .addVersionInfo("lab1")
                .addVersionInfo("lab1", "e1")
                .addVersionInfo("lab2", "e2")
                .addVersionInfo("lab3", "language3")
                .addVersionInfo("lab4", "e2")
                .addVersionInfo("lab5", "e2")
                .addVersionInfo("lab5");
        ReadWriteUtils.print(m);
        Property p = OWL.versionInfo;
        Assert.assertEquals(2, Models.langValues(id, p, null).count());
        Assert.assertEquals(3, Models.langValues(id, p, "e2").count());
        Assert.assertEquals(1, Models.langValues(id, p, "language3").count());
        Assert.assertEquals(7, m.listObjectsOfProperty(id, p).toSet().size());
    }

    @Test
    public void testInsertModel() {
        OntModel a1 = OntModelFactory.createModel().setID("http://a").getModel();
        OntModel a2 = OntModelFactory.createModel().setID("http://a").getModel();
        OntClass c1 = a1.createOntClass("http://a#Class-a1");
        OntClass c2 = a2.createOntClass("http://a#Class-a2");

        // collection depending on a1
        OntModel m1 = OntModelFactory.createModel().setID("http://m1").getModel().addImport(a1);
        OntModel m2 = OntModelFactory.createModel().setID("http://m2").getModel().addImport(a1);
        Assert.assertTrue(ModelFactory.createModelForGraph(m1.getGraph()).containsResource(c1));
        Assert.assertFalse(ModelFactory.createModelForGraph(m1.getGraph()).containsResource(c2));
        Assert.assertTrue(ModelFactory.createModelForGraph(m2.getGraph()).containsResource(c1));
        Assert.assertFalse(ModelFactory.createModelForGraph(m2.getGraph()).containsResource(c2));

        OntModels.insert(() -> Stream.of(m1, m2), a2, true);
        Assert.assertTrue(ModelFactory.createModelForGraph(m1.getGraph()).containsResource(c2));
        Assert.assertFalse(ModelFactory.createModelForGraph(m1.getGraph()).containsResource(c1));
        Assert.assertTrue(ModelFactory.createModelForGraph(m2.getGraph()).containsResource(c2));
        Assert.assertFalse(ModelFactory.createModelForGraph(m2.getGraph()).containsResource(c1));
    }

    @Test
    public void testMiscModelsFunctionality() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass a = m.createOntClass("A");
        OntClass b = m.createOntClass("B");
        Resource t = m.getResource("type");
        RDFList list = Models.createTypedList(m, t, Arrays.asList(a, b));
        Assert.assertNotNull(list);
        ReadWriteUtils.print(m);
        Assert.assertEquals(8, m.size());
        Assert.assertEquals(2, m.listStatements(null, RDF.type, t).toList().size());

        Assert.assertTrue(Models.isInList(m, a));
        Assert.assertTrue(Models.isInList(m, b));

        Assert.assertEquals(6, Iter.peek(Models.listDescendingStatements(list),
                s -> Assert.assertTrue(RDF.type.equals(s.getPredicate()) || Models.isInList(s))).toList().size());

        Assert.assertEquals(2, Models.subjects(t).count());
        Assert.assertEquals(2, Models.listAscendingStatements(RDF.nil.inModel(m)).toList().size());
    }

    @Test
    public void testStatementsComparators() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass a = m.createOntClass("A");
        OntClass b = m.createOntClass("B");
        m.createObjectSomeValuesFrom(m.createObjectProperty("P"), m.createObjectComplementOf(m.createObjectUnionOf(a, b)));

        testStatementsComparator(m, Models.STATEMENT_COMPARATOR);
    }

    @SuppressWarnings("SameParameterValue")
    private static void testStatementsComparator(Model m, Comparator<Statement> comp) {
        List<Statement> first = Iter.asStream(m.listStatements()).sorted(comp).collect(Collectors.toList());
        List<Statement> tmp;
        Collections.shuffle(tmp = m.listStatements().toList());
        List<Statement> second = tmp.stream().sorted(comp).collect(Collectors.toList());
        Assert.assertEquals(first, second);
    }

    @Test
    public void testGetRoots() {
        OntModel m = OntModelFactory.createModel(ReadWriteUtils.loadResourceTTLFile("/ontapi/pizza.ttl").getGraph());
        Statement a = getStatement(m, null, RDF.first, getResource(m, ":FourCheesesTopping"));
        Set<Statement> res1 = Models.getRootStatements(a);
        Assert.assertEquals(1, res1.size());
        Assert.assertEquals(getResource(m, ":QuattroFormaggi"), res1.iterator().next().getSubject());
        Assert.assertEquals(RDFS.subClassOf, res1.iterator().next().getPredicate());
        Assert.assertTrue(res1.iterator().next().getObject().isAnon());

        Literal x = m.createLiteral("CoberturaDeCaper", "pt");
        Statement b = getStatement(m, null, RDFS.label, x);
        Set<Statement> res2 = Models.getRootStatements(b);
        Assert.assertEquals(1, res2.size());
        Assert.assertEquals(getResource(m, ":CaperTopping"), res2.iterator().next().getSubject());
        Assert.assertEquals(RDFS.label, res2.iterator().next().getPredicate());
        Assert.assertEquals(x, res2.iterator().next().getObject());

        Statement c = getStatement(m, null, OWL.someValuesFrom, getResource(m, ":RocketTopping"));
        Set<Statement> res3 = Models.getRootStatements(c);
        Assert.assertEquals(1, res3.size());
        Resource soho = getResource(m, ":Soho");
        Assert.assertEquals(soho, res3.iterator().next().getSubject());
        Assert.assertEquals(RDFS.subClassOf, res3.iterator().next().getPredicate());
        Assert.assertTrue(res3.iterator().next().getObject().isAnon());

        Resource spiciness = getResource(m, ":Spiciness").addProperty(RDFS.subClassOf, c.getSubject());
        Set<Statement> res4 = Models.getRootStatements(c);
        Assert.assertEquals(2, res4.stream()
                .peek(p -> {
                    Assert.assertEquals(RDFS.subClassOf, p.getPredicate());
                    Assert.assertEquals(c.getSubject(), p.getObject());
                })
                .count());
        Assert.assertTrue(res4.stream().map(Statement::getSubject).anyMatch(soho::equals));
        Assert.assertTrue(res4.stream().map(Statement::getSubject).anyMatch(spiciness::equals));

        OntDisjoint.Individuals disjoint = m.ontObjects(OntDisjoint.Individuals.class)
                .findFirst().orElseThrow(AssertionError::new);
        OntStatement d = disjoint.getMainStatement();
        Assert.assertEquals(1, OntModels.listRootStatements(d).toSet().size());

        OntIndividual i = m.getOntClass(soho).createIndividual();
        OntStatement e = i.statement(RDF.type).orElseThrow(AssertionError::new);
        Assert.assertEquals(1, OntModels.listRootStatements(e).toSet().size());
        disjoint.getList().addFirst(i);
        Assert.assertEquals(2, OntModels.listRootStatements(e).toSet().size());
    }

    private static Resource getResource(Model m, String shortName) {
        return m.createResource(m.expandPrefix(shortName));
    }

    @SuppressWarnings("SameParameterValue")
    private static Statement getStatement(Model m, Resource s, Property p, RDFNode o) {
        List<Statement> statements = m.listStatements(s, p, o).toList();
        Assert.assertEquals(1, statements.size());
        return statements.get(0);
    }

}
