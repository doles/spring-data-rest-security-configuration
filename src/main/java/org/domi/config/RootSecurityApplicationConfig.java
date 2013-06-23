package org.domi.config;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import javax.servlet.Filter;
import net.sf.ehcache.CacheException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.authentication.AnonymousAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.access.expression.ExpressionBasedFilterInvocationSecurityMetadataSource;
import org.springframework.security.web.access.expression.WebExpressionVoter;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.RequestCacheAwareFilter;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.springframework.security.web.util.AntPathRequestMatcher;
import org.springframework.security.web.util.RequestMatcher;

@Configuration
@ComponentScan(basePackageClasses = RootSecurityApplicationConfig.class)
public class RootSecurityApplicationConfig {

    
    public static final String ANONYMOUS_TOKEN = "anonumous";

    /**
     * Defines filter chains per url.
     *
     * @return
     */
    @Bean(name = "springSecurityFilterChain")
    public FilterChainProxy portalFilterChainProxy() {

        List<SecurityFilterChain> securityFilterChains = new ArrayList<SecurityFilterChain>();
        //first chain for "/api"
        securityFilterChains.add(apiSecurityFilterChain());

        //second chain for "/app"	
        securityFilterChains.add(appSecurityFilterChain());

        FilterChainProxy filterChainProxy = new FilterChainProxy(securityFilterChains);
        return filterChainProxy;

    }

    /**
     * One chain is per path, you can reuse filter beans to create new url
     * filter chains.
     *
     * @return
     */
    @Bean
    public SecurityFilterChain apiSecurityFilterChain() {

        RequestMatcher requestMatcher = new AntPathRequestMatcher("/api/**");

        List<Filter> filters = new ArrayList<Filter>();

        //always add filters in the order
        //1. SecurityContextPersistenceFilter - mandatory
        filters.add(securityContextPersistenceFilter());
        //2. logout filter - we don't need logout for the moment
        //3. login filter - we don't need login filetr for the moment, as login is performed somewhere else
        //4. request cache
        filters.add(requestCacheAwareFilter());
        //5. servlet-api-provision filter - filter that wraps the request with class that exposes the role security methods  
        filters.add(securityContextHolderAwareRequestFilter());
        //6. our custom auth filter
        filters.add(legacyPortalIn1AuthenticationFilter());
        //7. anonymous filter - not sure if we need one
        filters.add(anonymousAuthenticationFilter());
        //8. session management filter - skipping as it cannot be used with statelsess config
        //9. exception filter - it must be above filter#9, to catch ist exception 
        filters.add(exceptionTranslationFilter());
        //10. filterSecurityInterceptor - last one, most important, it will auth the request
        filters.add(filterSecurityInterceptor());


        return new DefaultSecurityFilterChain(requestMatcher, filters);

    }

    /**
     * One chain is per path, you can reuse filter beans to create new url
     * filter chains.
     *
     * @return
     */
    @Bean
    public SecurityFilterChain appSecurityFilterChain() {

        RequestMatcher requestMatcher = new AntPathRequestMatcher("/app/**");

        List<Filter> filters = new ArrayList<Filter>();

        //always add filters in the order
        //1. SecurityContextPersistenceFilter - mandatory
        filters.add(securityContextPersistenceFilter());
        //2. logout filter - we don't need logout for the moment
        //3. login filter - we don't need login filetr for the moment, as login is performed somewhere else
        //4. request cache
        filters.add(requestCacheAwareFilter());
        //5. servlet-api-provision filter - filter that wraps the request with class that exposes the role security methods  
        filters.add(securityContextHolderAwareRequestFilter());
        //6. our custom auth filter
        filters.add(legacyPortalIn1AuthenticationFilter());
        //7. anonymous filter - not sure if we need one
        filters.add(anonymousAuthenticationFilter());
        //8. session management filter - skipping as it cannot be used with statelsess config
        //9. exception filter - it must be above filter#9, to catch ist exception 
        filters.add(exceptionTranslationFilter());
        //10. filterSecurityInterceptor - last one, most important, it will auth the request
        filters.add(filterSecurityInterceptor());


        return new DefaultSecurityFilterChain(requestMatcher, filters);

    }

    /**
     * This filter is stateless - does not create a session!!
     *
     * @return
     */
    @Bean
    public SecurityContextPersistenceFilter securityContextPersistenceFilter() {

        SecurityContextPersistenceFilter filter = new SecurityContextPersistenceFilter(nullSecurityContextRepository());
        return filter;
    }

    /**
     * Stateless context repository
     */
    @Bean
    public SecurityContextRepository nullSecurityContextRepository() {
        return new NullSecurityContextRepository();
    }

