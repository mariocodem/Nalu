package co.nalu;

import org.springframework.boot.SpringApplication;

public class TestNaluApplication {

	public static void main(String[] args) {
		SpringApplication.from(NaluApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
