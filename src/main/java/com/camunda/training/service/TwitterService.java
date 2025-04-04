package com.camunda.training.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TwitterService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwitterService.class);

    public void updateStatus(String content) {
        if (content.equals("Network error")) {
            throw new IllegalArgumentException("simulated network error");
        }
        LOGGER.info("Tweet: {}", content);
    }
}
