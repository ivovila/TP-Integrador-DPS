package ar.edu.itba.certiflow.web.security;

import ar.edu.itba.certiflow.infrastructure.security.SpringDataUserAccountRepository;
import ar.edu.itba.certiflow.infrastructure.security.UserAccountEntity;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile({"demo", "demo-users"})
@Order(0)
public class DemoUsersSeeder implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "certiflow";

    private record DemoUser(String personId, String username, String name, StaffRole role) {
    }

    private static final List<DemoUser> DEMO_USERS = List.of(
            new DemoUser("EMP-006", "ana.torres", "Ana Torres", StaffRole.ADMINISTRATOR),
            new DemoUser("EMP-001", "paula.rios", "Paula Rios", StaffRole.INSPECTION_COORDINATOR),
            new DemoUser("EMP-002", "ines.duarte", "Ines Duarte", StaffRole.INSPECTOR),
            new DemoUser("EMP-003", "raul.mendez", "Raul Mendez", StaffRole.ASSET_RESPONSIBLE),
            new DemoUser("EMP-004", "vera.soto", "Vera Soto", StaffRole.VERIFIER),
            new DemoUser("EMP-005", "carlos.vidal", "Carlos Vidal", StaffRole.CERTIFICATION_OFFICER));

    private final SpringDataUserAccountRepository userAccounts;
    private final PasswordEncoder passwordEncoder;

    public DemoUsersSeeder(SpringDataUserAccountRepository userAccounts, PasswordEncoder passwordEncoder) {
        this.userAccounts = userAccounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        DEMO_USERS.stream()
                .filter(user -> !userAccounts.existsById(user.personId()))
                .forEach(user -> userAccounts.save(new UserAccountEntity(user.personId(), user.username(), user.name(), user.role().name(),
                        passwordEncoder.encode(DEMO_PASSWORD))));
    }
}
