package ru.quandastudio.lpsserver.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableJpaRepositories(basePackages = "ru.quandastudio.lpsserver.data.dao")
@EnableTransactionManagement
@RequiredArgsConstructor
public class DataConfiguration {

	@Bean(name = "entityManagerFactory")
	public LocalSessionFactoryBean sessionFactory(DataSource dataSource) {
		LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
		sessionFactory.setDataSource(dataSource);
		sessionFactory.setPackagesToScan("ru.quandastudio.lpsserver.data.entities");
		return sessionFactory;
	}

}
