package org.kalaider.transact.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "org.kalaider.transact")
@Validated
@Data
public class AppProperties {
    public enum Isolation {
        SERIALIZABLE,
        READ_COMMITTED,
        READ_UNCOMMITTED
    }
    public enum Effects {
        DIRTY_READS,
        REPEATABLE_READS
    }
    private Isolation isolation;
    private Effects desiredEffects;
}
