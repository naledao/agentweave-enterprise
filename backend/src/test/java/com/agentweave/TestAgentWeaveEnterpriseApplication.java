package com.agentweave;

import org.springframework.boot.SpringApplication;

public class TestAgentWeaveEnterpriseApplication {

	public static void main(String[] args) {
		SpringApplication.from(AgentWeaveEnterpriseApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
