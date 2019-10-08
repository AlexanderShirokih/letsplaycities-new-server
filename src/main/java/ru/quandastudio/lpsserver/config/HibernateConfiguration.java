package ru.quandastudio.lpsserver.config;

import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import ru.quandastudio.lpsserver.hibernate.HibernateSessionFactory;

@Configuration
public class HibernateConfiguration {

	@Bean(destroyMethod = "close")
	@Scope("singleton")
	public SessionFactory sessionFactory() {
		return HibernateSessionFactory.getSessionFactory();
	}

}
