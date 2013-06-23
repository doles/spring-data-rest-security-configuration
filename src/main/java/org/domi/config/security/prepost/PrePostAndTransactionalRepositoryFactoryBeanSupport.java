package org.domi.config.security.prepost;

import java.io.Serializable;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.RepositoryProxyPostProcessor;
import org.springframework.data.repository.util.TxUtils;
import org.springframework.security.access.intercept.aspectj.AspectJMethodSecurityInterceptor;
import org.springframework.util.Assert;

/**
 *
 * @author Dominika
 */
public abstract class PrePostAndTransactionalRepositoryFactoryBeanSupport<T extends Repository<S, ID>, S, ID extends Serializable> extends RepositoryFactoryBeanSupport<T, S, ID> {

    private String transactionManagerName = TxUtils.DEFAULT_TRANSACTION_MANAGER;
    private AspectJMethodSecurityInterceptor securityInterceptor;
    private RepositoryProxyPostProcessor prePostProcessor;
    private RepositoryProxyPostProcessor txPostProcessor;

    /**
     * Setter to configure which transaction manager to be used. We have to use
     * the bean name explicitly as otherwise the qualifier of the {@link org.springframework.transaction.annotation.Transactional}
     * annotation is used. By explicitly defining the transaction manager bean
     * name we favour let this one be the default one chosen.
     *
     * @param transactionManager
     */
    public void setTransactionManager(String transactionManager) {

        this.transactionManagerName = transactionManager == null ? TxUtils.DEFAULT_TRANSACTION_MANAGER : transactionManager;
    }

    public void setSecurityInterceptor(AspectJMethodSecurityInterceptor securityInterceptor) {
        this.securityInterceptor = securityInterceptor;
    }

    /**
     * Delegates {@link RepositoryFactorySupport} creation to {@link #doCreateRepositoryFactory()}
     * and applies the
     * {@link TransactionalRepositoryProxyPostProcessor} to the created
     * instance.
     *
     * @see
     * org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport
     * #createRepositoryFactory()
     */
    @Override
    protected final RepositoryFactorySupport createRepositoryFactory() {

        RepositoryFactorySupport factory = doCreateRepositoryFactory();

        
        
        System.out.println("txPostProcessor: "+txPostProcessor);
        factory.addRepositoryProxyPostProcessor(txPostProcessor);
        System.out.println("prePostProcessor: "+prePostProcessor);
        factory.addRepositoryProxyPostProcessor(prePostProcessor);
        return factory;
    }

    /**
     * Creates the actual {@link RepositoryFactorySupport} instance.
     *
     * @return
     */
    protected abstract RepositoryFactorySupport doCreateRepositoryFactory();

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org
     * .springframework.beans.factory.BeanFactory)
     */
    public void setBeanFactory(BeanFactory beanFactory) {

        Assert.isInstanceOf(ListableBeanFactory.class, beanFactory);

        this.txPostProcessor = new TransactionalRepositoryProxyPostProcessor((ListableBeanFactory) beanFactory, transactionManagerName);
        this.prePostProcessor = new RepositoryPrePostProxyPostProcessor((ListableBeanFactory) beanFactory, securityInterceptor);
    }
}
