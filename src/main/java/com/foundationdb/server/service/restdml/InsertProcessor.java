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
package com.foundationdb.server.service.restdml;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.foundationdb.server.types.service.TypesRegistryService;
import com.foundationdb.server.service.externaldata.TableRowTracker;
import com.fasterxml.jackson.databind.JsonNode;
import com.foundationdb.server.types.value.Value;
import com.foundationdb.server.types.value.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.foundationdb.ais.model.AkibanInformationSchema;
import com.foundationdb.ais.model.CacheValueGenerator;
import com.foundationdb.ais.model.Column;
import com.foundationdb.ais.model.Join;
import com.foundationdb.ais.model.TableName;
import com.foundationdb.qp.operator.API;
import com.foundationdb.qp.operator.Cursor;
import com.foundationdb.qp.operator.Operator;
import com.foundationdb.server.error.FKValueMismatchException;
import com.foundationdb.server.service.externaldata.JsonRowWriter;
import com.foundationdb.server.service.externaldata.JsonRowWriter.WriteCapturePKRow;
import com.foundationdb.server.service.session.Session;
import com.foundationdb.server.store.Store;
import com.foundationdb.server.types.TClass;
import com.foundationdb.util.AkibanAppender;

public class InsertProcessor extends DMLProcessor {
    private OperatorGenerator insertGenerator;
    private static final Logger LOG = LoggerFactory.getLogger(InsertProcessor.class);

    public InsertProcessor (
            Store store,
            TypesRegistryService typesRegistryService) {
        super (store, typesRegistryService);
    }
    
    private static final CacheValueGenerator<InsertGenerator> CACHED_INSERT_GENERATOR =
            new CacheValueGenerator<InsertGenerator>() {
                @Override
                public InsertGenerator valueFor(AkibanInformationSchema ais) {
                    return new InsertGenerator(ais);
                }
            };

    public String processInsert(Session session, AkibanInformationSchema ais, TableName rootTable, JsonNode node) {
        
        ProcessContext context = new ProcessContext ( ais, session, rootTable);
        insertGenerator = getGenerator(CACHED_INSERT_GENERATOR, context);

        StringBuilder builder = new StringBuilder();
        AkibanAppender appender = AkibanAppender.of(builder);

        processContainer (node, appender, context);
        
        return appender.toString();
    }
    
    private void processContainer (JsonNode node, AkibanAppender appender, ProcessContext context) {
        boolean first = true;
        Map<Column, ValueSource> pkValues = null;
        
        if (node.isObject()) {
            processTable (node, appender, context);
        } else if (node.isArray()) {
            appender.append('[');
            for (JsonNode arrayElement : node) {
                if (first) { 
                    pkValues = context.pkValues;
                    first = false;
                } else {
                    appender.append(',');
                }
                if (arrayElement.isObject()) {
                    processTable (arrayElement, appender, context);
                    context.pkValues = pkValues;
                }
                // else throw Bad Json Format Exception
            }
            appender.append(']');
        } // else throw Bad Json Format Exception
        
    }
    
    private void processTable (JsonNode node, AkibanAppender appender, ProcessContext context) {
        
        // Pass one, insert fields from the table
        Iterator<Entry<String,JsonNode>> i = node.fields();
        while (i.hasNext()) {
            Entry<String,JsonNode> field = i.next();
            if (field.getValue().isValueNode()) {
                setValue (field.getKey(), field.getValue(), context);
            }
        }
        runUpdate(context, appender);
        boolean first = true;
        // pass 2: insert the child nodes
        i = node.fields();
        while (i.hasNext()) {
            Entry<String,JsonNode> field = i.next();
            if (field.getValue().isContainerNode()) {
                if (first) {
                    first = false;
                    // Delete the closing } for the object
                    StringBuilder builder = (StringBuilder)appender.getAppendable();
                    builder.deleteCharAt(builder.length()-1);
                } 
                TableName tableName = TableName.parse(context.tableName.getSchemaName(), field.getKey());
                ProcessContext newContext = new ProcessContext(context.ais(), context.session, tableName);
                newContext.pkValues = context.pkValues;
                appender.append(",\"");
                appender.append(newContext.table.getNameForOutput());
                appender.append("\":");
                processContainer (field.getValue(), appender, newContext);
            }
        }
        // we appended at least one sub-object, so replace the object close brace. 
        if (!first) {
            appender.append('}');
        }
    }
    
    private void setValue (String field, JsonNode node, ProcessContext context) {
        Column column = getColumn (context.table, field);
        if (node.isNull()) {
            setValue (context.queryBindings, column, null, context.typesTranslator);
        } else {
            setValue (context.queryBindings, column, node.asText(), context.typesTranslator);
        }
    }

    private void runUpdate (ProcessContext context, AkibanAppender appender) {
        assert context != null : "Bad Json format";
        LOG.trace("Insert row into: {}, values {}", context.tableName, context.queryContext);
        Operator insert = insertGenerator.create(context.table.getName());
        // If Child table, write the parent group column values into the 
        // child table join key. 
        if (context.pkValues != null && context.table.getParentJoin() != null) {
            Join join = context.table.getParentJoin();
            for (Entry<Column, ValueSource> entry : context.pkValues.entrySet()) {
                
                int pos = join.getMatchingChild(entry.getKey()).getPosition();
                Value fkValue = getFKPvalue (entry.getValue(), context);
                
                if (context.queryBindings.getValue(pos).isNull()) {
                    context.queryBindings.setValue(join.getMatchingChild(entry.getKey()).getPosition(), fkValue);
                } else if (TClass.compare (context.queryBindings.getValue(pos).getType(),
                                context.queryBindings.getValue(pos),
                                fkValue.getType(),
                                fkValue) != 0) {
                    throw new FKValueMismatchException (join.getMatchingChild(entry.getKey()).getName());
                }
            }
        }
        Cursor cursor = API.cursor(insert, context.queryContext, context.queryBindings);
        JsonRowWriter writer = new JsonRowWriter(new TableRowTracker(context.table, 0));
        WriteCapturePKRow rowWriter = new WriteCapturePKRow();
        writer.writeRows(cursor, appender, context.anyUpdates ? "\n" : "", rowWriter);
        context.pkValues = rowWriter.getPKValues();
        context.anyUpdates = true;
    }
    
    private Value getFKPvalue (ValueSource pval, ProcessContext context) {
        AkibanAppender appender = AkibanAppender.of(new StringBuilder());
        pval.getType().format(pval, appender);
        String value = appender.toString();
        Value result = new Value(context.typesTranslator.typeForString(value), value);
        return result;
    }
}
