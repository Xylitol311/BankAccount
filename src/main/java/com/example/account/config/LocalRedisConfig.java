package com.example.account.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

@Slf4j
@Profile("local")
@Configuration
public class LocalRedisConfig {
    @Value("${spring.redis.port}")
    private int redisPort;

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() {
        int port = isRedisRunning() ? findAvailablePort() : redisPort;

        if (isArmMac()) {
            redisServer = new RedisServer(getRedisFileForArcMac(), port);
        } else {
            redisServer = RedisServer.builder()
                    .port(port)
                    .build();
        }

        try {
            redisServer.start();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private boolean isArmMac() {
        return Objects.equals(System.getProperty("os.arch"), "aarch64")
                && Objects.equals(System.getProperty("os.name"), "Mac OS X");
    }

    private File getRedisFileForArcMac() {
        try {
            return new ClassPathResource("binary/redis/redis-server-arm64").getFile();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    private boolean isArmArchitecture() {
        return System.getProperty("os.arch").contains("aarch64");
    }
    /**
     * 현재 PC 서버에서 사용 가능한 포트 조회
     */
    public int findAvailablePort() {
        for (int port = 10000; port <= 65535; port++) {
            Process process = executeGrepProcessCommand(port);

            if (!isRunning(process)) {
                return port;
            }
        }

        throw new RuntimeException();
    }

    /**
     * Embedded Redis가 현재 실행 중인지 확인
     */
    private boolean isRedisRunning() {
        return isRunning(executeGrepProcessCommand(redisPort));
    }

    /**
     * 해당 Port를 사용 중인 프로세스를 확인하는 sh 실행
     */
    private Process executeGrepProcessCommand(int redisPort) {
        // netstat -nat : 시스템 네트워크 연결 상태 확인 명령어
        // grep LISTEN : 'LISTEN'이 포함된 줄 확인 명령어
        // grep %d : 앞서 주어진 출력 내용에서 포트번호가 포함된 줄 확인 명령어
        String command = String.format("netstat -nat | grep LISTEN | grep %d", redisPort);

        //  '/bin/sh'에서 '-c' 옵션과 함께 위에서 만든 command를 실행하는 명령을 배열 형태로 구성
        String[] shell = {"/bin/sh", "-c", command};

        try {
            return Runtime.getRuntime().exec(shell);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    /**
     * 해당 Process가 현재 실행 중인지 확인
     */
    private boolean isRunning(Process process) {
        String line;
        StringBuilder pidInfo = new StringBuilder();

        try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while ((line = input.readLine()) != null) {
                pidInfo.append(line);
            }
        } catch (Exception e) {
            throw new RuntimeException();
        }

        return StringUtils.hasText(pidInfo.toString());
    }

}
