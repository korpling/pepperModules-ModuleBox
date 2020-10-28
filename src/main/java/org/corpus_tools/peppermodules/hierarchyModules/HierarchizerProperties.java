package org.corpus_tools.peppermodules.hierarchyModules;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	/** Provide a name for a layer around all generated trees. Providing no value will lead to no layer. */
	public static final String PROP_LAYER_NAME = "hierarchy.layer.name";
	/** This property assigns the named annotations as edge annotation between two hierarchical levels instead of as a hierarchical level itself. Those edge names are required to be enumerated as relevant for the hierarchy in "hierarchy.names" */
	public static final String PROP_NAMES_AS_EDGES = "hierarchy.edge.names";
	/** This assumes annotation IDs for relations have been annotated. */
	public static final String PROP_DISC_POINTERS = "pointers";
	/** Set a pointer id marker.*/
	public static final String PROP_POINTER_MARKER = "pointers.marker";
	/** Set the edge type name for the pointers. */
	public static final String PROP_POINTER_EDGE_TYPE = "pointers.edge.type";
	/** Assign a document root note dominating all tree roots. This can be useful when you want to analyze relations between nodes of different trees. */
	public static final String PROP_TEXT_ROOT = "hierarchy.common.root";
	/** This property determines the annotation value for the text root node. */
	public static final String PROP_TEXT_ROOT_VALUE = "hierarchy.root.value";
	
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
		addProperty(PepperModuleProperty.create()
				.withName(PROP_LAYER_NAME)
				.withType(String.class)
				.withDescription("Provide a name for a layer around all generated trees. Providing no value will lead to no layer.")
				.build());
		addProperty(PepperModuleProperty.create()
				.withName(PROP_NAMES_AS_EDGES)
				.withType(String.class)
				.withDescription("This property assigns the named annotations as edge annotation between two hierarchical levels instead of as a hierarchical level itself. Those edge names are required to be enumerated as relevant for the hierarchy in \"hierarchy.names\" ")
				.build());
		addProperty(PepperModuleProperty.create()
				.withName(PROP_DISC_POINTERS)
				.withType(Boolean.class)
				.withDescription("This assumes annotation IDs for relations have been annotated.")
				.withDefaultValue(false)
				.build());
		addProperty(PepperModuleProperty.create()
				.withName(PROP_POINTER_MARKER)
				.withType(String.class)
				.withDescription("Set a pointer id marker. Default is #")
				.withDefaultValue("#")
				.build());
		addProperty(PepperModuleProperty.create()
				.withName(PROP_POINTER_EDGE_TYPE)
				.withType(String.class)
				.withDescription("Set the edge type name for the pointers.")
				.withDefaultValue("refers_to")
				.build());
		addProperty(PepperModuleProperty.create()
				.withName(PROP_TEXT_ROOT)
				.withType(Boolean.class)
				.withDescription("Assign a document root note dominating all tree roots. This can be useful when you want to analyze relations between nodes of different trees.")
				.withDefaultValue(false)
				.build());
		addProperty(PepperModuleProperty.create()
				.withName(PROP_TEXT_ROOT_VALUE)
				.withType(String.class)
				.withDescription("This property determines the annotation value for the text root node.")
				.withDefaultValue("TEXT")
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
	
	public String getLayerName() {
		Object value = getProperty(PROP_LAYER_NAME).getValue();
		return value == null? null : (String) value;
	}
	
	public Set<String> getEdgeNames() {
		Object val = getProperty(PROP_NAMES_AS_EDGES).getValue();
		if (val == null) {
			return Collections.<String>emptySet();
		}
		String listStr = (String) val;		
		return Arrays.asList(StringUtils.split(listStr, ",")).stream().map(String::trim).collect(Collectors.toSet());
	}
	
	public boolean hasPointers() {
		return (Boolean) getProperty(PROP_DISC_POINTERS).getValue();		
 	}
	
	public String getPointerMarker() {
		return (String) getProperty(PROP_POINTER_MARKER).getValue();
	}
	
	public String getPointerType() {
		return (String) getProperty(PROP_POINTER_EDGE_TYPE).getValue();
	}
	
	public boolean treeifyForest() {
		return (Boolean) getProperty(PROP_TEXT_ROOT).getValue();
	}
	
	public String getRootValue() {
		return (String) getProperty(PROP_TEXT_ROOT_VALUE).getValue();
	}
}
