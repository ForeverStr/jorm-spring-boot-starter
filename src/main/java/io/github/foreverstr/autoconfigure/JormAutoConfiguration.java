package io.github.foreverstr.autoconfigure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.foreverstr.cache.CacheManager;
import io.github.foreverstr.cache.config.RedisCacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import io.github.foreverstr.session.base.JormSession;
import io.github.foreverstr.session.factory.Jorm;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;
import java.sql.SQLException;
@Configuration
@EnableConfigurationProperties({JormProperties.class, RedisCacheProperties.class})
public class JormAutoConfiguration {
    private final JormProperties properties;
    private final RedisCacheProperties cacheProperties;
    public JormAutoConfiguration(JormProperties properties, RedisCacheProperties cacheProperties) {
        this.properties = properties;
        this.cacheProperties = cacheProperties;
    }
    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getJdbcUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setDriverClassName(properties.getDriverClassName());
        config.setMaximumPoolSize(properties.getMaximumPoolSize());
        config.setMinimumIdle(properties.getMinimumIdle());
        config.setConnectionTimeout(properties.getConnectionTimeout());
        config.setIdleTimeout(properties.getIdleTimeout());
        config.setMaxLifetime(properties.getMaxLifetime());
        HikariDataSource dataSource = new HikariDataSource(config);
        Jorm.setDataSource(dataSource);
        return dataSource;
    }
    @Bean
    public JormSession jormSession(DataSource dataSource) throws SQLException {
        return new JormSession(dataSource.getConnection());
    }
    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    public CacheManager cacheManager(RedisTemplate<String, Object> redisTemplate) {
        CacheManager.init(redisTemplate, cacheProperties);
        return new CacheManager();
    }
}
