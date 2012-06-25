/**
 * END USER LICENSE AGREEMENT (“EULA”)
 *
 * READ THIS AGREEMENT CAREFULLY (date: 9/13/2011):
 * http://www.akiban.com/licensing/20110913
 *
 * BY INSTALLING OR USING ALL OR ANY PORTION OF THE SOFTWARE, YOU ARE ACCEPTING
 * ALL OF THE TERMS AND CONDITIONS OF THIS AGREEMENT. YOU AGREE THAT THIS
 * AGREEMENT IS ENFORCEABLE LIKE ANY WRITTEN AGREEMENT SIGNED BY YOU.
 *
 * IF YOU HAVE PAID A LICENSE FEE FOR USE OF THE SOFTWARE AND DO NOT AGREE TO
 * THESE TERMS, YOU MAY RETURN THE SOFTWARE FOR A FULL REFUND PROVIDED YOU (A) DO
 * NOT USE THE SOFTWARE AND (B) RETURN THE SOFTWARE WITHIN THIRTY (30) DAYS OF
 * YOUR INITIAL PURCHASE.
 *
 * IF YOU WISH TO USE THE SOFTWARE AS AN EMPLOYEE, CONTRACTOR, OR AGENT OF A
 * CORPORATION, PARTNERSHIP OR SIMILAR ENTITY, THEN YOU MUST BE AUTHORIZED TO SIGN
 * FOR AND BIND THE ENTITY IN ORDER TO ACCEPT THE TERMS OF THIS AGREEMENT. THE
 * LICENSES GRANTED UNDER THIS AGREEMENT ARE EXPRESSLY CONDITIONED UPON ACCEPTANCE
 * BY SUCH AUTHORIZED PERSONNEL.
 *
 * IF YOU HAVE ENTERED INTO A SEPARATE WRITTEN LICENSE AGREEMENT WITH AKIBAN FOR
 * USE OF THE SOFTWARE, THE TERMS AND CONDITIONS OF SUCH OTHER AGREEMENT SHALL
 * PREVAIL OVER ANY CONFLICTING TERMS OR CONDITIONS IN THIS AGREEMENT.
 */

package com.akiban.server.expression.std;

import com.akiban.server.types.ValueSourceIsNullException;
import java.util.EnumMap;
import com.akiban.server.error.WrongExpressionArityException;
import com.akiban.server.expression.Expression;
import com.akiban.server.expression.ExpressionComposer;
import com.akiban.server.types.AkType;
import com.akiban.server.types.extract.Extractors;
import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.*;

public class YearWeekExpressionTest extends ComposedExpressionTestBase
{
    private static final EnumMap<AkType, String> TESTCASE_1 = new EnumMap<AkType, String>(AkType.class);
    private static final int RESULT1[] = {200852, 200901, 200852, 200901, 200853, 200852, 200853, 200852};
    static
    {
        TESTCASE_1.put(AkType.DATE, "2009-01-01");
        TESTCASE_1.put(AkType.DATETIME, "2009-01-01 12:30:45");
        TESTCASE_1.put(AkType.TIMESTAMP, "2009-01-01 12:30: 45");
    }
    
    private static final EnumMap<AkType, String> TESTCASE_2 = new EnumMap<AkType, String>(AkType.class);
    private static final int RESULT2 = 201253;
    static
    {
        TESTCASE_2.put(AkType.DATE, "2012-12-31");
        TESTCASE_2.put(AkType.DATETIME, "2012-12-31 12:30:45");
        TESTCASE_2.put(AkType.TIMESTAMP, "2012-12-31 12:30: 45");
    }
    
    private static final EnumMap<AkType, String> TESTCASE_3 = new EnumMap<AkType, String>(AkType.class);
    private static final int RESULT3[] = {201119, 201118, 201119, 201118, 201119, 201118, 201119, 201118};
    static
    {
        TESTCASE_3.put(AkType.DATE, "2011-05-08");
        TESTCASE_3.put(AkType.DATETIME, "2011-05-08 12:30:45");
        TESTCASE_3.put(AkType.TIMESTAMP, "2011-05-08 12:30: 45");
    }
    
