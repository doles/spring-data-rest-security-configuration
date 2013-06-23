package org.domi.config.security.prepost;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.repository.core.support.RepositoryProxyPostProcessor;
import org.springframework.security.access.intercept.aspectj.AspectJMethodSecurityInterceptor;

/**
 *
 * @author Dominika
 */
public class RepositoryPrePostProxyPostProcessor implements RepositoryProxyPostProcessor {
    
    private AspectJMethodSecurityInterceptor aspectJMethodSecurityInterceptor;
    
    public RepositoryPrePostProxyPostProcessor(ListableBeanFactory beanFactory, AspectJMethodSecurityInterceptor aspectJMethodSecurityInterceptor) {
        this.aspectJMethodSecurityInterceptor = aspectJMethodSecurityInterceptor;
        
    }
    

    //@Override
    public void postProcess(ProxyFactory factory) {        
        factory.addAdvice(this.aspectJMethodSecurityInterceptor);
    }
    
    
    
}