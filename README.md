# Dnsmasq Syslog 解析

![GitHub](https://img.shields.io/github/license/simileas/dnsmasq-syslog)

## Overview

Dnsmasq 的 extra 日志格式如下：

```text
Dec 20 11:00:06 institute dnsmasq[13080]: 486153 172.20.9.205/57996 query[A] dlied6.qq.com from 172.20.9.205
Dec 20 11:00:06 institute dnsmasq[13080]: 486153 172.20.9.205/57996 cached dlied6.qq.com is <CNAME>
Dec 20 11:00:06 institute dnsmasq[13080]: 486153 172.20.9.205/57996 cached dlied6.qq.com.tcdn.qq.com is <CNAME>
Dec 20 11:00:06 institute dnsmasq[13080]: 486153 172.20.9.205/57996 cached dlied6.qq.com.yyb.sched.dcloudstc.com is 182.254.59.170
Dec 20 11:00:06 institute dnsmasq[13080]: 486153 172.20.9.205/57996 cached dlied6.qq.com.yyb.sched.dcloudstc.com is 182.254.59.187
```
可以看出详细记录包含解析的发起，CNAME 解析以及 A 解析出的多个 IP 地址。实现的功能是将这部分日志解析成如下 schema 的 avro 数据：

```json
{
  "type": "record",
  "name": "DnsmasqSyslog",
  "fields": [
    { "name": "log_time", "type": "long" },
    { "name": "dnsmasq_server", "type": "string" },
    { "name": "log_display_id", "type": "long" },
    { "name": "client_ip", "type": "string" },
    { "name": "client_port", "type": "int" },
    { "name": "query_type", "type": "string", "doc": "A, AAAA, PTR" },
    { "name": "question", "type": "string" },
    { "name": "server_ip", "type": "string" },
    { "name": "response_code", "type": "string", "doc": "NXDOMAIN, SERVERFAIL, NOTIMP" },
    { "name": "answer_type", "type": "string", "doc": "A, CNAME" },
    { "name": "answer", "type": "string" }
  ]
}
```
`answer` 字段存储解析完成的单个域名或者 IP 地址。

主要目的是收集流式数据用于学习和测试。

## 编译方法

> 所有的命令行在 *nix 系统下执行，如果在 windows 下执行请修改路径分隔符等差异的部分。

编译项目，生成 jar，忽略单元测试。

````shell script
./gradlew clean build -x test -info
````

生成的 jar 在 `build/libs` 下，安装包在 `build/distributions` 下。

## Systemd 部署

配置文件：`/etc/systemd/system/aqua-mail.service`

```text
[Unit]
Description=Dnsmasq Syslog
After=syslog.target

[Service]
User=admin
Environment='JAVA_HOME=/usr/java/default' 'JAVA_OPTS=-Xmx512m -Xms512m -Dspring.profiles.active=prod'
ExecStart=/opt/dnsmasq-syslog/dnsmasq-syslog.jar
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```
