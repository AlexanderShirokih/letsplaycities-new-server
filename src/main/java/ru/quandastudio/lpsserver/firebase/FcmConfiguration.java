package ru.quandastudio.lpsserver.firebase;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(FcmProperties.class)
@Slf4j
public class FcmConfiguration {

	private final FcmProperties fcmProperties;

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
	public FirebaseMessaging firebaseApp() {
		Resource resource = new ClassPathResource(fcmProperties.getServiceAccountFile());
		FirebaseOptions options = null;
		try {
			InputStream serviceAccount = resource.getInputStream();
			options = new FirebaseOptions.Builder().setCredentials(GoogleCredentials.fromStream(serviceAccount))
					.build();
		} catch (IOException e) {
			log.error("Error on initalization firebase messaging", e);
		}
		assert options != null;
		FirebaseApp app = FirebaseApp.initializeApp(options);

		return FirebaseMessaging.getInstance(app);
	}

}
