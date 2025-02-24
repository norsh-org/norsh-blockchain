package org.norsh.blockchain.config;

import org.norsh.util.Logger;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Auto-configuration for system logging.
 * <p>
 * This configuration ensures that logging is initialized before the web dispatcher, allowing early-stage logging during startup.
 * </p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
@AutoConfiguration(before = { DispatcherServletAutoConfiguration.class })
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class LogAutoConfiguration {

    /**
     * Provides the main logging instance.
     * <p>
     * This bean initializes the logging system with the current API configuration settings.
     * </p>
     *
     * @return an instance of {@link Logger}.
     */
    @Bean
    public Logger log() {
        return new Logger(BlockchainConfig.getInstance().getLogConfig());
    }
}
