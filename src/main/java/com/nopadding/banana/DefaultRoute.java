/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 simileas.
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

package com.nopadding.banana;

import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.avro.AvroDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class DefaultRoute extends RouteBuilder {

  @Autowired
  CamelContext camelContext;

  @Autowired
  RedisTemplate<String, Object> redisTemplate;

  @Override
  public void configure() {
    final Schema schema;
    try {
      schema = new Schema.Parser().parse(
          this.getClass().getClassLoader().getResourceAsStream("dnsmasq-syslog.avsc"));
    } catch (IOException e) {
      log.error("Unable to read schema file", e);
      return;
    }
    final AvroDataFormat avroFormat = new AvroDataFormat(schema);
    final GenericRecordBuilder builder = new GenericRecordBuilder(schema);
    camelContext.getRegistry().bind("syslogFrameDecoder", new SyslogFrameDecoder());
    from("netty:tcp://0.0.0.0:10514?sync=false&allowDefaultCodec=false&"
        + "decoders=syslogFrameDecoder")
        .process(new LogProcessor(redisTemplate))
        .choice()
        .when(simple("${body} != ''"))
        .process(exchange -> {
          DnsmasqSyslogEntity obj = exchange.getIn().getBody(DnsmasqSyslogEntity.class);
          GenericRecord record = builder.set("log_time", obj.getLogTime().getTime())
              .set("dnsmasq_server", obj.getDnsmasqServer())
              .set("log_display_id", obj.getLogDisplayId())
              .set("client_ip", obj.getClientIp())
              .set("client_port", obj.getClientPort())
              .set("query_type", obj.getQueryType())
              .set("question", obj.getQuestion())
              .set("server_ip", obj.getServerIp() == null ? "" : obj.getServerIp())
              .set("response_code", obj.getResponseCode())
              .set("answer_type", obj.getAnswerType() == null ? "" : obj.getAnswerType())
              .set("answer", obj.getAnswer() == null ? "" : obj.getAnswer()).build();
          exchange.getIn().setBody(record);
        })
        .marshal(avroFormat)
        .to("kafka:dns--dnsmasq-syslog?"
            + "valueSerializer=org.apache.kafka.common.serialization.ByteArraySerializer")
        .routeId("dnsmasq-syslog-analyzer");
  }
}
