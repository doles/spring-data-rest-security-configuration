package org.domi.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheFactoryBean;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.hibernate4.HibernateExceptionTranslator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.*;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.access.intercept.AfterInvocationProviderManager;
import org.springframework.security.access.intercept.aspectj.AspectJMethodSecurityInterceptor;
import org.springframework.security.access.intercept.aspectj.aspect.AnnotationSecurityAspect;
import org.springframework.security.access.method.DelegatingMethodSecurityMetadataSource;
import org.springframework.security.access.method.MethodSecurityMetadataSource;
import org.springframework.security.access.prepost.PostInvocationAdviceProvider;
import org.springframework.security.access.prepost.PreInvocationAuthorizationAdviceVoter;
import org.springframework.security.access.prepost.PrePostAnnotationSecurityMetadataSource;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.*;
import org.springframework.security.acls.jdbc.BasicLookupStrategy;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.authentication.AnonymousAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
//import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;

@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)//equivalent of <tx:annotation-driven mode="aspectj" transaction-manager="transactionManager"/>, it will look up any PlatformTransactionManager bean
@EnableAspectJAutoProxy //equivalent of <context:aspectj-autoproxy/>
@EnableSpringConfigured //equivalent of <context:spring-configured>, this activates @Configurable, and PersistenceUnit injection
@PropertySource(value = {"database.properties"})
@ComponentScan(
        basePackages="org.domi",      
        excludeFilters={
        	@ComponentScan.Filter(type= FilterType.ANNOTATION,value=Controller.class)
        }
)
//@AnnotationDrivenConfig//equivalent of <context:annotation-config/> 
@Configuration
public class RootApplicationConfig implements TransactionManagementConfigurer  {
    
    
    public static String JPA_PERSISTENCE_NAME = "persistenceUnit";
    public static String ACL_CACHE_NAME = "aclCache";
    
    /**
     * Allows access to the properties defined in from @PropertySource
     */
    @Autowired
    private Environment env;
    
    
    //~~~~~~~~~~~~~~~ JPA CONFIG ~~~~~~~~~~~~~~~ //
    
    public PlatformTransactionManager annotationDrivenTransactionManager() {
    	return jpaTransactionManager();
    }

  
    /**
     * Required to be registered
     */
    @Bean
    public HibernateExceptionTranslator hibernateExceptionTranslator() {
        return new HibernateExceptionTranslator();
    }

    /**
     * Bean that allows to get data base connection.
     *
     * @return
     */
    @Bean
    public DataSource jpaDataSource() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(env.getProperty("database.driverClassName"));
        dataSource.setUrl(env.getProperty("database.url"));
        dataSource.setUsername(env.getProperty("database.username"));
        dataSource.setPassword(env.getProperty("database.password"));
        
