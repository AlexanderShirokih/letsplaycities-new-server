package ru.quandastudio.lpsserver.firebase;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "fcm")
@Component
@Getter
@Setter
public class FcmProperties {

	private String serviceAccountFile;

}
