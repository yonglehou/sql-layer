package com.akiba.cserver;

public class FieldDef {

	private final FieldType type;

	private final int maxWidth;

	public FieldDef(final FieldType type) {
		this.type = type;
		this.maxWidth = type.getMaxWidth();
	}

	public FieldDef(final FieldType type, final int maxWidth) {
		this.type = type;
		if (maxWidth >= type.getMinWidth() && maxWidth <= type.getMaxWidth()) {
			this.maxWidth = maxWidth;
		} else {
			throw new IllegalArgumentException("MaxWidth value " + maxWidth
					+ " out of bounds for type " + type);
		}
	}

	public FieldType getType() {
		return type;
	}

	public int getMaxWidth() {
		return maxWidth;
	}

	public int getMinWidth() {
		return type.getMinWidth();
	}

	public boolean isFixedWidth() {
		return type.isFixedWidth();
	}

	public String toString() {
		return type + "(" + getMinWidth() + "," + getMaxWidth() + ")";
	}
}
