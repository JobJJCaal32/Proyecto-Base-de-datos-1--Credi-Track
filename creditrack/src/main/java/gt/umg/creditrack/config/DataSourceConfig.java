package gt.umg.creditrack.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import gt.umg.creditrack.comun.bd.ContextoSesionDataSource;

@Configuration(proxyBeanMethods = false)
public class DataSourceConfig {

    @Bean
    static BeanPostProcessor contextoSesionDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource dataSource && !(bean instanceof ContextoSesionDataSource)) {
                    return new ContextoSesionDataSource(dataSource);
                }
                return bean;
            }
        };
    }
}
