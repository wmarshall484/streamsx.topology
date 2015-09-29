/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015  
 */
package com.ibm.streamsx.topology.test.api;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.ibm.streamsx.topology.TStream;
import com.ibm.streamsx.topology.Topology;
import com.ibm.streamsx.topology.context.StreamsContext;
import com.ibm.streamsx.topology.context.StreamsContextFactory;
import com.ibm.streamsx.topology.function.Function;
import com.ibm.streamsx.topology.function.FunctionContext;
import com.ibm.streamsx.topology.function.Initializable;
import com.ibm.streamsx.topology.function.UnaryOperator;
import com.ibm.streamsx.topology.test.AllowAll;
import com.ibm.streamsx.topology.test.TestTopology;
import com.ibm.streamsx.topology.tester.Condition;
import com.ibm.streamsx.topology.tester.MultiLongCondition;
import com.ibm.streamsx.topology.tester.Tester;

public class IsolateTest extends TestTopology {

    @Test
    public void simpleIsolationTest() throws Exception {
        assumeTrue(SC_OK);
        assumeTrue(getTesterType() == StreamsContext.Type.DISTRIBUTED_TESTER);
        
        Topology topology = new Topology("simpleIsolationTest");

        // Construct topology
        TStream<String> ss = topology.strings("hello");
        TStream<String> ss1 = ss.transform(getContainerId()).isolate();
        TStream<String> ss2 = ss.isolate().transform(getContainerId())
                .isolate();

        Tester tester = topology.getTester();

        Condition<List<String>> condss1 = tester.stringContents(ss1, "");
        Condition<List<String>> condss2 = tester.stringContents(ss2, "");

        Condition<Long> condss1Cnt = tester.tupleCount(ss1, 1);
        Condition<Long> condss2Cnt = tester.tupleCount(ss2, 1);
        Condition<Long> endCond = new MultiLongCondition(Arrays.asList(condss1Cnt, condss2Cnt));
        
        complete(topology.getTester(), endCond, 15, TimeUnit.SECONDS);

        Integer result1 = Integer.parseInt(condss1.getResult().get(0));
        Integer result2 = Integer.parseInt(condss2.getResult().get(0));

        Set<Integer> m = new HashSet<>();

        m.add(result1);
        m.add(result2);
        assertTrue(m.size() == 2);
    }
    
    @Test
    public void isolateIsEndOfStreamTest() throws Exception {
        assumeTrue(SC_OK);
        assumeTrue(getTesterType() == StreamsContext.Type.DISTRIBUTED_TESTER);
        Topology topology = new Topology("isolateIsEndOfStreamTest");

        // Construct topology
        TStream<String> ss = topology.strings("hello");
        TStream<String> ss1 = topology.strings("hello");
        TStream<String> un = ss.union(ss1);
        un.isolate();
        
        Tester tester = topology.getTester();
        
        Condition<List<String>> hellos = tester.stringContentsUnordered(un, "hello", "hello");
        complete(tester, hellos, 60, TimeUnit.SECONDS);
        
        assertTrue(hellos.valid());
    }

    @Test
    public void multipleIsolationTest() throws Exception {
        assumeTrue(SC_OK);
        assumeTrue(getTesterType() == StreamsContext.Type.DISTRIBUTED_TESTER);
        Topology topology = new Topology("multipleIsolationTest");

        TStream<String> ss = topology.strings("hello");
        TStream<String> ss0 = ss.isolate();
        TStream<String> ss1 = ss0.transform(getContainerId());
        TStream<String> ss8 = ss1.isolate().transform(getContainerId())
                .transform(getContainerId());

        TStream<String> ss3 = ss.transform(getContainerId()).isolate();
        TStream<String> ss4 = ss3.transform(getContainerId()).isolate();
        TStream<String> ss5 = ss4.transform(getContainerId()).isolate();
        ss5.transform(getContainerId());

        TStream<String> ss7 = ss3.transform(getContainerId());
        
        Set<TStream<String>> set = new HashSet<>();
        set.add(ss1);
        set.add(ss8);
        set.add(ss3);
        set.add(ss4);
        set.add(ss5);
        
        TStream<String> out = ss7.union(set).transform(uniqueStringCounter(6));
        
        Tester tester = topology.getTester();
        Condition<Long> numIsolateRegions =tester.tupleCount(out, 6);

        complete(tester, numIsolateRegions, 60, TimeUnit.SECONDS);
        
        assertTrue(numIsolateRegions.valid());
    }

