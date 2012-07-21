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

package com.akiban.server.types3.common.types;

import com.akiban.server.collation.AkCollator;
import com.akiban.server.collation.AkCollatorFactory;
import com.akiban.server.error.StringTruncationException;
import com.akiban.server.types3.TBundle;
import com.akiban.server.types3.TClass;
import com.akiban.server.types3.TClassFormatter;
import com.akiban.server.types3.TExecutionContext;
import com.akiban.server.types3.TFactory;
import com.akiban.server.types3.TInstance;
import com.akiban.server.types3.aksql.AkCategory;
import com.akiban.server.types3.pvalue.PUnderlying;
import com.akiban.server.types3.pvalue.PValueSource;
import com.akiban.server.types3.pvalue.PValueTarget;
import com.akiban.sql.types.DataTypeDescriptor;
import com.akiban.sql.types.TypeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.akiban.util.AkibanAppender;

public abstract class TString extends TClass
{
    protected TString (TypeId typeId, TBundle bundle, String name, int serialisationSize)
    {
        this(typeId, bundle, name, serialisationSize, -1);
    }

    protected TString (TypeId typeId, TBundle bundle, String name, int serialisationSize, int fixedLength)
    {
        super(bundle.id(),
                name,
                AkCategory.STRING_CHAR,
                StringAttribute.class,
                FORMAT.STRING,
                1,
                1,
                serialisationSize,
                PUnderlying.STRING);
        this.fixedLength = fixedLength;
        this.typeId = typeId;
    }
    
    private static enum FORMAT implements TClassFormatter {
        STRING {
            @Override
            public void format(TInstance instance, PValueSource source, AkibanAppender out) {
                out.append(source.getString());
            }
        }
    }

    @Override
    public void attributeToString(int attributeIndex, long value, StringBuilder output) {
        StringAttribute attribute = StringAttribute.values()[attributeIndex];
        switch (attribute) {
        case LENGTH:
            output.append(value);
            break;
        case CHARSET:
            StringFactory.Charset[] charsets = StringFactory.Charset.values();
            if (value < 0 || value >= charsets.length) {
                logger.warn("charset value out of range: {}", value);
                output.append(value);
            }
            else {
                output.append(charsets[(int)value]);
            }
            break;
        case COLLATION:
            AkCollator collator = AkCollatorFactory.getAkCollator((int)value);
            if (collator == null) {
                logger.warn("unknown collator for id " + value + " (" + ((int)value) + ')');
                output.append(value);
            }
            else {
                output.append(collator.getName());
            }
            break;
        }
    }

    @Override
    public DataTypeDescriptor dataTypeDescriptor(TInstance instance) {
        return new DataTypeDescriptor(
                typeId, instance.nullability(), instance.attribute(StringAttribute.LENGTH));
    }

    @Override
    public TInstance instance(int charsetId, int collationId) {
        return fixedLength < 0
                ? super.instance(charsetId, StringFactory.DEFAULT_CHARSET.ordinal(), collationId)
                : super.instance(fixedLength, charsetId, collationId);
    }

    @Override
    public void putSafety(TExecutionContext context, 
                          TInstance sourceInstance,
                          PValueSource sourceValue,
                          TInstance targetInstance,
                          PValueTarget targetValue)
    {
        // check type safety
        assert getClass().isAssignableFrom(sourceInstance.typeClass().getClass())
                    && getClass().isAssignableFrom(targetInstance.typeClass().getClass())
                : "expected instances of TString";
        
        String raw = (String) sourceValue.getObject();
        int maxLen = targetInstance.attribute(StringAttribute.LENGTH);
        
        if (raw.length() > maxLen)
        {   
            String truncated = raw.substring(0, maxLen);
            // TODO: check charset and collation, too
            context.warnClient(new StringTruncationException(raw, truncated));
            targetValue.putObject(truncated);
        }
        else
            targetValue.putObject(raw);
    }
     
    @Override
    public TInstance instance()
    {
        return instance(fixedLength >= 0 ? fixedLength : StringFactory.DEFAULT_LENGTH,
                        StringFactory.DEFAULT_CHARSET.ordinal(),
                        StringFactory.DEFAULT_COLLATION_ID);
    }

    @Override
    public TInstance instance(int length)
    {
        return instance(length < 0 ? 0 : length, 
                        StringFactory.DEFAULT_CHARSET.ordinal(),
                        StringFactory.DEFAULT_COLLATION_ID);
    }
    
    @Override
    public TFactory factory()
    {
        return new StringFactory(this);
    }

    @Override
    protected TInstance doPickInstance(TInstance instance0, TInstance instance1)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void validate(TInstance instance) {
        int length = instance.attribute(StringAttribute.LENGTH);
        int charsetId = instance.attribute(StringAttribute.CHARSET);
        int collaitonid = instance.attribute(StringAttribute.COLLATION);
        // TODO
    }
    
    private final int fixedLength;
    private final TypeId typeId;
    private static final Logger logger = LoggerFactory.getLogger(TString.class);
}
