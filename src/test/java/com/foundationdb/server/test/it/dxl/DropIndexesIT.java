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

package com.foundationdb.server.test.it.dxl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.foundationdb.ais.model.Table;
import com.foundationdb.ais.util.DDLGenerator;
import com.foundationdb.server.api.dml.scan.NewRow;
import com.foundationdb.server.error.InvalidOperationException;
import com.foundationdb.server.error.NoSuchTableException;
import com.foundationdb.server.error.NoSuchIndexException;
import com.foundationdb.server.error.ProtectedIndexException;

import com.foundationdb.server.test.it.ITBase;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

public final class DropIndexesIT extends ITBase {
    private void checkDDL(Integer tableId, String expected) {
        final Table table = getTable(tableId);
        DDLGenerator gen = new DDLGenerator();
        String actual = gen.createTable(table);
        assertEquals(table.getName() + "'s create statement", expected, actual);
    }

    
    @Test
    public void emptyIndexList() throws InvalidOperationException {
        int tid = createTable("test", "t", "id int not null primary key");
        ddl().dropTableIndexes(session(), tableName(tid), Collections.<String>emptyList());
    }
    
    @Test(expected=NoSuchTableException.class)
    public void unknownTable() throws InvalidOperationException {
        ddl().dropTableIndexes(session(), tableName("test", "bar"), Arrays.asList("bar"));
    }
    
    @Test(expected=NoSuchIndexException.class)
    public void unknownIndex() throws InvalidOperationException {
        int tId = createTable("test", "t", "id int not null primary key, name varchar(255)");
        ddl().dropTableIndexes(session(), tableName(tId), Arrays.asList("name"));
    }

    @Test(expected=NoSuchIndexException.class)
    public void hiddenPrimaryKey() throws InvalidOperationException {
        int tId = createTable("test", "t", "id int, name varchar(255)");
        ddl().dropTableIndexes(session(), tableName(tId), Arrays.asList("PRIMARY"));
    }
    
    @Test(expected=ProtectedIndexException.class)
    public void declaredPrimaryKey() throws InvalidOperationException {
        int tId = createTable("test", "t", "id int not null primary key, name varchar(255)");
        ddl().dropTableIndexes(session(), tableName(tId), Arrays.asList("PRIMARY"));
    }

    @Test
    public void basicConfirmNotInAIS() throws InvalidOperationException {
        int tId = createTable("test", "t", "id int not null primary key, name varchar(255)");
        createIndex("test", "t", "name", "name");
        ddl().dropTableIndexes(session(), tableName(tId), Arrays.asList("name"));

        // Index should be gone from Table
        Table table = getTable("test", "t");
        assertNotNull(table);
        assertNull(table.getIndex("name"));
    }
    
    @Test
    public void nonUniqueVarchar() throws InvalidOperationException {
        int tId = createTable("test", "t", "id int not null primary key, name varchar(255)");
        createIndex("test", "t", "name", "name");
        dml().writeRow(session(), createNewRow(tId, 1, "bob"));
        dml().writeRow(session(), createNewRow(tId, 2, "jim"));
        ddl().dropTableIndexes(session(), tableName(tId), Arrays.asList("name"));
        updateAISGeneration();

        checkDDL(tId, "create table `test`.`t`(`id` int NOT NULL, `name` varchar(255) NULL, PRIMARY KEY(`id`)) engine=akibandb DEFAULT CHARSET=UTF8 COLLATE=UCS_BINARY");

        List<NewRow> rows = scanAll(scanAllRequest(tId));
        assertEquals("rows from table scan", 2, rows.size());
    }
    