    /**
     * Test that a topology fails to generate SPL if an isolated stream is 
     * unioned with its parent.
     * @throws Exception Thrown because the ss4 stream is the parent of the ss7
     * stream. Taking the union of the two is currently not supported. In
     * future releases, we will automatically insert an isolation marker to 
     * support this kind of union.
     */
    @Test(expected = IllegalStateException.class)
    public void multipleIsolationExceptionTest() throws Exception {
        assumeTrue(SC_OK);
        assumeTrue(getTesterType() == StreamsContext.Type.DISTRIBUTED_TESTER);
        Topology topology = new Topology("multipleIsolationExceptionTest");

        TStream<String> ss = topology.strings("hello", "world");
        TStream<String> ss0 = ss.isolate();
        TStream<String> ss1 = ss0.transform(getContainerId());
        ss1.isolate().transform(getContainerId())
                .transform(getContainerId()).print();

        TStream<String> ss3 = ss.transform(getContainerId()).isolate();
        TStream<String> ss4 = ss3.transform(getContainerId()).isolate();
        TStream<String> ss5 = ss4.transform(getContainerId()).isolate();
        ss5.transform(getContainerId()).print();

        TStream<String> ss7 = ss3.transform(getContainerId());

        // Unions a stream with its parent.
        ss7.union(ss4).print();

        complete(topology.getTester());
    }
    
    @Test
    public void islandIsolationTest() throws Exception {
        assumeTrue(SC_OK);
        assumeTrue(getTesterType() == StreamsContext.Type.DISTRIBUTED_TESTER);
        Topology topology = new Topology("islandIsolationTest");

        TStream<String> ss = topology.strings("hello");
        TStream<String> out1 = ss.transform(getContainerId());
        TStream<String> out3 = out1.isolate()
                .transform(getContainerId());
        
        // Create island subgraph
        TStream<String> ss2 = topology.strings("hello");
        TStream<String> out2 = ss2.transform(getContainerId());
        Set<TStream<String>> set = new HashSet<>();
        set.add(out3);
        set.add(out2);
        TStream<String> regionCount = out1.union(set).transform(uniqueStringCounter(3));
        
        Tester tester = topology.getTester();
        Condition<Long> expectedCount = tester.tupleCount(regionCount, 3);
        complete(tester, expectedCount, 60, TimeUnit.SECONDS);
        
        assertTrue(expectedCount.valid());
    }

    @Test
    public void unionIsolateTest() throws Exception {
        assumeTrue(SC_OK);
        assumeTrue(getTesterType() == StreamsContext.Type.DISTRIBUTED_TESTER);
        Topology topology = new Topology("unionIsolateTest");

        TStream<String> s1 = topology.strings("1");
        TStream<String> s2 = topology.strings("2");
        TStream<String> s3 = topology.strings("3");
        TStream<String> s4 = topology.strings("4");

        Set<TStream<String>> l = new HashSet<>();
        Set<TStream<String>> outSet = new HashSet<>();
        l.add(s2);
        l.add(s3);
        l.add(s4);

        TStream<String> n = s1.union(l).isolate();

        TStream<String> out1 = n.filter(new AllowAll<String>());
        TStream<String> out2 = n.filter(new AllowAll<String>());
        TStream<String> out3 = n.filter(new AllowAll<String>());
        TStream<String> out4 = n.filter(new AllowAll<String>());
        outSet.add(out2);
        outSet.add(out3);
        outSet.add(out4);
        
        TStream<String> out_total = s1.union(outSet);

        Tester tester = topology.getTester();
        Condition<Long> expectedCounts1 = tester.tupleCount(out1, 4);
        Condition<List<String>> expectedContents1 = tester
                .stringContentsUnordered(out1, "1", "2", "3", "4");

        Condition<Long> expectedCounts2 = tester.tupleCount(out2, 4);
        Condition<List<String>> expectedContents2 = tester
                .stringContentsUnordered(out2, "1", "2", "3", "4");
        
        Condition<Long> expectedCounts3 = tester.tupleCount(out3, 4);
        Condition<List<String>> expectedContents3 = tester
                .stringContentsUnordered(out3, "1", "2", "3", "4");
        
        Condition<Long> expectedCounts4 = tester.tupleCount(out4, 4);
        Condition<List<String>> expectedContents4 = tester
                .stringContentsUnordered(out4, "1", "2", "3", "4");
        
        Condition<Long> total_count = tester.tupleCount(out_total, 16);
        
        complete(tester, total_count, 60, TimeUnit.SECONDS);

        assertTrue(expectedCounts1.valid());
        assertTrue(expectedContents1.valid());
        assertTrue(expectedCounts2.valid());
        assertTrue(expectedContents2.valid());
        assertTrue(expectedCounts3.valid());
        assertTrue(expectedContents3.valid());
        assertTrue(expectedCounts4.valid());
        assertTrue(expectedContents4.valid());
    }
    
