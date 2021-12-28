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

import static com.nopadding.banana.DnsResponseCode.NOERROR;
import static com.nopadding.banana.DnsResponseCode.NOTIMP;
import static com.nopadding.banana.DnsResponseCode.NXDOMAIN;
import static com.nopadding.banana.DnsResponseCode.SERVFAIL;

import io.netty.buffer.ByteBuf;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.syslog.SyslogConverter;
import org.apache.camel.component.syslog.SyslogMessage;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
public class LogProcessor implements Processor {

  private final RedisTemplate<String, Object> redisTemplate;

  LogProcessor(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    Message inMessage = exchange.getIn();
    String body = inMessage.getBody(ByteBuf.class).toString(Charset.defaultCharset());
    SyslogMessage message = SyslogConverter.toSyslogMessage(body);
    String content = message.getLogMessage();
    String[] splitted = content.split(" ");
    if (splitted.length > 4 && splitted[0].startsWith("dnsmasq[") && splitted[1]
        .matches("^\\d+$")) {
      long logDisplayId = Long.parseLong(splitted[1]);
      String key = splitted[2] + "_" + logDisplayId + "_" + message.getHostname();
      if (splitted[3].startsWith("query[") && splitted.length >= 7) {
        String[] address = splitted[2].split("/");
        String queryType = splitted[3].substring(6, splitted[3].length() - 1);
        DnsmasqSyslogEntity entity =
            DnsmasqSyslogEntity.builder().logTime(message.getTimestamp().getTime())
                .dnsmasqServer(message.getHostname()).logDisplayId(logDisplayId)
                .clientIp(address[0]).clientPort(Integer.parseInt(address[1]))
                .queryType(queryType).question(splitted[4]).build();
        redisTemplate.boundValueOps(key).set(entity);
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
      } else if (splitted[3].equals("forwarded") && splitted.length >= 7) {
        Object entity = redisTemplate.boundValueOps(key).get();
        if (entity != null && ((DnsmasqSyslogEntity) entity).getServerIp() == null) {
          ((DnsmasqSyslogEntity) entity).setServerIp(splitted[6]);
          redisTemplate.boundValueOps(key).set(entity);
          redisTemplate.expire(key, 1, TimeUnit.HOURS);
        }
      } else if (splitted[3].equals("reply") || splitted[3].equals("cached")
          || splitted[3].equals("config") || splitted[3].equals("/etc/hosts")) {
        Object object = redisTemplate.boundValueOps(key).get();
        if (object != null) {
          DnsmasqSyslogEntity entity = (DnsmasqSyslogEntity) object;
          if (splitted[4].equals("error")) {
            if (splitted[6].equals("not")) {
              entity.setResponseCode(NOTIMP);
              inMessage.setBody(entity);
              return;
            } else if (splitted[6].equals(SERVFAIL)) {
              entity.setResponseCode(SERVFAIL);
              inMessage.setBody(entity);
              return;
            } else {
              log.warn("Unkown error [{}]", message.getLogMessage());
            }
          } else if (splitted[6].equals("duplicate")) {
            log.debug("Duplicate reply <{}> [{}]", key, message.getLogMessage());
          } else if (splitted[6].equals("<CNAME>")) {
            if (entity.getAnswerType() == null) {
              entity.setAnswerType(splitted[6].substring(1, splitted[6].length() - 1));
            } else {
              entity.setResponseCode(NOERROR);
              entity.setAnswer(splitted[4]);
              inMessage.setBody(entity);
              entity.setQuestion(splitted[4]);
              return;
            }
          } else if (splitted[6].startsWith("NODATA")) {
            entity.setResponseCode(splitted[6]);
            inMessage.setBody(entity);
          } else if (splitted[6].equals(NXDOMAIN)) {
            entity.setResponseCode(NXDOMAIN);
            inMessage.setBody(entity);
            return;
          } else if (RegexUtil.isIpv4Address(splitted[6])) {
            entity.setAnswerType("A");
            entity.setResponseCode(NOERROR);
            entity.setAnswer(splitted[6]);
            inMessage.setBody(entity);
            return;
          } else if (RegexUtil.isIpv4Address(splitted[4])) {
            entity.setAnswerType("PTR");
            entity.setResponseCode(NOERROR);
            entity.setAnswer(splitted[6]);
            inMessage.setBody(entity);
            return;
          } else if (RegexUtil.isIpv6Address(splitted[6])) {
            entity.setAnswerType("AAAA");
            entity.setResponseCode(NOERROR);
            entity.setAnswer(splitted[6]);
            inMessage.setBody(entity);
            return;
          } else {
            log.warn("Unknown log message [{}]", message.getLogMessage());
          }
        } else {
          log.info("Key <{}> not found [{}]", key, message.getLogMessage());
        }
      }
      inMessage.setHeader(KafkaConstants.KEY, key);
    }
    inMessage.setBody("");
  }
}
