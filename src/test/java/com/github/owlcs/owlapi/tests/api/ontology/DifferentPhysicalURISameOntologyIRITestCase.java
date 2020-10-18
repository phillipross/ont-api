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
package com.github.owlcs.owlapi.tests.api.ontology;

import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;

/**
 * @author Matthew Horridge, The University of Manchester, Information Management Group
 */
public class DifferentPhysicalURISameOntologyIRITestCase extends TestBase {

    private static final String ONTOLOGY_A = "ImportsTestOntologyA.owl";
    private static final String ONTOLOGY_A_EMPTY = "ImportsTestOntologyAEmpty.owl";

    @Test
    public void testDifferentPhysicalURISameOntologyIRI() {
        Assertions.assertThrows(OWLOntologyAlreadyExistsException.class, this::differentPhysicalURISameOntologyIRI);
    }

    private void differentPhysicalURISameOntologyIRI() throws Exception {
        IRI ontologyADocumentIRI = IRI.create(DifferentPhysicalURISameOntologyIRITestCase.class
                .getResource("/owlapi/" + ONTOLOGY_A).toURI());
        IRI ontologyADocumentIRIB = IRI
                .create(DifferentPhysicalURISameOntologyIRITestCase.class
                        .getResource("/owlapi/" + ONTOLOGY_A_EMPTY).toURI());
        m.loadOntologyFromOntologyDocument(ontologyADocumentIRI);
        m.loadOntologyFromOntologyDocument(ontologyADocumentIRIB);
        Assertions.fail("Expected an exception to say that the ontology already exists");
    }
}