    @Test(expected = IllegalStateException.class)
    public void lowLatencyViolationTest() throws Exception {

        /** lowLatency -> ... isolate ... -> endLowLatency */
        
        final Topology topology = new Topology("lowLatencyViolationTest");
        
        topology.strings("a")
                .lowLatency()
                .modify(getContainerIdAppend())
                .isolate()  // expect ISE: not legal in a low latency region
                .modify(getContainerIdAppend())
                .endLowLatency()
                ;
    }
       
    /**
     * Get the container ids from a tuple of the form produced with
     * getContainerIdAgg() - i.e. <some-tag> <id1> [<id2> ...]
     * @param results
     * @return
     */
    public static Set<String> getContainerIds(List<String> results) {
        Set<String> ids = new HashSet<>();
        for (String s : results) {
            boolean first = true;
            for (String stok : s.split(" ")) {
                if (first) {
                    first = false;
                    continue;
                }
                // see GetContainerIdAndChannelAppend
                String[] idParts = stok.split("::ch-");
                ids.add(idParts[0]); // just the container id
            }
        }        
        return ids;
    }


    public static Function<String, String> getContainerId() {
        return new GetContainerId();
    }
    
    @SuppressWarnings("serial")
    public static final class GetContainerId implements
            Function<String, String> , Initializable {
        
        private String id;
        @Override
        public String apply(String v) {
            return id;
        }

        @Override
        public void initialize(FunctionContext functionContext)
                throws Exception {
            id = functionContext.getContainer().getId();
        }
    }

    /**
     * Create a UnaryOperator function that appends the fn's container id
     * onto the tuple's value.
     * @return the function
     */
    public static UnaryOperator<String> getContainerIdAppend() {
        return new GetContainerIdAppend();
    }
    
    /**
     * A UnaryOperator that appends the fn's container id onto the tuple's value.
     */
    @SuppressWarnings("serial")
    public static final class GetContainerIdAppend implements
            UnaryOperator<String> , Initializable {
        
        private String id;
        @Override
        public String apply(String v) {
            return v + " " + id;
        }

        @Override
        public void initialize(FunctionContext functionContext)
                throws Exception {
            id = functionContext.getContainer().getId();
        }
    }

    /**
     * Create a UnaryOperator function that appends the fn's container id
     * and channel onto the tuple's value.
     * @return the function
     */
    public static UnaryOperator<String> getContainerIdAndChannelAppend() {
        return new GetContainerIdAppend();
    }
    
    /**
     * A UnaryOperator that appends the fn's container id and parallel channel onto the tuple's value.
     */
    @SuppressWarnings("serial")
    public static final class GetContainerIdAndChannelAppend implements
            UnaryOperator<String> , Initializable {
        
        private String id;
        private int channel;
        @Override
        public String apply(String v) {
            return String.format("%s %s::ch-%d", v, id, channel);
        }

        @Override
        public void initialize(FunctionContext functionContext)
                throws Exception {
            id = functionContext.getContainer().getId();
            channel = functionContext.getChannel();
        }
    }
    
    @SuppressWarnings("serial")
    public static Function<String, String> uniqueStringCounter(final int count){
        return new Function<String, String>(){
            Set<String> hset = new HashSet<>();
            int _count = 0;
            @Override
            public String apply(String v) {
                _count++;
                hset.add(v);
                if(_count == count)
                    return Integer.toString(hset.size());
                return null;
            }
       
        };
    }
}
