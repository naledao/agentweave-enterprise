package com.agentweave.workflow.graph;

import com.agentweave.workflow.state.AgentWorkflowStateSerializer;
import javax.sql.DataSource;
import org.bsc.langgraph4j.checkpoint.PostgresSaver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentWorkflowGraphConfig {

    @Bean
    public PostgresSaver langGraphPostgresSaver(
            DataSource dataSource,
            AgentWorkflowStateSerializer stateSerializer) throws java.sql.SQLException {
        return PostgresSaver.builder()
                .datasource(dataSource)
                .stateSerializer(stateSerializer)
                .createTables(true)
                .dropTablesFirst(false)
                .build();
    }
}
