package com.github.chaudhryfaisal.tinylog;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Example {
    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            try {
                log.info("loop {}", i);
                Thread.sleep(500);
                if (i % 100 == 0) {
                    throw new RuntimeException(" Runtime Exception i=" + i);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        Thread.sleep(10000);
    }
}
