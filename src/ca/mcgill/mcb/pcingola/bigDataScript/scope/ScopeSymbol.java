package ca.mcgill.mcb.pcingola.bigDataScript.scope;

import ca.mcgill.mcb.pcingola.bigDataScript.lang.Type;
import ca.mcgill.mcb.pcingola.bigDataScript.serialize.BigDataScriptSerialize;
import ca.mcgill.mcb.pcingola.bigDataScript.serialize.BigDataScriptSerializer;
import ca.mcgill.mcb.pcingola.bigDataScript.util.Gpr;
import ca.mcgill.mcb.pcingola.bigDataScript.util.GprString;

/**
 * A symbol in the scope
 * 
 * @author pcingola
 */
public class ScopeSymbol implements BigDataScriptSerialize {

	public static boolean debug = false;

	Type type;
	String name;
	Object value;
	boolean constant = false;

	public ScopeSymbol() {
	}

	public ScopeSymbol(String name, Type type) {
		this.name = name;
		this.type = type;
		// Set default value
		if (!type.isFunction()) value = type.defaultValue();
	}

	public ScopeSymbol(String name, Type type, Object value) {
		this.name = name;
		this.type = type;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}

	public boolean isConstant() {
		return constant;
	}

	@Override
	public void serializeParse(BigDataScriptSerializer serializer) {
		// Parse type
		name = serializer.getNextField();
		type = serializer.getNextFieldType();

		// Parse value
		value = serializer.getNextField(type);
	}

	@Override
	public String serializeSave(BigDataScriptSerializer serializer) {
		return getClass().getSimpleName() //
				+ "\t" + name //
				+ "\t" + BigDataScriptSerializer.TYPE_IDENTIFIER + type.toStringSerializer() //
				+ "\t" + serializer.serializeSaveValue(value) //
				+ "\n";
	}

	public void setConstant(boolean constant) {
		this.constant = constant;
	}

	public void setValue(Object value) {
		if (debug) Gpr.debug("Setting value:\t" + name + " = " + value);
		this.value = value;
	}

	@Override
	public String toString() {
		String valStr = "null";

		if ((type != null) && type.isString()) valStr = "\"" + GprString.escape(value.toString()) + "\"";
		else valStr = "" + value;

		return type //
				+ " : " + name //
				+ " = " + valStr;
	}

}