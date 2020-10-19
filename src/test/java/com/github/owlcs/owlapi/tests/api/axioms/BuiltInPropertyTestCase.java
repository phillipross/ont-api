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
package com.github.owlcs.owlapi.tests.api.axioms;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

/**
 * @author Matthew Horridge, The University of Manchester, Information Management Group
 */
public class BuiltInPropertyTestCase extends TestBase {

    @Test
    public void testTopObjectPropertyPositive() {
        OWLObjectPropertyExpression prop = df.getOWLTopObjectProperty();
        Assert.assertTrue(prop.isOWLTopObjectProperty());
    }

    @Test
    public void testBottomObjectPropertyPositive() {
        OWLObjectPropertyExpression prop = df.getOWLBottomObjectProperty();
        Assert.assertTrue(prop.isOWLBottomObjectProperty());
    }

    @Test
    public void testTopObjectPropertyNegative() {
        OWLObjectPropertyExpression prop = OWLFunctionalSyntaxFactory.ObjectProperty(iri("prop"));
        Assert.assertFalse(prop.isOWLTopObjectProperty());
    }

    @Test
    public void testBottomObjectPropertyNegative() {
        OWLObjectPropertyExpression prop = OWLFunctionalSyntaxFactory.ObjectProperty(iri("prop"));
        Assert.assertFalse(prop.isOWLBottomObjectProperty());
    }

    @Test
    public void testTopDataPropertyPositive() {
        OWLDataPropertyExpression prop = df.getOWLTopDataProperty();
        Assert.assertTrue(prop.isOWLTopDataProperty());
    }

    @Test
    public void testBottomDataPropertyPositive() {
        OWLDataPropertyExpression prop = df.getOWLBottomDataProperty();
        Assert.assertTrue(prop.isOWLBottomDataProperty());
    }

    @Test
    public void testTopDataPropertyNegative() {
        OWLDataPropertyExpression prop = OWLFunctionalSyntaxFactory.DataProperty(iri("prop"));
        Assert.assertFalse(prop.isOWLTopDataProperty());
    }

    @Test
    public void testBottomDataPropertyNegative() {
        OWLDataPropertyExpression prop = OWLFunctionalSyntaxFactory.DataProperty(iri("prop"));
        Assert.assertFalse(prop.isOWLBottomDataProperty());
    }
}
