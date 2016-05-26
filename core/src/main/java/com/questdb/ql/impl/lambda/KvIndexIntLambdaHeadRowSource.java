/*******************************************************************************
 *    ___                  _   ____  ____
 *   / _ \ _   _  ___  ___| |_|  _ \| __ )
 *  | | | | | | |/ _ \/ __| __| | | |  _ \
 *  | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *   \__\_\\__,_|\___||___/\__|____/|____/
 *
 * Copyright (C) 2014-2016 Appsicle
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * As a special exception, the copyright holders give permission to link the
 * code of portions of this program with the OpenSSL library under certain
 * conditions as described in each individual source file and distribute
 * linked combinations including the program with the OpenSSL library. You
 * must comply with the GNU Affero General Public License in all respects for
 * all of the code used other than as permitted herein. If you modify file(s)
 * with this exception, you may extend this exception to your version of the
 * file(s), but you are not obligated to do so. If you do not wish to do so,
 * delete this exception statement from your version. If you delete this
 * exception statement from all source files in the program, then also delete
 * it in the license file.
 *
 ******************************************************************************/

package com.questdb.ql.impl.lambda;

import com.questdb.Partition;
import com.questdb.ex.JournalException;
import com.questdb.ex.JournalRuntimeException;
import com.questdb.factory.configuration.JournalMetadata;
import com.questdb.ql.*;
import com.questdb.ql.impl.AbstractRowSource;
import com.questdb.ql.impl.JournalRecord;
import com.questdb.ql.ops.VirtualColumn;
import com.questdb.std.CharSink;
import com.questdb.std.IntHashSet;
import com.questdb.std.LongList;
import com.questdb.store.FixedColumn;
import com.questdb.store.IndexCursor;
import com.questdb.store.KVIndex;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class KvIndexIntLambdaHeadRowSource extends AbstractRowSource {

    public static final LatestByLambdaRowSourceFactory FACTORY = new Factory();
    private final String column;
    private final VirtualColumn filter;
    private final RecordSource recordSource;
    private final int recordSourceColumn;
    private final LongList rows = new LongList();
    private final IntHashSet keys = new IntHashSet();
    private JournalRecord rec;
    private int cursor;
    private int buckets;
    private int columnIndex;

    private KvIndexIntLambdaHeadRowSource(String column, RecordSource recordSource, int recordSourceColumn, VirtualColumn filter) {
        this.column = column;
        this.recordSource = recordSource;
        this.recordSourceColumn = recordSourceColumn;
        this.filter = filter;
    }

    @Override
    public void configure(JournalMetadata metadata) {
        this.rec = new JournalRecord(metadata);
        this.columnIndex = metadata.getColumnIndex(column);
        this.buckets = metadata.getColumnQuick(columnIndex).distinctCountHint;
    }

    @SuppressFBWarnings({"EXS_EXCEPTION_SOFTENING_NO_CHECKED"})
    @Override
    public RowCursor prepareCursor(PartitionSlice slice) {
        try {
            Partition partition = rec.partition = slice.partition.open();
            KVIndex index = partition.getIndexForColumn(columnIndex);
            FixedColumn col = partition.fixCol(columnIndex);

            long lo = slice.lo - 1;
            long hi = slice.calcHi ? partition.size() : slice.hi + 1;
            rows.clear();

            for (int i = 0, n = keys.size(); i < n; i++) {
                IndexCursor c = index.cursor(keys.get(i) & buckets);
                while (c.hasNext()) {
                    long r = rec.rowid = c.next();
                    if (r > lo && r < hi && col.getInt(r) == keys.get(i) && (filter == null || filter.getBool(rec))) {
                        rows.add(r);
                        break;
                    }
                }
            }
            rows.sort();
            cursor = 0;
            return this;
        } catch (JournalException e) {
            throw new JournalRuntimeException(e);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public boolean hasNext() {
        return cursor < rows.size();
    }

    @Override
    public long next() {
        return rec.rowid = rows.getQuick(cursor++);
    }

    @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
    @Override
    public void prepare(StorageFacade fa, CancellationHandler cancellationHandler) {
        if (filter != null) {
            filter.prepare(fa);
        }

        keys.clear();
        try {
            for (Record r : recordSource.prepareCursor(fa.getFactory(), cancellationHandler)) {
                keys.add(r.getInt(recordSourceColumn));
            }
        } catch (JournalException e) {
            throw new JournalRuntimeException(e);
        }

    }

    @Override
    public void toSink(CharSink sink) {
        sink.put('{');
        sink.putQuoted("op").put(':').putQuoted("KvIndexIntLambdaHeadRowSource").put(',');
        sink.putQuoted("column").put(':').putQuoted(column).put(',');
        sink.putQuoted("src").put(':').put(recordSource);
        sink.put('}');
    }

    private static class Factory implements LatestByLambdaRowSourceFactory {
        @Override
        public RowSource newInstance(String column, RecordSource recordSource, int recordSourceColumn, VirtualColumn filter) {
            return new KvIndexIntLambdaHeadRowSource(column, recordSource, recordSourceColumn, filter);
        }
    }
}