package com.regulus.platform.agents.annotations;

import com.regulus.platform.agents.autoconfigure.AiAgentsAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AiAgentsAutoConfiguration.class)
public @interface EnableAiAgents {
}
