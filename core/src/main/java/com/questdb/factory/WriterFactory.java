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
 ******************************************************************************/

package com.questdb.factory;

import com.questdb.JournalKey;
import com.questdb.JournalWriter;
import com.questdb.ex.JournalException;
import com.questdb.factory.configuration.JournalConfiguration;
import com.questdb.factory.configuration.JournalMetadata;
import com.questdb.factory.configuration.MetadataBuilder;

public interface WriterFactory {

    JournalConfiguration getConfiguration();

    <T> JournalWriter<T> writer(Class<T> clazz) throws JournalException;

    <T> JournalWriter<T> writer(Class<T> clazz, String location) throws JournalException;

    <T> JournalWriter<T> writer(Class<T> clazz, String location, int recordHint) throws JournalException;

    JournalWriter writer(String location) throws JournalException;

    <T> JournalWriter<T> writer(JournalKey<T> key) throws JournalException;

    <T> JournalWriter<T> writer(MetadataBuilder<T> metadataBuilder) throws JournalException;

    <T> JournalWriter<T> writer(JournalMetadata<T> metadata) throws JournalException;
}