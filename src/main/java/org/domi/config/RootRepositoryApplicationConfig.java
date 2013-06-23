package org.domi.config;

import org.domi.config.security.prepost.CustomJpaRepositoryFactoryBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
*
* @author Dominika
*/

@EnableJpaRepositories(basePackages={"org.domi.repository"},repositoryFactoryBeanClass=CustomJpaRepositoryFactoryBean.class)
@ComponentScan(basePackageClasses=RootRepositoryApplicationConfig.class)
@Configuration
public class RootRepositoryApplicationConfig {


  
}