/**
 * Copyright 2009 Humboldt-Universität zu Berlin, INRIA.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
package org.corpus_tools.peppermodules.ridgesModules;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;

/**
 * ]
 * 
 * @author Florian Zipser
 *
 */
public class SOrderRelationAdderProperties extends PepperModuleProperties {

	public final static String PROP_SEGMENTATION_LAYERS = "segmentation-layers";

	// public final static String
	// PROP_SEGMENTATION_LAYERS_DEFAULT="{dipl, clean, norm}";
	public SOrderRelationAdderProperties() {
		this.addProperty(new PepperModuleProperty<String>(PROP_SEGMENTATION_LAYERS, String.class, "Specifies a list of annotation names, which should be used to extract SOrderRelation objects. Use the following syntax: {SEGMENT (,SEGMENT)*}", false));
	}

	private Set<String> segmentations = null;

	public Set<String> getSegmentations() {
		if (segmentations == null) {
			segmentations = new HashSet<String>();
			String segs = (String) this.getProperty(PROP_SEGMENTATION_LAYERS).getValue();
			if (segs != null) {
				segs = StringUtils.strip(segs, "{}");
				for (String seg : segs.split("\\s*,\\s*")) {
					segmentations.add(seg.trim());
				}
			}
		}

		return (segmentations);
	}
}
