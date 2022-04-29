/*
 * Copyright 2016-2017 Leon Chen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.moilioncircle.redis.replicator.rdb.dump.parser;

import static com.moilioncircle.redis.replicator.Constants.RDB_OPCODE_FUNCTION;
import static com.moilioncircle.redis.replicator.Constants.RDB_OPCODE_FUNCTION2;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_HASH;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_HASH_LISTPACK;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_HASH_ZIPLIST;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_HASH_ZIPMAP;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_LIST;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_LIST_QUICKLIST;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_LIST_QUICKLIST_2;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_LIST_ZIPLIST;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_MODULE;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_MODULE_2;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_SET;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_SET_INTSET;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_STREAM_LISTPACKS;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_STREAM_LISTPACKS_2;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_STRING;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_ZSET;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_ZSET_2;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_ZSET_LISTPACK;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_ZSET_ZIPLIST;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.event.EventListener;
import com.moilioncircle.redis.replicator.io.RedisInputStream;
import com.moilioncircle.redis.replicator.rdb.RdbValueVisitor;
import com.moilioncircle.redis.replicator.rdb.datatype.Function;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePairs;
import com.moilioncircle.redis.replicator.rdb.dump.datatype.DumpFunction;
import com.moilioncircle.redis.replicator.rdb.dump.datatype.DumpKeyValuePair;
import com.moilioncircle.redis.replicator.rdb.iterable.ValueIterableEventListener;
import com.moilioncircle.redis.replicator.rdb.iterable.ValueIterableRdbValueVisitor;
import com.moilioncircle.redis.replicator.util.ByteArray;

/**
 * @author Leon Chen
 * @since 3.1.0
 */
public class IterableDumpValueParser implements DumpValueParser {

    protected final int batchSize;
    protected final boolean order;
    protected final Replicator replicator;
    protected final RdbValueVisitor valueVisitor;

    public IterableDumpValueParser(Replicator replicator) {
        this(64, replicator);
    }

    public IterableDumpValueParser(int batchSize, Replicator replicator) {
        this(true, batchSize, replicator);
    }

    public IterableDumpValueParser(boolean order, int batchSize, Replicator replicator) {
        Objects.requireNonNull(replicator);
        this.order = order;
        this.batchSize = batchSize;
        this.replicator = replicator;
        this.valueVisitor = new ValueIterableRdbValueVisitor(replicator);
    }

    @Override
    public void parse(DumpKeyValuePair kv, EventListener listener) {
        Objects.requireNonNull(listener);
        new ValueIterableEventListener(order, batchSize, listener).onEvent(replicator, parse(kv));
    }
    
    @Override
    public void parse(DumpFunction function, EventListener listener) {
        Objects.requireNonNull(listener);
        new ValueIterableEventListener(order, batchSize, listener).onEvent(replicator, parse(function));
    }
    
    @Override
    public Function parse(DumpFunction function) {
        Objects.requireNonNull(function);
        try (RedisInputStream in = new RedisInputStream(new ByteArray(function.getSerialized()))) {
            int valueType = in.read();
            if (valueType == RDB_OPCODE_FUNCTION) {
                return valueVisitor.applyFunction(in, 0);
            } else if (valueType == RDB_OPCODE_FUNCTION2) {
                return valueVisitor.applyFunction2(in, 0);
            } else {
                throw new AssertionError("unexpected value type:" + valueType);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    @Override
    public KeyValuePair<?, ?> parse(DumpKeyValuePair kv) {
        Objects.requireNonNull(kv);
        try (RedisInputStream in = new RedisInputStream(new ByteArray(kv.getValue()))) {
            int valueType = in.read();
            switch (valueType) {
                case RDB_TYPE_STRING:
                    return KeyValuePairs.string(kv, valueVisitor.applyString(in, 0));
                case RDB_TYPE_LIST:
                    return KeyValuePairs.iterList(kv, valueVisitor.applyList(in, 0));
                case RDB_TYPE_SET:
                    return KeyValuePairs.iterSet(kv, valueVisitor.applySet(in, 0));
                case RDB_TYPE_ZSET:
                    return KeyValuePairs.iterZset(kv, valueVisitor.applyZSet(in, 0));
                case RDB_TYPE_ZSET_2:
                    return KeyValuePairs.iterZset(kv, valueVisitor.applyZSet2(in, 0));
                case RDB_TYPE_HASH:
                    return KeyValuePairs.iterHash(kv, valueVisitor.applyHash(in, 0));
                case RDB_TYPE_HASH_ZIPMAP:
                    return KeyValuePairs.iterHash(kv, valueVisitor.applyHashZipMap(in, 0));
                case RDB_TYPE_LIST_ZIPLIST:
                    return KeyValuePairs.iterList(kv, valueVisitor.applyListZipList(in, 0));
                case RDB_TYPE_SET_INTSET:
                    return KeyValuePairs.iterSet(kv, valueVisitor.applySetIntSet(in, 0));
                case RDB_TYPE_ZSET_ZIPLIST:
                    return KeyValuePairs.iterZset(kv, valueVisitor.applyZSetZipList(in, 0));
                case RDB_TYPE_ZSET_LISTPACK:
                    return KeyValuePairs.iterZset(kv, valueVisitor.applyZSetListPack(in, 0));
                case RDB_TYPE_HASH_ZIPLIST:
                    return KeyValuePairs.iterHash(kv, valueVisitor.applyHashZipList(in, 0));
                case RDB_TYPE_HASH_LISTPACK:
                    return KeyValuePairs.iterHash(kv, valueVisitor.applyHashListPack(in, 0));
                case RDB_TYPE_LIST_QUICKLIST:
                    return KeyValuePairs.iterList(kv, valueVisitor.applyListQuickList(in, 0));
                case RDB_TYPE_LIST_QUICKLIST_2:
                    return KeyValuePairs.iterList(kv, valueVisitor.applyListQuickList2(in, 0));
                case RDB_TYPE_MODULE:
                    return KeyValuePairs.module(kv, valueVisitor.applyModule(in, 0));
                case RDB_TYPE_MODULE_2:
                    return KeyValuePairs.module(kv, valueVisitor.applyModule2(in, 0));
                case RDB_TYPE_STREAM_LISTPACKS:
                    return KeyValuePairs.stream(kv, valueVisitor.applyStreamListPacks(in, 0));
                case RDB_TYPE_STREAM_LISTPACKS_2:
                    return KeyValuePairs.stream(kv, valueVisitor.applyStreamListPacks2(in, 0));
                default:
                    throw new AssertionError("unexpected value type:" + valueType);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
