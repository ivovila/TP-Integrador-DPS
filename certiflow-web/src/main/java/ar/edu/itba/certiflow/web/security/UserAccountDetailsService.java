package ar.edu.itba.certiflow.web.security;

import ar.edu.itba.certiflow.infrastructure.security.SpringDataUserAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class UserAccountDetailsService implements UserDetailsService {

    private final SpringDataUserAccountRepository userAccounts;

    public UserAccountDetailsService(SpringDataUserAccountRepository userAccounts) {
        this.userAccounts = userAccounts;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userAccounts.findByUsername(username)
                .map(account -> User.withUsername(account.username()).password(account.passwordHash()).roles(account.role()).build())
                .orElseThrow(() -> new UsernameNotFoundException("No user account for " + username));
    }
}
