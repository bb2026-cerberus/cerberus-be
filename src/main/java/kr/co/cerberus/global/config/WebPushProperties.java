package kr.co.cerberus.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "webpush")
public class WebPushProperties {
    private String subject;
    private String publicKey;
    private String privateKey;
}