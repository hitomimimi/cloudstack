// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.agent.mockvm;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine.State;

public class MockVmMgrTest {

    private MockVmMgr vmMgr;

    @Before
    public void setUp() {
        vmMgr = new MockVmMgr();
        Map<String, Object> params = new HashMap<>();
        params.put("memory", "1073741824");
        params.put("cpus", "4");
        params.put("cpuspeed", "4000");
        vmMgr.configure(params);
    }

    @Test
    public void testStartVmSuccessfully() {
        String result = vmMgr.startVM("vm1", "vnet1", "10.0.0.1", "8.8.8.8",
                "192.168.1.10", "aa:bb:cc:dd:ee:01", "255.255.255.0",
                "10.0.0.10", "aa:bb:cc:dd:ee:02", "255.255.255.0",
                2, 50, 1024, "/local/path", "password");

        Assert.assertNull(result);
        Assert.assertNotNull(vmMgr.getVm("vm1"));
        Assert.assertEquals(State.Running, vmMgr.getVm("vm1").getState());
    }

    @Test
    public void testStartVmAlreadyExistsDoesNotDuplicate() {
        vmMgr.startVM("vm1", "vnet1", "10.0.0.1", "8.8.8.8",
                "192.168.1.10", "aa:bb:cc:dd:ee:01", "255.255.255.0",
                "10.0.0.10", "aa:bb:cc:dd:ee:02", "255.255.255.0",
                2, 50, 1024, "/local/path", "password");

        String result = vmMgr.startVM("vm1", "vnet1", "10.0.0.1", "8.8.8.8",
                "192.168.1.10", "aa:bb:cc:dd:ee:01", "255.255.255.0",
                "10.0.0.10", "aa:bb:cc:dd:ee:02", "255.255.255.0",
                2, 50, 1024, "/local/path", "password");

        Assert.assertNull(result);
    }

    @Test
    public void testStartVmOutOfMemory() {
        String result = vmMgr.startVM("vm-big", "vnet1", "10.0.0.1", "8.8.8.8",
                "192.168.1.10", "aa:bb:cc:dd:ee:01", "255.255.255.0",
                "10.0.0.10", "aa:bb:cc:dd:ee:02", "255.255.255.0",
                2, 50, Long.MAX_VALUE, "/local/path", "password");

        Assert.assertEquals("Out of memory", result);
    }

    @Test
    public void testStopVmExisting() {
        vmMgr.startVM("vm1", "vnet1", "10.0.0.1", "8.8.8.8",
                "192.168.1.10", "aa:bb:cc:dd:ee:01", "255.255.255.0",
                "10.0.0.10", "aa:bb:cc:dd:ee:02", "255.255.255.0",
                2, 50, 1024, "/local/path", "password");

        String result = vmMgr.stopVM("vm1", false);

        Assert.assertNull(result);
        Assert.assertEquals(State.Stopped, vmMgr.getVm("vm1").getState());
    }

    @Test
    public void testStopVmNonExisting() {
        String result = vmMgr.stopVM("nonexistent", false);
        Assert.assertNull(result);
    }

    @Test
    public void testRebootVm() {
        vmMgr.startVM("vm1", "vnet1", "10.0.0.1", "8.8.8.8",
                "192.168.1.10", "aa:bb:cc:dd:ee:01", "255.255.255.0",
                "10.0.0.10", "aa:bb:cc:dd:ee:02", "255.255.255.0",
                2, 50, 1024, "/local/path", "password");

        vmMgr.stopVM("vm1", false);
        vmMgr.rebootVM("vm1");

        Assert.assertEquals(State.Running, vmMgr.getVm("vm1").getState());
    }

    @Test
    public void testMigrateVmExisting() {
        vmMgr.startVM("vm1", "vnet1", "10.0.0.1", "8.8.8.8",
                "192.168.1.10", "aa:bb:cc:dd:ee:01", "255.255.255.0",
                "10.0.0.10", "aa:bb:cc:dd:ee:02", "255.255.255.0",
                2, 50, 1024, "/local/path", "password");

        boolean result = vmMgr.migrate("vm1", "params");

        Assert.assertTrue(result);
        Assert.assertNull(vmMgr.getVm("vm1"));
    }

    @Test
    public void testMigrateVmNonExisting() {
        boolean result = vmMgr.migrate("nonexistent", "params");
        Assert.assertFalse(result);
    }

    @Test
    public void testCheckVmStateRunning() {
        vmMgr.startVM("vm1", "vnet1", "10.0.0.1", "8.8.8.8",
                "192.168.1.10", "aa:bb:cc:dd:ee:01", "255.255.255.0",
                "10.0.0.10", "aa:bb:cc:dd:ee:02", "255.255.255.0",
                2, 50, 1024, "/local/path", "password");

        Assert.assertEquals(State.Running, vmMgr.checkVmState("vm1"));
    }

