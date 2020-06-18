package org.corpus_tools.peppermodules.hierarchyModules;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;

public class HierarchizerProperties extends PepperModuleProperties {
	/** The list of span layers that encode the hierarchy in top-down order. */
	public static final String PROP_HIERARCHY_NAMES = "hierarchy.names";
	/** Delete hierarchy-indicating span annotations after building the structure. */
	public static final String PROP_DELETE_SPANS = "delete.span.annos";
	/** Annotation name for structures (default is "cat"). */
	public static final String PROP_STRUCT_ANNO_NAME = "struct.anno.name";
	
	public HierarchizerProperties() {
		super();
		addProperty(PepperModuleProperty.create()
				.withName(PROP_HIERARCHY_NAMES)
				.withType(String.class)
				.withDescription("")
				.isRequired(true)
				.build());
		addProperty(PepperModuleProperty.create()
				.withName(PROP_DELETE_SPANS)
				.withType(Boolean.class)
				.withDescription("")
				.withDefaultValue(true)
				.build());
		addProperty(PepperModuleProperty.create()
				.withName(PROP_STRUCT_ANNO_NAME)
				.withType(String.class)
				.withDescription("Annotation name for structures (default is \"cat\").")
				.withDefaultValue("cat")
				.build());
	}
	
	public List<String> getHierarchyNames() {
		String listStr = (String) getProperty(PROP_HIERARCHY_NAMES).getValue();
		return Arrays.asList(StringUtils.split(listStr, ","));
	}
	
	public boolean deleteSpanAnnotations() {
		return (Boolean) getProperty(PROP_DELETE_SPANS).getValue();
	}
	
	public String getStructAnnoName() {
		return (String) getProperty(PROP_STRUCT_ANNO_NAME).getValue();
	}
}
