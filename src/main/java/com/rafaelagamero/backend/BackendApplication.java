package com.rafaelagamero.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class BackendApplication {

	private static final Logger log = LoggerFactory.getLogger(BackendApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	public CommandLineRunner autoMigrateDatabase(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				log.info("Executando migrações automáticas de colunas no PostgreSQL...");
				jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE NOT NULL;");
				jdbcTemplate.execute("UPDATE products SET active = TRUE WHERE active IS NULL;");
				jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS tracking_code VARCHAR(50);");
				jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_cost NUMERIC(10,2) DEFAULT 0.00;");
				jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_method VARCHAR(100);");
				log.info("Migrações automáticas concluídas com sucesso!");
			} catch (Exception e) {
				log.warn("Aviso na migração automática de banco: {}", e.getMessage());
			}
		};
	}

}
