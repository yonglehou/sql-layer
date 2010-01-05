package com.akiba.cserver;

import java.util.Arrays;
import java.util.Random;

import junit.framework.TestCase;

public class RowDefTest extends TestCase {

	private final static boolean VERBOSE = false;

	private final static FieldDef[][] FIELD_DEF_CASES = new FieldDef[][] {
			{ new FieldDef(FieldType.TINYINT), new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.SMALLINT), },

			{ new FieldDef(FieldType.VARCHAR, 100),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.VARCHAR, 100),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.VARCHAR, 100),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.VARCHAR, 100),
					new FieldDef(FieldType.TINYINT), },

			{ new FieldDef(FieldType.TINYINT), new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.TINYINT), },

			{ new FieldDef(FieldType.VARCHAR, 100),
					new FieldDef(FieldType.VARCHAR, 100),
					new FieldDef(FieldType.VARCHAR, 100),
					new FieldDef(FieldType.VARCHAR, 100),
					new FieldDef(FieldType.VARCHAR, 100),
					new FieldDef(FieldType.VARCHAR, 100),
					new FieldDef(FieldType.VARCHAR, 100),
					new FieldDef(FieldType.VARCHAR, 100),
					new FieldDef(FieldType.VARCHAR, 100),
					new FieldDef(FieldType.VARCHAR, 100),
					new FieldDef(FieldType.VARCHAR, 100),
					new FieldDef(FieldType.VARCHAR, 100), },

			{
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.SMALLINT),
					new FieldDef(FieldType.MEDIUMINT),
					new FieldDef(FieldType.INT),
					new FieldDef(FieldType.BIGINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.SMALLINT),
					new FieldDef(FieldType.MEDIUMINT),
					new FieldDef(FieldType.INT),
					new FieldDef(FieldType.BIGINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.VARCHAR, 200),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.SMALLINT),
					new FieldDef(FieldType.MEDIUMINT),
					new FieldDef(FieldType.INT),
					new FieldDef(FieldType.BIGINT),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.SMALLINT),
					new FieldDef(FieldType.MEDIUMINT),
					new FieldDef(FieldType.INT),
					new FieldDef(FieldType.BIGINT),
					new FieldDef(FieldType.VARCHAR, 200),
					new FieldDef(FieldType.TINYINT),
					new FieldDef(FieldType.TINYINT),
			},

	};

	private final static Object[][][] DATA_CASES = new Object[][][] {

			new Object[][] { new Object[] { 1, 2, 3 },
					new Object[] { null, null, null },
					new Object[] { null, 2, null },
					new Object[] { 1, null, 3 }, },

			new Object[][] {
					new Object[] { null, 2, null, 4, null, 6, null, 8 },
					new Object[] { null, null, null, null, null, null, null,
							null },
					new Object[] { null, 3, "def", 5, "ghi", 7, null, 8 },
					new Object[] { "a", 2, "b", 4, "c", 6, "d", 8 }, },

			new Object[][] {
					new Object[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
							14, 15 },
					new Object[] { null, null, null, null, null, null, null, 8,
							null, null, null, null, 13, null, 15 }, },

			new Object[][] {
					new Object[] { "a", "b", "c", "d", "e", "f", "g", "h", "i",
							"j", "k", "l" },
					new Object[] { "a", null, "c", null, "e", null, "g", null,
							"i", null, "k", null },
					new Object[] { null, "b", null, "d", null, "f", null, null,
							null, "j", null, "l" },
					new Object[] { null, null, null, null, null, null, null,
							null, null, null, null, "end" },
					new Object[] {}, },

			new Object[][] { new Object[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 
					"foo", 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,  
					"bar", 26, 27 }

			}, };

	public void testComputeFieldLocations1() {
		for (int def = 0; def < FIELD_DEF_CASES.length; def++) {
			final FieldDef[] fieldDefs = FIELD_DEF_CASES[def];
			final RowDef rowDef = new RowDef(def + 100, fieldDefs);
			if (VERBOSE) {
				System.out.println(rowDef);
			}

			for (int data = 0; data < DATA_CASES[def].length; data++) {
				final RowData rowData = new RowData(new byte[1024]);
				rowData.createRow(rowDef, DATA_CASES[def][data]);
				if (VERBOSE) {
					System.out.println("From data: "
							+ Arrays.asList(DATA_CASES[def][data]));
					System.out.println("Def " + def + " Data " + data);
					System.out.println("RowData:\n");
					System.out.println(Util.dump(rowData.getBytes(), rowData
							.getRowStart(), rowData.getRowEnd()
							- rowData.getRowStart()));
				}
				for (int i = 0; i < fieldDefs.length; i++) {
					final long location = rowDef.fieldLocation(rowData, i);
					if (VERBOSE) {
						System.out.println(String.format(
								"Field# %3d offset %4d width %4d", i,
								location & 0xFFFFFFFFL, location >>> 32));
					}
					final FieldDef fieldDef = fieldDefs[i];
					Object value = i < DATA_CASES[def][data].length ? DATA_CASES[def][data][i]
							: null;

					assertValuesAreEqual(value, fieldDef, rowData, location);

					if (value == null) {
						assertEquals(0, location);
					}
					if (location == 0) {
						assertNull(value);
					} else if (fieldDef.isFixedWidth()) {
						long decodedValue = rowData.getIntegerValue(
								(int) location, (int) (location >>> 32));
						assertEquals(((Number) value).longValue(), decodedValue);
					} else {
						String decodedValue = new String(rowData.getBytes(),
								(int) location, (int) (location >>> 32));
						assertEquals((String) value, decodedValue);
					}
				}
			}
		}
	}

	public void testComputeFieldLocations2() throws Exception {
		final Random random = new Random(1);
		final byte[] randomBytes = new byte[100000];
		for (int i = 0; i < randomBytes.length; i++) {
			randomBytes[i] = (byte) ((i % 26) + 97);
		}
		final FieldDef[] fieldDefs = new FieldDef[123];
		final FieldType[] allTypes = FieldType.values();
		int maxSize = 20;
		for (int i = 0; i < fieldDefs.length; i++) {
			FieldType type = allTypes[i % /* allTypes.length */13];
			if (type.isFixedWidth()) {
				fieldDefs[i] = new FieldDef(type);
				maxSize += type.getMaxWidth();
			} else {
				int maxWidth = random.nextInt(type.getMaxWidth()
						- type.getMinWidth() + 1)
						+ type.getMinWidth();
				fieldDefs[i] = new FieldDef(type, maxWidth);
				maxSize += /* maxWidth */1 + 3;
			}
		}
		final RowDef rowDef = new RowDef(12345, fieldDefs);
		if (VERBOSE) {
			System.out.println(rowDef);
		}

		final Object[] values = new Object[fieldDefs.length];
		final RowData data = new RowData(new byte[maxSize]);

		for (int i = 0; i < fieldDefs.length; i++) {
			Object value = null;
			if (random.nextInt(10) != 0) {
				switch (fieldDefs[i].getType()) {
				case U_TINYINT:
				case TINYINT:
					value = random.nextInt(256);
					break;
				case U_SMALLINT:
				case SMALLINT:
					value = random.nextInt(65536);
					break;
				case U_MEDIUMINT:
				case MEDIUMINT:
					value = random.nextInt(65536 * 256);
					break;
				case FLOAT:
				case U_INT:
				case INT:
					value = random.nextInt();
					break;
				case DOUBLE:
				case U_BIGINT:
				case BIGINT:
					value = random.nextLong();
					break;
				case VARCHAR:
				case BINCHAR:
				case BINVARCHAR:
				case CHAR:
					int size = 3; // fieldDefs[i].getMaxWidth();
					value = new byte[size];
					System.arraycopy(randomBytes, i /*random.nextInt(20000) */, value,
							0, size);
				default:
				}
				values[i] = value;
			}
		}
		data.createRow(rowDef, values);
		
		if (VERBOSE) {
			System.out.println(data);
		}
		for (int i = fieldDefs.length; --i >= 0; ) {
			final long location = rowDef.fieldLocation(data, i);
			assertValuesAreEqual(values[i], fieldDefs[i], data, location);
		}
	}

	private void assertValuesAreEqual(final Object value,
			final FieldDef fieldDef, final RowData rowData, final long location) {
		if (value == null) {
			assertEquals(0, location);
		}
		if (location == 0) {
			assertNull(value);
		} else if (fieldDef.isFixedWidth()) {
			long decodedValue = rowData.getIntegerValue((int) location,
					(int) (location >>> 32));
			assertEquals(((Number) value).longValue(), decodedValue);
		} else {
			byte[] decodedBytes = new byte[(int) (location >>> 32)];
			System.arraycopy(rowData.getBytes(), (int) location, decodedBytes,
					0, decodedBytes.length);
			byte[] bytes;
			if (value instanceof String) {
				bytes = ((String) value).getBytes();
			} else {
				bytes = (byte[]) value;
			}
			assertTrue(Arrays.equals(bytes, decodedBytes));
		}
	}
}