    @Test
    public void testCheckVmStateUnknownForNonExisting() {
        Assert.assertEquals(State.Unknown, vmMgr.checkVmState("nonexistent"));
    }

    @Test
    public void testGetCurrentVMs() {
        vmMgr.startVM("vm1", "vnet1", "10.0.0.1", "8.8.8.8",
                "192.168.1.10", "aa:bb:cc:dd:ee:01", "255.255.255.0",
                "10.0.0.10", "aa:bb:cc:dd:ee:02", "255.255.255.0",
                2, 50, 1024, "/local/path", "password");
        vmMgr.startVM("vm2", "vnet2", "10.0.0.1", "8.8.8.8",
                "192.168.1.11", "aa:bb:cc:dd:ee:03", "255.255.255.0",
                "10.0.0.11", "aa:bb:cc:dd:ee:04", "255.255.255.0",
                4, 80, 2048, "/local/path", "password");

        Set<String> currentVMs = vmMgr.getCurrentVMs();

        Assert.assertEquals(2, currentVMs.size());
        Assert.assertTrue(currentVMs.contains("vm1"));
        Assert.assertTrue(currentVMs.contains("vm2"));
    }

    @Test
    public void testGetVmStates() {
        vmMgr.startVM("vm1", "vnet1", "10.0.0.1", "8.8.8.8",
                "192.168.1.10", "aa:bb:cc:dd:ee:01", "255.255.255.0",
                "10.0.0.10", "aa:bb:cc:dd:ee:02", "255.255.255.0",
                2, 50, 1024, "/local/path", "password");
        vmMgr.startVM("vm2", "vnet2", "10.0.0.1", "8.8.8.8",
                "192.168.1.11", "aa:bb:cc:dd:ee:03", "255.255.255.0",
                "10.0.0.11", "aa:bb:cc:dd:ee:04", "255.255.255.0",
                4, 80, 2048, "/local/path", "password");
        vmMgr.stopVM("vm2", false);

        Map<String, State> states = vmMgr.getVmStates();

        Assert.assertEquals(State.Running, states.get("vm1"));
        Assert.assertEquals(State.Stopped, states.get("vm2"));
    }

    @Test
    public void testCleanupVm() {
        vmMgr.startVM("vm1", "vnet1", "10.0.0.1", "8.8.8.8",
                "192.168.1.10", "aa:bb:cc:dd:ee:01", "255.255.255.0",
                "10.0.0.10", "aa:bb:cc:dd:ee:02", "255.255.255.0",
                2, 50, 1024, "/local/path", "password");

        vmMgr.cleanupVM("vm1", "/local", "vnet1");

        Assert.assertNull(vmMgr.getVm("vm1"));
        Assert.assertFalse(vmMgr.getCurrentVMs().contains("vm1"));
    }

    @Test
    public void testGetVncPort() {
        vmMgr.startVM("vm1", "vnet1", "10.0.0.1", "8.8.8.8",
                "192.168.1.10", "aa:bb:cc:dd:ee:01", "255.255.255.0",
                "10.0.0.10", "aa:bb:cc:dd:ee:02", "255.255.255.0",
                2, 50, 1024, "/local/path", "password");

        Integer port = vmMgr.getVncPort("vm1");
        Assert.assertTrue(port >= 0);
    }

    @Test
    public void testGetVncPortNonExistingVm() {
        Integer port = vmMgr.getVncPort("nonexistent");
        Assert.assertEquals(Integer.valueOf(-1), port);
    }

    @Test
    public void testAllocVncPortSequentialAllocation() {
        int port1 = vmMgr.allocVncPort();
        int port2 = vmMgr.allocVncPort();

        Assert.assertEquals(0, port1);
        Assert.assertEquals(1, port2);
    }

    @Test
    public void testFreeVncPortAndReallocate() {
        int port1 = vmMgr.allocVncPort();
        int port2 = vmMgr.allocVncPort();
        vmMgr.freeVncPort(port1);

        int port3 = vmMgr.allocVncPort();

        Assert.assertEquals(port1, port3);
    }

    @Test
    public void testAllocVncPortExhaustion() {
        for (int i = 0; i < 64; i++) {
            int port = vmMgr.allocVncPort();
            Assert.assertEquals(i, port);
        }

        int overflow = vmMgr.allocVncPort();
        Assert.assertEquals(-1, overflow);
    }

    @Test
    public void testGetHostFreeMemoryReducedByRunningVMs() {
        long freeBeforeVm = vmMgr.getHostFreeMemory();

        long ramSize = 1024L;
        vmMgr.startVM("vm1", "vnet1", "10.0.0.1", "8.8.8.8",
                "192.168.1.10", "aa:bb:cc:dd:ee:01", "255.255.255.0",
                "10.0.0.10", "aa:bb:cc:dd:ee:02", "255.255.255.0",
                2, 50, ramSize, "/local/path", "password");

        long freeAfterVm = vmMgr.getHostFreeMemory();
        Assert.assertEquals(freeBeforeVm - ramSize, freeAfterVm);
    }

