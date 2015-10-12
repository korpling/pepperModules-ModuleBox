/**
 * Copyright 2009 Humboldt-Universität zu Berlin, INRIA.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package org.corpus_tools.peppermodules.ridgesModules;

import junit.framework.TestCase;

import org.corpus_tools.pepper.modules.PepperModuleProperty;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Florian Zipser
 * @version 1.0
 *
 */
public class SOrderRelationAdderPropertiesTest extends TestCase
{
	private SOrderRelationAdderProperties fixture= null;

	public SOrderRelationAdderProperties getFixture()
	{
		return fixture;
	}

	public void setFixture(SOrderRelationAdderProperties fixture)
	{
		this.fixture = fixture;
	}
	@Before
	public void setUp()
	{
		this.setFixture(new SOrderRelationAdderProperties());
	}

	@Test
	public void testProp_SegmentationLayer2()
	{
		PepperModuleProperty<String> prop= (PepperModuleProperty<String>)getFixture().getProperty(SOrderRelationAdderProperties.PROP_SEGMENTATION_LAYERS);
		prop.setValue("{bla, blub}");
		
		assertEquals(""+getFixture().getSegmentations(), 2, getFixture().getSegmentations().size());
		assertTrue(getFixture().getSegmentations().contains("bla"));
		assertTrue(getFixture().getSegmentations().contains("blub"));
	}
}
