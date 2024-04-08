package com.example.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTestService {
//    final을 붙여야 @RequiredArgs 어노테이션이 자동으로 생성자를 주입해준다.
    private final RedissonClient redissonClient;

    public String getLock() {
        // redissonClient의 sampleLock을 가져옴
        RLock lock = redissonClient.getLock("sampleLock");

        try {
            // 락 획득 시도. 1초간 기다리고 획득 시 5초간 락을 함
            boolean isLock = lock.tryLock(1, 5, TimeUnit.SECONDS);
            if(!isLock) {
                // 락 획득 실패 시
                log.error("======Lock acquisition failed=====");
                return "Lock failed";
            }
        } catch (Exception e) {
            log.error("Redis lock failed");
        }

        return "Lock success";
    }
}
