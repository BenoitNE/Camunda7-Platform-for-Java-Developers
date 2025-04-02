package com.camunda.training.bpmn;

import com.camunda.training.service.TwitterService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class CreateTweetDelegate implements JavaDelegate {
    TwitterService twitter = new TwitterService();

    public void execute(DelegateExecution execution) throws Exception {
        String content = (String) execution.getVariable("content");
        twitter.updateStatus(content);
    }
}