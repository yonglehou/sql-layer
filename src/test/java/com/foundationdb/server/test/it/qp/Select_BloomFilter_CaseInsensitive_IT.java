/**
 * Copyright (C) 2009-2013 FoundationDB, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.foundationdb.server.test.it.qp;

import com.foundationdb.qp.expression.IndexBound;
import com.foundationdb.qp.expression.IndexKeyRange;
import com.foundationdb.qp.expression.RowBasedUnboundExpressions;
import com.foundationdb.qp.operator.ExpressionGenerator;
import com.foundationdb.qp.operator.Operator;
import com.foundationdb.qp.row.Row;
import com.foundationdb.qp.rowtype.IndexRowType;
import com.foundationdb.qp.rowtype.RowType;
import com.foundationdb.qp.rowtype.Schema;
import com.foundationdb.qp.rowtype.TableRowType;
import com.foundationdb.server.PersistitKeyValueSource;
import com.foundationdb.server.api.dml.SetColumnSelector;
import com.foundationdb.server.api.dml.scan.NewRow;
import com.foundationdb.server.collation.AkCollator;
import com.foundationdb.server.collation.AkCollatorFactory;
import com.foundationdb.server.collation.AkCollatorMySQL;
import com.foundationdb.server.test.ExpressionGenerators;
import com.foundationdb.server.types.mcompat.mtypes.MString;
import com.foundationdb.server.types.value.ValueSources;
import com.foundationdb.server.types.texpressions.Comparison;
import com.persistit.Key;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.foundationdb.qp.operator.API.*;
import static org.junit.Assert.assertTrue;

// Like Select_BloomFilterIT, but testing with case-insensitive strings

public class Select_BloomFilter_CaseInsensitive_IT extends OperatorITBase
{
    @Override
    protected void setupCreateSchema()
    {
        AkCollatorMySQL.useForTesting();
        // Tables are Driving (D) and Filtering (F). Find Filtering rows with a given test id, yielding
        // a set of (a, b) rows. Then find Driving rows matching a and b.
        d = createTable(
            "schema", "driving",
            "test_id int not null",
            "a varchar(10) collate latin1_swedish_ci",
            "b varchar(10) collate latin1_swedish_ci");
        f = createTable(
            "schema", "filtering",
            "test_id int not null",
            "a varchar(10) collate latin1_swedish_ci",
            "b varchar(10) collate latin1_swedish_ci");
        createIndex("schema", "driving", "idx_d", "test_id", "a", "b");
        createIndex("schema", "filtering", "idx_fab", "a", "b");
    }

    @Override
    protected void setupPostCreateSchema()
    {
        schema = new Schema(ais());
        dRowType = schema.tableRowType(table(d));
        fRowType = schema.tableRowType(table(f));
        dIndexRowType = indexType(d, "test_id", "a", "b");
        fabIndexRowType = indexType(f, "a", "b");
        adapter = newStoreAdapter(schema);
        queryContext = queryContext(adapter);
        queryBindings = queryContext.createBindings();
        ciCollator = dRowType.table().getColumn("a").getCollator();
        db = new NewRow[]{
            // Test 0: No d or f rows
            // Test 1: No f rows
            createNewRow(f, 1L, "xy", 100L),
            // Test 2: No d rows
            createNewRow(f, 2L, "xy", 200L),
            // Test 3: 1 d row, no matching f rows
            createNewRow(d, 3L, "xy", "A"),
            createNewRow(f, 3L, "xz", "A"),
            createNewRow(f, 3L, "xy", "B"),
            // Test 4: 1 d row, 1 matching f rows
            createNewRow(d, 4L, "xy", "a"),
            createNewRow(f, 4L, "XY", "A"),
            createNewRow(f, 4L, "XZ", "A"),
            createNewRow(f, 4L, "XY", "B"),
            // Test 5: multiple d rows, no matching f rows
            createNewRow(d, 5L, "xy", "a"),
            createNewRow(d, 5L, "xz", "b"),
            createNewRow(f, 5L, "XY", "B"),
            createNewRow(f, 5L, "XZ", "A"),
            // Test 6: multiple d rows, multiple f rows, multiple matches
            createNewRow(d, 6L, "xy", "A"),
            createNewRow(d, 6L, "XY", "aB"),
            createNewRow(d, 6L, "Xy", "Ac"),
            createNewRow(d, 6L, "xY", "a"),
            createNewRow(f, 6L, "XY", "aZ"),
            createNewRow(f, 6L, "xy",  "Ab"),
            createNewRow(f, 6L, "Xy", "ac"),
            createNewRow(f, 6L, "xy", "aZ"),
            // Test 7: Null columns in d
            createNewRow(d, 7L, null, null),
            // Test 8: Null columns in f
            createNewRow(f, 8L, null, null),
        };
        use(db);
    }

    // Test ValueSourceHasher

    @Test
    public void testValueSourceHasher()
    {
        AkCollator caseInsensitiveCollator = AkCollatorFactory.getAkCollator("latin1_swedish_ci");
        AkCollator binaryCollator = AkCollatorFactory.getAkCollator(AkCollatorFactory.UCS_BINARY);
        PersistitKeyValueSource source = new PersistitKeyValueSource(MString.VARCHAR.instance(true));
        long hash_AB;
        long hash_ab;
        Key key = store().createKey();
        {
            binaryCollator.append(key.clear(), "AB");
            source.attach(key, 0, MString.VARCHAR.instance(true));
            hash_AB = ValueSources.hash(source, binaryCollator);
            binaryCollator.append(key.clear(), "ab");
            source.attach(key, 0, MString.VARCHAR.instance(true));
            hash_ab = ValueSources.hash(source, binaryCollator);
            assertTrue(hash_AB != hash_ab);
        }
        {
            caseInsensitiveCollator.append(key.clear(), "AB");
            source.attach(key, 0, MString.VARCHAR.instance(true));
            hash_AB = ValueSources.hash(source, caseInsensitiveCollator);
            caseInsensitiveCollator.append(key.clear(), "ab");
            source.attach(key, 0, MString.VARCHAR.instance(true));
            hash_ab = ValueSources.hash(source, caseInsensitiveCollator);
            assertTrue(hash_AB == hash_ab);
        }
    }

    // Test operator execution

    @Test
    public void test0()
    {
        Operator plan = plan(0);
        Row[] expected = new Row[] {
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings), null, ciCollator, ciCollator);
    }

    @Test
    public void test1()
    {
        Operator plan = plan(1);
        Row[] expected = new Row[] {
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings), null, ciCollator, ciCollator);
    }

    @Test
    public void test2()
    {
        Operator plan = plan(2);
        Row[] expected = new Row[] {
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings), null, ciCollator, ciCollator);
    }

    @Test
    public void test3()
    {
        Operator plan = plan(3);
        Row[] expected = new Row[] {
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings), null, ciCollator, ciCollator);
    }

    @Test
    public void test4()
    {
        Operator plan = plan(4);
        Row[] expected = new Row[] {
            row(outputRowType, 4L, "xy", "a"),
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings), null, ciCollator, ciCollator);
    }

    @Test
    public void test5()
    {
        Operator plan = plan(5);
        Row[] expected = new Row[] {
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings), null, ciCollator, ciCollator);
    }

    @Test
    public void test6()
    {
        Operator plan = plan(6);
        Row[] expected = new Row[] {
            row(outputRowType, 6L, "xy", "ab"),
            row(outputRowType, 6L, "xy", "ac"),
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings), null, ciCollator, ciCollator);
    }

    @Test
    public void test7()
    {
        Operator plan = plan(7);
        Row[] expected = new Row[] {
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings), null, ciCollator, ciCollator);
    }

    @Test
    public void test8()
    {
        Operator plan = plan(8);
        Row[] expected = new Row[] {
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings), null, ciCollator, ciCollator);
    }

    @Test
    public void testCursor()
    {
        Operator plan = plan(6);
        CursorLifecycleTestCase testCase = new CursorLifecycleTestCase()
        {
            @Override
            public Row[] firstExpectedRows()
            {
                return new Row[] {
                    row(outputRowType, 6L, "xy", "ab"),
                    row(outputRowType, 6L, "xy", "ac"),
                };
            }
        };
        testCursorLifecycle(plan, testCase, null, ciCollator, ciCollator);
    }

    public Operator plan(long testId)
    {
        List<AkCollator> collators = Arrays.asList(ciCollator, ciCollator);
        // loadFilter loads the filter with F rows containing the given testId.
        Operator loadFilter = project_DefaultTest(
            select_HKeyOrdered(
                filter_Default(
                    groupScan_Default(group(f)),
                    Collections.singleton(fRowType)),
                fRowType,
                ExpressionGenerators.compare(
                    ExpressionGenerators.field(fRowType, 0),
                    Comparison.EQ,
                    ExpressionGenerators.literal(testId), castResolver())),
            fRowType,
            Arrays.asList(ExpressionGenerators.field(fRowType, 1),
                          ExpressionGenerators.field(fRowType, 2)));
        // For the index scan retriving rows from the D(test_id) index
        IndexBound testIdBound =
            new IndexBound(row(dIndexRowType, testId), new SetColumnSelector(0));
        IndexKeyRange dTestIdKeyRange =
            IndexKeyRange.bounded(dIndexRowType, testIdBound, true, testIdBound, true);
        // For the  index scan retrieving rows from the F(a, b) index given a D index row
        IndexBound abBound = new IndexBound(
            new RowBasedUnboundExpressions(
                loadFilter.rowType(),
                Arrays.asList(
                    ExpressionGenerators.boundField(dIndexRowType, 0, 1),
                    ExpressionGenerators.boundField(dIndexRowType, 0, 2)), true),
            new SetColumnSelector(0, 1));
        IndexKeyRange fabKeyRange =
            IndexKeyRange.bounded(fabIndexRowType, abBound, true, abBound, true);
        // Use a bloom filter loaded by loadFilter. Then for each input row, check the filter (projecting
        // D rows on (a, b)), and, for positives, check F using an index scan keyed by D.a and D.b.
        Operator plan =
            project_DefaultTest(
                using_BloomFilter(
                    // filterInput
                    loadFilter,
                    // filterRowType
                    loadFilter.rowType(),
                    // estimatedRowCount
                    10,
                    // filterBindingPosition
                    0,
                    // streamInput
                    select_BloomFilter(
                        // input
                        indexScan_Default(dIndexRowType, dTestIdKeyRange, new Ordering()),
                        // onPositive
                        indexScan_Default(
                            fabIndexRowType,
                            fabKeyRange,
                            new Ordering()),
                        // filterFields
                        Arrays.asList(
                            ExpressionGenerators.field(dIndexRowType, 1),
                            ExpressionGenerators.field(dIndexRowType, 2)),
                        // collators
                        collators,
                        // filterBindingPosition
                        0, false, 1,
                        ExpressionGenerator.ErasureMaker.MARK),
                    // collators
                    collators
                    ),
                dIndexRowType,
                Arrays.asList(
                    ExpressionGenerators.field(dIndexRowType, 0),   // test_id
                    ExpressionGenerators.field(dIndexRowType, 1),   // a
                    ExpressionGenerators.field(dIndexRowType, 2))); // b
        outputRowType = plan.rowType();
        return plan;
    }

    private int d;
    private int f;
    private TableRowType dRowType;
    private TableRowType fRowType;
    private RowType outputRowType;
    IndexRowType dIndexRowType;
    IndexRowType fabIndexRowType;

}