        return dataSource;
    }

    /**
     * This transaction manage is only for JPA.
     */
    @Bean(name = "transactionManager")
    public JpaTransactionManager jpaTransactionManager() {

        JpaTransactionManager tm = new JpaTransactionManager();
        tm.setEntityManagerFactory(jpaEmf());
        return tm;

    }

    @Bean(name = "entityManagerFactory")
    public EntityManagerFactory jpaEmf() {


        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(jpaDataSource());
        emf.setPersistenceUnitName(JPA_PERSISTENCE_NAME);
        //below is not needed as it will look up /META-INF/persistence.xml by default
        //emf.setPersistenceXmlLocation(persistenceXmlLocation) 
        emf.afterPropertiesSet();
        return emf.getObject();


    }
    
    
    //Comented out as this will be registered on the Repository via TransactionalRepositoryProxyPostProcessor
    //@Bean
    //public PersistenceExceptionTranslationPostProcessor persistenceExceptionTranslationPostProcessor(){
    //    return new PersistenceExceptionTranslationPostProcessor();
    //}
    
    

    //~~~~~~~~~~~~~~~ ACL CONFIG ~~~~~~~~~~~~~~~ //
    
    @Bean
    public AclAuthorizationStrategy aclAuthorizationStrategy() {
        return new AclAuthorizationStrategyImpl(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Bean(name = "cacheManager")
    public CacheManager ehCacheManager() throws IOException {
        //CacheManager manager = CacheManager.getCacheManager(CacheManager.DEFAULT_NAME);
        //if(manager!=null){
        //    return manager;
        //}
        EhCacheManagerFactoryBean factory = new EhCacheManagerFactoryBean();
        factory.setCacheManagerName(CacheManager.DEFAULT_NAME);
        factory.setShared(true);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    @Bean(name = "ehcache")
    public Ehcache ehCacheFactory() throws CacheException, IOException {
        EhCacheFactoryBean factory = new EhCacheFactoryBean();
        factory.setCacheManager(ehCacheManager());
        factory.setCacheName(ACL_CACHE_NAME);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    @Bean
    public PermissionGrantingStrategy aclPermissionGrantingStrategy() {
        return new DefaultPermissionGrantingStrategy(aclAuditLogger());
    }

    @Bean
    public AclCache aclCache() throws CacheException, IOException {
        return new EhCacheBasedAclCache(ehCacheFactory(), aclPermissionGrantingStrategy(), aclAuthorizationStrategy());
    }

    @Bean
    public AuditLogger aclAuditLogger() {
        return new ConsoleAuditLogger();
    }

    @Bean
    public LookupStrategy aclLookupStrategy() throws CacheException, IOException {
        BasicLookupStrategy lookupStrategy = new BasicLookupStrategy(jpaDataSource(), aclCache(), aclAuthorizationStrategy(), aclPermissionGrantingStrategy());
        lookupStrategy.setPermissionFactory(aclPermissionFactory());
        return lookupStrategy;
    }

    @Bean
    public MutableAclService aclService() throws CacheException, IOException {

        JdbcMutableAclService aclService = new JdbcMutableAclService(jpaDataSource(), aclLookupStrategy(), aclCache());
        aclService.setClassIdentityQuery("SELECT @@IDENTITY");
        aclService.setSidIdentityQuery("SELECT @@IDENTITY");
        return aclService;
    }

    @Bean
    public PermissionEvaluator aclPermissionEvaluator() throws CacheException, IOException {
        return new AclPermissionEvaluator(aclService());
    }

    @Bean
    public DefaultPermissionFactory aclPermissionFactory() {
        return new DefaultPermissionFactory();
    }

    @Bean
    public RoleHierarchy aclRoleHierarchy() {
        RoleHierarchyImpl rh = new RoleHierarchyImpl();
        rh.setHierarchy("ROLE_ADMIN > ROLE_PROPERTY");
        return rh;
    }

    /**
     * This is a separate transaction manager for ACL
     *
     * @return
     */
    @Bean
    public DataSourceTransactionManager aclTransactionManager() {
        DataSourceTransactionManager tm = new DataSourceTransactionManager(jpaDataSource());
        return tm;
    }

    //~~~~~~~~~~~~~~~~~~~ JAVA CONFIG TO ENABLE SECURITY ANNOTATIONS ~~~~~~~~~~~~~~ //
    //~~~~~~~~~~ Allows us to enable expression based security annotations ~~~~~~~~ //
    
    
    @Bean(name="expressionHandler")
    public DefaultMethodSecurityExpressionHandler aclExpressionHandler() throws CacheException, IOException {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(aclPermissionEvaluator());
        expressionHandler.setRoleHierarchy(aclRoleHierarchy());
        return expressionHandler;

    }

    /*
     * Used by 'prePostAnnotationSecurityMetadataSource' bean
     */
    @Bean
    public ExpressionBasedAnnotationAttributeFactory expressionBasedAnnotationAttributeFactory() throws CacheException, IOException {
        return new ExpressionBasedAnnotationAttributeFactory(aclExpressionHandler());
    }

    /*
     * Used by 'delegatingMethodSecurityMetadataSource' bean
     *
     */
    @Bean
    public PrePostAnnotationSecurityMetadataSource prePostAnnotationSecurityMetadataSource() throws CacheException, IOException {
        return new PrePostAnnotationSecurityMetadataSource(expressionBasedAnnotationAttributeFactory());
    }

    /*
     * Used by method advice
     */
    @Bean(name = "delegatingMethodSecurityMetadataSource")
    public DelegatingMethodSecurityMetadataSource delegatingMethodSecurityMetadataSource() throws CacheException, IOException {
        List<MethodSecurityMetadataSource> methodSecurityMetadateSources = new ArrayList<MethodSecurityMetadataSource>();
        methodSecurityMetadateSources.add(prePostAnnotationSecurityMetadataSource());

        return new DelegatingMethodSecurityMetadataSource(methodSecurityMetadateSources);
    }

    /*
     * Used by 'preInvocationAuthorizationAdviceVoter' bean
     */
    @Bean
    public ExpressionBasedPreInvocationAdvice expressionBasedPreInvocationAdvice() throws CacheException, IOException {

        ExpressionBasedPreInvocationAdvice advice = new ExpressionBasedPreInvocationAdvice();
        advice.setExpressionHandler(aclExpressionHandler());
        return advice;
    }

    /*
     * Used by acl access decision manager
     */
    @Bean
    public PreInvocationAuthorizationAdviceVoter preInvocationAuthorizationAdviceVoter() throws CacheException, IOException {
        return new PreInvocationAuthorizationAdviceVoter(expressionBasedPreInvocationAdvice());
    }

    /**
     * The access will be determined based on role and if user is authenticated
     *
     * @return
     */
    @Bean
    public AccessDecisionManager aclAccessDecisionManager() throws CacheException, IOException {

        @SuppressWarnings("rawtypes")
        List<AccessDecisionVoter> voters = new ArrayList<AccessDecisionVoter>(2);
        voters.add(preInvocationAuthorizationAdviceVoter());
        voters.add(new RoleVoter());
        voters.add(new AuthenticatedVoter());

        AffirmativeBased adm = new AffirmativeBased(voters);

        return adm;

    }

    /*
     * Used by 'postInvocationAdviceProvider' bean
     */
    @Bean
    public ExpressionBasedPostInvocationAdvice expressionBasedPostInvocationAdvice() throws CacheException, IOException {
        return new ExpressionBasedPostInvocationAdvice(aclExpressionHandler());
    }

    /*
     * Used by 'afterInvocationProviderManager' bean
     */
    @Bean
    public PostInvocationAdviceProvider postInvocationAdviceProvider() throws CacheException, IOException {
        return new PostInvocationAdviceProvider(expressionBasedPostInvocationAdvice());
    }

    /*
     * Used by 'methodSecurityInterceptor' bean
     */
    @SuppressWarnings("unchecked")
    @Bean
    public AfterInvocationProviderManager afterInvocationProviderManager() throws CacheException, IOException {

        @SuppressWarnings("rawtypes")
        List list = new ArrayList();
        list.add(postInvocationAdviceProvider());


        AfterInvocationProviderManager mgmt = new AfterInvocationProviderManager();
        mgmt.setProviders(list);

        return mgmt;

    }

    /**
     * If we use aspectj.
     */
    @Bean
    public AspectJMethodSecurityInterceptor aspectJMethodSecurityInterceptor() throws CacheException, IOException, Exception{
        
        AspectJMethodSecurityInterceptor interceptor = new AspectJMethodSecurityInterceptor();
        interceptor.setAccessDecisionManager(aclAccessDecisionManager());
        interceptor.setAuthenticationManager(aclAuthenticationManager());
        interceptor.setSecurityMetadataSource(delegatingMethodSecurityMetadataSource());
        interceptor.setAfterInvocationManager(afterInvocationProviderManager());
        interceptor.afterPropertiesSet();
        return interceptor;        
    }
    

    /*
     * If we don't have aspectj mode. This is used by the method advisor bean
     */
    /*
    @Bean(name = "methodSecurityInterceptor")
    public MethodSecurityInterceptor methodSecurityInterceptor() throws CacheException, IOException {

        MethodSecurityInterceptor interceptor = new MethodSecurityInterceptor();
        interceptor.setAccessDecisionManager(aclAccessDecisionManager());
        interceptor.setAuthenticationManager(aclAuthenticationManager());
        interceptor.setSecurityMetadataSource(delegatingMethodSecurityMetadataSource());
        interceptor.setAfterInvocationManager(afterInvocationProviderManager());
        return interceptor;

    }
    */    
    
    @Bean
    public AnnotationSecurityAspect annotationSecurityAspect() throws CacheException, IOException, Exception{                    
        
        AnnotationSecurityAspect aspect = new AnnotationSecurityAspect();
        aspect.setSecurityInterceptor(aspectJMethodSecurityInterceptor());
        aspect.afterPropertiesSet();
        return aspect;
    
    }    
    
    /*
    @Bean 
    public SomeController someController() throws CacheException, IOException, Exception{
        
        AspectJProxyFactory factory = new AspectJProxyFactory(new SomeController());
        factory.addAspect(annotationSecurityAspect());
        return factory.getProxy();
    }
    */

    /*
     * Advisor that will be used by AOP to enable securing methods using when not using aspectj
     * pre/post
     */
    /*
    @Bean(name = BeanIds.METHOD_SECURITY_METADATA_SOURCE_ADVISOR)
    public MethodSecurityMetadataSourceAdvisor methodSecurityMetadataSourceAdvisor() throws CacheException, IOException {
        MethodSecurityMetadataSourceAdvisor advisor = new MethodSecurityMetadataSourceAdvisor("methodSecurityInterceptor", delegatingMethodSecurityMetadataSource(), "delegatingMethodSecurityMetadataSource");
        return advisor;
    }

    //LAST BIT IS NO ENABLE AOP
    @Bean(name = "org.springframework.aop.config.internalAutoProxyCreator")
    public InfrastructureAdvisorAutoProxyCreator infrastructureAdvisorAutoProxyCreator() {
        return new InfrastructureAdvisorAutoProxyCreator();
    }
    */

    
    
    //~~~~~~~~~~~~~~~~ duplicated authentication providers from WEB Security Config into Root ACL Config ~~~~~~~~~~~~~~~~~~~~~ //
    
    @Bean
    public AnonymousAuthenticationProvider aclAnonymousAuthenticationProvider() {    	
        return new AnonymousAuthenticationProvider(RootSecurityApplicationConfig.ANONYMOUS_TOKEN);
    }

    @Bean
    public AuthenticationProvider aclDaoAuthenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService());
        return p;
    }

    @Bean
    public UserDetailsService userDetailsService() {
    	
    	//RETURN HERE YOUR OWN USER DETAIL SERVICE
        return /*new RestUserDetailsService()*/ null;
    }

    @Bean
    public AuthenticationManager aclAuthenticationManager() {
        List<AuthenticationProvider> providers = new ArrayList<AuthenticationProvider>(2);
        providers.add(aclDaoAuthenticationProvider());
        providers.add(aclAnonymousAuthenticationProvider());

        ProviderManager am = new ProviderManager(providers);
        return am;
    }
}