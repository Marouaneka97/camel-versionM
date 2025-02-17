/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.jms;

import javax.jms.ConnectionFactory;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import static org.apache.camel.component.jms.JmsComponent.jmsComponentAutoAcknowledge;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JmsSetBodyNullErrorHandlerUseOriginalMessageTest extends CamelTestSupport {

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();
        ConnectionFactory connectionFactory = CamelJmsTestHelper.createPersistentConnectionFactory();
        camelContext.addComponent("activemq", jmsComponentAutoAcknowledge(connectionFactory));
        return camelContext;
    }

    @Test
    public void testSetNull() throws Exception {
        getMockEndpoint("mock:foo").expectedBodiesReceived("Hello World");
        getMockEndpoint("mock:bar").expectedMessageCount(1);
        getMockEndpoint("mock:bar").message(0).body().isNull();

        template.sendBody("activemq:queue:foo", "Hello World");

        assertMockEndpointsSatisfied();

        String body = consumer.receiveBody("activemq:queue:dead", 5000, String.class);
        assertEquals("Hello World", body);
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                errorHandler(deadLetterChannel("activemq:queue:dead").useOriginalMessage());

                from("activemq:queue:foo")
                        .to("mock:foo")
                        .process(exchange -> {
                            // an end user may set the message body explicit to null
                            exchange.getIn().setBody(null);
                        })
                        .to("mock:bar")
                        .throwException(new IllegalArgumentException("Forced"));
            }
        };
    }
}
