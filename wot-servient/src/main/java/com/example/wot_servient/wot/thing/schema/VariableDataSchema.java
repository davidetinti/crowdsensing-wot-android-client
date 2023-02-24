package com.example.wot_servient.wot.thing.schema;

/**
 * Describes data whose type is determined at runtime.
 */
public class VariableDataSchema extends AbstractDataSchema<Object> {

	protected String type;

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public Class getClassType() {
		switch (type) {
			case ArraySchema.TYPE:
				return ArraySchema.CLASS_TYPE;
			case BooleanSchema.TYPE:
				return BooleanSchema.CLASS_TYPE;
			case IntegerSchema.TYPE:
				return IntegerSchema.CLASS_TYPE;
			case NullSchema.TYPE:
				return NullSchema.CLASS_TYPE;
			case NumberSchema.TYPE:
				return NumberSchema.CLASS_TYPE;
			case ObjectSchema.TYPE:
				return ObjectSchema.CLASS_TYPE;
			default:
				return StringSchema.CLASS_TYPE;
		}
	}

	@Override
	public String toString() {
		return "VariableDataSchema{" + "type='" + type + '\'' + '}';
	}
}