    @Test
    public void testGetHostFreeMemoryNotReducedByStoppedVMs() {
        vmMgr.startVM("vm1", "vnet1", "10.0.0.1", "8.8.8.8",
                "192.168.1.10", "aa:bb:cc:dd:ee:01", "255.255.255.0",
                "10.0.0.10", "aa:bb:cc:dd:ee:02", "255.255.255.0",
                2, 50, 1024L, "/local/path", "password");
        vmMgr.stopVM("vm1", false);

        long freeMemory = vmMgr.getHostFreeMemory();
        long expectedFree = vmMgr.getHostTotalMemory() - vmMgr.getHostDom0Memory();
        Assert.assertEquals(expectedFree, freeMemory);
    }

    @Test
    public void testGetHostDom0Memory() {
        long dom0Mem = vmMgr.getHostDom0Memory();
        Assert.assertEquals(128L * 1024 * 1024, dom0Mem);
    }

    @Test
    public void testGetHostCpuCount() {
        Assert.assertEquals(4, vmMgr.getHostCpuCount());
    }

    @Test
    public void testGetHostCpuSpeed() {
        Assert.assertEquals(4000L, vmMgr.getHostCpuSpeed());
    }

    @Test
    public void testGetHostTotalMemory() {
        Assert.assertEquals(1073741824L, vmMgr.getHostTotalMemory());
    }

    @Test
    public void testGetHostCpuUtilization() {
        Assert.assertEquals(0.0d, vmMgr.getHostCpuUtilization(), 0.001);
    }

    @Test
    public void testCreateVmFromSpec() {
        VirtualMachineTO vmSpec = Mockito.mock(VirtualMachineTO.class);
        Mockito.when(vmSpec.getName()).thenReturn("spec-vm");
        Mockito.when(vmSpec.getMinRam()).thenReturn(2048L);
        Mockito.when(vmSpec.getCpus()).thenReturn(4);

        MockVm vm = vmMgr.createVmFromSpec(vmSpec);

        Assert.assertNotNull(vm);
        Assert.assertEquals("spec-vm", vm.getName());
        Assert.assertEquals(State.Running, vm.getState());
        Assert.assertEquals(2048L, vm.getRamSize());
        Assert.assertEquals(4, vm.getCpuCount());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCreateVmFromSpecOutOfMemory() {
        VirtualMachineTO vmSpec = Mockito.mock(VirtualMachineTO.class);
        Mockito.when(vmSpec.getName()).thenReturn("big-vm");
        Mockito.when(vmSpec.getMinRam()).thenReturn(Long.MAX_VALUE);
        Mockito.lenient().when(vmSpec.getCpus()).thenReturn(1);

        vmMgr.createVmFromSpec(vmSpec);
    }

    @Test
    public void testCreateVmFromSpecAlreadyExists() {
        VirtualMachineTO vmSpec = Mockito.mock(VirtualMachineTO.class);
        Mockito.when(vmSpec.getName()).thenReturn("existing-vm");
        Mockito.when(vmSpec.getMinRam()).thenReturn(1024L);
        Mockito.when(vmSpec.getCpus()).thenReturn(2);

        MockVm vm1 = vmMgr.createVmFromSpec(vmSpec);
        MockVm vm2 = vmMgr.createVmFromSpec(vmSpec);

        Assert.assertSame(vm1, vm2);
    }

    @Test
    public void testCleanupVnet() {
        Assert.assertNull(vmMgr.cleanupVnet("vnet1"));
    }

    @Test
    public void testConfigureWithCustomValues() {
        MockVmMgr customMgr = new MockVmMgr();
        Map<String, Object> params = new HashMap<>();
        params.put("memory", "2147483648");
        params.put("cpus", "8");
        params.put("cpuspeed", "5000");
        customMgr.configure(params);

        Assert.assertEquals(2147483648L, customMgr.getHostTotalMemory());
        Assert.assertEquals(8, customMgr.getHostCpuCount());
        Assert.assertEquals(5000L, customMgr.getHostCpuSpeed());
    }

    @Test
    public void testConfigureWithDefaults() {
        MockVmMgr defaultMgr = new MockVmMgr();
        Map<String, Object> params = new HashMap<>();
        defaultMgr.configure(params);

        Assert.assertEquals(16000L, defaultMgr.getHostTotalMemory());
        Assert.assertEquals(4, defaultMgr.getHostCpuCount());
        Assert.assertEquals(4000L, defaultMgr.getHostCpuSpeed());
    }
}
