/**
 * Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package com.akiban.server.itests.bugs.bug696156;

import com.akiban.server.InvalidOperationException;
import com.akiban.server.api.dml.DuplicateKeyException;
import com.akiban.server.api.dml.scan.ScanAllRequest;
import com.akiban.server.itests.ApiTestBase;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MultipleNullUniqueIndexIT  extends ApiTestBase {

    @Test
    public void reportTestCase() throws InvalidOperationException {
        final int tid = createTable("test", "t1", "c1 TINYINT AUTO_INCREMENT NULL UNIQUE KEY");
        writeRows(createNewRow(tid, null, -1L));
        writeRows(createNewRow(tid, null, -1L));
        expectFullRows(tid,
                       createNewRow(tid, (Object)null),
                       createNewRow(tid, (Object)null));
    }

    @Test
    public void singleColumnUniqueWithNulls() throws InvalidOperationException {
        final int tid = createTable("test", "t1", "id int key, name varchar(32), unique(name)");
        writeRows(createNewRow(tid, 1, "abc"),
                  createNewRow(tid, 2, "def"),
                  createNewRow(tid, 3, null),
                  createNewRow(tid, 4, "ghi"),
                  createNewRow(tid, 5, null));
        assertEquals(5, scanAll(new ScanAllRequest(tid, null)).size());

        try {
            writeRows(createNewRow(tid, 6, "abc"));
            Assert.fail("DuplicateKeyException expected");
        }
        catch(DuplicateKeyException e) {
        }
    }

    @Test
    public void multiColumnUniqueWithNulls() throws InvalidOperationException {
        final int tid = createTable("test", "t1", "id int key, seg1 int, seg2 int, seg3 int, unique(seg1,seg2,seg3)");
        writeRows(createNewRow(tid, 1, 1, 1, 1),
                  createNewRow(tid, 2, 1, 1, null),
                  createNewRow(tid, 3, 1, null, 1),
                  createNewRow(tid, 4, 1, null, null),
                  createNewRow(tid, 5, null, 1, 1),
                  createNewRow(tid, 6, null, 1, null),
                  createNewRow(tid, 7, null, null, null),
                  createNewRow(tid, 8, null, null, null));
        assertEquals(8, scanAll(new ScanAllRequest(tid, null)).size());

        try {
            writeRows(createNewRow(tid, 9, 1, 1, 1));
            Assert.fail("DuplicateKeyException expected");
        }
        catch(DuplicateKeyException e) {
        }
    }

    @Test
    public void singleColumnIndexWithNulls() throws InvalidOperationException {
        final int tid = createTable("test", "t1", "id int key, name varchar(32), index(name)");
        writeRows(createNewRow(tid, 1, "abc"),
                  createNewRow(tid, 2, "def"),
                  createNewRow(tid, 3, "abc"),
                  createNewRow(tid, 4, null),
                  createNewRow(tid, 5, null));
        assertEquals(5, scanAll(new ScanAllRequest(tid, null)).size());
    }

    @Test
    public void multiColumnIndexWithNulls() throws InvalidOperationException {
        final int tid = createTable("test", "t1", "id int key, seg1 int, seg2 int, index(seg1,seg2)");
        writeRows(createNewRow(tid, 1, 1, 1),
                  createNewRow(tid, 2, 2, 2),
                  createNewRow(tid, 3, 1, 1),
                  createNewRow(tid, 4, 1, null),
                  createNewRow(tid, 5, null, 1),
                  createNewRow(tid, 6, 1, null),
                  createNewRow(tid, 7, null, 1),
                  createNewRow(tid, 8, null, null),
                  createNewRow(tid, 9, null, null));
        assertEquals(9, scanAll(new ScanAllRequest(tid, null)).size());
    }
}