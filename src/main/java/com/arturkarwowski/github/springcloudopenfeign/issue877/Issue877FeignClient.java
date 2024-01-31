package com.arturkarwowski.github.springcloudopenfeign.issue877;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@FeignClient(
        name = "issue-877-feign-client",
        url = "${issue877-feign.url}",
        dismiss404 = true)
public interface Issue877FeignClient {

    @GetMapping(value = "/test-url")
    Optional<Issue877Response> get();
}