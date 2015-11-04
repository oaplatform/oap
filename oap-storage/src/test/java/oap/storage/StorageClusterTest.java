/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import oap.application.Application;
import oap.application.Kernel;
import oap.application.Module;
import oap.replication.TestReplication;
import oap.testng.AbstractTest;
import oap.testng.Asserts;
import oap.testng.Env;
import oap.util.Maps;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static oap.util.Pair.__;
import static oap.ws.testng.HttpAsserts.HTTP_PREFIX;
import static org.testng.Assert.assertEquals;

/**
 * Created by Igor Petrenko on 05.10.2015.
 */
public class StorageClusterTest extends AbstractTest {
    private static final TypeReference<List<Metadata<Bean>>> REF = new TypeReference<List<Metadata<Bean>>>() {
    };
    private final Kernel kernel = new Kernel( Module.fromClassPath() );

    private LinkedHashMap<String, Map<String, Object>> properties = Maps.of(
        __( "oap-ws-server", Maps.of( __( "port", Env.port() ) ) ),
        __( "test-storage-master", Maps.of( __( "path", Env.tmp( "/data/beans" ) ) ) ),
        __( "test-storage-replication", Maps.of( __( "replicationUrl", HTTP_PREFIX + "/replication/" ) ) )
    );

    @BeforeClass
    public void beforeClass() {
        kernel.start( properties );
    }

    @AfterClass
    @Override
    public void afterClass() {
        kernel.stop();

        super.afterClass();
    }

    @BeforeMethod
    @Override
    public void beforeMethod() {
        super.beforeMethod();

        Application.<Storage<?>>service( "test-storage" ).clear();
        Application.<Storage<?>>service( "master.test-storage" ).clear();
    }

    @Test
    public void testGet() {
        Storage<Bean> storageMaster = Application.service( "master.test-storage" );
        storageMaster.store( new Bean( "1" ) );

        final long time = DateTimeUtils.currentTimeMillis();

        List<Metadata<Bean>> metadata = TestReplication.sync( REF, -1, "master.test-storage" );
        assertEquals( metadata.size(), 1 );
        assertEquals( metadata.get( 0 ).object.id, "1" );

        List<Metadata<Bean>> metadata2 = TestReplication.sync( REF, time, "master.test-storage" );
        assertEquals( metadata2.size(), 0 );

        storageMaster.store( new Bean( "1", "bbb" ) );
        List<Metadata<Bean>> metadata3 = TestReplication.sync( REF, time, "master.test-storage" );
        assertEquals( metadata3.size(), 1 );
        assertEquals( metadata3.get( 0 ).object.s, "bbb" );
    }

    @Test
    public void testSlaveStorage() {
        Storage<Bean> storageSlave = Application.service( "test-storage" );

        Asserts.assertException( Throwable.class, () -> storageSlave.store( new Bean( "1" ) ) );
        Asserts.assertException( Throwable.class, () -> storageSlave.update( "1", ( b ) -> {
        } ) );
        Asserts.assertException( Throwable.class, () -> storageSlave.update( "1", ( b ) -> {
        }, () -> null ) );

    }

    @Test
    public void testSync() {
        Storage<Bean> storageMaster = Application.service( "master.test-storage" );
        storageMaster.store( new Bean( "1" ) );

        Storage<Bean> storageSlave = Application.service( "test-storage" );
        StorageReplicationGet<Bean> storageSlaveRep = Application.service( "test-storage-replication" );

        assertEquals( storageSlave.select().count(), 0 );
        storageSlaveRep.run();
        assertEquals( storageSlave.select().count(), 1 );
        assertEquals( storageSlave.select().findFirst().get().id, "1" );
    }
}
