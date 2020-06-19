package org.corpus_tools.peppermodules.hierarchyModules;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
	/** Provide a default value for the category annotation instead of extracting the value from a span's annotation. Provide comma-separated pairs of "annotation_name:=default_value" */
	public static final String PROP_DEFAULT_VALUES = "hierarchy.default.values";
	/** This property configures the edge type that is set to all generated dominance relations. It is not an annotation. */
	public static final String PROP_EDGE_TYPE = "hierarchy.edge.type";
	
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
		addProperty(PepperModuleProperty.create()
				.withName(PROP_DEFAULT_VALUES)
				.withType(String.class)
				.withDescription("Provide a default value for the category annotation instead of extracting the value from a span's annotation. Provide comma-separated pairs of \"annotation_name:=default_value\"")
				.build());
		addProperty(PepperModuleProperty.create()
				.withName(PROP_EDGE_TYPE)
				.withType(String.class)
				.withDescription("This property configures the edge type that is set to all generated dominance relations. It is not an annotation.")
				.withDefaultValue("edge")
				.build());
	}
	
	public List<String> getHierarchyNames() {
		String listStr = (String) getProperty(PROP_HIERARCHY_NAMES).getValue();		
		return Arrays.asList(StringUtils.split(listStr, ",")).stream().map(String::trim).collect(Collectors.toList());
	}
	
	public boolean deleteSpanAnnotations() {
		return (Boolean) getProperty(PROP_DELETE_SPANS).getValue();
	}
	
	public String getStructAnnoName() {
		return (String) getProperty(PROP_STRUCT_ANNO_NAME).getValue();
	}
	
	public Map<String, String> getDefaultValues() {
		Object value = getProperty(PROP_DEFAULT_VALUES).getValue();
		if (value == null) {
			return Collections.<String, String>emptyMap();
		}
		Map<String, String> defVals = new HashMap<String, String>();
		for (String kvPair : StringUtils.split((String) value, ",")) {
			String[] k_v = kvPair.split(":=");
			defVals.put(k_v[0].trim(), k_v[1].trim());
		}
		return defVals;
	}
	
	public String getEdgeType() {
		return (String) getProperty(PROP_EDGE_TYPE).getValue();		
	}
}
