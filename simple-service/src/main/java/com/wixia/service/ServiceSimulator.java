package com.wixia.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceSimulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceSimulator.class);

    @GetMapping("/service")
    public QueryResponseMessage query(@RequestParam(value = "querystring") String queryString,
                                      @RequestParam(value = "servicename") String serviceName,
                                      @RequestParam(value = "delay") int delay) throws InterruptedException {

        LOGGER.info("Service {} receiving:: {}", serviceName, queryString);

        Thread.sleep(1000L * delay);

        QueryResponseMessage queryResponseMessage = new QueryResponseMessage(
            String.format("%s, service name: %s, virtual delay: %d", queryString, serviceName, delay));

        LOGGER.info("Service {} response: {}",
                    serviceName, queryResponseMessage.responseContent());

        return queryResponseMessage;
    }
}