    @Test
    public void nonUniqueVarcharMiddleOfGroup() throws InvalidOperationException {
        int cId = createTable("coi", "c", "cid int not null primary key, name varchar(32)");
        int oId = createTable("coi", "o", "oid int not null primary key, c_id int, tag varchar(32), GROUPING FOREIGN KEY (c_id) REFERENCES c(cid)");
        createIndex("coi", "o", "tag", "tag");
        createGroupingFKIndex("coi", "o", "__akiban_fk_c", "c_id");
        int iId = createTable("coi", "i", "iid int not null primary key, o_id int, idesc varchar(32), GROUPING FOREIGN KEY (o_id) REFERENCES o(oid)");

        // One customer, two orders, 5 items
        dml().writeRow(session(), createNewRow(cId, 1, "bob"));
        dml().writeRow(session(), createNewRow(oId, 1, 1, "supplies"));
        dml().writeRow(session(), createNewRow(oId, 2, 1, "random"));
        dml().writeRow(session(), createNewRow(iId, 1, 1, "foo"));
        dml().writeRow(session(), createNewRow(iId, 2, 1, "bar"));
        dml().writeRow(session(), createNewRow(iId, 3, 2, "zap"));
        dml().writeRow(session(), createNewRow(iId, 4, 2, "fob"));
        dml().writeRow(session(), createNewRow(iId, 5, 2, "baz"));
        
        ddl().dropTableIndexes(session(), tableName(oId), Arrays.asList("tag"));
        updateAISGeneration();
        
        checkDDL(oId, "create table `coi`.`o`(`oid` int NOT NULL, `c_id` int NULL, `tag` varchar(32) NULL, PRIMARY KEY(`oid`), "+
                      "CONSTRAINT `__akiban_fk_c` FOREIGN KEY `__akiban_fk_c`(`c_id`) REFERENCES `c`(`cid`)) engine=akibandb DEFAULT CHARSET=UTF8 COLLATE=UCS_BINARY");

        List<NewRow> rows = scanAll(scanAllRequest(cId));
        assertEquals("customers from table scan", 1, rows.size());
        rows = scanAll(scanAllRequest(oId));
        assertEquals("orders from table scan", 2, rows.size());
        rows = scanAll(scanAllRequest(iId));
        assertEquals("items from table scan", 5, rows.size());
    }
    
    @Test
    public void nonUniqueCompoundVarcharVarchar() throws InvalidOperationException {
        int tId = createTable("test", "t", "id int not null primary key, \"first\" varchar(250), \"last\" varchar(250)");
        createIndex("test", "t", "name", "\"first\"", "\"last\"");
        dml().writeRow(session(), createNewRow(tId, 1, "foo", "bar"));
        dml().writeRow(session(), createNewRow(tId, 2, "zap", "snap"));
        dml().writeRow(session(), createNewRow(tId, 3, "baz", "fob"));
        ddl().dropTableIndexes(session(), tableName(tId), Arrays.asList("name"));
        updateAISGeneration();
        
        checkDDL(tId, "create table `test`.`t`(`id` int NOT NULL, `first` varchar(250) NULL, `last` varchar(250) NULL, PRIMARY KEY(`id`)) engine=akibandb DEFAULT CHARSET=UTF8 COLLATE=UCS_BINARY");

        List<NewRow> rows = scanAll(scanAllRequest(tId));
        assertEquals("rows from table scan", 3, rows.size());
    }
    
    @Test
    public void uniqueChar() throws InvalidOperationException {
        int tId = createTable("test", "t", "id int not null primary key, state char(2), unique(state)");
        dml().writeRow(session(), createNewRow(tId, 1, "IA"));
        dml().writeRow(session(), createNewRow(tId, 2, "WA"));
        dml().writeRow(session(), createNewRow(tId, 3, "MA"));
        
        ddl().dropTableIndexes(session(), tableName(tId), Arrays.asList("state"));
        updateAISGeneration();
        
        checkDDL(tId, "create table `test`.`t`(`id` int NOT NULL, `state` char(2) NULL, PRIMARY KEY(`id`)) engine=akibandb DEFAULT CHARSET=UTF8 COLLATE=UCS_BINARY");

        List<NewRow> rows = scanAll(scanAllRequest(tId));
        assertEquals("rows from table scan", 3, rows.size());
    }
    
    @Test
    public void uniqueIntNonUniqueDecimal() throws InvalidOperationException {
        int tId = createTable("test", "t", "id int not null primary key, otherid int, price decimal(10,2), unique(otherid)");
        createIndex("test", "t", "price", "price");
        dml().writeRow(session(), createNewRow(tId, 1, 1337, "10.50"));
        dml().writeRow(session(), createNewRow(tId, 2, 5000, "10.50"));
        dml().writeRow(session(), createNewRow(tId, 3, 47000, "9.99"));
        
        ddl().dropTableIndexes(session(), tableName(tId), Arrays.asList("otherId", "price"));
        updateAISGeneration();
        
        checkDDL(tId, "create table `test`.`t`(`id` int NOT NULL, `otherid` int NULL, `price` decimal(10, 2) NULL, PRIMARY KEY(`id`)) engine=akibandb DEFAULT CHARSET=UTF8 COLLATE=UCS_BINARY");

        List<NewRow> rows = scanAll(scanAllRequest(tId));
        assertEquals("rows from table scan", 3, rows.size());
    }
}