    @Test
    public void testRegularCases()
    {
        // test first day
        for (AkType type : TESTCASE_1.keySet())
            for (int mode = 0; mode < 8; ++mode)
                test(type, TESTCASE_1.get(type), mode, RESULT1[mode]);
            
        // test last day
        for (AkType type : TESTCASE_2.keySet())
            for (int mode = 0; mode < 8; ++mode)
                test(type, TESTCASE_2.get(type), mode, RESULT2);

        // test 2010-may-08      
        for (AkType type : TESTCASE_3.keySet())
            for (int mode = 0; mode < 8; ++mode)
                test(type, TESTCASE_3.get(type), mode, RESULT3[mode]);
    }

    @Test
    public void bug_testYearWeek () // bug 905502 - unit test passes
    {
        String st = "1987-01-01";

        // date
        testBug(AkType.DATE, st);

        // datetime
        testBug(AkType.DATETIME, st += " 01:15:33");

        // timestamp
        testBug(AkType.TIMESTAMP, st);
    }

    private void testBug (AkType type, String st)
    {
        test(type, st, -1, 198652);
    }
    
    @Test
    public void testNullFirst ()
    {
        Expression yearWeek = new YearWeekExpression(Arrays.asList(LiteralExpression.forNull(),
                new LiteralExpression(AkType.LONG, 4)));
        assertEquals(AkType.INT,yearWeek.valueType());
        assertTrue(yearWeek.evaluation().eval().isNull());
    }

    @Test
    public void testNullSecond ()
    {
        Expression yearWeak = new YearWeekExpression(Arrays.asList(new LiteralExpression(AkType.DATE, 12345L),
                LiteralExpression.forNull()));
        assertEquals(AkType.INT,yearWeak.valueType());
        assertTrue(yearWeak.evaluation().eval().isNull());
    }

    @Test (expected = WrongExpressionArityException.class)
    public void testWrongArity()
    {
        Expression yearWeek = new YearWeekExpression(Arrays.asList(new LiteralExpression(AkType.DATE, 12345L),
                new LiteralExpression(AkType.INT, 4),
                new LiteralExpression(AkType.INT, 4)));
    }

    @Test (expected = ValueSourceIsNullException.class)
    public void testZeroYear()
    {
        test(AkType.DATETIME, "0000-12-2 12:10:15", 0, 0);
    }

    @Test (expected = ValueSourceIsNullException.class)
    public void testZeroMonth()
    {
        test(AkType.DATETIME, "0001-00-02 12:10:15", 0, 0);
    }

    @Test (expected = ValueSourceIsNullException.class)
    public void testZeroDay()
    {
        test(AkType.DATE, "0001-02-00", 0, 0);
    }

    @Test (expected = ValueSourceIsNullException.class)
    public void testInvalidMode()
    {
        test(AkType.DATE, "2009-12-2", 10, 0);
    }

    private void test(AkType type, String dateS, int mode, int exp)
    {
        long date = Extractors.getLongExtractor(type).getLong(dateS);

        Expression d = new LiteralExpression(type, date);
        Expression m = new LiteralExpression(AkType.INT, mode);
        Expression yearWeek = new YearWeekExpression(mode < 0? Arrays.asList(d) : Arrays.asList(d, m));

        int actual = (int) yearWeek.evaluation().eval().getInt();
        assertEquals("assert topType is INT", AkType.INT, yearWeek.valueType());
        assertEquals("DATE: " + date + ", mode " + mode, actual, exp);
    }

    @Override
    protected CompositionTestInfo getTestInfo()
    {
        return new CompositionTestInfo(1, AkType.DATE, true);
    }

    @Override
    protected ExpressionComposer getComposer()
    {
        return YearWeekExpression.WEEK_COMPOSER;
    }

    @Override
    protected boolean alreadyExc()
    {
        return false;
    }
}
