package com.alibaba.dubbo.performance.demo.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgentApp {
    // agent会作为sidecar，部署在每一个Provider和Consumer机器上
    // 在Provider端启动agent时，添加JVM参数-Dtype=provider -Dserver.port=30000 -Ddubbo.protocol.port=20889
    // 在Consumer端启动agent时，添加JVM参数-Dtype=consumer -Dserver.port=20000
    // 添加日志保存目录: -Dlogs.dir=/path/to/your/logs/dir。请安装自己的环境来设置日志目录。

    // java -jar -Xms1536M -Xmx1536M -Dtype=consumer -Dserver.port=20000 -Detcd.url=http://127.0.0.1:2379 mesh-agent-1.0-SNAPSHOT.jar
    // java -jar -Xms1536M -Xmx1536M -Dtype=provider -Ddubbo.protocol.port=20880 -Dserver.port=30000 -Detcd.url=http://127.0.0.1:2379 mesh-agent-1.0-SNAPSHOT.jar

    public static void main(String[] args) {
        SpringApplication.run(AgentApp.class, args);
    }
}
