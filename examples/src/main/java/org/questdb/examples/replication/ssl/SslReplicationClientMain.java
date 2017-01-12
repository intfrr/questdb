/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2016 Appsicle
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package org.questdb.examples.replication.ssl;

import com.questdb.Journal;
import com.questdb.JournalIterators;
import com.questdb.factory.MegaFactory;
import com.questdb.factory.configuration.JournalConfiguration;
import com.questdb.factory.configuration.JournalConfigurationBuilder;
import com.questdb.net.ha.JournalClient;
import com.questdb.net.ha.config.ClientConfig;
import com.questdb.store.JournalListener;
import org.questdb.examples.support.Price;

import java.io.InputStream;

/**
 * Single journal replication client example.
 *
 * @since 2.0.1
 */
public class SslReplicationClientMain {
    public static void main(String[] args) throws Exception {
        JournalConfiguration configuration = new JournalConfigurationBuilder().build(args[0]);
        MegaFactory factory = new MegaFactory(configuration, 1000, 1);

        final JournalClient client = new JournalClient(
                new ClientConfig() {{
                    getSslConfig().setSecure(true);

                    // client has to have server Cert in trust store
                    // unless Cert is issued by recognised authority
                    // for purpose of this demo our Cert is self-signed

                    // it is possible to trust "all" server certs,
                    // which eliminates need for trust store maintenance
                    // to do that uncomment the following line:

                    //getSslConfig().setTrustAll(true);

                    try (InputStream is = this.getClass().getResourceAsStream("/keystore/singlekey.ks")) {
                        getSslConfig().setTrustStore(is, "changeit");
                    }
                }}
                , factory
        );

        final Journal<Price> reader = factory.reader(Price.class, "price-copy");
        reader.setSequentialAccess(true);

        client.subscribe(Price.class, null, "price-copy", new JournalListener() {
            @Override
            public void onCommit() {
                int count = 0;
                long t = 0;
                for (Price p : JournalIterators.incrementBufferedIterator(reader)) {
                    if (count == 0) {
                        t = p.getNanos();
                    }
                    count++;
                }
                System.out.println("took: " + (System.nanoTime() - t) + ", count=" + count);
            }

            @Override
            public void onEvent(int event) {
                System.out.println("There was an error");
            }
        });
        client.start();

        System.out.println("Client started");
    }
}
