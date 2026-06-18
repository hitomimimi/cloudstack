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
package com.cloud.agent.resource.consoleproxy;

import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.host.Host;

@RunWith(MockitoJUnitRunner.class)
public class ConsoleProxyResourceTest {

    @Spy
    private ConsoleProxyResource resource;

    @Before
    public void setUp() throws ConfigurationException {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "testProxy");
        params.put("zone", "1");
        params.put("pod", "1");
        params.put("guid", "test-guid");
        params.put("proxy_vm", "100");
        params.put("consoleproxy.httpListenPort", "8080");
        resource.configure("testProxy", params);
    }

    @Test
    public void testGetType() {
        Assert.assertEquals(Host.Type.ConsoleProxy, resource.getType());
    }

    @Test
    public void testExecuteReadyCommand() {
        ReadyCommand cmd = new ReadyCommand();
        Answer answer = resource.executeRequest(cmd);

        Assert.assertNotNull(answer);
        Assert.assertTrue(answer.getResult());
    }

    @Test
    public void testExecuteCheckHealthCommand() {
        CheckHealthCommand cmd = new CheckHealthCommand();
        Answer answer = resource.executeRequest(cmd);

        Assert.assertNotNull(answer);
        Assert.assertTrue(answer.getResult());
    }

    @Test
    public void testExecuteUnsupportedCommand() {
        Command cmd = new Command() {
            @Override
            public boolean executeInSequence() {
                return false;
            }
        };
        Answer answer = resource.executeRequest(cmd);

        Assert.assertNotNull(answer);
        Assert.assertFalse(answer.getResult());
    }

    @Test
    public void testConfigureWithPremiumPort() throws ConfigurationException {
        ConsoleProxyResource premiumResource = new ConsoleProxyResource();
        Map<String, Object> params = new HashMap<>();
        params.put("name", "premiumProxy");
        params.put("zone", "1");
        params.put("pod", "1");
        params.put("guid", "premium-guid");
        params.put("proxy_vm", "200");
        params.put("premium", "premium");
        premiumResource.configure("premiumProxy", params);

        Assert.assertEquals(443, premiumResource.proxyPort);
    }

    @Test
    public void testConfigureWithCustomPort() throws ConfigurationException {
        Assert.assertEquals(8080, resource.proxyPort);
    }

    @Test
    public void testConfigureWithDefaultPort() throws ConfigurationException {
        ConsoleProxyResource defaultResource = new ConsoleProxyResource();
        Map<String, Object> params = new HashMap<>();
        params.put("name", "defaultProxy");
        params.put("zone", "1");
        params.put("pod", "1");
        params.put("guid", "default-guid");
        params.put("proxy_vm", "300");
        defaultResource.configure("defaultProxy", params);

        Assert.assertEquals(80, defaultResource.proxyPort);
    }

    @Test
    public void testConfigureProxyVmId() throws ConfigurationException {
        Assert.assertEquals(100L, resource.proxyVmId);
    }

    @Test
    public void testConfigureEth1Ip() throws ConfigurationException {
        ConsoleProxyResource ethResource = new ConsoleProxyResource();
        Map<String, Object> params = new HashMap<>();
        params.put("name", "ethProxy");
        params.put("zone", "1");
        params.put("pod", "1");
        params.put("guid", "eth-guid");
        params.put("proxy_vm", "400");
        params.put("eth1ip", "192.168.1.100");
        params.put("eth1mask", "255.255.255.0");
        ethResource.configure("ethProxy", params);

        Assert.assertEquals("192.168.1.100", ethResource.eth1Ip);
        Assert.assertEquals("255.255.255.0", ethResource.eth1Mask);
        Assert.assertEquals("eth1", params.get("private.network.device"));
    }

    @Test
    public void testConfigureEth2Ip() throws ConfigurationException {
        ConsoleProxyResource ethResource = new ConsoleProxyResource();
        Map<String, Object> params = new HashMap<>();
        params.put("name", "ethProxy");
        params.put("zone", "1");
        params.put("pod", "1");
        params.put("guid", "eth-guid");
        params.put("proxy_vm", "400");
        params.put("eth2ip", "10.0.0.100");
        ethResource.configure("ethProxy", params);

        Assert.assertEquals("eth2", params.get("public.network.device"));
    }

    @Test
    public void testConfigurePublicIp() throws ConfigurationException {
        ConsoleProxyResource pubResource = new ConsoleProxyResource();
        Map<String, Object> params = new HashMap<>();
        params.put("name", "pubProxy");
        params.put("zone", "1");
        params.put("pod", "1");
        params.put("guid", "pub-guid");
        params.put("proxy_vm", "500");
        params.put("public.ip", "203.0.113.50");
        pubResource.configure("pubProxy", params);

        Assert.assertEquals("203.0.113.50", pubResource.publicIp);
    }

    @Test
    public void testInitialize() {
        var startupCmds = resource.initialize();

        Assert.assertNotNull(startupCmds);
        Assert.assertEquals(1, startupCmds.length);
    }

    @Test
    public void testGetCurrentStatus() {
        var pingCmd = resource.getCurrentStatus(1L);

        Assert.assertNotNull(pingCmd);
    }

    @Test
    public void testGetRunLevel() {
        Assert.assertEquals(0, resource.getRunLevel());
    }

    @Test
    public void testGetConfigParams() {
        Map<String, Object> configParams = resource.getConfigParams();
        Assert.assertNotNull(configParams);
    }

    @Test
    public void testGetDefaultScriptsDir() throws Exception {
        java.lang.reflect.Method method = ConsoleProxyResource.class.getDeclaredMethod("getDefaultScriptsDir");
        method.setAccessible(true);
        Object result = method.invoke(resource);
        Assert.assertNull(result);
    }
}