    @Bean
    public RequestCacheAwareFilter requestCacheAwareFilter() {
        return new RequestCacheAwareFilter(nullRequestCache());
    }

    /**
     * Stateless request cache
     *
     * @return
     */
    @Bean
    public RequestCache nullRequestCache() {
        return new NullRequestCache();
    }

    /**
     * Allows to secure urls with roles
     *
     * @return
     */
    @Bean
    public SecurityContextHolderAwareRequestFilter securityContextHolderAwareRequestFilter() {
        SecurityContextHolderAwareRequestFilter f = new SecurityContextHolderAwareRequestFilter();
        f.setRolePrefix("");
        return f;
    }

    /**
     * If there is no authentication it will populate the security context with
     * 'ROLE_ANONYMOUS', using the AnonymousAuthenticationToken, the token is
     * the key.
     *
     * @return
     */
    @Bean
    public AnonymousAuthenticationFilter anonymousAuthenticationFilter() {


        AnonymousAuthenticationFilter f = new AnonymousAuthenticationFilter(ANONYMOUS_TOKEN);
        return f;
    }

    /**
     * If anything goes wrong, or basically if the request cannot be
     * authenticated, this filter catches the error, and redirects to he entry
     * point, which in this case is not login, but just error http response
     *
     * @return
     */
    @Bean
    public ExceptionTranslationFilter exceptionTranslationFilter() {

        ExceptionTranslationFilter f = new ExceptionTranslationFilter(restAuthenticationEntryPoint(), nullRequestCache());
        //f.setAccessDeniedHandler(accessDeniedHandler)
        return f;

    }

    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return new RestAuthenticationEntryPoint();
    }

    /**
     * MOST IMPORTANT - IT WILL TRIGGER AUTHENTICATION PER URL
     */
    @Bean
    public FilterSecurityInterceptor filterSecurityInterceptor() {


        FilterSecurityInterceptor f = new FilterSecurityInterceptor();
        f.setAccessDecisionManager(accessDecisionManager());
        f.setAuthenticationManager(authenticationManager());
        f.setSecurityMetadataSource(filterInvocationSecurityMetadataSource());

        return f;


    }

    /**
     * The access will be determined based on role and if is authenticated
     *
     * @return
     */
    @Bean
    public AccessDecisionManager accessDecisionManager() {

        @SuppressWarnings("rawtypes")
        List<AccessDecisionVoter> voters = new ArrayList<AccessDecisionVoter>(2);
        voters.add(new RoleVoter());
        voters.add(new WebExpressionVoter());
        voters.add(new AuthenticatedVoter());

        AffirmativeBased adm = new AffirmativeBased(voters);

        return adm;

    }

    @Bean
    public AuthenticationManager authenticationManager() {

        List<AuthenticationProvider> providers = new ArrayList<AuthenticationProvider>(2);
        providers.add(daoAuthenticationProvider());
        providers.add(anonymousAuthenticationProvider());

        ProviderManager am = new ProviderManager(providers);
        return am;
    }

    @Bean
    public AnonymousAuthenticationProvider anonymousAuthenticationProvider() {
        return new AnonymousAuthenticationProvider(ANONYMOUS_TOKEN);

    }

    @Bean
    public AuthenticationProvider daoAuthenticationProvider() {

        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService());
        return p;

    }

    @Bean
    public UserDetailsService userDetailsService() {

        return new RestUserDetailsService();
    }

    @Bean
    public FilterInvocationSecurityMetadataSource filterInvocationSecurityMetadataSource() {

        LinkedHashMap<RequestMatcher, Collection<ConfigAttribute>> requestMap = new LinkedHashMap<RequestMatcher, Collection<ConfigAttribute>>();

        /*
         * Equivalent to:
         *
         * <intercept-url pattern="/gocardless/**"
         * access="ROLE_GOCARDLESS_CUSTOMER" /> <intercept-url
         * pattern="/resources/**" access="permitAll" /> <intercept-url
         * pattern="/" access="isAuthenticated()" /> <intercept-url
         * pattern="/**" access="permitAll" />
         */
        List<ConfigAttribute> authenticatedAllowed = SecurityConfig.createList("isAuthenticated()");

        requestMap.put(new AntPathRequestMatcher("/api/**")/* any method*/, authenticatedAllowed);
        requestMap.put(new AntPathRequestMatcher("/app/**")/* any method*/, authenticatedAllowed);
 
        return new ExpressionBasedFilterInvocationSecurityMetadataSource(requestMap,new DefaultWebSecurityExpressionHandler());
        
        //return new RestFilterInvocationSecurityMetadataSource(requestMap);
    }
}
