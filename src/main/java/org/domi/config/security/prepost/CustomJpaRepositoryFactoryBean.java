package org.domi.config.security.prepost;


import java.io.Serializable;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.security.access.intercept.aspectj.AspectJMethodSecurityInterceptor;
import org.springframework.util.Assert;

/**
 *
 * @author Dominika
 */
public class CustomJpaRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable> extends PrePostAndTransactionalRepositoryFactoryBeanSupport<T, S, ID> {

    @Autowired
    private AspectJMethodSecurityInterceptor aspectJMethodSecurityInterceptor;

    @Autowired
    private ApplicationContext applicationContext;
    
    private EntityManager entityManager;

    /**
     * The {@link EntityManager} to be used.
     *
     * @param entityManager the entityManager to set
     */
    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {

        this.entityManager = entityManager;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.data.repository.support.
     * TransactionalRepositoryFactoryBeanSupport#doCreateRepositoryFactory()
     */
    @Override
    protected RepositoryFactorySupport doCreateRepositoryFactory() {
        return createRepositoryFactory(entityManager);
    }

    /**
     * Returns a {@link RepositoryFactorySupport}.
     *
     * @param entityManager
     * @return
     */
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
        System.out.println("entityManager2: "+entityManager);
        return new JpaRepositoryFactory(entityManager);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() {

        //super.afterPropertiesSet will call createRepositoryFactory, which is JpaRepositoryFactory,
        //but aspeccj interceptor should be set here before this call
        Assert.notNull(aspectJMethodSecurityInterceptor, "AspectJMethodSecurityInterceptor must not be null!");
        super.setSecurityInterceptor(aspectJMethodSecurityInterceptor);
        Assert.notNull(entityManager, "EntityManager must not be null!");
        setBeanFactory(applicationContext);
        super.afterPropertiesSet();
    }
}
